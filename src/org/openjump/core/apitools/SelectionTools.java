/*
 * Created on 11.02.2005 for PIROL
 *
 * SVN header information:
 *  $Author: michaudm $
 *  $Rev: 1670 $
 *  $Date: 2009-02-25 00:33:13 +0100 (mer., 25 févr. 2009) $
 *  $Id: SelectionTools.java 1670 2009-02-24 23:33:13Z michaudm $
 */
package org.openjump.core.apitools;

import com.vividsolutions.jts.geom.Geometry;
import com.osfac.dmt.feature.Feature;
import com.osfac.dmt.feature.FeatureCollection;
import com.osfac.dmt.feature.FeatureCollectionWrapper;
import com.osfac.dmt.workbench.model.FenceLayerFinder;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.ui.SelectionManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class to easily handle selections and selection tools. Also has methods to
 * find features by given geometries.
 *
 * @author Ole Rahn
 *
 * FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck Project
 * PIROL 2005 Daten- und Wissensmanagement
 *
 */
public class SelectionTools extends ToolToMakeYourLifeEasier {

    protected PlugInContext context = null;

    public SelectionTools(PlugInContext context) {
        super();
        this.context = context;
    }

    /**
     * create a selection out of the given features that is visible in the Jump
     * map
     *
     * @param features features to be selected
     */
    public void selectFeatures(List features) {
        SelectionTools.selectFeatures(features, this.context);
    }

    public static void selectLayer(PlugInContext context, Layer layer) {
        // TODO: how to select a layer???       
    }

    public static void selectFeatures(List features, PlugInContext context) {
        SelectionManager sm = context.getLayerViewPanel().getSelectionManager();
        sm.clear();

        Map layer2FeatList = LayerTools.getLayer2FeatureMap(features, context);
        Layer[] layersWithFeatures = (Layer[]) layer2FeatList.keySet().toArray(new Layer[0]);

        for (int i = 0; i < layersWithFeatures.length; i++) {
            sm.getFeatureSelection().selectItems(layersWithFeatures[i], (ArrayList) layer2FeatList.get(layersWithFeatures[i]));
        }

    }

    public static List<Feature> getSelectedFeaturesFromLayer(PlugInContext context, Layer layer) {
        return new ArrayList<Feature>(context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer));
    }

    public static List<Feature> getSelectedFeatures(PlugInContext context) {
        return new ArrayList<Feature>(context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems());
    }

    public List<Feature> getSelectedFeatures() {
        return SelectionTools.getSelectedFeatures(this.context);
    }

    /**
     *
     * @param context current PlugIn context
     * @return the geometry of the current fence, or null if there is currently
     * no fence
     */
    public static Geometry getFenceGeometry(PlugInContext context) {
        Layer fence = new FenceLayerFinder(context.getLayerViewPanel()).getLayer();
        if (fence == null) {
            return null;
        }
        FeatureCollectionWrapper fenceCollWrap = fence.getFeatureCollectionWrapper();
        Feature fencePolygon = (Feature) fenceCollWrap.getUltimateWrappee().getFeatures().get(0);
        return fencePolygon.getGeometry();
    }

    /**
     * @return the geometry of the current fence, or null if there is currently
     * no fence
     */
    public Geometry getFenceGeometry() {
        return SelectionTools.getFenceGeometry(this.context);
    }

    public List getFeaturesInFence() {
        Geometry fenceGeometry = this.getFenceGeometry();
        return this.getFeaturesInGeometry(fenceGeometry);
    }

    public List getFeaturesInGeometry(Geometry fenceGeometry) {
        ArrayList featsInFence = new ArrayList();
        List layers = this.context.getLayerManager().getVisibleLayers(false);

        Iterator iter = layers.iterator();
        Layer tmp;
        while (iter.hasNext()) {
            tmp = (Layer) iter.next();
            featsInFence.addAll(SelectionTools.getFeaturesInFenceInLayerAsList(tmp, fenceGeometry));
        }

        return featsInFence;
    }

    /**
     * Get a list of those features from the given layer that are included by
     * the given fence geometry.
     *
     * @param layer - Layer to search in
     * @param fenceGeometry - Geometry to search in
     */
    public static Feature[] getFeaturesInFenceInLayer(Layer layer, Geometry fenceGeometry) {
        return SelectionTools.getFeaturesOnTheSameSpot(layer, fenceGeometry, false);
    }

    /**
     * Get a list of those features from the given layer that are included by
     * the given fence geometry.
     *
     * @param layer - Layer to search in
     * @param fenceGeometry - Geometry to search in
     */
    public static List getFeaturesInFenceInLayerAsList(Layer layer, Geometry fenceGeometry) {
        Feature[] featureArray = SelectionTools.getFeaturesOnTheSameSpot(layer, fenceGeometry, false);
        ArrayList result = new ArrayList();

        for (int i = 0; i < featureArray.length; i++) {
            result.add(featureArray[i]);
        }

        return result;
    }

    /**
     * Get a list of features (a sub list of the given array) that are included
     * by the given fence geometry.
     *
     * @param featArray - Array of features to search in
     * @param fenceGeometry - Geometry to search in
     */
    public static Feature[] getFeaturesInFenceInLayer(Feature[] featArray, Geometry fenceGeometry) {
        return SelectionTools.getFeaturesOnTheSameSpot(featArray, fenceGeometry, false);
    }

    /**
     * Get a list of those features from the given layer that are included by
     * the given fence geometry.
     *
     * @param layer - Layer to search in
     * @param fenceGeometry - Geometry to search in
     * @param bothWays - sets if it's also a hit if the feature's geometry
     * includes the fence geometry
     */
    public static Feature[] getFeaturesOnTheSameSpot(Layer layer, Geometry fenceGeometry, boolean bothWays) {
        FeatureCollection featColl = layer.getFeatureCollectionWrapper().getUltimateWrappee();
        return SelectionTools.getFeaturesOnTheSameSpot(FeatureCollectionTools.FeatureCollection2FeatureArray(featColl), fenceGeometry, bothWays);
    }

    /**
     * Get a list of features (a sub list of the given array) that are included
     * by the given fence geometry.
     *
     * @param featArray - Array of features to search in
     * @param fenceGeometry - Geometry to search in
     * @param bothWays - sets if it's also a hit if the feature's geometry
     * includes the fence geometry
     */
    public static Feature[] getFeaturesOnTheSameSpot(Feature[] featArray, Geometry fenceGeometry, boolean bothWays) {
        ArrayList featuresInsideTheFence = new ArrayList();

//		Envelope fenceEnv = fenceGeometry.getEnvelopeInternal();
//		Envelope featEnv;
        Geometry featGeom = null;

        Feature feat;

        int numFeats = featArray.length;

        for (int i = 0; i < numFeats; i++) {
            feat = featArray[i];

            featGeom = feat.getGeometry();
            /*
             featEnv = featGeom.getEnvelopeInternal();
			
             // quick test, first:
             if (!fenceEnv.contains(featEnv) && !fenceEnv.intersects(featEnv) && !bothWays){
             continue;
             } else if (bothWays){
             if (!fenceEnv.contains(featEnv) && !fenceEnv.intersects(featGeom.getEnvelopeInternal()) && !featEnv.contains(fenceEnv) )
             continue;
             }
             */
            if (!fenceGeometry.disjoint(featGeom)) {
                featuresInsideTheFence.add(feat);
            } else if (bothWays) {
                if (!featGeom.disjoint(fenceGeometry)) {
                    featuresInsideTheFence.add(feat);
                }
            }
        }
        return (Feature[]) featuresInsideTheFence.toArray(new Feature[0]);
    }
}
