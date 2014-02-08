
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

package com.osfac.dmt.workbench.driver;

import com.osfac.dmt.workbench.model.Layer;
import com.osfac.dmt.workbench.ui.AbstractDriverPanel;
import com.osfac.dmt.workbench.ui.BasicFileDriverPanel;
import com.osfac.dmt.workbench.ui.ErrorHandler;


//<<TODO:DESIGN>> Convert to interface. There's little reason for this code to remain
//an abstract class. [Bob Boseko]
/**
 * @deprecated Use DataSourceQueryChooser instead
 */
public abstract class AbstractOutputDriver extends AbstractDriver {
    private BasicFileDriverPanel panel;

    public AbstractOutputDriver() {
    }

    /**
     * Called after the user has pressed OK on the DriverDialog.
     */
    public abstract void output(Layer layer) throws Exception;

    public AbstractDriverPanel getPanel() {
        return panel;
    }

    public void initialize(DriverManager driverManager,
        ErrorHandler errorHandler) {
        super.initialize(driverManager, errorHandler);
        panel = driverManager.getSharedSaveBasicFileDriverPanel();
    }
}