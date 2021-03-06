package com.osfac.dmt.workbench.ui;

import com.vividsolutions.jts.geom.*;
import com.osfac.dmt.I18N;
import com.osfac.dmt.feature.Feature;
import com.osfac.dmt.util.FlexibleDateParser;
import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.model.CategoryEvent;
import com.osfac.dmt.workbench.model.FeatureEvent;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.model.LayerEvent;
import com.osfac.dmt.workbench.model.LayerEventType;
import com.osfac.dmt.workbench.model.LayerListener;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.ui.images.IconLoader;
import com.osfac.dmt.workbench.ui.plugin.EditSelectedFeaturePlugIn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * Implements an AttributeTable panel. Table-size changes are absorbed by the
 * last column. Rows are striped for non-editable table.
 */
public class AttributeTablePanel extends JPanel {

    /**
     * The property name of the columns width map in the project file (resides
     * in the data-source subtree).
     */
    public static final String ATTRIBUTE_COLUMNS_WIDTH_MAP = "AttributeColumnsWidthMap";

    public static interface FeatureEditor {

        void edit(PlugInContext context, Feature feature, Layer layer)
                throws Exception;
    }
    private FeatureEditor featureEditor = new FeatureEditor() {
        public void edit(PlugInContext context, Feature feature, final Layer myLayer)
                throws Exception {
            new EditSelectedFeaturePlugIn() {
                @Override
                protected Layer layer(PlugInContext context) {
                    //Hopefully nobody will ever delete or rename the
                    // superclass' #layer method.
                    //[Bob Boseko]
                    return myLayer;
                    //Name "myLayer" because we don't want the
                    //superclass' "layer" [Bob Boseko 2004-03-17]
                }
            }.execute(context, feature, myLayer.isEditable());
        }
    };
    private GridBagLayout gridBagLayout1 = new GridBagLayout();

    private class MyTable extends JTable {

