package com.osfac.dmt.workbench.ui.plugin.wms;

import com.osfac.dmt.I18N;
import com.osfac.dmt.workbench.plugin.EnableCheck;
import com.osfac.dmt.workbench.ui.InputChangedListener;
import com.osfac.dmt.workbench.ui.TransparencyPanel;
import com.osfac.wms.WMService;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

public class EditWMSQueryPanel extends JPanel {

    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private MapLayerPanel mapLayerPanel = new MapLayerPanel();
    private JLabel srsLabel = new JLabel();
    private JLabel formatLabel = new JLabel();
    private DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    private JComboBox srsComboBox = new JComboBox(comboBoxModel);
    private DefaultComboBoxModel formatBoxModel = new DefaultComboBoxModel();
    private JComboBox formatComboBox = new JComboBox(formatBoxModel);
    private Border border1;
    private TransparencyPanel transparencyPanel = new TransparencyPanel();
    private JLabel transparencyLabel = new JLabel();
    private JLabel urlLabel = new JLabel();
    private JTextField urlTextField = new JTextField();
    private EnableCheck[] enableChecks =
            new EnableCheck[]{
        new EnableCheck() {
            public String check(JComponent component) {
                return mapLayerPanel.getChosenMapLayers().isEmpty()
                        ? I18N.get("ui.plugin.wms.EditWMSQueryPanel.at-least-one-wms-must-be-chosen")
                        : null;
            }
        }, new EnableCheck() {
    public String check(JComponent component) {
        return srsComboBox.getSelectedItem() == null
                ? MapLayerWizardPanel.NO_COMMON_SRS_MESSAGE
                : null;
    }
}
    };

    public EditWMSQueryPanel(
            WMService service,
            List initialChosenMapLayers,
            String initialSRS,
            int alpha, String format) {
        try {
            jbInit();
            String url = service.getServerUrl();
            if (url.endsWith("?") || url.endsWith("&")) {
                url = url.substring(0, url.length() - 1);
            }
            urlTextField.setText(url);
            mapLayerPanel.init(service, initialChosenMapLayers);

            updateComboBox();
            String srsName = SRSUtils.getName(initialSRS);
            srsComboBox.setSelectedItem(srsName);

            formatBoxModel.removeAllElements();
            for (String f : service.getCapabilities().getMapFormats()) {
                formatBoxModel.addElement(f);
            }

            formatComboBox.setSelectedItem(format);

            mapLayerPanel.add(new InputChangedListener() {
                public void inputChanged() {
                    updateComboBox();
                }
            });
            setAlpha(alpha);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public int getAlpha() {
        return 255 - transparencyPanel.getSlider().getValue();
    }

    private void setAlpha(int alpha) {
        transparencyPanel.getSlider().setValue(255 - alpha);
    }

    public String getSRS() {
        int index = srsComboBox.getSelectedIndex();
        String srsCode = (String) mapLayerPanel.commonSRSList().get(index);
        return srsCode;
    }

    public String getFormat() {
        return (String) formatComboBox.getSelectedItem();
    }

    /**
     * Method updateComboBox.
     */
    private void updateComboBox() {
        String selectedSRS = (String) srsComboBox.getSelectedItem();

        // this method does get called many times when no SRS are available here
        // this makes sure that the selected SRS stays selected when available
        if (mapLayerPanel.commonSRSList().isEmpty()) {
            return;
        }

        comboBoxModel.removeAllElements();

        for (Iterator i = mapLayerPanel.commonSRSList().iterator(); i.hasNext();) {
            String commonSRS = (String) i.next();
            String srsName = SRSUtils.getName(commonSRS);
            comboBoxModel.addElement(srsName);
        }

        //selectedSRS might no longer be in the combobox, in which case nothing will be selected. [Bob Boseko]
        srsComboBox.setSelectedItem(selectedSRS);
        if ((srsComboBox.getSelectedItem() == null)
                && (srsComboBox.getItemCount() > 0)) {
            srsComboBox.setSelectedIndex(0);
        }
    }

    void jbInit() throws Exception {
        border1 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        this.setLayout(gridBagLayout1);
        srsLabel.setText(I18N.get("ui.plugin.wms.EditWMSQueryPanel.coordinate-reference-system"));
        formatLabel.setText(I18N.get("ui.plugin.wms.SRSWizardPanel.image-format"));
        this.setBorder(border1);
        this.setToolTipText("");
        srsComboBox.setMinimumSize(new Dimension(125, 21));
        srsComboBox.setToolTipText("");
        transparencyLabel.setText(I18N.get("ui.plugin.wms.EditWMSQueryPanel.transparency"));
        urlLabel.setText("URL:");
        urlTextField.setBorder(null);
        urlTextField.setOpaque(false);
        urlTextField.setEditable(false);
        this.add(
                mapLayerPanel,
                new GridBagConstraints(
                1,
                2,
                5,
                1,
                1.0,
                1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(10, 0, 10, 0),
                0,
                0));
        this.add(
                srsLabel,
                new GridBagConstraints(
                1,
                3,
                2,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 10, 5),
                0,
                0));
        this.add(
                formatLabel,
                new GridBagConstraints(
                4,
                3,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5),
                0,
                0));
        this.add(
                formatComboBox,
                new GridBagConstraints(
                5,
                3,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5),
                0,
                0));
        this.add(
                srsComboBox,
                new GridBagConstraints(
                3,
                3,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 10, 0),
                0,
                0));
        this.add(
                transparencyPanel,
                new GridBagConstraints(
                3,
                6,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
                transparencyLabel,
                new GridBagConstraints(
                1,
                6,
                2,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
                urlLabel,
                new GridBagConstraints(
                1,
                0,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 5),
                0,
                0));
        this.add(
                urlTextField,
                new GridBagConstraints(
                2,
                0,
                2,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));
    }

    public List getChosenMapLayers() {
        return mapLayerPanel.getChosenMapLayers();
    }

    public EnableCheck[] getEnableChecks() {
        return enableChecks;
    }
}
