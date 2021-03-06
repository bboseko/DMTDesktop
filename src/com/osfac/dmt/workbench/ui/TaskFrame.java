package com.osfac.dmt.workbench.ui;

import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.model.LayerManager;
import com.osfac.dmt.workbench.model.LayerManagerProxy;
import com.osfac.dmt.workbench.model.LayerTreeModel;
import com.osfac.dmt.workbench.model.Layerable;
import com.osfac.dmt.workbench.model.Task;
import com.osfac.dmt.workbench.ui.cursortool.DummyTool;
import com.osfac.dmt.workbench.ui.renderer.Renderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JInternalFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class TaskFrame extends JInternalFrame implements TaskFrameProxy,
        CloneableInternalFrame, LayerViewPanelProxy, LayerNamePanelProxy,
        LayerManagerProxy, SelectionManagerProxy, Task.NameListener {

    public TaskFrame getTaskFrame() {
        return this;
    }
    private int cloneIndex;
    private InfoFrame infoFrame = null;
    private LayerNamePanel layerNamePanel = new DummyLayerNamePanel();
    private LayerViewPanel layerViewPanel;
    private Task task;
    private WorkbenchContext workbenchContext;
    //private LayerManager layerManager;
    private JSplitPane splitPane = new JSplitPane();
    private Timer timer;

    public TaskFrame(Task task, WorkbenchContext workbenchContext) {
        this(task, 0, workbenchContext);
    }

    public SelectionManager getSelectionManager() {
        return getLayerViewPanel().getSelectionManager();
    }

    public TaskFrame() {
    }

    private TaskFrame(Task task, int cloneIndex, final WorkbenchContext workbenchContext) {
        this.task = task;
        //this.layerManager = task.getLayerManager();
        this.cloneIndex = cloneIndex;
        this.workbenchContext = workbenchContext;
        addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameDeactivated(InternalFrameEvent e) {
                //Deactivate the current CursorTool. Otherwise, the following
                // problem
                //can occur:
                //  -- Start drawing a linestring on a task frame. Don't
                // double-click
                //      to end the gesture.
                //  -- Open a new task frame. You're still drawing the
                // linestring!
                //      This shouldn't happen; instead, the drawing should be
                // cancelled.
                //[Bob Boseko]
                layerViewPanel.setCurrentCursorTool(new DummyTool());
            }

            public void internalFrameClosed(InternalFrameEvent e) {
                try {
                    // Code to manage TaskFrame INTERNAL_FRAME_CLOSED event
                    // has been moved to closeTaskFrame method in WorkbenchFrame
                    // I let this method because of the timer.stop [mmichaud]
                    // Maybe the WorkbenchFrame.closeTaskFrame should be moved here...
                    timer.stop();
                    //memoryCleanup();
                } catch (Throwable t) {
                    workbenchContext.getWorkbench().getFrame().handleThrowable(
                            t);
                }
            }

            public void internalFrameOpened(InternalFrameEvent e) {
                //Set the layerNamePanel when the frame is opened, not in the
                // constructor,
                //because #createLayerNamePanel may be overriden in a subclass,
                // and the
                //subclass has not yet been constructed -- weird things happen,
                // like variables
                //are unexpectedly null. [Bob Boseko]
                splitPane.remove((Component) layerNamePanel);
                layerNamePanel = createLayerNamePanel();
                splitPane.add((Component) layerNamePanel, JSplitPane.LEFT);
                layerNamePanel.addListener(workbenchContext.getWorkbench()
                        .getFrame().getLayerNamePanelListener());
            }
        });
        layerViewPanel = new LayerViewPanel(task.getLayerManager(), workbenchContext.getWorkbench().getFrame());
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        layerViewPanel.addListener(workbenchContext.getWorkbench().getFrame().getLayerViewPanelListener());
        layerViewPanel.getViewport().addListener(workbenchContext.getWorkbench().getFrame());
        task.add(this);
        installAnimator();
    }

    protected LayerNamePanel createLayerNamePanel() {
        TreeLayerNamePanel treeLayerNamePanel = new TreeLayerNamePanel(this,
                new LayerTreeModel(this), this.layerViewPanel
                .getRenderingManager(), new HashMap());
        Map nodeClassToPopupMenuMap = this.workbenchContext.getWorkbench()
                .getFrame().getNodeClassToPopupMenuMap();
        for (Iterator i = nodeClassToPopupMenuMap.keySet().iterator(); i
                .hasNext();) {
            Class nodeClass = (Class) i.next();
            treeLayerNamePanel.addPopupMenu(nodeClass,
                    (JPopupMenu) nodeClassToPopupMenuMap.get(nodeClass));
        }
        return treeLayerNamePanel;
    }

    //
    // When the internal frame closes there still seem to be Swing objects
    // with references to it. Clean up the memory as much as we can. We probably
    // should not overwrite the dispose() method of JInternalFrame.
    //
    // Code to manage TaskFrame INTERNAL_FRAME_CLOSED event has been moved to
    // closeTaskFrame method in WorkbenchFrame [mmichaud]
    // [NOTE] Several JInternalFrames subclasses add listeners here and there
    // It probably need some clean up
    /*
     public void memoryCleanup() {
     timer.stop();
        
     getLayerManager().setFiringEvents(false);
     getLayerManager().dispose();
        
     layerViewPanel.dispose();
     layerNamePanel.dispose();

     if (infoFrame != null) {
     infoFrame.dispose();
     infoFrame = null;
     }

     layerViewPanel = null;
     layerNamePanel = null;
     task = null;
     workbenchContext = null;
     timer = null;
     splitPane = null;
     }
     */
    public LayerManager getLayerManager() {
        return task.getLayerManager();
    }

    public InfoFrame getInfoFrame() {
        if (infoFrame == null || infoFrame.isClosed()) {
            infoFrame = new PrimaryInfoFrame(workbenchContext, this, this);
        }
        return infoFrame;
    }

    public LayerNamePanel getLayerNamePanel() {
        return layerNamePanel;
    }

    public LayerViewPanel getLayerViewPanel() {
        return layerViewPanel;
    }

    public void setTask(Task task) {
        if (this.task != null) {
            throw new IllegalStateException("Task is already set");
        } else {
            this.task = task;
        }
    }

    public Task getTask() {
        return task;
    }

    private int nextCloneIndex() {
        String key = getClass().getName() + " - LAST_CLONE_INDEX";
        task.getLayerManager().getBlackboard().put(key, 1 + task.getLayerManager().getBlackboard().get(key, 0));
        return task.getLayerManager().getBlackboard().getInt(key);
    }

    public JInternalFrame internalFrameClone() {
        TaskFrame clone = new TaskFrame(task, nextCloneIndex(), workbenchContext);
        clone.splitPane.setDividerLocation(0);
        clone.setSize(300, 300);

        if (task.getLayerManager().size() > 0) {
            clone.getLayerViewPanel().getViewport().initialize(
                    getLayerViewPanel().getViewport().getScale(),
                    getLayerViewPanel().getViewport()
                    .getOriginInModelCoordinates());
            clone.getLayerViewPanel().setViewportInitialized(true);
        }

        return clone;
    }

    public void taskNameChanged(String name) {
        updateTitle();
    }

    //The border around the tree layer panel looks a bit thick under JDK 1.4.
    //Remedied by removing the split pane's border. [Bob Boseko]
    private void jbInit() throws Exception {
        this.setResizable(true);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        //Allow some of the background to show so that user sees this is an MDI app
        this.setSize(680, 380);
        this.getContentPane().setLayout(new BorderLayout());
        splitPane.setBorder(null);
        this.getContentPane().add(splitPane, BorderLayout.CENTER);
        splitPane.add((Component) layerNamePanel, JSplitPane.LEFT);
        splitPane.add(layerViewPanel, JSplitPane.RIGHT);
        splitPane.setDividerLocation(200);
        updateTitle();
    }

    protected void updateTitle() {
        String title = task.getName();
        if (cloneIndex > 0) {
            title += " (View " + (cloneIndex + 1) + ")";
        }
        setTitle(title);
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    protected void installAnimator() {
        timer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (clockedRenderingInProgress()) {
                    repaint();
                } else if (clocksShown()) {
                    repaint();
                }
            }

            private boolean clockedRenderingInProgress() {
                for (Iterator i = getLayerManager().getLayerables(
                        Layerable.class).iterator(); i.hasNext();) {
                    Layerable layerable = (Layerable) i.next();
                    if (!layerable.getBlackboard().get(
                            LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, false)) {
                        continue;
                    }
                    Renderer renderer = layerViewPanel.getRenderingManager()
                            .getRenderer(layerable);
                    if (renderer != null && renderer.isRendering()) {
                        return true;
                    }
                }
                return false;
            }

            // Previously we had a flag to keep track of whether
            // clocks were displayed. However that was not sufficient,
            // as quick-rendering layers were missed by the timer,
            // and thus the clock icon, if painted (e.g. by #zoomChanged
            // in TreeLayerNamePanel), would not be cleared. So here
            // we do a more thorough check for whether any clocks are
            // displayed. [Bob Boseko 2005-03-14]
            private boolean clocksShown() {
                for (Iterator i = getLayerManager().getLayerables(Layerable.class).iterator(); i.hasNext();) {
                    Layerable layerable = (Layerable) i.next();
                    if (layerable.getBlackboard().get(LayerNameRenderer.PROGRESS_ICON_KEY) != null) {
                        return true;
                    }
                }
                return false;
            }
        });
        timer.setCoalesce(true);
        timer.start();
    }
}