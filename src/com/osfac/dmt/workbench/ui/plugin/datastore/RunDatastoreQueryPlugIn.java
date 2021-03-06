package com.osfac.dmt.workbench.ui.plugin.datastore;

import com.osfac.dmt.I18N;
import com.osfac.dmt.datastore.AdhocQuery;
import com.osfac.dmt.feature.FeatureDataset;
import com.osfac.dmt.io.FeatureInputStream;
import com.osfac.dmt.task.TaskMonitor;
import com.osfac.dmt.workbench.datastore.ConnectionManager;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.model.Layerable;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.ui.MenuNames;
import com.osfac.dmt.workbench.ui.images.IconLoader;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import javax.swing.Icon;

/**
 * This PlugIn runs a SQL query against a datastore and creates a Layer from the
 * result.
 */
public class RunDatastoreQueryPlugIn extends AbstractAddDatastoreLayerPlugIn {

    protected ConnectionPanel createPanel(PlugInContext context) {
        return new RunDatastoreQueryPanel(context.getWorkbenchContext());
    }

    public void initialize(final PlugInContext context) throws Exception {
        super.initialize(context);
        context.getFeatureInstaller()
                .addMainMenuItem(new String[]{MenuNames.FILE}, this, 3);
    }

    protected Layerable createLayerable(
            ConnectionPanel panel,
            TaskMonitor monitor,
            PlugInContext context) throws Exception {
        return createLayer((RunDatastoreQueryPanel) panel, monitor, context);
    }

    public String getName() {
        return I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPlugIn.Run-Datastore-Query");
    }

    public Icon getIcon() {
        return IconLoader.icon("sql.png");
    }

    private Layer createLayer(final RunDatastoreQueryPanel panel,
            TaskMonitor monitor,
            final PlugInContext context) throws Exception {

        panel.saveQuery();

        monitor.allowCancellationRequests();
        monitor.report(I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPlugIn.Creating-layer"));

        //int maxFeatures = ((Integer)LangUtil.ifNull( panel.getMaxFeatures(),
        //    new Integer(Integer.MAX_VALUE))).intValue();

        // added by Michael Michaud on 2009-11-22 to use aliases representing
        // view rectangle or selection in a query
        String driver = panel.getConnectionDescriptor().getDataStoreDriverClassName();
        String query = panel.getQuery();
        if (driver.contains("Postgis") && query.matches("(?s).*\\$\\{[^\\{\\}]*\\}.*")) {
            query = expandQuery(query, context);
        }
        // end
        FeatureInputStream featureInputStream =
                ConnectionManager.instance(context.getWorkbenchContext())
                .getOpenConnection(panel.getConnectionDescriptor())
                .execute(new AdhocQuery(query));
        try {
            FeatureDataset featureDataset = new FeatureDataset(
                    featureInputStream.getFeatureSchema());
            int i = 0;
            while (featureInputStream.hasNext() && !monitor.isCancelRequested()) {
                featureDataset.add(featureInputStream.next());
                monitor.report(++i, -1, I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPlugIn.features"));
            }
            String name = panel.getLayerName();
            Layer layer = new Layer(name,
                    context.getLayerManager().generateLayerFillColor(),
                    featureDataset, context.getLayerManager());
            layer.setDataSourceQuery(new com.osfac.dmt.io.datasource.DataSourceQuery(
                    new DataStoreQueryDataSource(name,
                    panel.getQuery(),
                    panel.getConnectionDescriptor(),
                    context.getWorkbenchContext()),
                    panel.getQuery(), name));
            return layer;
        } finally {
            // This code had been added as an attempt to cancel a long running query
            // but it has a side effect on the connection which is closed
            // This peace of code is removed until a better solution is found 
            //if (featureInputStream instanceof com.osfac.dmt.datastore.postgis.PostgisFeatureInputStream) {
            //    java.sql.Statement stmt = 
            //    ((com.osfac.dmt.datastore.postgis.PostgisFeatureInputStream)featureInputStream).getStatement();
            //    if (stmt != null) stmt.cancel();
            //}
            featureInputStream.close();
        }
    }

    private String expandQuery(String query, PlugInContext context) {
        GeometryFactory gf = new GeometryFactory();
        Geometry viewG = gf.toGeometry(context.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates());
        Geometry fenceG = context.getLayerViewPanel().getFence();
        if (viewG != null) {
            query = query.replaceAll("\\$\\{view\\}", "\\${view:-1}");
            query = query.replaceAll("\\$\\{view(?::(-?[0-9]+))\\}", "ST_GeomFromText('" + viewG.toText() + "',$1)");
        }
        if (fenceG != null) {
            query = query.replaceAll("\\$\\{fence\\}", "\\${fence:-1}");
            query = query.replaceAll("\\$\\{fence(?::(-?[0-9]+))\\}", "ST_GeomFromText('" + fenceG.toText() + "',$1)");
        }
        return query;
    }
}
