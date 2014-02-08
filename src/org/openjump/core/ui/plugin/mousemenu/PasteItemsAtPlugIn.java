package org.openjump.core.ui.plugin.mousemenu;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.openjump.core.ui.images.IconLoader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.osfac.dmt.I18N;
import com.osfac.dmt.feature.Feature;
import com.osfac.dmt.geom.CoordUtil;
import com.osfac.dmt.io.WKTReader;
import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.model.UndoableCommand;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.ui.GUIUtil;
import com.osfac.dmt.workbench.ui.LayerViewPanel;
import com.osfac.dmt.workbench.ui.SelectionManager;
import com.osfac.dmt.workbench.ui.plugin.FeatureInstaller;
import com.osfac.dmt.workbench.ui.plugin.clipboard.CollectionOfFeaturesTransferable;
import com.osfac.dmt.workbench.ui.plugin.clipboard.PasteItemsPlugIn;

public class PasteItemsAtPlugIn extends PasteItemsPlugIn {

	public static ImageIcon ICON = IconLoader.icon("shape_paste_point.png");	
	
    WKTReader reader = new WKTReader();
	private static final String PASTE_ITEMS_AT_POINT = I18N.get("org.openjump.core.ui.plugin.mousemenu.PasteItemsAtPlugIn.Paste-Items-At-Point");
	
    public void initialize(PlugInContext context) throws Exception
	    {     
    		WorkbenchContext workbenchContext = context.getWorkbenchContext();
	        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
	        JPopupMenu popupMenu = LayerViewPanel.popupMenu();
	        featureInstaller.addPopupMenuItem(popupMenu,
	            this, getNameWithMnemonic() + "{pos:10}",
	            false, this.getIcon(),
	            this.createEnableCheck(workbenchContext));
	    }
    
	public String getName() {
		return PASTE_ITEMS_AT_POINT;
	}

    public boolean execute(final PlugInContext context)
    throws Exception {
    reportNothingToUndoYet(context);

    Collection features;
    Transferable transferable = GUIUtil.getContents(Toolkit.getDefaultToolkit()
                                                           .getSystemClipboard());

    if (transferable.isDataFlavorSupported(
                CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR)) {
        features = (Collection) GUIUtil.getContents(Toolkit.getDefaultToolkit()
                                                           .getSystemClipboard())
                                       .getTransferData(CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR);
    } else {
        //Allow the user to paste features using WKT. [Bob Boseko]
        features = reader.read(new StringReader(
                    (String) transferable.getTransferData(
                        DataFlavor.stringFlavor))).getFeatures();
    }

    final SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();
    final Layer layer = context.getSelectedLayer(0);
    final Collection featureCopies = conform(features,
            layer.getFeatureCollectionWrapper().getFeatureSchema());
    Feature feature = ((Feature) featureCopies.iterator().next());
	Coordinate firstPoint = feature.getGeometry().getCoordinate();
	Coordinate cursorPt = context.getLayerViewPanel().getViewport().toModelCoordinate(
            context.getLayerViewPanel().getLastClickedPoint());
	Coordinate displacement = CoordUtil.subtract(cursorPt, firstPoint);
	moveAll(featureCopies,displacement);
    
    execute(new UndoableCommand(getName()) {
    	public void execute() {
    		layer.getFeatureCollectionWrapper().addAll(featureCopies);
    		selectionManager.clear();
    		selectionManager.getFeatureSelection().selectItems(layer, featureCopies);
    	}

    	public void unexecute() {
    		layer.getFeatureCollectionWrapper().removeAll(featureCopies);
    	}
    }, context);

    return true;
}

    private void moveAll( Collection featureCopies, Coordinate displacement) {
    	for (Iterator j = featureCopies.iterator(); j.hasNext();) {
    		Feature item = (Feature) j.next();
    		move(item.getGeometry(), displacement);
    		item.getGeometry().geometryChanged();
    	}
    }

    private void move(Geometry geometry, final Coordinate displacement) {
        geometry.apply(new CoordinateFilter() {
            public void filter(Coordinate coordinate) {
                //coordinate.setCoordinate(CoordUtil.add(coordinate, displacement));
                coordinate.x += displacement.x;
                coordinate.y += displacement.y;
           }
        });
    }
    
    public ImageIcon getIcon() {
        return ICON;
    }

}