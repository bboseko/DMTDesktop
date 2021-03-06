package com.osfac.dmt.qa.diff;

import com.osfac.dmt.feature.FeatureCollection;
import com.osfac.dmt.feature.FeatureDatasetFactory;
import com.osfac.dmt.geom.LineSegmentUtil;
import com.vividsolutions.jts.geom.*;
import java.util.*;

/**
 * <code>DiffEdges</code> find all line segments in two FeatureCollections which
 * occur once only.
 */
public class DiffSegmentsWithTolerance {

    private static GeometryFactory geomFactory = new GeometryFactory();
    private FeatureCollection[] inputFC = new FeatureCollection[2];
    private double tolerance;
    //private FeatureCollection[] fc = new FeatureCollection[2];
    private List diffGeom[] = new ArrayList[2];

    public DiffSegmentsWithTolerance(FeatureCollection fc0, FeatureCollection fc1, double tolerance) {
        inputFC[0] = fc0;
        inputFC[1] = fc1;
        this.tolerance = tolerance;
    }

    public FeatureCollection[] diff() {
        compute(inputFC[0], inputFC[1]);
        FeatureCollection[] diffFC = new FeatureCollection[2];
        diffFC[0] = FeatureDatasetFactory.createFromGeometry(diffGeom[0]);
        diffFC[1] = FeatureDatasetFactory.createFromGeometry(diffGeom[1]);
        return diffFC;
    }

    private void compute(FeatureCollection fc0, FeatureCollection fc1) {
        diffGeom[0] = findUniqueSegmentGeometries(fc0, fc1);
        diffGeom[1] = findUniqueSegmentGeometries(fc1, fc0);
    }

    private List findUniqueSegmentGeometries(FeatureCollection fc0, FeatureCollection fc1) {
        List segGeomList = new ArrayList();
        UniqueSegmentsWithToleranceFinder finder = new UniqueSegmentsWithToleranceFinder(fc0, fc1);
        List segs = finder.findUniqueSegments(tolerance);
        for (Iterator i = segs.iterator(); i.hasNext();) {
            LineSegment seg = (LineSegment) i.next();
            segGeomList.add(LineSegmentUtil.asGeometry(geomFactory, seg));
        }
        return segGeomList;
    }
}
