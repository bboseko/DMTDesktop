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

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JButton;

public class GrayableIcon implements Icon {

    private Icon originalIcon;
    private Icon grayedIcon;
    private Icon currentIcon;          

    public GrayableIcon(Icon originalIcon) {
        this.originalIcon = originalIcon;
        grayedIcon = new JButton(originalIcon).getDisabledIcon();
        currentIcon = originalIcon;        
    }
    
    public void setGrayed(boolean grayed) {
        currentIcon = grayed ? grayedIcon : originalIcon;
    }
    
    public boolean isGrayed() { return currentIcon == grayedIcon; }

    public int getIconHeight() {
        return currentIcon.getIconHeight();
    }

    public int getIconWidth() {
        return currentIcon.getIconWidth();
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        currentIcon.paintIcon(c, g, x, y);
    }

}