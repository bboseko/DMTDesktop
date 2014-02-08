package org.openjump.core.ui.plugin.datastore;

import com.osfac.dmt.I18N;
import com.osfac.dmt.coordsys.CoordinateSystemRegistry;
import com.osfac.dmt.task.TaskMonitor;
import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.model.Layerable;
import com.osfac.dmt.workbench.plugin.EnableCheck;
import com.osfac.dmt.workbench.plugin.EnableCheckFactory;
import com.osfac.dmt.workbench.plugin.MultiEnableCheck;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.plugin.ThreadedBasePlugIn;
import com.osfac.dmt.workbench.ui.MenuNames;
import com.osfac.dmt.workbench.ui.plugin.FeatureInstaller;
import com.osfac.dmt.workbench.ui.plugin.OpenProjectPlugIn;
import com.osfac.dmt.workbench.ui.plugin.datastore.DataStoreQueryDataSource;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.openjump.core.ui.images.IconLoader;

/**
 * <code>RefreshDatastoreQueryPlugIn</code> runs the query associated to this
 * layer and replace the dataset.
 *
 * @author <a href="mailto:michael.michaud@free.fr">Michael Michaud</a>
 */
public class RefreshDataStoreQueryPlugIn extends ThreadedBasePlugIn {

    public static final ImageIcon ICON = IconLoader.icon("arrow_refresh_sql.png");

    @Override
    public void initialize(PlugInContext context) throws Exception {
        WorkbenchContext workbenchContext = context.getWorkbenchContext();
        EnableCheck enableCheck = createEnableCheck(workbenchContext);
        FeatureInstaller installer = new FeatureInstaller(workbenchContext);
        JPopupMenu popupMenu = workbenchContext.getWorkbench().getFrame()
                .getLayerNamePopupMenu();
        installer.addPopupMenuItem(popupMenu, this, new String[]{MenuNames.DATASTORE},
                getName(), false, ICON, enableCheck);
    }

    @Override
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.datastore.RefreshDataStoreQueryPlugIn.Refresh-datastore-query");
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        return true;
    }

    public void run(TaskMonitor monitor, final PlugInContext context) throws Exception {
        Layer[] selectedLayers = context.getSelectedLayers();
        for (final Layer layer : selectedLayers) {
            OpenProjectPlugIn.load(layer,
                    CoordinateSystemRegistry.instance(context.getWorkbenchContext().getBlackboard()),
                    monitor);
            // setFeatureCollectionModified(false) must be set after fireFeaturesChanged
            // As in Layer.setFeatureCollection method, fireFeaturesChanged is
            // called in an invokeLater thread, setFeatureCollectionModified
            // must also be called in an invokeLater clause.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    layer.setFeatureCollectionModified(false);
                }
            });
        }
    }

    /**
     * Creates an EnableCheck object to enable the plugin if a project is active
     * and if only layers connected to a DataStoreQueryDataSource are selected.
     *
     * @param workbenchContext
     * @return an enable check
     */
    public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        final WorkbenchContext wc = workbenchContext;
        EnableCheckFactory enableCheckFactory = new EnableCheckFactory(workbenchContext);
        MultiEnableCheck enableCheck = new MultiEnableCheck();
        enableCheck.add(enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck());
        enableCheck.add(enableCheckFactory.createAtLeastNLayerablesMustBeSelectedCheck(1, Layerable.class));
        enableCheck.add(new EnableCheck() {
            public String check(javax.swing.JComponent component) {
                Layer[] selectedLayers = wc.getLayerNamePanel().getSelectedLayers();
                for (Layer layer : selectedLayers) {
                    if (layer.getDataSourceQuery() == null
                            || !(layer.getDataSourceQuery().getDataSource() instanceof DataStoreQueryDataSource)) {
                        return I18N.get("org.openjump.core.ui.plugin.datastore.RefreshDataStoreQueryPlugIn.Only-datastore-query-layers-must-be-selected");
                    }
                }
                return null;
            }
        });
        return enableCheck;
    }
}