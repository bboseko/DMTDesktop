package org.openjump.sigle.plugin.joinTable;

import com.osfac.dmt.workbench.plugin.Extension;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import org.openjump.core.ui.plugin.tools.JoinTablePlugIn;

/**
 * @author Olivier BEDEL Laboratoire RESO UMR 6590 CNRS Bassin Versant du
 * Jaudy-Guindy-Bizien 27 oct. 2004 license Licence CeCILL
 * http://www.cecill.info/
 *
 */
public class JoinTableExtension extends Extension {

    public void configure(PlugInContext context) throws Exception {
        JoinTablePlugIn joinTablePlugIn = new JoinTablePlugIn();
        joinTablePlugIn.initialize(context);
    }
}
