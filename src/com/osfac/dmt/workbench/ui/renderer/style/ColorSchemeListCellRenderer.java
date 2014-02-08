package com.osfac.dmt.workbench.ui.renderer.style;

import com.osfac.dmt.util.CollectionUtil;
import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.ui.ColorPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.*;

public class ColorSchemeListCellRenderer
        extends JPanel
        implements ListCellRenderer {

    private ColorPanel colorPanel1 = new ColorPanel();
    private ColorPanel colorPanel2 = new ColorPanel();
    private ColorPanel colorPanel3 = new ColorPanel();
    private ColorPanel colorPanel4 = new ColorPanel();
    private ColorPanel colorPanel5 = new ColorPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JLabel label = new JLabel();

    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        String name = (String) value;
        Collection colors = colorScheme(name).getColors();
        Iterator i = CollectionUtil.stretch(colors, new ArrayList(), 5).iterator();
        label.setText("(" + colors.size() + ") " + name);
        color(colorPanel1, (Color) i.next());
        color(colorPanel2, (Color) i.next());
        color(colorPanel3, (Color) i.next());
        color(colorPanel4, (Color) i.next());
        color(colorPanel5, (Color) i.next());
        if (isSelected) {
            label.setForeground(list.getSelectionForeground());
            label.setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            setBackground(list.getSelectionBackground());
        } else {
            label.setForeground(list.getForeground());
            label.setBackground(list.getBackground());
            setForeground(list.getForeground());
            setBackground(list.getBackground());
        }
        return this;
    }

    protected ColorScheme colorScheme(String name) {
        return ColorScheme.create(name);
    }

    private void color(ColorPanel colorPanel, Color fillColor) {
        color(colorPanel, fillColor, Layer.defaultLineColor(fillColor));
    }

    protected void color(ColorPanel colorPanel, Color fillColor, Color lineColor) {
        colorPanel.setFillColor(fillColor);
        colorPanel.setLineColor(lineColor);
    }

    public ColorSchemeListCellRenderer() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.setLayout(gridBagLayout1);
        label.setText("jLabel1");
        this.add(
                colorPanel1,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 2, 0, 0), 0, 0));
        this.add(
                colorPanel2,
                new GridBagConstraints(
                1,
                0,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
                colorPanel3,
                new GridBagConstraints(
                2,
                0,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
                colorPanel4,
                new GridBagConstraints(
                3,
                0,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
                colorPanel5,
                new GridBagConstraints(
                4,
                0,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
                label,
                new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 0), 0, 0));
    }

    /**
     * Workaround for bug 4238829 in the Java bug database: "JComboBox
     * containing JPanel fails to display selected item at creation time"
     */
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        validate();
    }
}
