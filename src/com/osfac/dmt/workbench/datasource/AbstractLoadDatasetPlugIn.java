package com.osfac.dmt.workbench.datasource;

import com.osfac.dmt.I18N;
import com.osfac.dmt.coordsys.CoordinateSystemRegistry;
import com.osfac.dmt.feature.FeatureCollection;
import com.osfac.dmt.io.datasource.Connection;
import com.osfac.dmt.io.datasource.DataSourceQuery;
import com.osfac.dmt.task.TaskMonitor;
import com.osfac.dmt.util.CollectionUtil;
import com.osfac.dmt.util.StringUtil;
import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.model.StandardCategoryNames;
import com.osfac.dmt.workbench.plugin.EnableCheckFactory;
import com.osfac.dmt.workbench.plugin.MultiEnableCheck;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.ui.GUIUtil;
import com.osfac.dmt.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jts.util.Assert;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public abstract class AbstractLoadDatasetPlugIn extends AbstractLoadSaveDatasetPlugIn {

    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        //Seamus Thomas Carroll [mailto:carrolls@cpsc.ucalgary.ca]
        //was concerned when he noticed that #getDataSourceQueries
        //was being called twice. So call it once only. [Bob Boseko 2004-02-05]
        Assert.isTrue(!getDataSourceQueries().isEmpty());

        boolean exceptionsEncountered = false;
        for (Iterator i = getDataSourceQueries().iterator(); i.hasNext();) {
            DataSourceQuery dataSourceQuery = (DataSourceQuery) i.next();
            ArrayList exceptions = new ArrayList();
            Assert.isTrue(dataSourceQuery.getDataSource().isReadable());
            monitor.report(I18N.get("datasource.LoadDatasetPlugIn.loading") + " " + dataSourceQuery.toString() + "...");

            Connection connection = dataSourceQuery.getDataSource()
                    .getConnection();
            try {
                FeatureCollection dataset = dataSourceQuery.getDataSource().installCoordinateSystem(
                        connection.executeQuery(dataSourceQuery.getQuery(),
                        exceptions,
                        monitor),
                        CoordinateSystemRegistry.instance(
                        context.getWorkbenchContext().getBlackboard()));
                if (dataset != null) {
                    context.getLayerManager()
                            .addLayer(chooseCategory(context),
                            dataSourceQuery.toString(), dataset)
                            .setDataSourceQuery(dataSourceQuery)
                            .setFeatureCollectionModified(false);
                }
            } finally {
                connection.close();
            }
            if (!exceptions.isEmpty()) {
                if (!exceptionsEncountered) {
                    context.getOutputFrame().createNewDocument();
                    exceptionsEncountered = true;
                }
                reportExceptions(exceptions, dataSourceQuery, context);
            }
        }
        if (exceptionsEncountered) {
            context.getWorkbenchFrame().warnUser(I18N.get("datasource.LoadDatasetPlugIn.problems-were-encountered"));
        }
    }

    private void reportExceptions(ArrayList exceptions,
            DataSourceQuery dataSourceQuery, PlugInContext context) {
        context.getOutputFrame().addHeader(1,
                exceptions.size() + " " + I18N.get("datasource.LoadDatasetPlugIn.problem") + StringUtil.s(exceptions.size())
                + " " + I18N.get("datasource.LoadDatasetPlugIn.loading") + " " + dataSourceQuery.toString() + "."
                + ((exceptions.size() > 10) ? " " + I18N.get("datasource.LoadDatasetPlugIn.first-and-last-five") : ""));
        context.getOutputFrame().addText(I18N.get("datasource.LoadDatasetPlugIn.see-view-log"));
        context.getOutputFrame().append("<ul>");

        Collection exceptionsToReport = exceptions.size() <= 10 ? exceptions
                : CollectionUtil.concatenate(Arrays.asList(
                new Collection[]{
                    exceptions.subList(0, 5),
                    exceptions.subList(exceptions.size() - 5,
                    exceptions.size())
                }));
        for (Iterator j = exceptionsToReport.iterator(); j.hasNext();) {
            Exception exception = (Exception) j.next();
            context.getWorkbenchFrame().log(StringUtil.stackTrace(exception));
            context.getOutputFrame().append("<li>");
            context.getOutputFrame().append(GUIUtil.escapeHTML(
                    WorkbenchFrame.toMessage(exception), true, true));
            context.getOutputFrame().append("</li>");
        }
        context.getOutputFrame().append("</ul>");
    }

    private String chooseCategory(PlugInContext context) {
        return context.getLayerNamePanel().getSelectedCategories().isEmpty()
                ? StandardCategoryNames.WORKING
                : context.getLayerNamePanel().getSelectedCategories().iterator().next()
                .toString();
    }

    public static MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck());
    }
}
