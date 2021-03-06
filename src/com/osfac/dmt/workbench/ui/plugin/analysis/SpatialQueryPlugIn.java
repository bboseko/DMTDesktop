package com.osfac.dmt.workbench.ui.plugin.analysis;

import com.osfac.dmt.I18N;
import com.osfac.dmt.feature.FeatureCollection;
import com.osfac.dmt.task.TaskMonitor;
import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.model.StandardCategoryNames;
import com.osfac.dmt.workbench.plugin.AbstractPlugIn;
import com.osfac.dmt.workbench.plugin.EnableCheckFactory;
import com.osfac.dmt.workbench.plugin.MultiEnableCheck;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.plugin.ThreadedPlugIn;
import com.osfac.dmt.workbench.plugin.util.LayerNameGenerator;
import com.osfac.dmt.workbench.ui.GUIUtil;
import com.osfac.dmt.workbench.ui.GenericNames;
import com.osfac.dmt.workbench.ui.MenuNames;
import com.osfac.dmt.workbench.ui.MultiInputDialog;
import com.osfac.dmt.workbench.ui.SelectionManager;
import com.osfac.dmt.workbench.ui.plugin.FeatureInstaller;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Queries a layer by a spatial predicate.
 */
public class SpatialQueryPlugIn extends AbstractPlugIn implements ThreadedPlugIn {

    private static String UPDATE_SRC = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Select-features-in-the-source-layer");
    private static String CREATE_LYR = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Create-a-new-layer-for-the-results");
    private static String MASK_LAYER = GenericNames.MASK_LAYER;
    private static String SRC_LAYER = GenericNames.SOURCE_LAYER;
    private static String PREDICATE = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Relation");
    private static String PARAM = GenericNames.PARAMETER;
    private static String DIALOG_COMPLEMENT = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Complement-Result");
    private static String ALLOW_DUPS = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Allow-Duplicates-in-Result");
    private JTextField paramField;
    private Collection functionNames;
    private MultiInputDialog dialog;
    private Layer maskLyr;
    private Layer srcLayer;
    private String funcNameToRun;
    private GeometryPredicate functionToRun = null;
    private boolean complementResult = false;
    private boolean allowDups = false;
    private boolean exceptionThrown = false;
    private JRadioButton updateSourceRB;
    private JRadioButton createNewLayerRB;
    private boolean createLayer = true;
    private double[] params = new double[2];

    public SpatialQueryPlugIn() {
        functionNames = GeometryPredicate.getNames();
    }
    private String categoryName = StandardCategoryNames.RESULT;

    public void setCategoryName(String value) {
        categoryName = value;
    }

