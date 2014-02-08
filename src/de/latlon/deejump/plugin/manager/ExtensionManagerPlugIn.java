/* This file is *not* under GPL or any other public license
 * Copyright 2005 Ugo Taddei 
 */
package de.latlon.deejump.plugin.manager;

import com.osfac.dmt.I18N;
import com.osfac.dmt.task.TaskMonitor;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.plugin.ThreadedBasePlugIn;
import com.osfac.dmt.workbench.ui.MenuNames;

public class ExtensionManagerPlugIn extends ThreadedBasePlugIn {
    
    private ExtensionManagerDialog managerDialog;
    
    public ExtensionManagerPlugIn() {
        //nuffin to do
    }

    public String getName(){
    	return I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Extension-Manager");
    }
    
    public boolean execute( PlugInContext context ) throws Exception {
        if (managerDialog == null) {
            managerDialog = new ExtensionManagerDialog(
                    context.getWorkbenchFrame(), 
                    context.getWorkbenchContext(),
                    "http://jump-pilot.sourceforge.net/download/"
                    //"file:///e:/proj/openjump/plugins/"
            );
        }
        managerDialog.setVisible( true );
        return managerDialog.isOkClicked();
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        managerDialog.updateExtensions( monitor );
        context.getWorkbenchFrame().setStatusMessage( I18N.get("deejump.pluging.manager.ExtensionManagerPlugIn.Plug-ins-will-only-be-removed-after-next-start"));
    }
    
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(
            // [Michael Michaud 2007-03-23] Change MenuNames.TOOLS to MenuNames.CUSTOMIZE
            this, MenuNames.CUSTOMIZE,
    		this.getName(), null,
    		null);
    }

}
