package com.osfac.dmt.workbench.ui.snap;

import com.jidesoft.dialog.ButtonEvent;
import com.jidesoft.dialog.ButtonNames;
import com.osfac.dmt.I18N;
import com.osfac.dmt.setting.SettingOptionsDialog;
import com.osfac.dmt.util.Blackboard;
import com.osfac.dmt.workbench.ui.ValidatingTextField;
import com.vividsolutions.jts.util.Assert;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class SnapOptionsPanel extends JPanel {

    private BorderLayout borderLayout1 = new BorderLayout();
    private JPanel jPanel1 = new JPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JPanel jPanel2 = new JPanel();
    private GridBagLayout gridBagLayout2 = new GridBagLayout();
    private TitledBorder titledBorder1;
    private JCheckBox snapToFeaturesCheckBox = new JCheckBox();
    private JCheckBox snapToVerticesCheckBox = new JCheckBox();
    private JPanel jPanel3 = new JPanel();
    private JPanel jPanel4 = new JPanel();
    private JCheckBox snapToGridCheckBox = new JCheckBox();
    private GridBagLayout gridBagLayout3 = new GridBagLayout();
    private JPanel jPanel5 = new JPanel();
    private Border border2;
    private TitledBorder titledBorder2;
    private GridBagLayout gridBagLayout4 = new GridBagLayout();
    private GridBagLayout gridBagLayout6 = new GridBagLayout();
    private JTextField gridSizeTextField = new JTextField(7);
    private JPanel jPanel7 = new JPanel();
    private JRadioButton showGridDotsRadioButton = new JRadioButton();
    private JRadioButton showGridLinesRadioButton = new JRadioButton();
    private JPanel jPanel6 = new JPanel();
    private Blackboard blackboard;
    private ButtonGroup buttonGroup = new ButtonGroup();
    private ValidatingTextField toleranceTextField = new ValidatingTextField("",
            3, SwingConstants.RIGHT,
            new ValidatingTextField.BoundedIntValidator(0, 100),
            ValidatingTextField.DUMMY_CLEANER);
    private JCheckBox showGridCheckBox = new JCheckBox();
    private JPanel tolerancePanel = new JPanel();
    private GridBagLayout gridBagLayout5 = new GridBagLayout();
    private JLabel toleranceLabel = new JLabel();
    private JLabel toleranceUnitsLabel = new JLabel();
    private JLabel showGridUnitsLabel = new JLabel();

    public SnapOptionsPanel(Blackboard blackboard) {
        this.blackboard = blackboard;

        try {
            jbInit();
        } catch (Exception e) {
            Assert.shouldNeverReachHere(e.toString());
        }

        buttonGroup.add(showGridDotsRadioButton);
        buttonGroup.add(showGridLinesRadioButton);
        snapToFeaturesCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateEnabled();
                if (start) {
                    SettingOptionsDialog.page.fireButtonEvent(ButtonEvent.ENABLE_BUTTON, ButtonNames.APPLY);
                }
            }
        });
    }

    private void updateEnabled() {
        snapToVerticesCheckBox.setEnabled(!snapToFeaturesCheckBox.isSelected());
        gridSizeTextField.setEnabled(showGridCheckBox.isSelected());
        showGridDotsRadioButton.setEnabled(showGridCheckBox.isSelected());
        showGridLinesRadioButton.setEnabled(showGridCheckBox.isSelected());
    }

    public String validateInput() {
        String errorMessage = "\"" + gridSizeTextField.getText()
                + "\" " + I18N.get("ui.snap.SnapOptionsPanel.is-not-a-valid-grid-size");

        try {
            if (Double.parseDouble(gridSizeTextField.getText()) <= 0) {
                return errorMessage;
            }
        } catch (NumberFormatException e) {
            return errorMessage;
        }

        return null;
    }

    public void okPressed() {
        blackboard.put(SnapToVerticesPolicy.ENABLED_KEY,
                snapToVerticesCheckBox.isSelected());
        if (!toleranceTextField.getText().isEmpty()) {
            SnapManager.setToleranceInPixels(toleranceTextField.getInteger(), blackboard);
        }
        blackboard.put(SnapToFeaturesPolicy.ENABLED_KEY,
                snapToFeaturesCheckBox.isSelected());
        blackboard.put(SnapToGridPolicy.ENABLED_KEY,
                snapToGridCheckBox.isSelected());
        blackboard.put(SnapToGridPolicy.GRID_SIZE_KEY,
                Double.parseDouble(gridSizeTextField.getText()));
        blackboard.put(GridRenderer.DOTS_ENABLED_KEY,
                showGridDotsRadioButton.isSelected());
        blackboard.put(GridRenderer.LINES_ENABLED_KEY,
                showGridLinesRadioButton.isSelected());
        blackboard.put(GridRenderer.ENABLED_KEY, showGridCheckBox.isSelected());
    }

    public void init() {
        snapToVerticesCheckBox.setSelected(blackboard.get(
                SnapToVerticesPolicy.ENABLED_KEY, false));
        toleranceTextField.setText(""
                + SnapManager.getToleranceInPixels(blackboard));
        snapToFeaturesCheckBox.setSelected(blackboard.get(
                SnapToFeaturesPolicy.ENABLED_KEY, false));
        snapToGridCheckBox.setSelected(blackboard.get(
                SnapToGridPolicy.ENABLED_KEY, false));
        gridSizeTextField.setText(""
                + blackboard.get(SnapToGridPolicy.GRID_SIZE_KEY, 20d));
        showGridCheckBox.setSelected(blackboard.get(GridRenderer.ENABLED_KEY,
                false));
        showGridDotsRadioButton.setSelected(blackboard.get(
                GridRenderer.DOTS_ENABLED_KEY, false));
        showGridLinesRadioButton.setSelected(blackboard.get(
                GridRenderer.LINES_ENABLED_KEY, true));
        updateEnabled();
    }

    private void jbInit() throws Exception {
        showGridCheckBox.setFocusable(false);
        showGridCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (start) {
                    SettingOptionsDialog.page.fireButtonEvent(ButtonEvent.ENABLE_BUTTON, ButtonNames.APPLY);
                }
            }
        });
        showGridLinesRadioButton.setFocusable(false);
        showGridDotsRadioButton.setFocusable(false);
        showGridDotsRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (start) {
                    SettingOptionsDialog.page.fireButtonEvent(ButtonEvent.ENABLE_BUTTON, ButtonNames.APPLY);
                }
            }
        });
        snapToGridCheckBox.setFocusable(false);
        snapToGridCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (start) {
                    SettingOptionsDialog.page.fireButtonEvent(ButtonEvent.ENABLE_BUTTON, ButtonNames.APPLY);
                }
            }
        });
        snapToVerticesCheckBox.setFocusable(false);
        snapToVerticesCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (start) {
                    SettingOptionsDialog.page.fireButtonEvent(ButtonEvent.ENABLE_BUTTON, ButtonNames.APPLY);
                }
            }
        });
        snapToFeaturesCheckBox.setFocusable(false);
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                I18N.get("ui.snap.SnapOptionsPanel.snapping"));
        border2 = BorderFactory.createEtchedBorder(Color.white,
                new Color(148, 145, 140));
        titledBorder2 = new TitledBorder(border2, I18N.get("ui.snap.SnapOptionsPanel.grid-display"));
        this.setLayout(borderLayout1);
        jPanel1.setLayout(gridBagLayout1);
        jPanel2.setLayout(gridBagLayout2);
        jPanel2.setBorder(titledBorder1);
        snapToFeaturesCheckBox.setText(I18N.get("ui.snap.SnapOptionsPanel.snap-to-vertices-and-lines"));
        snapToVerticesCheckBox.setText(I18N.get("ui.snap.SnapOptionsPanel.snap-to-vertices"));
        snapToGridCheckBox.setText(I18N.get("ui.snap.SnapOptionsPanel.snap-to-grid"));
        jPanel4.setLayout(gridBagLayout3);
        jPanel5.setBorder(titledBorder2);
        jPanel5.setLayout(gridBagLayout4);
        gridSizeTextField.setText("20");
        gridSizeTextField.setHorizontalAlignment(SwingConstants.TRAILING);
        jPanel7.setLayout(gridBagLayout6);
        showGridDotsRadioButton.setSelected(true);
        showGridDotsRadioButton.setText(I18N.get("ui.snap.SnapOptionsPanel.show-grid-as-dots"));
        showGridLinesRadioButton.setText(I18N.get("ui.snap.SnapOptionsPanel.show-grid-as-lines"));
        showGridCheckBox.setToolTipText("");
        showGridCheckBox.setText(I18N.get("ui.snap.SnapOptionsPanel.show-grid-size"));
        showGridCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showGridCheckBox_actionPerformed(e);
            }
        });
        tolerancePanel.setLayout(gridBagLayout5);
        toleranceLabel.setText(I18N.get("ui.snap.SnapOptionsPanel.tolerance"));
        toleranceUnitsLabel.setText(I18N.get("ui.snap.SnapOptionsPanel.pixels"));
        showGridUnitsLabel.setText(I18N.get("ui.snap.SnapOptionsPanel.model-units"));
        this.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jPanel2,
                new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 10, 10), 0, 0));
        jPanel2.add(snapToFeaturesCheckBox,
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(snapToVerticesCheckBox,
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(jPanel3,
                new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(jPanel4,
                new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel4.add(snapToGridCheckBox,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(tolerancePanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        tolerancePanel.add(toleranceLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
        tolerancePanel.add(toleranceTextField, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
        tolerancePanel.add(toleranceUnitsLabel, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(jPanel5,
                new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 10, 10, 10), 0, 0));
        jPanel5.add(jPanel7,
                new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel7.add(gridSizeTextField,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
        jPanel7.add(showGridCheckBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel5.add(showGridDotsRadioButton,
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        jPanel5.add(showGridLinesRadioButton,
                new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(jPanel6,
                new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 0), 0, 0));
        jPanel7.add(showGridUnitsLabel, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        start = true;
    }

    void showGridCheckBox_actionPerformed(ActionEvent e) {
        updateEnabled();
    }
    boolean start = false;
}