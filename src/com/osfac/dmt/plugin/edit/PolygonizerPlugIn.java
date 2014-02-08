package com.osfac.dmt.plugin.edit;

import com.osfac.dmt.I18N;
import com.osfac.dmt.feature.Feature;
import com.osfac.dmt.feature.FeatureCollection;
import com.osfac.dmt.feature.FeatureDatasetFactory;
import com.osfac.dmt.task.TaskMonitor;
import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.model.LayerStyleUtil;
import com.osfac.dmt.workbench.model.StandardCategoryNames;
import com.osfac.dmt.workbench.plugin.EnableCheck;
import com.osfac.dmt.workbench.plugin.EnableCheckFactory;
import com.osfac.dmt.workbench.plugin.MultiEnableCheck;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.ui.GUIUtil;
import com.osfac.dmt.workbench.ui.GenericNames;
import com.osfac.dmt.workbench.ui.MenuNames;
import com.osfac.dmt.workbench.ui.MultiInputDialog;
import com.osfac.dmt.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.operation.polygonize.*;
import java.awt.Color;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

public class PolygonizerPlugIn extends AbstractThreadedUiPlugIn {

    private final static String SRC_LAYER = I18N.get("jump.plugin.edit.PolygonizerPlugIn.Line-Layer");
    private final static String NODE_INPUT = I18N.get("jump.plugin.edit.PolygonizerPlugIn.Node-input-before-polygonizing");
    private final static String SELECTED_ONLY = GenericNames.USE_SELECTED_FEATURES_ONLY;
    private boolean useSelected = false;
    private String layerName;
    private boolean splitLineStrings = false;
    private boolean nodeInputLines = false;
    private int inputEdgeCount = 0;
    private int dangleCount = 0;
    private int cutCount = 0;
    private int invalidRingCount = 0;
    private GeometryFactory fact = new GeometryFactory();

    public PolygonizerPlugIn() {
    }

    /**
     * Returns a very brief description of this task.
     *
     * @return the name of this task
     */
    public String getName() {
        return I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygonize");
    }

    public void initialize(PlugInContext context) throws Exception {

        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuItem(
                new String[]{MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY, MenuNames.CONVERT},
                this,
                new JMenuItem(getName() + "..."),
                createEnableCheck(context.getWorkbenchContext()),
                -1);
    }

    public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

    public boolean execute(PlugInContext context) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(),
                I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygonize"), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();

        Polygonizer polygonizer = new Polygonizer();
        monitor.report(I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygonizing"));

        Layer layer = context.getLayerManager().getLayer(layerName);
        Collection inputFeatures = getFeaturesToProcess(layer, context);
        inputEdgeCount = inputFeatures.size();

        Collection lines = getLines(inputFeatures);
        Collection nodedLines = lines;
        if (nodeInputLines) {
            monitor.report(I18N.get("jump.plugin.edit.PolygonizerPlugIn.Noding-input-lines"));
            nodedLines = nodeLines((List) lines);
        }

        for (Iterator i = nodedLines.iterator(); i.hasNext();) {
            Geometry g = (Geometry) i.next();
            polygonizer.add(g);
        }
        if (monitor.isCancelRequested()) {
            return;
        }
        createLayers(context, polygonizer);
    }

    private Collection getFeaturesToProcess(Layer lyr, PlugInContext context) {
        if (useSelected) {
            return context.getLayerViewPanel()
                    .getSelectionManager().getFeaturesWithSelectedItems(lyr);
        }
        return lyr.getFeatureCollectionWrapper().getFeatures();
    }

    private Collection getLines(Collection inputFeatures) {
        List linesList = new ArrayList();
        LinearComponentExtracter lineFilter = new LinearComponentExtracter(linesList);
        for (Iterator i = inputFeatures.iterator(); i.hasNext();) {
            Feature f = (Feature) i.next();
            Geometry g = f.getGeometry();
            g.apply(lineFilter);
        }
        return linesList;
    }

    /**
     * Nodes a collection of linestrings. Noding is done via JTS union, which is
     * reasonably effective but may exhibit robustness failures.
     *
     * @param lines the linear geometries to node
     * @return a collection of linear geometries, noded together
     */
    private Collection nodeLines(Collection lines) {
        Geometry linesGeom = fact.createMultiLineString(fact.toLineStringArray(lines));

        Geometry unionInput = fact.createMultiLineString(null);
        // force the unionInput to be non-empty if possible, to ensure union is not optimized away
        Geometry point = extractPoint(lines);
        if (point != null) {
            unionInput = point;
        }

        Geometry noded = linesGeom.union(unionInput);
        List nodedList = new ArrayList();
        nodedList.add(noded);
        return nodedList;
    }

