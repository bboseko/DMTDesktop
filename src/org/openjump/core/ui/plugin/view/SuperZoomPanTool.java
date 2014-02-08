package org.openjump.core.ui.plugin.view;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.osfac.dmt.workbench.ui.LayerViewPanel;
import com.osfac.dmt.workbench.ui.Viewport;
import com.osfac.dmt.workbench.ui.cursortool.DragTool;
import com.osfac.dmt.workbench.ui.images.IconLoader;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import org.openjump.core.ui.util.ScreenScale;

/**
 * This tool have the following functions: - zoom in/out with left/right mouse
 * click - pan with left mouse drag - zoom in/out with mousewheel and then left
 * for zomm and right for cancel In wheelMode you can see the new area after
 * zooming. Also known as "Area of interest".
 *
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class SuperZoomPanTool extends DragTool implements MouseWheelListener {

    public static final double WHEEL_ZOOM_FACTOR = 1.15;
    public static final double ZOOM_IN_FACTOR = 2;
    public static Cursor CURSOR_ZOOM = createCursor(IconLoader.icon("MagnifyCursor.gif").getImage()); // TODO: new cursors
    public static Cursor CURSOR_PAN = createCursor(IconLoader.icon("Hand.gif").getImage());
    public static Cursor CURSOR_WHEEL = createCursor(IconLoader.icon("Hand.gif").getImage()); // TODO: third cursor
    private boolean dragging = false;
    private Image origImage;
    private Image auxImage = null;
    private Point mousePosition = null;
    private boolean wheelMode = false;
    private int mouseWheelCount;
    private double scale = 1d;
    private boolean mouseWheelListenerAdded = false;
    private boolean isAnimatingZoom = false;

    public SuperZoomPanTool() {
    }

    @Override
    public void activate(LayerViewPanel layerViewPanel) {
        super.activate(layerViewPanel);
        // during instanciation we do not have the LayerViewPanel, so we must add here the MouseWheelListener, but only once!
        if (!mouseWheelListenerAdded) {
            getWorkbench().getContext().getLayerViewPanel().addMouseWheelListener(this);
            mouseWheelListenerAdded = true;
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        // if the tool was deactivated, its better to reset all parameters
        wheelMode = false;
        getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_PAN);
        scale = 1;
        mouseWheelCount = 0;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        getWorkbench().getFrame().setStatusMessage("Left mousebutton -> zoom, right mousebutton cancel."); // TODO i18n
        int nclicks = e.getWheelRotation();  //negative is up/away
        mouseWheelCount = mouseWheelCount + nclicks;
        if (mouseWheelCount == 0) {
            scale = 1d;
        } else if (mouseWheelCount < 0) {
            scale = Math.abs(mouseWheelCount) * WHEEL_ZOOM_FACTOR;
        } else {
            scale = 1 / (mouseWheelCount * WHEEL_ZOOM_FACTOR);
        }
        wheelMode = true;
        // change the Cursor to reflect the new mode
        getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_WHEEL);
        getWorkbench().getFrame().setTimeMessage("1:" + (int) Math.floor(ScreenScale.getHorizontalMapScale(panel.getViewport()) / scale));
        try {
            redrawShape();
        } catch (Exception ex) {
            getPanel().getContext().handleThrowable(ex);
        }
    }

    @Override
    protected Shape getShape() throws Exception {
        // we need a Shape which represents the area of interest
        Dimension onScreenRectangleDimension = null;
        setColor(Color.magenta); // TODO und fill transparent;
        setStroke(new BasicStroke(1)); // TODO
        onScreenRectangleDimension = getPanel().getSize();
        onScreenRectangleDimension.setSize(onScreenRectangleDimension.getWidth() * 1 / scale, onScreenRectangleDimension.getHeight() * 1 / scale);
        if (wheelMode) {
            return new Rectangle((int) (mousePosition.getX() - onScreenRectangleDimension.getWidth() / 2), (int) (mousePosition.getY() - onScreenRectangleDimension.getHeight() / 2),
                    (int) onScreenRectangleDimension.getWidth(), (int) onScreenRectangleDimension.getHeight());
        } else {
            return null;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        try {
            mousePosition = e.getPoint();
            redrawShape();
        } catch (Exception ex) {
            getPanel().getContext().handleThrowable(ex);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        if (wheelMode) {
            // left button performs the zoom
            if (SwingUtilities.isLeftMouseButton(e)) {
                try {
                    getPanel().getViewport().zoomToViewPoint(new Point(e.getX(), e.getY()), scale);
                } catch (NoninvertibleTransformException ex) {
                }
                wheelMode = false;
                getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_PAN);
                scale = 1;
                mouseWheelCount = 0;
                try {
                    redrawShape();
                } catch (Exception ex) {
                    getPanel().getContext().handleThrowable(ex);
                }
            }

            // right button cancel the zoom
            if (SwingUtilities.isRightMouseButton(e)) {
                wheelMode = false;
                getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_PAN);
                scale = 1;
                mouseWheelCount = 0;
                try {
                    redrawShape();
                } catch (Exception ex) {
                    getPanel().getContext().handleThrowable(ex);
                }
            }
            getWorkbench().getFrame().setStatusMessage("");
            getWorkbench().getFrame().setTimeMessage("");
        } else {
            double zoomFactor = SwingUtilities.isRightMouseButton(e) ? (1 / ZOOM_IN_FACTOR) : ZOOM_IN_FACTOR;
            try {
                zoomAt(e.getPoint(), zoomFactor);
            } catch (Throwable t) {
                getPanel().getContext().handleThrowable(t);
            }
        }
    }

    @Override
    public boolean isRightMouseButtonUsed() {
//		return wheelMode;
        return true;
    }

    @Override
    public Cursor getCursor() {
        if (wheelMode) {
            return CURSOR_WHEEL;
        } else {
            return CURSOR_PAN;
        }
    }

    public Icon getIcon() {
        return IconLoader.icon("BigHand.gif"); // TODO: new icon
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        try {
            if (!dragging) {
                dragging = true;
                getPanel().getRenderingManager().setPaintingEnabled(false);
                cacheImage();
            }

            drawImage(e.getPoint());
            super.mouseDragged(e);
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging) {
            return;
        }

        getPanel().getRenderingManager().setPaintingEnabled(true);
        dragging = false;
        super.mouseReleased(e);
    }

    @Override
    protected Shape getShape(Point2D source, Point2D destination) {
        return null;
    }

    protected void gestureFinished() throws NoninvertibleTransformException {
        reportNothingToUndoYet();

        double xDisplacement = getModelDestination().x - getModelSource().x;
        double yDisplacement = getModelDestination().y - getModelSource().y;
        Envelope oldEnvelope = getPanel().getViewport().getEnvelopeInModelCoordinates();
        getPanel().getViewport().zoom(new Envelope(oldEnvelope.getMinX()
                - xDisplacement, oldEnvelope.getMaxX() - xDisplacement,
                oldEnvelope.getMinY() - yDisplacement,
                oldEnvelope.getMaxY() - yDisplacement));
    }

    /**
     * Creates a new Image if currImage doesn't exist or is the wrong size for
     * the panel.
     *
     * @param currImage an image buffer
     * @return a new image, or the existing one if it's compatible
     */
    public Image createImageIfNeeded(Image currImage) {
        if (currImage == null
                || currImage.getHeight(null) != getPanel().getHeight()
                || currImage.getWidth(null) != getPanel().getWidth()) {
            Graphics2D g = (Graphics2D) getPanel().getGraphics();
            Image img = g.getDeviceConfiguration().createCompatibleImage(
                    getPanel().getWidth(), getPanel().getHeight(), Transparency.OPAQUE);
            return img;

        }
        //return getPanel().createBlankPanelImage();
        return currImage;
    }

    public void cacheImage() {
        origImage = createImageIfNeeded(origImage);
        getPanel().paint(origImage.getGraphics());
    }

    private void drawImage(Point p) throws NoninvertibleTransformException {
        double dx = p.getX() - getViewSource().getX();
        double dy = p.getY() - getViewSource().getY();

        auxImage = createImageIfNeeded(auxImage);
        auxImage.getGraphics().setColor(Color.WHITE);
        auxImage.getGraphics().fillRect(0, 0, auxImage.getWidth(getPanel()), auxImage.getHeight(getPanel()));
        auxImage.getGraphics().drawImage(origImage, (int) dx, (int) dy, getPanel());
        getPanel().getGraphics().drawImage(auxImage, 0, 0, getPanel());
    }

    public boolean setAnimatingZoom(boolean animating) {
        boolean previousValue = isAnimatingZoom;
        isAnimatingZoom = animating;
        return previousValue;
    }

    public boolean getAnimatingZoom() {
        return isAnimatingZoom;
    }

    private void zoomAt(Point2D p, double zoomFactor)
            throws NoninvertibleTransformException {
        //getPanel().getViewport().zoomToViewPoint(p, zoomFactor);
        zoomAt(p, zoomFactor, getAnimatingZoom());
    }

    protected void zoomAt(Point2D p, double zoomFactor, boolean animatingZoom)
            throws NoninvertibleTransformException { //zoom while keeping cursor over same model point                         
        Viewport vp = getPanel().getViewport();
        Point2D zoomPoint = vp.toModelPoint(p);
        Envelope modelEnvelope = vp.getEnvelopeInModelCoordinates();
        Coordinate centre = modelEnvelope.centre();
        double width = modelEnvelope.getWidth();
        double height = modelEnvelope.getHeight();
        double dx = (zoomPoint.getX() - centre.x) / zoomFactor;
        double dy = (zoomPoint.getY() - centre.y) / zoomFactor;
        Envelope zoomModelEnvelope = new Envelope(
                zoomPoint.getX() - (0.5 * (width / zoomFactor)) - dx,
                zoomPoint.getX() + (0.5 * (width / zoomFactor)) - dx,
                zoomPoint.getY() - (0.5 * (height / zoomFactor)) - dy,
                zoomPoint.getY() + (0.5 * (height / zoomFactor)) - dy);
        vp.zoom(zoomModelEnvelope);
        //getPanel().getViewport().zoomToViewPoint(p, zoomFactor);
    }
}