    public String getName() {
        return I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Spatial-Query");
    }

    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuItem(
                this,
                new String[]{MenuNames.TOOLS, MenuNames.TOOLS_QUERIES},
                new JMenuItem(this.getName() + "..."),
                createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(2));
    }

    public boolean execute(PlugInContext context) throws Exception {
        //[sstein] added again for correct language setting
        UPDATE_SRC = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Select-features-in-the-source-layer");
        CREATE_LYR = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Create-a-new-layer-for-the-results");
        MASK_LAYER = GenericNames.MASK_LAYER;
        SRC_LAYER = GenericNames.SOURCE_LAYER;
        PREDICATE = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Relation");
        PARAM = GenericNames.PARAMETER;
        DIALOG_COMPLEMENT = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Complement-Result");
        ALLOW_DUPS = I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Allow-Duplicates-in-Result");

        dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        monitor.allowCancellationRequests();

        // input-proofing
        if (functionToRun == null) {
            return;
        }
        if (maskLyr == null) {
            return;
        }
        if (srcLayer == null) {
            return;
        }

        monitor.report(I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Executing-query") + " " + functionToRun.getName() + "...");

        FeatureCollection maskFC = maskLyr.getFeatureCollectionWrapper();
        FeatureCollection sourceFC = srcLayer.getFeatureCollectionWrapper();

        int nArgs = functionToRun.getGeometryArgumentCount();

        SpatialQueryExecuter executer = new SpatialQueryExecuter(maskFC, sourceFC);
        executer.setAllowDuplicates(allowDups);
        executer.setComplementResult(complementResult);

        // Code added by the Sunburned Surveyor to allow
        // the creation of "normal" selections if a new
        // layer isn't being created for the features
        // selected as part of the spatial analysis.
        executer.setCreateNewLayer(createLayer);

        FeatureCollection resultFC = executer.getResultFC();
        executer.execute(monitor, functionToRun, params, resultFC);

        if (monitor.isCancelRequested()) {
            return;
        }

        if (createLayer) {
            String outputLayerName = LayerNameGenerator.generateOperationOnLayerName(
                    funcNameToRun,
                    srcLayer.getName());
            context.getLayerManager().addCategory(categoryName);
            context.addLayer(categoryName, outputLayerName, resultFC);
        } else {
            SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();
            selectionManager.clear();
            selectionManager.getFeatureSelection().selectItems(srcLayer, resultFC.getFeatures());
        }

        if (exceptionThrown) {
            context.getWorkbenchFrame().warnUser(I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Errors-found-while-executing-query"));
        }
    }

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        //dialog.setSideBarImage(new ImageIcon(getClass().getResource("DiffSegments.png")));
        dialog.setSideBarDescription(
                I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.Finds-the-Source-features-which-have-a-given-spatial-relationship-to-some-feature-in-the-Mask-layer")
                + " (" + I18N.get("ui.plugin.analysis.SpatialQueryPlugIn.ie-where-Source.Relationship(Mask)-is-true") + ")");

        //Set initial layer values to the first and second layers in the layer list.
        //In #initialize we've already checked that the number of layers >= 1. [Bob Boseko]
        Layer initLayer = (srcLayer == null) ? context.getCandidateLayer(0) : srcLayer;

        dialog.addLayerComboBox(SRC_LAYER, initLayer, context.getLayerManager());
        JComboBox functionComboBox = dialog.addComboBox(PREDICATE, funcNameToRun, functionNames, null);
        functionComboBox.addItemListener(new MethodItemListener());
        dialog.addLayerComboBox(MASK_LAYER, maskLyr, context.getLayerManager());

        paramField = dialog.addDoubleField(PARAM, params[0], 10);
        dialog.addCheckBox(ALLOW_DUPS, allowDups);
        dialog.addCheckBox(DIALOG_COMPLEMENT, complementResult);

        final String OUTPUT_GROUP = "OUTPUT_GROUP";
        createNewLayerRB = dialog.addRadioButton(CREATE_LYR, OUTPUT_GROUP, createLayer, CREATE_LYR);
        updateSourceRB = dialog.addRadioButton(UPDATE_SRC, OUTPUT_GROUP, !createLayer, UPDATE_SRC);

        updateUIForFunction(funcNameToRun);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        maskLyr = dialog.getLayer(MASK_LAYER);
        srcLayer = dialog.getLayer(SRC_LAYER);
        funcNameToRun = dialog.getText(PREDICATE);
        functionToRun = GeometryPredicate.getPredicate(funcNameToRun);
        params[0] = dialog.getDouble(PARAM);
        allowDups = dialog.getBoolean(ALLOW_DUPS);
        complementResult = dialog.getBoolean(DIALOG_COMPLEMENT);
        createLayer = dialog.getBoolean(CREATE_LYR);
    }

    private void updateUIForFunction(String funcName) {
        boolean paramUsed = false;
        GeometryPredicate func = GeometryPredicate.getPredicate(funcName);
        if (func != null) {
            paramUsed = func.getParameterCount() > 0;
        }
        paramField.setEnabled(paramUsed);
        // this has the effect of making the background gray (disabled)
        paramField.setOpaque(paramUsed);
    }

    private class MethodItemListener
            implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            updateUIForFunction((String) e.getItem());
        }
    }
}
