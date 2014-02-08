package org.openjump.core.ui.plugin.view;

import com.osfac.dmt.I18N;
import com.osfac.dmt.feature.AttributeType;
import com.osfac.dmt.feature.Feature;
import com.osfac.dmt.feature.FeatureCollectionWrapper;
import com.osfac.dmt.feature.FeatureSchema;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.model.LayerManager;
import com.osfac.dmt.workbench.plugin.AbstractPlugIn;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.ui.LayerViewPanel;
import com.osfac.dmt.workbench.ui.TaskFrame;
import com.osfac.dmt.workbench.ui.ToolTipWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.UniqueCoordinateArrayFilter;
import java.awt.Component;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import org.openjump.core.geomutils.GeoUtils;

public class MapToolTipPlugIn extends AbstractPlugIn {

    private class GeoData {

        public String type;
        public double distance;
        public int side;
        public double length;
        public double angle;
        public double area;

        public void set(GeoData geoData) {
            this.type = geoData.type;
            this.distance = geoData.distance;
            this.side = geoData.side;
            this.length = geoData.length;
            this.angle = geoData.angle;
            this.area = geoData.area;
        }
    }
    PlugInContext gContext;
    final static String sErrorSeeOutputWindow = I18N.get("org.openjump.core.ui.plugin.view.MapToolTipPlugIn.Error-See-Output-Window");
    final static String sPoint = I18N.get("org.openjump.core.ui.plugin.view.MapToolTipPlugIn.Point");
    final static String sSide = I18N.get("org.openjump.core.ui.plugin.view.MapToolTipPlugIn.Side");
    final static String sLength = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.length");
    final static String sAngle = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.angle");
    final static String sDegrees = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.degrees");
    final static String sNoData = I18N.get("org.openjump.core.ui.plugin.view.MapToolTipPlugIn.No-Data");
    final static String sArea = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.area");
    final static String pictureSuffix = "_p";
    private MouseMotionAdapter mouseMotionAdapter =
            new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    if (gContext.getWorkbenchContext().getLayerViewPanel() == null) {
                        return;
                    }
                    ToolTipWriter toolTipWriter = new ToolTipWriter(gContext.getWorkbenchContext().getLayerViewPanel());
                    toolTipWriter.setEnabled(gContext.getWorkbenchContext().getLayerViewPanel().getToolTipWriter().isEnabled());
                    String fid = toolTipWriter.write("{FID}", e.getPoint());

