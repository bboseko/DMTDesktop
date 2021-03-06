package com.osfac.dmt.qa.diff;

import com.osfac.dmt.I18N;
import com.osfac.dmt.feature.Feature;
import com.osfac.dmt.feature.FeatureCollection;
import com.osfac.dmt.feature.FeatureDataset;
import com.osfac.dmt.task.TaskMonitor;
import com.vividsolutions.jts.geom.*;
import java.util.*;

public class DiffGeometry {

    private FeatureCollection[] inputFC = new FeatureCollection[2];
    private TaskMonitor monitor;
    private DiffGeometryMatcher diffMatcher = new ExactGeometryMatcher();
    private boolean splitIntoComponents = false;
    /*
     private String sfeatures = I18N.get("com.osfac.dmt.qa.diff.DiffGeometry.features");
     private String sMatchingfeatures = I18N.get("com.osfac.dmt.qa.diff.DiffGeometry.Matching-features");
     */

    public DiffGeometry(FeatureCollection fc0, FeatureCollection fc1, TaskMonitor monitor) {
        inputFC[0] = fc0;
        inputFC[1] = fc1;
        this.monitor = monitor;
    }

    public void setNormalize(boolean normalizeGeometry) {
        diffMatcher = new NormalizedExactGeometryMatcher();
    }

    public void setSplitIntoComponents(boolean splitIntoComponents) {
        this.splitIntoComponents = splitIntoComponents;
    }

    public void setMatcher(DiffGeometryMatcher diffMatcher) {
        this.diffMatcher = diffMatcher;
    }

    public FeatureCollection[] diff() {
        return compute(inputFC[0], inputFC[1]);
    }

    /*
     private FeatureCollection[] OLDcompute(FeatureCollection fc0, FeatureCollection fc1)
     {
     FeatureCollection[] diffFC = new FeatureCollection[2];
     monitor.report("Matching features in dataset 1");
     diffFC[0] = findUnmatchedFeatures(fc0, fc1);
     monitor.report("Matching features in dataset 2");
     diffFC[1] = findUnmatchedFeatures(fc1, fc0);
     return diffFC;
     }
     */
    private FeatureCollection[] compute(FeatureCollection fc0, FeatureCollection fc1) {
        DiffGeometryIndex diffIndex = new DiffGeometryIndex(fc1, diffMatcher, splitIntoComponents);

        monitor.report(I18N.get("com.osfac.dmt.qa.diff.DiffGeometry.Matching-features"));
        FeatureCollection[] diffFC = new FeatureCollection[2];
        diffFC[0] = matchFeatures(fc0, diffIndex);
        diffFC[1] = new FeatureDataset(fc1.getFeatureSchema());
        diffFC[1].addAll(diffIndex.getUnmatchedFeatures());

        return diffFC;
    }

    private FeatureCollection matchFeatures(FeatureCollection fc0, DiffGeometryIndex diffIndex) {
        FeatureCollection noMatch = new FeatureDataset(fc0.getFeatureSchema());
        int count = 1;
        int totalItems = fc0.size();
        for (Iterator i = fc0.iterator(); i.hasNext();) {

            monitor.report(count++, totalItems, I18N.get("com.osfac.dmt.qa.diff.DiffGeometry.features"));

            Feature f = (Feature) i.next();
            Geometry geom = f.getGeometry();

            Collection list = DiffGeometryIndex.splitGeometry(geom, splitIntoComponents);
            for (Iterator j = list.iterator(); j.hasNext();) {
                Geometry g = (Geometry) j.next();
                if (!diffIndex.hasMatch(g)) {
                    noMatch.add(f);
                }
            }
        }
        return noMatch;
    }
}