        public MyTable() {
            //We want table-size changes to be absorbed by the last column.
            //By default, AUTO_RESIZE_LAST_COLUMN will not achieve this
            //(it works for column-size changes only). But I am overriding
            //#sizeColumnsToFit (for J2SE 1.3) and
            //JTableHeader#getResizingColumn (for J2SE 1.4)
            //#so that it will work for table-size changes. [Bob Boseko]
            setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            GUIUtil.doNotRoundDoubles(this);
            setDefaultEditor(Date.class, new FlexibleDateParser.CellEditor());
        }
        //Row-stripe colour recommended in
        //Java Look and Feel Design Guidelines: Advanced Topics [Bob Boseko]
        private final Color LIGHT_GRAY = new Color(230, 230, 230);
        private GeometryCellRenderer geomCellRenderer = new GeometryCellRenderer();

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (!isEditButtonColumn(column)) {
                JComponent renderer = (JComponent) super.getCellRenderer(row,
                        column);
                if (AttributeTablePanel.this.getModel().getLayer().isEditable()
                        && !AttributeTablePanel.this.getModel()
                        .isCellEditable(row, column)) // Shade readonly cells light gray
                {
                    renderer.setBackground(LIGHT_GRAY);
                } else {
                    // If not editable, use row striping, as recommended in
                    // Java Look and Feel Design Guidelines: Advanced Topics
                    // [Bob Boseko]
                    renderer.setBackground((AttributeTablePanel.this.getModel()
                            .getLayer().isEditable() || ((row % 2) == 0)) ? Color.white
                            : LIGHT_GRAY);
                }
                return (TableCellRenderer) renderer;
            }
            return geomCellRenderer;
        }
    };

    private class GeometryCellRenderer implements TableCellRenderer {

        private JButton button = new JButton(IconLoader.icon("Pencil.gif"));
        private JButton buttonPoint = new JButton(IconLoader.icon("EditPoint.gif"));
        private JButton buttonMultiPoint = new JButton(IconLoader.icon("EditMultiPoint.gif"));
        private JButton buttonLineString = new JButton(IconLoader.icon("EditLineString.gif"));
        private JButton buttonMultiLineString = new JButton(IconLoader.icon("EditMultiLineString.gif"));
        private JButton buttonPolygon = new JButton(IconLoader.icon("EditPolygon.gif"));
        private JButton buttonMultiPolygon = new JButton(IconLoader.icon("EditMultiPolygon.gif"));
        private JButton buttonGC = new JButton(IconLoader.icon("EditGeometryCollection.gif"));
        private JButton buttonEmptyGC = new JButton(IconLoader.icon("EditEmptyGC.gif"));

        GeometryCellRenderer() {
            buttonPoint.setToolTipText("View/Edit Point");
            buttonMultiPoint.setToolTipText("View/Edit MultiPoint");
            buttonLineString.setToolTipText("View/Edit LineString");
            buttonMultiLineString.setToolTipText("View/Edit MultiLineString");
            buttonPolygon.setToolTipText("View/Edit Polygon");
            buttonMultiPolygon.setToolTipText("View/Edit MultiPolygon");
            buttonGC.setToolTipText("View/Edit GeometryCollection");
            buttonEmptyGC.setToolTipText("View/Edit empty GeometryCollection");
            button.setToolTipText("View/Edit Geometry");
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Feature f = (Feature) value;
            Geometry g = f.getGeometry();
            if (g instanceof com.vividsolutions.jts.geom.Point) {
                return buttonPoint;
            }
            if (g instanceof com.vividsolutions.jts.geom.MultiPoint) {
                return buttonMultiPoint;
            }
            if (g instanceof com.vividsolutions.jts.geom.LineString) {
                return buttonLineString;
            }
            if (g instanceof com.vividsolutions.jts.geom.MultiLineString) {
                return buttonMultiLineString;
            }
            if (g instanceof com.vividsolutions.jts.geom.Polygon) {
                return buttonPolygon;
            }
            if (g instanceof com.vividsolutions.jts.geom.MultiPolygon) {
                return buttonMultiPolygon;
            }
            if (g.isEmpty()) {
                return buttonEmptyGC;
            }
            return buttonGC;
        }
    }
    private boolean columnWidthsInitialized = false;
    private MyTable table = new MyTable();
    private TableCellRenderer headerRenderer = new TableCellRenderer() {
        private Icon clearIcon = IconLoader.icon("Clear.gif");
        private Icon downIcon = IconLoader.icon("Down.gif");
        private TableCellRenderer originalRenderer = table.getTableHeader()
                .getDefaultRenderer();
        private Icon upIcon = IconLoader.icon("Up.gif");

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            JLabel label = (JLabel) originalRenderer
                    .getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            if ((getModel().getSortedColumnName() == null)
                    || !getModel().getSortedColumnName().equals(
                    table.getColumnName(column))) {
                label.setIcon(clearIcon);
            } else if (getModel().isSortAscending()) {
                label.setIcon(upIcon);
            } else {
                label.setIcon(downIcon);
            }
            label.setHorizontalTextPosition(SwingConstants.LEFT);
            return label;
        }
    };
    private LayerNameRenderer layerNameRenderer = new LayerNameRenderer();
    private ArrayList listeners = new ArrayList();
    private WorkbenchContext workbenchContext;

    public AttributeTablePanel(final LayerTableModel model, boolean addScrollPane,
            final WorkbenchContext workbenchContext) {
        this();
        if (addScrollPane) {
            remove(table);
            remove(table.getTableHeader());
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.getViewport().add(table);
            this.add(scrollPane, new GridBagConstraints(0, 2, 1, 1, 1, 1,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
                    0, 0, 0, 0), 0, 0));
        }
        updateGrid(model.getLayer());
        model.getLayer().getLayerManager().addLayerListener(
                new LayerListener() {
                    public void categoryChanged(CategoryEvent e) {
                    }

                    public void featuresChanged(FeatureEvent e) {
                    }

                    public void layerChanged(LayerEvent e) {
                        if (e.getLayerable() != model.getLayer()) {
                            return;
                        }
                        if (e.getType() == LayerEventType.METADATA_CHANGED) {
                            //If layer becomes editable, apply row striping
                            // and remove gridlines,
                            //as recommended in Java Look and Feel Design
                            // Guidelines: Advanced Topics [Bob Boseko]
                            updateGrid(model.getLayer());
                            repaint();
                        }
                    }
                });
        try {
            JList list = new JList();
            list.setBackground(new JLabel().getBackground());
            layerNameRenderer.getListCellRendererComponent(list, model
                    .getLayer(), -1, false, false);
            table.setModel(model);
            model.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                        //Structure changed (LayerTableModel specifies
                        // HEADER_ROW).
                        //Add this listener after the table adds its listeners
                        //(in table.setModel above) so that this listener will
                        // initialize the column
                        //widths after the table re-adds the columns. [Jon
                        // Aquino]
                        initColumnWidths();
                    }
                }
            });
            layerNameRenderer.getLabel().setFont(
                    layerNameRenderer.getLabel().getFont()
                    .deriveFont(Font.BOLD));
            model.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    updateLabel();
                }
            });
            updateLabel();
            this.workbenchContext = workbenchContext;
            table.setSelectionModel(new SelectionModelWrapper(this));
            table.getTableHeader().setDefaultRenderer(headerRenderer);
            initColumnWidths();
            setToolTips();
            setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0,
                    new FeatureInfoWriter().sidebarColor(model.getLayer())));
            table.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int column = table.columnAtPoint(e.getPoint());
                        if (column < 0) {
                            return;
                        }
                        if (isEditButtonColumn(column)) {
                            return;
                        }
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            model.sort(table.getColumnName(column));
                        }
                    } catch (Throwable t) {
                        workbenchContext.getErrorHandler().handleThrowable(t);
                    }
                }
            });
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int column = table.columnAtPoint(e.getPoint());
                        int row = table.rowAtPoint(e.getPoint());
                        if (isEditButtonColumn(column)) {
                            PlugInContext context = new PlugInContext(
                                    workbenchContext, null, model.getLayer(),
                                    null, null);
                            model.getLayer().getLayerManager()
                                    .getUndoableEditReceiver().startReceiving();
                            try {
                                featureEditor.edit(context, model
                                        .getFeature(row), model.getLayer());
                            } finally {
                                model.getLayer().getLayerManager()
                                        .getUndoableEditReceiver()
                                        .stopReceiving();
                            }
                            return;
                        }
                    } catch (Throwable t) {
                        workbenchContext.getErrorHandler().handleThrowable(t);
                    }
                }
            });
        } catch (Throwable t) {
            workbenchContext.getErrorHandler().handleThrowable(t);
        }
    }

    private AttributeTablePanel() {
        try {
            jbInit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updateGrid(Layer layer) {
        table.setShowGrid(layer.isEditable());
    }

    private boolean isEditButtonColumn(int column) {
        return getModel().getColumnName(0).equals(table.getColumnName(column));
    }

    private void updateLabel() {//[sstein] change for translation
        if (getModel().getRowCount() == 1) {
            layerNameRenderer.getLabel().setText(
                    getModel().getLayer().getName() + " ("
                    + getModel().getRowCount() + " "
                    + I18N.get("ui.AttributeTablePanel.feature") + ")");
        } else {
            layerNameRenderer.getLabel().setText(
                    getModel().getLayer().getName() + " ("
                    + getModel().getRowCount() + " "
                    + I18N.get("ui.AttributeTablePanel.features") + ")");
        }
    }

    public LayerTableModel getModel() {
        return (LayerTableModel) table.getModel();
    }

    public JTable getTable() {
        return table;
    }

    public void addListener(AttributeTablePanelListener listener) {
        listeners.add(listener);
    }

    void jbInit() throws Exception {
        this.setLayout(gridBagLayout1);
        this.add(layerNameRenderer, new GridBagConstraints(0, 0, 2, 1, 1.0,
                0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        this.add(table.getTableHeader(), new GridBagConstraints(0, 1, 1, 1, 0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(table, new GridBagConstraints(0, 2, 1, 1, 0, 0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
                0, 0, 0, 0), 0, 0));
    }

    private void initColumnWidths() {
        GUIUtil.chooseGoodColumnWidths(table);
        int editButtonWidth = 16;
        table.getColumnModel().getColumn(0).setMinWidth(editButtonWidth);
        table.getColumnModel().getColumn(0).setMaxWidth(editButtonWidth);
        table.getColumnModel().getColumn(0).setPreferredWidth(editButtonWidth);

        // check if we have previoisly saved columns witdh
        Layer layer = workbenchContext.getTask().getLayerManager().getLayer(getModel().getLayer().getName());
        if (layer.getDataSourceQuery() == null) {
            return;
        }
        HashMap columnsWithMap = (HashMap) layer.getDataSourceQuery().getDataSource().getProperties().get(ATTRIBUTE_COLUMNS_WIDTH_MAP);
        if (columnsWithMap != null) {
            for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
                Integer columnWidth = (Integer) columnsWithMap.get(table.getColumnModel().getColumn(i).getHeaderValue());
                if (columnWidth != null) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(columnWidth);
                }
            }

        }

        // add the Listener for changes
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent e) {
                handleColumnModelChanges((TableColumnModel) e.getSource());
            }

            public void columnRemoved(TableColumnModelEvent e) {
                handleColumnModelChanges((TableColumnModel) e.getSource());
            }

            public void columnMoved(TableColumnModelEvent e) {
                handleColumnModelChanges((TableColumnModel) e.getSource());
            }

            public void columnMarginChanged(ChangeEvent e) {
                handleColumnModelChanges((TableColumnModel) e.getSource());
            }

            public void columnSelectionChanged(ListSelectionEvent e) {
                // do nothing
            }
        });

        columnWidthsInitialized = true;
    }

    /**
     * This method handle the changes on the TableColumnModel. The width of all
     * columns will be stored in the datasource properties. Later this can be
     * saved within the projectfile.
     *
     * @param columnModel
     */
    private void handleColumnModelChanges(TableColumnModel columnModel) {
        int columns = columnModel.getColumnCount();
        HashMap columnsWithMap = new HashMap(columns);
        // loop over all columns in this table
        for (int i = 0; i < columns; i++) {
            // we map the headername of a column to his width, because in the case
            // of changing the column order we have a problem if we use the index!
            columnsWithMap.put(columnModel.getColumn(i).getHeaderValue(), columnModel.getColumn(i).getWidth());
        }
        // and finaly add the map to the projects properties
        workbenchContext.getTask().getLayerManager().getLayer(getModel().getLayer().getName()).getDataSourceQuery().getDataSource().getProperties().put(ATTRIBUTE_COLUMNS_WIDTH_MAP, columnsWithMap);
    }

    private void setToolTips() {
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column == -1) {
                    return;
                }
                table.setToolTipText(table.getColumnName(column) + " ["
                        + getModel().getLayer().getName() + "]");
            }
        });
    }

    /**
     * Called when the user creates a new selection, rather than adding to the
     * existing selection
     */
    private void fireSelectionReplaced() {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            AttributeTablePanelListener listener = (AttributeTablePanelListener) i
                    .next();
            listener.selectionReplaced(this);
        }
    }

    private static class SelectionModelWrapper implements ListSelectionModel {

        private AttributeTablePanel panel;
        private ListSelectionModel selectionModel;

        public SelectionModelWrapper(AttributeTablePanel panel) {
            this.panel = panel;
            selectionModel = panel.table.getSelectionModel();
        }

        public void setAnchorSelectionIndex(int index) {
            selectionModel.setAnchorSelectionIndex(index);
        }

        public void setLeadSelectionIndex(int index) {
            selectionModel.setLeadSelectionIndex(index);
        }

        public void setSelectionInterval(int index0, int index1) {
            selectionModel.setSelectionInterval(index0, index1);
            panel.fireSelectionReplaced();
        }

        public void setSelectionMode(int selectionMode) {
            selectionModel.setSelectionMode(selectionMode);
        }

        public void setValueIsAdjusting(boolean valueIsAdjusting) {
            selectionModel.setValueIsAdjusting(valueIsAdjusting);
        }

        public int getAnchorSelectionIndex() {
            return selectionModel.getAnchorSelectionIndex();
        }

        public int getLeadSelectionIndex() {
            return selectionModel.getLeadSelectionIndex();
        }

        public int getMaxSelectionIndex() {
            return selectionModel.getMaxSelectionIndex();
        }

        public int getMinSelectionIndex() {
            return selectionModel.getMinSelectionIndex();
        }

        public int getSelectionMode() {
            return selectionModel.getSelectionMode();
        }

        public boolean getValueIsAdjusting() {
            return selectionModel.getValueIsAdjusting();
        }

        public boolean isSelectedIndex(int index) {
            return selectionModel.isSelectedIndex(index);
        }

        public boolean isSelectionEmpty() {
            return selectionModel.isSelectionEmpty();
        }

        public void addListSelectionListener(ListSelectionListener x) {
            selectionModel.addListSelectionListener(x);
        }

        public void addSelectionInterval(int index0, int index1) {
            selectionModel.addSelectionInterval(index0, index1);
        }

        public void clearSelection() {
            selectionModel.clearSelection();
        }

        public void insertIndexInterval(int index, int length, boolean before) {
            selectionModel.insertIndexInterval(index, length, before);
        }

        public void removeIndexInterval(int index0, int index1) {
            selectionModel.removeIndexInterval(index0, index1);
        }

        public void removeListSelectionListener(ListSelectionListener x) {
            selectionModel.removeListSelectionListener(x);
        }

        public void removeSelectionInterval(int index0, int index1) {
            selectionModel.removeSelectionInterval(index0, index1);
        }
    }

    public Collection getSelectedFeatures() {
        ArrayList selectedFeatures = new ArrayList();
        if (getModel().getRowCount() == 0) {
            return selectedFeatures;
        }
        int[] selectedRows = table.getSelectedRows();
        for (int i = 0; i < selectedRows.length; i++) {
            selectedFeatures.add(getModel().getFeature(selectedRows[i]));
        }
        return selectedFeatures;
    }

    public LayerNameRenderer getLayerNameRenderer() {
        return layerNameRenderer;
    }

    public void setFeatureEditor(FeatureEditor featureEditor) {
        this.featureEditor = featureEditor;
    }
}