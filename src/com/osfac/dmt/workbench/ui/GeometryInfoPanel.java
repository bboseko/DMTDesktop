
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.osfac.dmt.workbench.ui;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.osfac.dmt.workbench.model.Layer;


public class GeometryInfoPanel extends JPanel implements InfoModelListener {
    //Need the scrollpane on the panel because the scrollpane has to go around
    //the editorpane itself -- otherwise the editorpane won't wrap. [Bob Boseko]
    private BorderLayout borderLayout1 = new BorderLayout();
    private JEditorPane editorPane = new JEditorPane();
    private FeatureInfoWriter writer = new FeatureInfoWriter();
    private InfoModel model;
    private JScrollPane scrollPane = new JScrollPane();
    private FeatureInfoWriter.Writer geometryWriter;
    private FeatureInfoWriter.Writer attributeWriter;

    public GeometryInfoPanel(InfoModel model) {
        setModel(model);

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        editorPane.setEditable(false);
        editorPane.setText("jEditorPane1");
        editorPane.setContentType("text/html");
        this.setLayout(borderLayout1);
        this.add(scrollPane, BorderLayout.CENTER);
        scrollPane.getViewport().add(editorPane, null);
    }

    public void setModel(InfoModel model) {
        this.model = model;
        model.addListener(this);
    }

    public void layerAdded(LayerTableModel layerTableModel) {
        updateText();
        layerTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    updateText();
                }
            });
    }

    public void layerRemoved(LayerTableModel layerTableModel) {
        updateText();
    }

    public void updateText() {
        editorPane.setText(writer.writeGeom(layerToFeaturesMap(),
                geometryWriter, attributeWriter));
        editorPane.setCaretPosition(0);
    }

    private Map layerToFeaturesMap() {
        HashMap layerToFeaturesMap = new HashMap();

        for (Iterator i = model.getLayers().iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            layerToFeaturesMap.put(layer,
                model.getTableModel(layer).getFeatures());
        }

        return layerToFeaturesMap;
    }

    public void setGeometryWriter(
        FeatureInfoWriter.Writer geometryWriter) {
        this.geometryWriter = geometryWriter;
    }

    public void setAttributeWriter(
        FeatureInfoWriter.Writer attributeWriter) {
        this.attributeWriter = attributeWriter;
    }
}