                    if (fid != null) {
                        String toolTipText = getData(Integer.parseInt(fid), e.getPoint());
                        gContext.getWorkbenchContext().getLayerViewPanel().setToolTipText(toolTipText);
                    }
                }
            };

    public void initialize(final PlugInContext context) throws Exception {
        gContext = context;
        context.getWorkbenchFrame().getDesktopPane().addContainerListener(
                new ContainerListener() {
                    public void componentAdded(ContainerEvent e) {
                        Component child = e.getChild();
                        if (child.getClass().getName().equals("com.osfac.dmt.workbench.ui.TaskFrame")) {
                            ((TaskFrame) child).getLayerViewPanel().addMouseMotionListener(mouseMotionAdapter);
                        }
                    }

                    public void componentRemoved(ContainerEvent e) {
                        Component child = e.getChild();
                        if (child.getClass().getName().equals("com.osfac.dmt.workbench.ui.TaskFrame")) {
                            ((TaskFrame) child).getLayerViewPanel().removeMouseMotionListener(mouseMotionAdapter);
                        }
                    }
                });
    }

    public boolean execute(PlugInContext context) throws Exception {
        try {
            return true;
        } catch (Exception e) {
            context.getWorkbenchFrame().warnUser(sErrorSeeOutputWindow);
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame().getOutputFrame().addText("MapToolTipPlugIn Exception:" + e.toString());
            return false;
        }
    }

    private String getData(int fID, Point2D mouseLocation) {
        int maxLinesOfData = 10;
        String dataText = "<html>";
        LayerViewPanel panel = gContext.getWorkbenchContext().getLayerViewPanel();
        if (panel == null) {
            return "";
        }
        LayerManager layerManager = panel.getLayerManager();
        List layerList = layerManager.getVisibleLayers(false);
        for (Iterator i = layerList.iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            FeatureSchema featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
            int numAttribs = featureSchema.getAttributeCount();
            FeatureCollectionWrapper featureCollection = layer.getFeatureCollectionWrapper();
            List featureList = featureCollection.getFeatures();

            //for each layer iterate thru featureList
            for (Iterator j = featureList.iterator(); j.hasNext();) {
                Feature feature = (Feature) j.next();
                int fid = feature.getID();

                if (fid == fID) {
                    Geometry geo = feature.getGeometry();
                    try {
                        Coordinate coord = panel.getViewport().toModelCoordinate(mouseLocation);
                        dataText += getGeoData(geo, coord);
                    } catch (NoninvertibleTransformException e) {
                        //let it go
                    }

                    int NumLinesOfData = 1;

                    for (int num = 0; num < numAttribs; num++) {
                        AttributeType type = featureSchema.getAttributeType(num);

                        if (type == AttributeType.STRING) {
                            String name = featureSchema.getAttributeName(num);
                            String data = feature.getString(name).trim();
                            if (name.endsWith(pictureSuffix)) {
                                data = "<img src=\"file:///" + data + "\">";
                            }
                            if ((!data.equals("")) && (NumLinesOfData < maxLinesOfData)) {
                                dataText += "<br>" + name + ": " + data;
                                NumLinesOfData++;
                            }
                        }
                    }
                    return dataText + "</html>";
                }
            }
        }
        dataText += sNoData + "</html>";
        return dataText;
    }

    private String getGeoData(Geometry geo, Coordinate mousePt) {
        GeoData geoData = getClosest(geo, mousePt);
        DecimalFormat df2 = new DecimalFormat("##0.0#");
        DecimalFormat df3 = new DecimalFormat("###,###,##0.0##");
        String geoText;
        if (geoData.area > 0) {
            geoText = geoData.type + ": " + sSide + ": " + geoData.side + ", " + sLength + ": " + df3.format(geoData.length) + ", " + sAngle + ": " + df2.format(geoData.angle) + " " + sDegrees + ", " + sArea + ": " + df2.format(geoData.area);
        } else {
            geoText = geoData.type + ": " + sSide + ": " + geoData.side + ", " + sLength + ": " + df3.format(geoData.length) + ", " + sAngle + ": " + df2.format(geoData.angle) + " " + sDegrees;
        }
        if (geoData.type.equals("Point")) {
            geoText = "Point";
        }
        return geoText;
    }

    private GeoData getClosest(Geometry geo, Coordinate mousePt) {
        GeoData geoData;

        if ((geo.getGeometryType().equals("GeometryCollection"))
                || (geo.getGeometryType().equals("MultiPoint"))
                || (geo.getGeometryType().equals("MultiLineString"))
                || (geo.getGeometryType().equals("MultiPolygon"))) {
            geoData = getClosest(((GeometryCollection) geo).getGeometryN(0), mousePt);
            for (int i = 1; i < ((GeometryCollection) geo).getNumGeometries(); i++) {
                GeoData geoData2 = getClosest(((GeometryCollection) geo).getGeometryN(i), mousePt);
                if (geoData2.distance < geoData.distance) {
                    geoData.set(geoData2);
                }
            }
        } else {
            geo.getCoordinates();
            CoordinateList coords = new CoordinateList();
            UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();
            geo.apply(filter);
            coords.add(filter.getCoordinates(), false);

            //need to do this since UniqueCoordinateArrayFilter keeps the poly from being closed
            if ((geo instanceof Polygon) || (geo instanceof LinearRing)) {
                coords.add(coords.getCoordinate(0));
            }

            int maxIndex = coords.size() - 1;
            int side = 1;
            double length = 0;
            double angle = 0;
            Coordinate p0;
            Coordinate p1;
            double distToClosestSide = mousePt.distance(coords.getCoordinate(0));

            if (coords.size() > 1) {
                p0 = coords.getCoordinate(0);
                p1 = coords.getCoordinate(1);
                length = p0.distance(p1);
                angle = GeoUtils.getBearing180(p0, p1);
                distToClosestSide = GeoUtils.getDistance(mousePt, p0, p1);
            }

            for (int i = 1; i < maxIndex; i++) {
                p0 = coords.getCoordinate(i);
                p1 = coords.getCoordinate(i + 1);
                double distToSide = GeoUtils.getDistance(mousePt, p0, p1);

                if (distToSide < distToClosestSide) {
                    side = i + 1;
                    length = p0.distance(p1);
                    angle = GeoUtils.getBearing180(p0, p1);
                    distToClosestSide = distToSide;
                }
            }
            geoData = new GeoData();
            geoData.type = geo.getGeometryType();
            geoData.side = side;
            geoData.length = length;
            geoData.angle = angle;
            geoData.distance = distToClosestSide;
            geoData.area = geo.getArea();
        }
        return geoData;
    }
}
// this code shows how to add an ItemListener to a menu item
// so that you know when a menu item changes
//    private ItemListener itemListener =
//    new ItemListener()
//    {
//        public void itemStateChanged(ItemEvent e)
//        {
////                   String chk = context.getFeatureInstaller().menuBar().getMenu(2).getItem(13).getText();
//            java.awt.Toolkit.getDefaultToolkit().beep();
//        }
//    };
// add this to the initializer
//        context.getFeatureInstaller().menuBar().getMenu(2).getItem(13).addItemListener(itemListener);
