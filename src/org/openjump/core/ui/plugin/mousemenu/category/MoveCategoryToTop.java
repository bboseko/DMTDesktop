package org.openjump.core.ui.plugin.mousemenu.category;

import com.osfac.dmt.I18N;
import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.model.Category;
import com.osfac.dmt.workbench.plugin.AbstractPlugIn;
import com.osfac.dmt.workbench.plugin.EnableCheckFactory;
import com.osfac.dmt.workbench.plugin.MultiEnableCheck;
import com.osfac.dmt.workbench.plugin.PlugInContext;
import com.osfac.dmt.workbench.ui.GUIUtil;
import com.osfac.dmt.workbench.ui.plugin.FeatureInstaller;
import java.util.Collection;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

/**
 *
 *
 * @author Ole Rahn
 * @author FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * Project: PIROL (2005), Subproject: Daten- und Wissensmanagement
 *
 */
public class MoveCategoryToTop extends AbstractPlugIn {

    public boolean execute(PlugInContext context) throws Exception {
        CategoryMover cm = new CategoryMover(context);

        Collection cats = context.getLayerNamePanel().getSelectedCategories();

        if (cats.size() > 1 || cats.size() <= 0) {
            String s = I18N.get("org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryOneDown.Only-a-single-category-can-be-moved!");
            context.getWorkbenchFrame().warnUser(s);
            return false;
        }

        Object[] catsArray = cats.toArray();

        cm.moveCategoryToTop((Category) catsArray[0]);

        return true;
    }

    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("bullet_arrow_top.png"));
    }

    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryToTop.Move-Category-To-Top");
    }

    public void initialize(PlugInContext context) throws Exception {

        JPopupMenu layerNamePopupMenu = context.getWorkbenchContext().getWorkbench().getFrame().getCategoryPopupMenu();
        FeatureInstaller featInst = context.getFeatureInstaller();

        featInst.addPopupMenuItem(layerNamePopupMenu,
                this, this.getName() + "...", false,
                GUIUtil.toSmallIcon((ImageIcon) this.getIcon()),
                MoveCategoryToTop.createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {

        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();

        multiEnableCheck.add(checkFactory.createAtLeastNCategoriesMustBeSelectedCheck(1));
        multiEnableCheck.add(checkFactory.createExactlyNCategoriesMustBeSelectedCheck(1));

        return multiEnableCheck;
    }
}