    private Geometry extractPoint(Collection lines) {
        int minPts = Integer.MAX_VALUE;
        Geometry point = null;
        // extract first point from first non-empty geometry
        for (Iterator i = lines.iterator(); i.hasNext();) {
            Geometry g = (Geometry) i.next();
            if (!g.isEmpty()) {
                Coordinate p = g.getCoordinate();
                point = g.getFactory().createPoint(p);
            }
        }
        return point;
    }

    private void createLayers(PlugInContext context,
            Polygonizer polygonizer) throws Exception {
        FeatureCollection dangleFC = FeatureDatasetFactory.createFromGeometry(polygonizer.getDangles());
        dangleCount = dangleFC.size();
        if (dangleFC.size() > 0) {
            Layer lyr4 = context.addLayer(
                    StandardCategoryNames.QA,
                    I18N.get("jump.plugin.edit.PolygonizerPlugIn.Dangles"),
                    dangleFC);
            LayerStyleUtil.setLinearStyle(lyr4, Color.red, 2, 0);
            lyr4.setDescription(I18N.get("jump.plugin.edit.PolygonizerPlugIn.Dangling-edges"));
        }

        FeatureCollection cutFC = FeatureDatasetFactory.createFromGeometry(polygonizer.getCutEdges());
        cutCount = cutFC.size();

        if (cutFC.size() > 0) {
            Layer lyr = context.addLayer(
                    StandardCategoryNames.QA,
                    I18N.get("jump.plugin.edit.PolygonizerPlugIn.Cuts"),
                    cutFC);
            LayerStyleUtil.setLinearStyle(lyr, Color.blue, 2, 0);
            lyr.setDescription(I18N.get("jump.plugin.edit.PolygonizerPlugIn.Cut-edges"));
        }

        FeatureCollection invalidRingFC = FeatureDatasetFactory.createFromGeometry(polygonizer.getInvalidRingLines());
        invalidRingCount = invalidRingFC.size();

        if (invalidRingFC.size() > 0) {
            Layer lyr = context.addLayer(
                    StandardCategoryNames.QA,
                    I18N.get("jump.plugin.edit.PolygonizerPlugIn.Invalid-Rings"),
                    invalidRingFC);
            LayerStyleUtil.setLinearStyle(lyr, Color.blue, 2, 0);
            lyr.setDescription(I18N.get("jump.plugin.edit.PolygonizerPlugIn.Invalid-Rings"));
        }

        FeatureCollection polyFC = FeatureDatasetFactory.createFromGeometry(polygonizer.getPolygons());
        context.addLayer(
                StandardCategoryNames.RESULT,
                layerName + " " + I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygons"),
                polyFC);

        createOutput(context, polyFC);

    }

    private void createOutput(PlugInContext context, FeatureCollection polyFC) {
        context.getOutputFrame().createNewDocument();
        context.getOutputFrame().addHeader(1,
                I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygonization"));
        context.getOutputFrame().addField("Layer: ", layerName);


        context.getOutputFrame().addText(" ");
        context.getOutputFrame().addField(
                I18N.get("jump.plugin.edit.PolygonizerPlugIn.Number-of-input-edges"), "" + inputEdgeCount);
        context.getOutputFrame().addField(
                I18N.get("jump.plugin.edit.PolygonizerPlugIn.Number-of-polygons-created"), "" + polyFC.size());
        context.getOutputFrame().addField(
                I18N.get("jump.plugin.edit.PolygonizerPlugIn.Number-of-dangling-edges-found"), "" + dangleCount);
        context.getOutputFrame().addField(
                I18N.get("jump.plugin.edit.PolygonizerPlugIn.Number-of-cut-edges-found"), "" + cutCount);
        context.getOutputFrame().addField(
                I18N.get("jump.plugin.edit.PolygonizerPlugIn.Number-of-invalid-rings-found"), "" + invalidRingCount);
    }

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        dialog.setSideBarImage(new ImageIcon(getClass().getResource("Polygonize.png")));
        dialog.setSideBarDescription(I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygonizes-the-line-segments-in-a-layer")
                + " " + I18N.get("jump.plugin.edit.PolygonizerPlugIn.Polygonization-requires-correctly-noded-data")
                + " " + I18N.get("jump.plugin.edit.PolygonizerPlugIn.If-desired-the-input-data-may-be-noded-before-polygonizing-is-performed")
                + " " + I18N.get("jump.plugin.edit.PolygonizerPlugIn.Dangles-Cutlines-and-Invalid-Rings-are-identified"));
        String fieldName = SRC_LAYER;
        JComboBox addLayerComboBox = dialog.addLayerComboBox(fieldName, context.getCandidateLayer(0), null, context.getLayerManager());
        dialog.addCheckBox(SELECTED_ONLY, useSelected);
        dialog.addCheckBox(NODE_INPUT, nodeInputLines, NODE_INPUT);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        Layer layer = dialog.getLayer(SRC_LAYER);
        layerName = layer.getName();
        useSelected = dialog.getBoolean(SELECTED_ONLY);
        nodeInputLines = dialog.getBoolean(NODE_INPUT);
    }
}