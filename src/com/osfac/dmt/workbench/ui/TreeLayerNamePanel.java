package com.osfac.dmt.workbench.ui;

import com.osfac.dmt.util.CollectionUtil;
import com.osfac.dmt.util.LangUtil;
import com.osfac.dmt.workbench.model.Category;
import com.osfac.dmt.workbench.model.CategoryEvent;
import com.osfac.dmt.workbench.model.CategoryEventType;
import com.osfac.dmt.workbench.model.FeatureEvent;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.model.LayerEvent;
import com.osfac.dmt.workbench.model.LayerEventType;
import com.osfac.dmt.workbench.model.LayerListener;
import com.osfac.dmt.workbench.model.LayerManager;
import com.osfac.dmt.workbench.model.LayerManagerProxy;
import com.osfac.dmt.workbench.model.LayerTreeModel;
import com.osfac.dmt.workbench.model.Layerable;
import com.osfac.dmt.workbench.model.WMSLayer;
import com.osfac.dmt.workbench.ui.renderer.RenderingManager;
import com.osfac.dmt.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class TreeLayerNamePanel extends JPanel
        implements
        LayerListener,
        LayerNamePanel,
        LayerNamePanelProxy,
        PopupNodeProxy {

    private Map nodeClassToPopupMenuMap = new HashMap();
    BorderLayout borderLayout1 = new BorderLayout();
    JTree tree = new JTree() {
        public boolean isPathEditable(TreePath path) {
            if (!isEditable()) {
                return false;
            }

            return path.getLastPathComponent() instanceof Layerable
                    || path.getLastPathComponent() instanceof Category;
        }

        // Workaround for Java Bug 4199956 "JTree shows container can be
        // expanded - even when empty", posted by bertrand.allo in the Java Bug
        // Database. [Bob Boseko]
        public boolean hasBeenExpanded(TreePath path) {
            return super.hasBeenExpanded(path)
                    || !this.getModel().isLeaf(path.getLastPathComponent());
        }
        // Added by Michael Michaud on 2008-08-10
        // This adds the number of features in tooltip
        // And removed on 2008-11-01
        // The ToolTipText content is better managed in LayerNameRenderer class
    };
    private LayerTreeCellRenderer layerTreeCellRenderer;
    private TreeCellEditor cellEditor = new LayerTreeCellEditor(tree);
    private Object popupNode;
    private ArrayList listeners = new ArrayList();
    private LayerManagerProxy layerManagerProxy;
    JScrollPane scrollPane = new JScrollPane();
    private FirableTreeModelWrapper firableTreeModelWrapper;
    // used to drag Layerables among Categories
    private TreePath movingTreePath = null;
    private boolean firstTimeDragging = true;
    private int lastHoveringRow = -1;

    /**
     */
    public TreeLayerNamePanel(LayerManagerProxy layerManagerProxy,
            TreeModel treeModel, RenderingManager renderingManager,
            Map additionalNodeClassToTreeCellRendererMap) {
        layerManagerProxy.getLayerManager().addLayerListener(this);
        this.layerManagerProxy = layerManagerProxy;

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        firableTreeModelWrapper = new FirableTreeModelWrapper(treeModel);
        tree.setModel(firableTreeModelWrapper);
        layerTreeCellRenderer = new LayerTreeCellRenderer(renderingManager);
        renderingManager.getPanel().getViewport().addListener(
                new ViewportListener() {
                    public void zoomChanged(Envelope modelEnvelope) {
                        // After a zoom, the scale may be outside the visible
                        // scale range for one or more layers, in which case we
                        // want to update the layer names to be grey. So
                        // repaint. [Bob Boseko 2005-03-10]
                        TreeLayerNamePanel.this.repaint();
                    }
                });
        setCellRenderer(additionalNodeClassToTreeCellRendererMap);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        tree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                handleCheckBoxClick(e);
            }

            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    movingTreePath = tree.getPathForLocation(e.getX(), e.getY());
                    // move only Layerables, not Categories
                    if (movingTreePath != null
                            && !(movingTreePath.getLastPathComponent() instanceof Layerable)) {
                        movingTreePath = null;
                    } else if (movingTreePath != null
                            && !tree.isRowSelected(tree.getClosestRowForLocation(e.getX(), e.getY()))) {
                        movingTreePath = null;
                    } else {
                        lastHoveringRow = tree.getClosestRowForLocation(e.getX(), e.getY());
                    }
                } else {
                    movingTreePath = null;
                }
            }

            public void mouseReleased(MouseEvent e) {

                if (e.getButton() != MouseEvent.BUTTON1 || movingTreePath == null) {
                    return;
                }

                Object node = movingTreePath.getLastPathComponent();
                TreePath tpDestination = tree.getClosestPathForLocation(e
                        .getX(), e.getY());

                // Fix: When dragging a Layerable onto a Category, and then
                // selecting a different layer, the last XOR placement of the
                // dragbar would appear over the Category. Need to reset
                // firstTimeDragging to true before returning.
                //movingTreePath = null;
                firstTimeDragging = true;
                lastHoveringRow = -1;

                if (tpDestination == null) {
                    return;
                }

                // remove remnants of horizontal drag bar by refreshing display
                tree.repaint();
                // Changed #update to #repaint -- less flickery for some reason
                // [Bob Boseko 2004-03-17]

                // dragging a layerable
                if (node instanceof Layerable) {
                    Layerable layerable = (Layerable) node;
                    int index = 0;
                    Category cat = null;

                    int oldRow = tree.getRowForPath(movingTreePath);
                    int newRow = tree.getRowForPath(tpDestination);
                    Category oldCat = (Category) movingTreePath.getParentPath().getLastPathComponent();

                    if (tpDestination.getLastPathComponent() instanceof Layerable) {

                        // Fix: When shift-clicking to select a range of nodes,
                        // last node would unselect because the layer would get
                        // removed then re-added. [Bob Boseko 2004-03-11]
                        if (layerable == tpDestination.getLastPathComponent()) {
                            return;
                        }

                        cat = getLayerManager().getCategory(
                                (Layerable) tpDestination.getLastPathComponent());

                        index = tree.getModel().getIndexOfChild(
                                tpDestination.getParentPath().getLastPathComponent(),
                                tpDestination.getLastPathComponent());
                        // adjust where the Layer will be drop exactly
                        if (newRow < oldRow && cat.equals(oldCat)) {
                            index++;
                        } else if (!cat.equals(oldCat)) {
                            index++;
                        }
                    } else if (tpDestination.getLastPathComponent() instanceof Category) {
                        cat = (Category) tpDestination.getLastPathComponent();
                        // Prevent unnecessary removals and re-additions
                        // [Bob Boseko 2004-03-11]
                        // if (cat.contains(layerable)) {
                        //return;
                        // }
                    } else {
                        // Can get here if the node is, for example,
                        // a LayerTreeModel.ColorThemingValue [Bob Boseko 2005-07-25]
                        return;
                    }
                    getLayerManager().remove(layerable);
                    cat.add(index, layerable);
                    getLayerManager().fireLayerChanged(layerable, LayerEventType.METADATA_CHANGED);
                    movingTreePath = null;
                }
            }
        });

        tree.addMouseMotionListener(new MouseMotionAdapter() {
            int rowNew;
            Rectangle dragBar;

            public void mouseDragged(MouseEvent e) {
                // return if mouse is dragged while not originating on a tree node
                if (movingTreePath == null) {
                    firstTimeDragging = true;
                    return;
                }

                int rowOld = tree.getRowForPath(movingTreePath);
                rowNew = tree.getClosestRowForLocation(e.getX(), e.getY());
                //rowOld = tree.getRowForPath(movingTreePath);
                // if the dragging of a row hasn't moved outside of the bounds
                // of the currently selected row, don't show the horizontal drag
                // bar.
                if (rowNew == lastHoveringRow
                        || rowNew == rowOld - 1 || rowNew == rowOld) {
                    return;
                }

                if (!(tree.getPathForRow(rowNew).getLastPathComponent() instanceof Layer)) {
                    tree.expandRow(rowNew);
                }

                Graphics2D g2 = (Graphics2D) tree.getGraphics();
                g2.setColor(Color.RED);
                g2.setXORMode(Color.WHITE);
                // if this is the first time moving the dragbar, draw the
                // dragbar so XOR drawing works properly
                if (firstTimeDragging) {
                    dragBar = new Rectangle(0, 0, tree.getWidth(), 3);
                    dragBar.setLocation(0, tree.getRowBounds(lastHoveringRow).y + tree.getRowBounds(lastHoveringRow).height - 3);
                    g2.fill(dragBar);
                    firstTimeDragging = false;
                }
                // XOR drawing mode of horizontal drag bar
                g2.fill(dragBar);
                dragBar.setLocation(0, tree.getRowBounds(rowNew).y + tree.getRowBounds(rowNew).height - 3);
                g2.fill(dragBar);
                lastHoveringRow = rowNew;
            }
        });
        tree.setCellEditor(cellEditor);
        tree.setInvokesStopCellEditing(true);
        tree.setBackground(getBackground());
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                fireLayerSelectionChanged();
            }
        });
        tree.getModel().addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent e) {
            }

            public void treeNodesInserted(TreeModelEvent e) {
                for (int i = 0; i < e.getChildren().length; i++) {
                    TreeUtil.visit(tree.getModel(), e.getTreePath()
                            .pathByAddingChild(e.getChildren()[i]),
                            new TreeUtil.Visitor() {
                                public void visit(Stack path) {
                                    // When opening a task file, don't expand the ColorThemingValues. 
                                    // [Bob Boseko 2005-08-01]
                                    if (path.peek() instanceof LayerTreeModel.ColorThemingValue) {
                                        return;
                                    }
                                    tree.makeVisible(new TreePath(path
                                            .toArray()));
                                }
                            });
                }
            }

            public void treeNodesRemoved(TreeModelEvent e) {
            }

            public void treeStructureChanged(TreeModelEvent e) {
            }
        });
        TreeUtil.expandAll(tree, new TreePath(tree.getModel().getRoot()));
    }

    public void addPopupMenu(Class nodeClass, JPopupMenu popupMenu) {
        nodeClassToPopupMenuMap.put(nodeClass, popupMenu);
    }

    private void setCellRenderer(Map additionalNodeClassToTreeCellRendererMap) {
        final Map map = createNodeClassToTreeCellRendererMap();
        map.putAll(additionalNodeClassToTreeCellRendererMap);
        tree.setCellRenderer(new TreeCellRenderer() {
            private DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer() {
                {
                    // Transparent. [Bob Boseko]
                    setBackgroundNonSelectionColor(new Color(0, 0, 0, 0));
                }
            };

            public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean selected, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                return ((TreeCellRenderer) LangUtil.ifNull(CollectionUtil.get(
                        value.getClass(), map), defaultRenderer))
                        .getTreeCellRendererComponent(tree, value, selected,
                        expanded, leaf, row, hasFocus);
            }
        });
    }

    private Map createNodeClassToTreeCellRendererMap() {
        HashMap map = new HashMap();
        map.put(Layer.class, layerTreeCellRenderer);
        map.put(WMSLayer.class, layerTreeCellRenderer);
        map.put(Category.class, layerTreeCellRenderer);
        map.put(LayerTreeModel.ColorThemingValue.class, createColorThemingValueRenderer());
        return map;
    }

    private TreeCellRenderer createColorThemingValueRenderer() {
        return new TreeCellRenderer() {
            private JPanel panel = new JPanel(new GridBagLayout());
            private ColorPanel colorPanel = new ColorPanel();
            private JLabel label = new JLabel();

            {
                panel.add(colorPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                panel.add(label, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
            }

            public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean selected, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                label.setText(((LayerTreeModel.ColorThemingValue) value)
                        .toString());
                BasicStyle style = ((LayerTreeModel.ColorThemingValue) value)
                        .getStyle();
                colorPanel.setLineColor(style.isRenderingLine()
                        ? GUIUtil.alphaColor(style.getLineColor(), style
                        .getAlpha())
                        : GUIUtil.alphaColor(Color.BLACK, 0));
                colorPanel.setFillColor(style.isRenderingFill()
                        ? GUIUtil.alphaColor(style.getFillColor(), style
                        .getAlpha())
                        : GUIUtil.alphaColor(Color.BLACK, 0));
                return panel;
            }
        };
    }

    void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                tree_mouseReleased(e);
            }
        });
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setEditable(true);
        tree.setRootVisible(false);
        // Row height is set to -1 because otherwise, in Java 1.4, tree nodes
        // will be "chopped off" at the bottom [Bob Boseko]
        tree.setRowHeight(-1);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        tree.setShowsRootHandles(true);
        scrollPane
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.getViewport().add(tree);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    void tree_mouseReleased(MouseEvent e) {
        if (!SwingUtilities.isRightMouseButton(e)) {
            return;
        }

        TreePath popupPath = tree.getPathForLocation(e.getX(), e.getY());

        if (popupPath == null) {
            return;
        }

        popupNode = popupPath.getLastPathComponent();

        // #isAltDown returns true on a middle-click; #isMetaDown returns true
        // on a right-click[Bob Boseko]
        // Third check can't simply user JTree#isPathSelected because the node
        // wrappers are value objects and thus can't reliably be compared by
        // reference (which is what #isPathSelected seems to do). [Bob Boseko]
        if (!(e.isControlDown() || e.isShiftDown() || selectedNodes(
                Object.class).contains(popupNode))) {
            tree.getSelectionModel().clearSelection();
        }

        tree.getSelectionModel().addSelectionPath(popupPath);

        if (getPopupMenu(popupNode.getClass()) != null) {
            getPopupMenu(popupNode.getClass()).show(e.getComponent(), e.getX(),
                    e.getY());
        }
    }

    private JPopupMenu getPopupMenu(Class nodeClass) {
        return (JPopupMenu) CollectionUtil.get(nodeClass,
                nodeClassToPopupMenuMap);
    }

    private void handleCheckBoxClick(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        TreePath path = tree.getPathForLocation(e.getX(), e.getY());

        if (path == null) {
            return;
        }

        Object node = path.getLastPathComponent();

        if (!(node instanceof Layerable)) {
            return;
        }

        Layerable layerable = (Layerable) node;
        Point layerNodeLocation = tree.getUI().getPathBounds(tree, path)
                .getLocation();

        // Initialize the LayerNameRenderer with the current node.
        // checkBoxBounds will be different for Layers and WMSLayers. [Jon
        // Aquino]
        layerTreeCellRenderer.getLayerNameRenderer()
                .getTreeCellRendererComponent(tree,
                path.getLastPathComponent(), false, false, false, 0,
                false);

        Rectangle checkBoxBounds = layerTreeCellRenderer.getLayerNameRenderer()
                .getCheckBoxBounds();
        checkBoxBounds.translate((int) layerNodeLocation.getX(),
                (int) layerNodeLocation.getY());

        if (checkBoxBounds.contains(e.getPoint())) {
            layerable.setVisible(!layerable.isVisible());
        }
    }

    public Layer[] getSelectedLayers() {
        return selectedLayers(this);
    }

    public static Layer[] selectedLayers(LayerNamePanel layerNamePanel) {
        return (Layer[]) layerNamePanel.selectedNodes(Layer.class).toArray(
                new Layer[]{});
    }

    public Collection getSelectedCategories() {
        return selectedNodes(Category.class);
    }

    public Collection selectedNodes(Class c) {
        return selectedNodes(c, tree);
    }

    public static Collection selectedNodes(Class c, JTree tree) {
        ArrayList selectedNodes = new ArrayList();
        TreePath[] selectionPaths = tree.getSelectionPaths();

        if (selectionPaths == null) {
            return new ArrayList();
        }

        for (int i = 0; i < selectionPaths.length; i++) {
            Object node = selectionPaths[i].getLastPathComponent();

            if (c.isInstance(node)) {
                selectedNodes.add(node);
            }
        }

        return selectedNodes;
    }

    public void setSelectedLayers(Layer[] layers) {
        tree.getSelectionModel().clearSelection();

        for (int i = 0; i < layers.length; i++) {
            addSelectedLayer(layers[i]);
        }
    }

    protected void addSelectedLayer(Layer layer) {
        tree.addSelectionPath(TreeUtil.findTreePath(layer, tree.getModel()));
    }

    public void layerChanged(final LayerEvent e) {
        TreeModelEvent treeModelEvent = new TreeModelEvent(this, new Object[]{
                    tree.getModel().getRoot(), e.getCategory()}, new int[]{e
                    .getLayerableIndex()}, new Object[]{e.getLayerable()});

        if (e.getType() == LayerEventType.ADDED) {
            firableTreeModelWrapper.fireTreeNodesInserted(treeModelEvent);
            // firing TreeStructureChanged solve the problem ID 3418067
            // But I did not find yet the true origin of the bug which is
            // probably somewhere else.
            // The strange thing is that the bug appears only in the cloned
            // windows, not in the original one [mmichaud 2011-10-16]
            firableTreeModelWrapper.fireTreeStructureChanged(treeModelEvent);
            if ((selectedNodes(Layerable.class)).size() == 0
                    && e.getLayerable() instanceof Layer) {
                addSelectedLayer((Layer) e.getLayerable());
            }

            return;
        }

        if (e.getType() == LayerEventType.REMOVED) {
            firableTreeModelWrapper.fireTreeNodesRemoved(treeModelEvent);

            return;
        }

        if (e.getType() == LayerEventType.APPEARANCE_CHANGED) {
            // For some reason, if we don't use #invokeLater to call #fireTreeStructureChanged,
            // blank lines get inserted into the JTree. For more information, see Java Bug 4498762,
            // "When expandPath() is called by a JTree method, extra blank lines appear",
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4498762
            // [Bob Boseko 2005-07-25]
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Specify the path of the subtree rooted at the layer -- the ColorThemingValues
                    // may have changed. [Bob Boseko 2005-07-25]
                    firableTreeModelWrapper.fireTreeStructureChanged(new TreeModelEvent(
                            this, new Object[]{tree.getModel().getRoot(),
                                e.getCategory(), e.getLayerable()}));
                }
            });
            return;
        }

        if (e.getType() == LayerEventType.METADATA_CHANGED) {
            firableTreeModelWrapper.fireTreeNodesChanged(treeModelEvent);

            return;
        }

        if (e.getType() == LayerEventType.VISIBILITY_CHANGED) {
            firableTreeModelWrapper.fireTreeNodesChanged(treeModelEvent);

            return;
        }

        Assert.shouldNeverReachHere();
    }

    public void categoryChanged(CategoryEvent e) {
        TreeModelEvent treeModelEvent = new TreeModelEvent(this,
                new Object[]{tree.getModel().getRoot()}, new int[]{e
                    .getCategoryIndex()
                    + indexOfFirstCategoryInTree()}, new Object[]{e
                    .getCategory()});

        if (e.getType() == CategoryEventType.ADDED) {
            firableTreeModelWrapper.fireTreeNodesInserted(treeModelEvent);

            return;
        }

        if (e.getType() == CategoryEventType.REMOVED) {
            firableTreeModelWrapper.fireTreeNodesRemoved(treeModelEvent);

            return;
        }

        if (e.getType() == CategoryEventType.METADATA_CHANGED) {
            firableTreeModelWrapper.fireTreeNodesChanged(treeModelEvent);

            return;
        }

        Assert.shouldNeverReachHere();
    }

    private int indexOfFirstCategoryInTree() {
        // Not 0 in ESE. [Bob Boseko]
        for (int i = 0; i < tree.getModel().getChildCount(
                tree.getModel().getRoot()); i++) {
            if (tree.getModel().getChild(tree.getModel().getRoot(), i) instanceof Category) {
                return i;
            }
        }

        Assert.shouldNeverReachHere();

        return -1;
    }

    public void featuresChanged(FeatureEvent e) {
    }

    public void dispose() {
        // Layer events could still be fired after the TaskWindow containing
        // this LayerNamePanel is closed (e.g. by clones of the TaskWindow, or
        // by an attribute viewer). [Bob Boseko]
        layerManagerProxy.getLayerManager().removeLayerListener(this);
    }

    public JTree getTree() {
        return tree;
    }

    public void addListener(LayerNamePanelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LayerNamePanelListener listener) {
        listeners.remove(listener);
    }

    public void fireLayerSelectionChanged() {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            LayerNamePanelListener l = (LayerNamePanelListener) i.next();
            l.layerSelectionChanged();
        }
    }

    public LayerManager getLayerManager() {
        return layerManagerProxy.getLayerManager();
    }

    public static Layer chooseEditableLayer(LayerNamePanel panel) {
        for (Iterator i = Arrays.asList(panel.getSelectedLayers()).iterator(); i
                .hasNext();) {
            Layer layer = (Layer) i.next();

            if (layer.isEditable()) {
                return layer;
            }
        }

        if (panel.getLayerManager().getEditableLayers().isEmpty()) {
            return null;
        }

        return (Layer) panel.getLayerManager().getEditableLayers().iterator()
                .next();
    }

    public Layer chooseEditableLayer() {
        return chooseEditableLayer(this);
    }

    public LayerNamePanel getLayerNamePanel() {
        return this;
    }

    protected FirableTreeModelWrapper getFirableTreeModelWrapper() {
        return firableTreeModelWrapper;
    }

    public Object getPopupNode() {
        return popupNode;
    }

    protected LayerTreeCellRenderer getLayerTreeCellRenderer() {
        return layerTreeCellRenderer;
    }
}