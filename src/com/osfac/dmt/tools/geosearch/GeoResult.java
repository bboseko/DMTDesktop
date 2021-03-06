package com.osfac.dmt.tools.geosearch;

import com.jidesoft.grid.*;
import com.jidesoft.swing.CheckBoxList;
import com.osfac.dmt.CheckBoxHeader;
import com.osfac.dmt.Config;
import com.osfac.dmt.I18N;
import com.osfac.dmt.JImagePanel;
import com.osfac.dmt.download.DownloadData;
import com.osfac.dmt.feature.Feature;
import com.osfac.dmt.feature.FeatureCollection;
import com.osfac.dmt.feature.FeatureSchema;
import com.osfac.dmt.feature.FeatureUtil;
import com.osfac.dmt.form.DataRequestForm;
import com.osfac.dmt.io.WKTReader;
import com.osfac.dmt.setting.SettingKeyFactory;
import com.osfac.dmt.tools.AdvancedSelection;
import com.osfac.dmt.tools.search.ImagePreview;
import com.osfac.dmt.workbench.DMTWorkbench;
import com.osfac.dmt.workbench.model.LayerManagerProxy;
import com.osfac.dmt.workbench.model.UndoableCommand;
import com.osfac.dmt.workbench.ui.WorkbenchFrame;
import com.osfac.dmt.workbench.ui.plugin.WKTDisplayHelper;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

public class GeoResult extends JDialog {

    public GeoResult(java.awt.Frame parent, boolean modal, ArrayList<Integer> IDList) {
        super(parent, modal);
        this.IDList = IDList;
        tableModel = new MyTableModel();
        table = new SortableTable(tableModel);
//        table.setSortable(false);
        MyItemListener myItemListener = new MyItemListener();
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxHeader(myItemListener));
        table.getTableHeader().setReorderingAllowed(false);
        table.setTableStyleProvider(new RowStripeTableStyleProvider(new Color[]{Config.getColorFromKey(Config.pref.get(
                    SettingKeyFactory.FontColor.RStripe21Color1, "253, 253, 244")), Config.getColorFromKey(Config.pref.get(
                    SettingKeyFactory.FontColor.RStripe21Color2, "230, 230, 255"))}));
        table.setColumnResizable(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.getColumnModel().getColumn(0).setMinWidth(23);
        table.getColumnModel().getColumn(0).setMaxWidth(23);
        table.getColumnModel().getColumn(1).setMinWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(50);
        table.getColumnModel().getColumn(tableModel.getColumnCount() - 1).setMinWidth(70);
        table.getColumnModel().getColumn(tableModel.getColumnCount() - 1).setMaxWidth(70);
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (table.getRowCount() <= 0 || table.columnAtPoint(e.getPoint()) == 0) {
                    table.unsort();
                    table.setSortingEnabled(false);
                } else {
                    table.setSortingEnabled(true);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (table.getRowCount() <= 0 || table.columnAtPoint(e.getPoint()) == 0) {
                    table.unsort();
                    table.setSortingEnabled(false);
                } else {
                    table.setSortingEnabled(true);
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    TabPopupMenu.show(table, evt.getX(), evt.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (table.getSelectedColumn() == 0 && MIChKMapPreview.isSelected()) {
                    if (table.getValueAt(table.getSelectedRow(), 0).equals(true)) {
                        String wkt = getShapeOfImage(table.getValueAt(table.getSelectedRow(), 1).toString());
                        drawingShapeFromDB(wkt);
                        commandKey.put(table.getSelectedRow(), currentCommand);
                    } else {
                        if (WorkbenchFrame.previewLayer != null && commandList.get(commandKey.get(table.getSelectedRow())) != null) {
                            deleteShapeDrawn(commandList.get(commandKey.get(table.getSelectedRow())));
                        }
                    }
                }
                if (e.getClickCount() == 1 && table.getSelectedRow() != -1) {
                    searchPreview();
                }
                if (e.getClickCount() == 2 && !labBusyApercu.isVisible() && table.getSelectedColumn() != 0) {
                    new ImagePreview(DMTWorkbench.frame, true, pathPreviewFile, table.getValueAt(table.getSelectedRow(), 2).toString()).setVisible(true);
                }
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    searchPreview();
                }
            }
        });
        TableUtils.autoResizeAllColumns(table);
        TableUtils.autoResizeAllRows(table);
        Config.centerTableHeadAndBold(table);
        initComponents(I18N.DMTResourceBundle);
        panImage = new JImagePanel();
        panImage.setLayout(new BorderLayout());
        labBusyApercu = new JXBusyLabel();
        panImage.setAutoSize(true);
        panImage.setBackground(Color.white);
        labBusyApercu.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labBusyApercu.setForeground(Color.gray);
        labBusyApercu.setBusy(true);
        labBusyApercu.setFont(new Font("Tahoma", Font.PLAIN, 10));
        labBusyApercu.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        labBusyApercu.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        panImage.add(labBusyApercu);
        labBusyApercu.setVisible(false);
        scrollImage.setViewportView(panImage);
        scrollImage.getViewport().setBackground(Color.white);
        category = new DefaultListModel();
        year = new DefaultListModel();
        try {
            for (int i = 0; i < IDList.size(); i++) {
                IDs += IDList.get(i) + ",";
            }
            if (!IDs.isEmpty()) {
                IDs = IDs.substring(0, IDs.length() - 1);
            }
            ResultSet res = Config.con.createStatement().executeQuery("select distinct category_name from dmt_category join dmt_image "
                    + "on dmt_image.id_category = dmt_category.id_category "
                    + "where id_image in (" + IDs + ") order by category_name asc");
            while (res.next()) {
                category.addElement(res.getString(1));
            }
            res = Config.con.createStatement().executeQuery("select distinct year(date) from dmt_image where id_image in "
                    + "(" + IDs + ") order by year(date) asc");
            while (res.next()) {
                year.addElement(res.getString(1));
            }
            category.insertElementAt(CheckBoxList.ALL, 0);
            year.insertElementAt(CheckBoxList.ALL, 0);
            ListCategory.setModel(category);
            ListYear.setModel(year);
            ListCategory.getCheckBoxListSelectionModel().addSelectionInterval(0, ListCategory.getModel().getSize() - 1);
            ListYear.getCheckBoxListSelectionModel().addSelectionInterval(0, ListYear.getModel().getSize() - 1);
            fillTable("select distinct * from dmt_image where dmt_image.id_image in (" + IDs + ") "
                    + "order by dmt_image.id_image asc");
            this.setTitle(table.getRowCount() + " " + I18N.get("GeoResult.title"));
        } catch (SQLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error")
                    + "", ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
        df.setMaximumFractionDigits(2);
        ScrlAll.setViewportView(table);
        table.requestFocus();
        Timer timer = new Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.getRowCount() > 0) {
                    LabImageDisplayed.setText(I18N.get("Text.Images-displayed") + " : " + table.getRowCount());
                    int rowChecked = 0;
                    for (int i = 0; i < table.getRowCount(); i++) {
                        if (table.getValueAt(i, 0).equals(true)) {
                            rowChecked++;
                        }
                    }
                    LabImageChecked.setText(I18N.get("Text.Images-checked") + " : " + rowChecked);
                } else {
                    LabImageDisplayed.setText(I18N.get("Text.Images-displayed") + " : 0");
                    LabImageChecked.setText(I18N.get("Text.Images-checked") + " : 0");
                }
                submitEnabled();
                enabledPopupItems();
            }
        });
        timer.start();
        this.geoResult = this;
        if (Config.isLiteVersion() || Config.isSimpleUser()) {
            CBForm.setVisible(false);
        }
        this.setLocation(5, 5);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TabPopupMenu = new javax.swing.JPopupMenu();
        MISubmit = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        MIExcel = new javax.swing.JMenuItem();
        MIChKMapPreview = new javax.swing.JCheckBoxMenuItem();
        MIChecking = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        MIClearTable = new javax.swing.JMenuItem();
        MIChekAll = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        MIProperties = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        ScrlAll = new javax.swing.JScrollPane();
        LabImageDisplayed = new javax.swing.JLabel();
        BSubmit = new com.jidesoft.swing.JideButton();
        LabImageChecked = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        ListCategory = new com.jidesoft.swing.CheckBoxList();
        jScrollPane2 = new javax.swing.JScrollPane();
        ListYear = new com.jidesoft.swing.CheckBoxList();
        scrollImage = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        CBForm = new javax.swing.JCheckBox();

        MISubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/apply(5).png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("language/dmt_en"); // NOI18N
        MISubmit.setText(bundle.getString("GeoResult.MISubmit.text")); // NOI18N
        MISubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MISubmitActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MISubmit);
        TabPopupMenu.add(jSeparator4);

        MIExcel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/excel.png"))); // NOI18N
        MIExcel.setText(bundle.getString("GeoResult.MIExcel.text")); // NOI18N
        MIExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIExcelActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIExcel);

        MIChKMapPreview.setSelected(true);
        MIChKMapPreview.setText(bundle.getString("GeoResult.MIChKMapPreview.text")); // NOI18N
        MIChKMapPreview.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                MIChKMapPreviewItemStateChanged(evt);
            }
        });
        TabPopupMenu.add(MIChKMapPreview);

        MIChecking.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/ok.png"))); // NOI18N
        MIChecking.setText(bundle.getString("GeoResult.MIChecking.text")); // NOI18N
        MIChecking.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MICheckingActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIChecking);
        TabPopupMenu.add(jSeparator3);

        MIClearTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/editclear(7).png"))); // NOI18N
        MIClearTable.setText(bundle.getString("GeoResult.MIClearTable.text")); // NOI18N
        MIClearTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIClearTableActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIClearTable);

        MIChekAll.setText(bundle.getString("GeoResult.MIChekAll.text")); // NOI18N
        MIChekAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIChekAllActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIChekAll);
        TabPopupMenu.add(jSeparator2);

        MIProperties.setText(bundle.getString("GeoResult.MIProperties.text")); // NOI18N
        MIProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIPropertiesActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIProperties);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("GeoResult.title")); // NOI18N

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        ScrlAll.setBackground(new java.awt.Color(255, 255, 255));

        LabImageDisplayed.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        LabImageDisplayed.setForeground(new java.awt.Color(0, 0, 204));
        LabImageDisplayed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabImageDisplayed.setText(bundle.getString("GeoResult.LabImageDisplayed.text")); // NOI18N
        LabImageDisplayed.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        BSubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/apply(5).png"))); // NOI18N
        BSubmit.setText(bundle.getString("GeoResult.BSubmit.text")); // NOI18N
        BSubmit.setButtonStyle(1);
        BSubmit.setFocusable(false);
        BSubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BSubmitActionPerformed(evt);
            }
        });

        LabImageChecked.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        LabImageChecked.setForeground(new java.awt.Color(51, 153, 0));
        LabImageChecked.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabImageChecked.setText(bundle.getString("GeoResult.LabImageChecked.text")); // NOI18N
        LabImageChecked.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        ListCategory.setClickInCheckBoxOnly(false);
        ListCategory.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ListCategoryMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(ListCategory);

        ListYear.setClickInCheckBoxOnly(false);
        ListYear.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ListYearMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(ListYear);

        scrollImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                scrollImageMouseClicked(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 153, 0));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText(bundle.getString("GeoResult.jLabel2.text")); // NOI18N
        jLabel2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 153, 0));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText(bundle.getString("GeoResult.jLabel1.text")); // NOI18N
        jLabel1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 153, 0));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText(bundle.getString("GeoResult.jLabel3.text")); // NOI18N
        jLabel3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        CBForm.setSelected(true);
        CBForm.setText(bundle.getString("GeoResult.CBForm.text")); // NOI18N
        CBForm.setFocusable(false);
        CBForm.setOpaque(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ScrlAll)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(LabImageDisplayed, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LabImageChecked)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(CBForm, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(BSubmit, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(scrollImage, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {LabImageChecked, LabImageDisplayed});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3))
                        .addGap(1, 1, 1)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(scrollImage, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(1, 1, 1)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrlAll, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(LabImageChecked, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LabImageDisplayed)
                    .addComponent(BSubmit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CBForm))
                .addGap(6, 6, 6))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {LabImageChecked, LabImageDisplayed});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jScrollPane1, jScrollPane2, scrollImage});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void initComponents(java.util.ResourceBundle bundle) {

        TabPopupMenu = new javax.swing.JPopupMenu();
        MISubmit = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        MIExcel = new javax.swing.JMenuItem();
        MIChKMapPreview = new javax.swing.JCheckBoxMenuItem();
        MIChecking = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        MIClearTable = new javax.swing.JMenuItem();
        MIChekAll = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        MIProperties = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        ScrlAll = new javax.swing.JScrollPane();
        LabImageDisplayed = new javax.swing.JLabel();
        BSubmit = new com.jidesoft.swing.JideButton();
        LabImageChecked = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        ListCategory = new com.jidesoft.swing.CheckBoxList();
        jScrollPane2 = new javax.swing.JScrollPane();
        ListYear = new com.jidesoft.swing.CheckBoxList();
        scrollImage = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        CBForm = new javax.swing.JCheckBox();

        MISubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/apply(5).png"))); // NOI18N

        MISubmit.setText(bundle.getString("GeoResult.MISubmit.text")); // NOI18N
        MISubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MISubmitActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MISubmit);
        TabPopupMenu.add(jSeparator4);

        MIExcel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/excel.png"))); // NOI18N
        MIExcel.setText(bundle.getString("GeoResult.MIExcel.text")); // NOI18N
        MIExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIExcelActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIExcel);

        MIChKMapPreview.setSelected(true);
        MIChKMapPreview.setText(bundle.getString("GeoResult.MIChKMapPreview.text")); // NOI18N
        MIChKMapPreview.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                MIChKMapPreviewItemStateChanged(evt);
            }
        });
        TabPopupMenu.add(MIChKMapPreview);

        MIChecking.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/ok.png"))); // NOI18N
        MIChecking.setText(bundle.getString("GeoResult.MIChecking.text")); // NOI18N
        MIChecking.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MICheckingActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIChecking);
        TabPopupMenu.add(jSeparator3);

        MIClearTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/editclear(7).png"))); // NOI18N
        MIClearTable.setText(bundle.getString("GeoResult.MIClearTable.text")); // NOI18N
        MIClearTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIClearTableActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIClearTable);

        MIChekAll.setText(bundle.getString("GeoResult.MIChekAll.text")); // NOI18N
        MIChekAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIChekAllActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIChekAll);
        TabPopupMenu.add(jSeparator2);

        MIProperties.setText(bundle.getString("GeoResult.MIProperties.text")); // NOI18N
        MIProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIPropertiesActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIProperties);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("GeoResult.title")); // NOI18N

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        ScrlAll.setBackground(new java.awt.Color(255, 255, 255));

        LabImageDisplayed.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        LabImageDisplayed.setForeground(new java.awt.Color(0, 0, 204));
        LabImageDisplayed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabImageDisplayed.setText(bundle.getString("GeoResult.LabImageDisplayed.text")); // NOI18N
        LabImageDisplayed.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        BSubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/apply(5).png"))); // NOI18N
        BSubmit.setText(bundle.getString("GeoResult.BSubmit.text")); // NOI18N
        BSubmit.setButtonStyle(1);
        BSubmit.setFocusable(false);
        BSubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BSubmitActionPerformed(evt);
            }
        });

        LabImageChecked.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        LabImageChecked.setForeground(new java.awt.Color(51, 153, 0));
        LabImageChecked.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabImageChecked.setText(bundle.getString("GeoResult.LabImageChecked.text")); // NOI18N
        LabImageChecked.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        ListCategory.setClickInCheckBoxOnly(false);
        ListCategory.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ListCategoryMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(ListCategory);

        ListYear.setClickInCheckBoxOnly(false);
        ListYear.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ListYearMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(ListYear);

        scrollImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                scrollImageMouseClicked(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 153, 0));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText(bundle.getString("GeoResult.jLabel2.text")); // NOI18N
        jLabel2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 153, 0));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText(bundle.getString("GeoResult.jLabel1.text")); // NOI18N
        jLabel1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 153, 0));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText(bundle.getString("GeoResult.jLabel3.text")); // NOI18N
        jLabel3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        CBForm.setSelected(true);
        CBForm.setText(bundle.getString("GeoResult.CBForm.text")); // NOI18N
        CBForm.setFocusable(false);
        CBForm.setOpaque(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(ScrlAll)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(LabImageDisplayed, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(LabImageChecked)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(CBForm, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(BSubmit, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                .addComponent(scrollImage, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap()));

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[]{LabImageChecked, LabImageDisplayed});

        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel1)
                .addComponent(jLabel3))
                .addGap(1, 1, 1)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(scrollImage, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addGap(1, 1, 1)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrlAll, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(LabImageChecked, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(LabImageDisplayed)
                .addComponent(BSubmit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(CBForm))
                .addGap(6, 6, 6)));

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[]{LabImageChecked, LabImageDisplayed});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[]{jScrollPane1, jScrollPane2, scrollImage});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        pack();
    }// </editor-fold>

    private void ListCategoryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ListCategoryMouseClicked
        table.unsort();
        if (ListCategory.getCheckBoxListSelectedValues().length <= 0) {
            ListYear.setModel(new DefaultListModel());
            cleanTable();
        } else {
            try {
                year.clear();
                ResultSet res = Config.con.createStatement().executeQuery("select distinct year(date) from dmt_category join dmt_image on "
                        + "dmt_image.id_category = dmt_category.id_category where dmt_image.id_image in (" + IDs + ") " + criteriaSearch() + "order by year(date) asc");
                while (res.next()) {
                    year.addElement(res.getString(1));
                }
                ListYear.setModel(year);
                year.insertElementAt(CheckBoxList.ALL, 0);
                ListYear.getCheckBoxListSelectionModel().addSelectionInterval(0, ListYear.getModel().getSize() - 1);
                fillTable("select distinct * from dmt_category join dmt_image on dmt_image.id_category = "
                        + "dmt_category.id_category where dmt_image.id_image in (" + IDs + ") "
                        + "" + criteriaSearch() + "order by dmt_image.id_image asc");
            } catch (SQLException ex) {
                JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error"), ex.getMessage(), null, null, ex, Level.SEVERE, null));
            }
        }
    }//GEN-LAST:event_ListCategoryMouseClicked

    private void scrollImageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scrollImageMouseClicked
        if (table.getRowCount() > 0 && table.getSelectedRow() > -1) {
            if (evt.getClickCount() == 2 && !labBusyApercu.isVisible()) {
                new ImagePreview(DMTWorkbench.frame, true, pathPreviewFile,
                        table.getValueAt(table.getSelectedRow(), 2).toString()).setVisible(true);
            }
        }
    }//GEN-LAST:event_scrollImageMouseClicked

    private void ListYearMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ListYearMouseClicked
        table.unsort();
        if (ListYear.getCheckBoxListSelectedValues().length <= 0) {
            cleanTable();
        } else {
            fillTable("select distinct * from dmt_category join dmt_image on dmt_image.id_category = "
                    + "dmt_category.id_category join dmt_concern on dmt_concern.id_image = dmt_image.id_image join dmt_pathrow "
                    + "on dmt_pathrow.path_row = dmt_concern.path_row where dmt_image.id_image in "
                    + "(" + IDs + ") " + criteriaSearch() + "order by dmt_image.id_image asc");
        }
    }//GEN-LAST:event_ListYearMouseClicked

    private void BSubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BSubmitActionPerformed
        ArrayList list = getIDImageChecked();
        ArrayList list2 = getPathRowImageChecked();
        if (WorkbenchFrame.previewLayer != null) {
            WorkbenchFrame.previewLayer.getLayerManager().dispose(DMTWorkbench.frame, WorkbenchFrame.previewLayer);
            WorkbenchFrame.previewLayer = null;
        }
        if (CBForm.isSelected()) {
            this.setVisible(false);
            new DataRequestForm(DMTWorkbench.frame, true, list, getSizeOfImages(list), list2, this).setVisible(true);
        } else {
            if (WorkbenchFrame.BSConnect.isEnabled()) {
                JOptionPane.showMessageDialog(DMTWorkbench.frame, I18N.get("GeoResult.message-server-connection-error"
                        + ""), I18N.get("Text.Warning"), JOptionPane.WARNING_MESSAGE);
            } else {
                this.dispose();
                new DownloadData(list).setVisible(true);
            }
        }
    }//GEN-LAST:event_BSubmitActionPerformed

    private void MIClearTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MIClearTableActionPerformed
        if (JOptionPane.showConfirmDialog(this, I18N.get("Text.Warning.before-clear-table"), I18N.get("Text.Warning"), 0) == 0) {
            cleanTable();
        }
    }//GEN-LAST:event_MIClearTableActionPerformed

    private void MIChKMapPreviewItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_MIChKMapPreviewItemStateChanged
        if (!MIChKMapPreview.isSelected()) {
            if (WorkbenchFrame.previewLayer != null) {
                WorkbenchFrame.previewLayer.getLayerManager().dispose(DMTWorkbench.frame, WorkbenchFrame.previewLayer);
                WorkbenchFrame.previewLayer = null;
            }
            for (int i = 0; i < commandList.size(); i++) {
                commandList.get(i).unexecute();
            }
            commandList.clear();
            commandKey.clear();
            currentCommand = 0;
            for (int i = 0; i < table.getRowCount(); i++) {
                table.setValueAt(false, i, 0);
            }
        }
    }//GEN-LAST:event_MIChKMapPreviewItemStateChanged

    private void MISubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MISubmitActionPerformed
        BSubmitActionPerformed(evt);
    }//GEN-LAST:event_MISubmitActionPerformed

    private void MIChekAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MIChekAllActionPerformed
        headerChecked = !headerChecked;
        for (int i = 0; i < table.getRowCount(); i++) {
            table.setValueAt(headerChecked, i, 0);
        }
    }//GEN-LAST:event_MIChekAllActionPerformed

    private void MIExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MIExcelActionPerformed
        JFileChooser fc = new JFileChooser(Config.getDefaultDirectory());
        fc.setFileFilter(new FileNameExtensionFilter(I18N.get("GeoResult.Export-file-type"), "xls"));
        File file = new File(Config.defaultDirectory + File.separator + "images.xls");
        fc.setSelectedFile(file);
        int result = fc.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            if (!fc.getSelectedFile().exists()) {
                Config.setDefaultDirectory(fc.getSelectedFile().getParent());
                if (!fc.getSelectedFile().getAbsolutePath().contains(".")) {
                    createExcelFile(new File(fc.getSelectedFile().getAbsolutePath() + ".xls"));
                } else {
                    createExcelFile(fc.getSelectedFile());
                }
            } else {
                JOptionPane.showMessageDialog(this, fc.getSelectedFile().getName() + " " + I18N.get("GeoResult.Export-already-exists-message")
                        + "", I18N.get("Text.Warning"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_MIExcelActionPerformed

    private void MICheckingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MICheckingActionPerformed
        new AdvancedSelection(DMTWorkbench.frame, true, table).setVisible(true);
    }//GEN-LAST:event_MICheckingActionPerformed

    private void MIPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MIPropertiesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_MIPropertiesActionPerformed

    private void execute(UndoableCommand command, LayerManagerProxy layerManagerProxy) {
        boolean exceptionOccurred = true;
        try {
            commandList.add(command);
            currentCommand = commandList.indexOf(command);
            command.execute();
            exceptionOccurred = false;
        } finally {
            if (exceptionOccurred) {
                layerManagerProxy.getLayerManager().getUndoableEditReceiver().getUndoManager().discardAllEdits();
            }
        }
    }

    private void drawingShapeFromDB(String wkt) {
        WorkbenchFrame.createPreviewLayer();
        try (StringReader stringReader = new StringReader(wkt)) {
            WKTReader wktReader = new WKTReader();
            try {
                FeatureCollection c = wktReader.read(stringReader);
                final ArrayList features = new ArrayList();
                FeatureSchema fs = WorkbenchFrame.previewLayer.getFeatureCollectionWrapper().getFeatureSchema();
                for (Iterator i = c.iterator(); i.hasNext();) {
                    Feature feature = (Feature) i.next();
                    features.add(FeatureUtil.toFeature(feature.getGeometry(), fs));
                }
                execute(new UndoableCommand(getName()) {
                    public void execute() {
                        if (WorkbenchFrame.previewLayer != null) {
                            WorkbenchFrame.previewLayer.getFeatureCollectionWrapper().addAll(features);
                        }
                    }

                    public void unexecute() {
                        if (WorkbenchFrame.previewLayer != null) {
                            WorkbenchFrame.previewLayer.getFeatureCollectionWrapper().removeAll(features);
                        }
                    }
                }, WorkbenchFrame.workbenchContext.createPlugInContext());
            } catch (Exception ex) {
                JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error"
                        + ""), ex.getMessage(), null, null, ex, Level.SEVERE, null));
            }
        }
    }

    private void deleteShapeDrawn(UndoableCommand command) {
        command.unexecute();
    }

    private String getShapeOfImage(String ID) {
        String shape = "";
        try {
            PreparedStatement ps = Config.con.prepareStatement("select AsText(shape) from dmt_image where id_image = ?");
            ps.setString(1, ID);
            ResultSet res = ps.executeQuery();
            while (res.next()) {
                return helper.format(res.getString(1));
            }
        } catch (SQLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error"
                    + ""), ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
        return shape;
    }

    private void searchPreview() {
        try {
            if (!table.getValueAt(table.getSelectedRow(), 1).toString().equals("")) {
                if (table.getSelectedColumn() != 0) {
                    messagePreview(I18N.get("Text.Loading"), true);
                    pathPreviewFile = getPathPreview(table.getValueAt(table.getSelectedRow(), 1).toString());
                    WorkbenchFrame.progress.setIndeterminate(true);
                    WorkbenchFrame.progress.setProgressStatus(I18N.get("Text.Loading2") + " \"" + new File(pathPreviewFile).getName()
                            + "\" " + I18N.get("Text.from.text") + " " + Config.pref.get(SettingKeyFactory.OtherFeatures.HOST, "http://www.osfac.net") + "  ...");
                    if (pathPreviewFile.equals("")) {
                        messagePreview(I18N.get("Text.Preview-not-available"), false);
                        WorkbenchFrame.progress.setProgress(100);
                    } else {
                        Thread th = new Thread() {
                            @Override
                            public void run() {
                                showPreview(pathPreviewFile);
                            }
                        };
                        th.start();
                    }
                }
            } else {
                messagePreview();
            }
        } catch (SQLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error")
                    + "", ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }

    private void showPreview(String path) {
        try {
            panImage.repaint();
            panImage.setAutoSize(true);
            String pathimg = Config.pref.get(SettingKeyFactory.OtherFeatures.HOST, "http://www.osfac.net") + path;
            panImage.setImage(new ImageIcon(new URL(pathimg)).getImage());
            labBusyApercu.setVisible(false);
            panImage.repaint();
            WorkbenchFrame.progress.setProgress(100);
        } catch (MalformedURLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error")
                    + "", ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }

    private void messagePreview(String text, boolean statu) {
        panImage.setImage(null);
        panImage.repaint();
        panImage.setAutoSize(false);
        labBusyApercu.setVisible(true);
        labBusyApercu.setBusy(statu);
        labBusyApercu.setText(text);
    }

    private void messagePreview() {
        labBusyApercu.setVisible(false);
        panImage.setImage(null);
        panImage.repaint();
    }

    private String getSizeOfImages(ArrayList list) {
        double totalSize = 0.0;
        try {
            ResultSet res = Config.con.createStatement().executeQuery("select sum(size) from dmt_image "
                    + "where id_image in (" + manyCriteria(list.toArray()) + ")");
            while (res.next()) {
                totalSize = res.getDouble(1);
            }
        } catch (SQLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error")
                    + "", ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
        String size = Config.convertOctetToAnyInDouble((long) (totalSize * (1024 * 1024)));
        return size;
    }

    private ArrayList getPathRowImageChecked() {
        ArrayList list = new ArrayList();
        for (int i = 0; i < table.getRowCount(); i++) {
            if (Boolean.valueOf(table.getValueAt(i, 0).toString()) == true && !list.contains(table.getValueAt(i, 3) + table.getValueAt(i, 4).toString())) {
                list.add(table.getValueAt(i, 3).toString() + table.getValueAt(i, 4).toString());
            }
        }
        return list;
    }

    private ArrayList getIDImageChecked() {
        ArrayList list = new ArrayList();
        for (int i = 0; i < table.getRowCount(); i++) {
            if (Boolean.valueOf(table.getValueAt(i, 0).toString()) == true) {
                list.add(table.getValueAt(i, 1));
            }
        }
        return list;
    }

    private void submitEnabled() {
        boolean available = false;
        for (int i = 0; i < table.getRowCount(); i++) {
            if (Boolean.valueOf(table.getValueAt(i, 0).toString()) == true) {
                BSubmit.setEnabled(true);
                available = true;
                break;
            }
        }
        if (!available) {
            BSubmit.setEnabled(false);
        }
    }

    private void enabledPopupItems() {
//        MIProperties.setEnabled(table.getSelectedRow() != -1);
        MIProperties.setEnabled(false);
        MISubmit.setEnabled(BSubmit.isEnabled());
        if (!headerChecked) {
            MIChekAll.setText(I18N.get("Text.select-All-categories"));
        } else {
            MIChekAll.setText(I18N.get("Text.Unselect-All-categories"));
        }
    }

    private void cleanTable() {
        for (int i = 0; i < commandList.size(); i++) {
            commandList.get(i).unexecute();
        }
        commandList.clear();
        commandKey.clear();
        currentCommand = 0;
        table.unsort();
        table.setSortingEnabled(false);
        panImage.setImage(null);
        panImage.repaint();
        int nbTable = table.getModel().getRowCount();
        while (nbTable > 0) {
            tableModel.removeNewRow(--nbTable);
        }
        LabImageDisplayed.setText(I18N.get("Text.Images-displayed") + " : 0");
        LabImageChecked.setText(I18N.get("Text.Images-checked") + " : 0");
    }

    private String getPathPreview(String idImage) throws SQLException {
        String imagePath = "";
        PreparedStatement ps = Config.con.prepareStatement("select preview_path from dmt_support where id_support = ("
                + "select id_support from dmt_image where id_image = ?)");
        ps.setString(1, idImage);
        ResultSet res = ps.executeQuery();
        while (res.next()) {
            return res.getString(1);
        }
        return imagePath;
    }

    private void fillTable(String query) {
        try {
            table.unsort();
            cleanTable();
            ResultSet res = Config.con.createStatement().executeQuery(query);
            int nbRow = 1;
            int previousID = 0;
            while (res.next()) {
                if (nbRow > tableModel.getRowCount()) {
                    tableModel.addNewRow();
                }
                if (previousID == res.getInt("dmt_image.id_image")) {
                    continue;
                }
                previousID = res.getInt("dmt_image.id_image");
                table.setValueAt(headerChecked, nbRow - 1, 0);
                table.setValueAt(res.getString("dmt_image.id_image"), nbRow - 1, 1);
                table.setValueAt(res.getString("image_name"), nbRow - 1, 2);
                table.setValueAt(res.getDate("date"), nbRow - 1, 3);
                table.setValueAt(res.getDouble("size"), nbRow - 1, 4);
                nbRow++;
            }
            TableUtils.autoResizeAllColumns(table);
            TableUtils.autoResizeAllRows(table);
        } catch (SQLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error")
                    + "", ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }

    private String criteriaSearch() {
        String where, categories, years;

        if (ListCategory.getCheckBoxListSelectedValues().length != 0) {
            categories = " AND (category_name in (" + manyCriteria(ListCategory.getCheckBoxListSelectedValues()) + "))";
        } else {
            categories = "";
        }
        if (ListYear.getCheckBoxListSelectedValues().length != 0) {
            years = " AND (year(date) in (" + manyCriteria(ListYear.getCheckBoxListSelectedValues()) + "))";
        } else {
            years = "";
        }
        where = categories + years;
        return where;
    }

    private String manyCriteria(Object[] list) {
        String values = "";
        for (int i = 0; i < list.length; i++) {
            values += "\'" + list[i].toString() + "\',";
        }
        values = values.substring(0, values.length() - 1);
        return values;
    }

    private void createExcelFile(File file) {
        try {
            WritableWorkbook wbb;
            WritableSheet sheet;
            Label label;
            wbb = Workbook.createWorkbook(file);
            String feuille = "OSFAC-DMT";
            sheet = wbb.createSheet(feuille, 0);
            int p = 0;
            for (int gh = 0; gh < table.getColumnCount(); gh++) {
                p = gh;
                if (p < table.getColumnCount()) {
                    label = new Label(gh, 0, table.getColumnName(p));
                    sheet.addCell(label);
                    p++;
                }
            }
            int a = 0;
            int b = 0;
            for (int i = 0; i < table.getColumnCount(); i++) {
                b = 0;
                for (int j = 1; j < table.getRowCount() + 1; j++) {
                    if (a < table.getColumnCount()) {
                        label = new Label(i, j, table.getValueAt(b, a).toString());
                        sheet.addCell(label);
                        b++;
                    }
                }
                a++;
            }
            wbb.write();
            wbb.close();
            JOptionPane.showMessageDialog(this, I18N.get("GeoResult.Export-confirmation-message"
                    + ""), I18N.get("Text.Confirm"), JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | WriteException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error"
                    + ""), ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }

    private class MyItemListener implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                headerChecked = true;
                for (int i = 0; i < table.getRowCount(); i++) {
                    table.setValueAt(headerChecked, i, 0);
                }
//////                
//////                for (int i = 0; i < table.getRowCount(); i++) {
//////                    String wkt = getShapeOfImage(table.getValueAt(i, 1).toString());
//////                    drawingShapeFromDB(wkt);
//////                    commandKey.put(i, currentCommand);
//////                }
            } else {
                headerChecked = false;
                for (int i = 0; i < table.getRowCount(); i++) {
                    table.setValueAt(headerChecked, i, 0);
                }
//////                
//////                for (int i = 0; i < commandList.size(); i++) {
//////                    commandList.get(i).unexecute();
//////                }
            }
        }
    }

    private class MyTableModel extends TreeTableModel {

        private String[] columnNames = {"", I18N.get("Text.ID"), I18N.get("Text.IMAGES"), I18N.get("Text.DATE"), I18N.get("Text.SIZE-IN-MO")};
        private ArrayList[] Data;

        public MyTableModel() {
            Data = new ArrayList[columnNames.length];
            for (int i = 0; i < columnNames.length; i++) {
                Data[i] = new ArrayList();
            }
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return Data[0].size();
        }

        public EditorContext getEditorContextAt(int row, int column) {
            if (column == 0) {
                return BooleanCheckBoxCellEditor.CONTEXT;
            }
            return null;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return Data[col].get(row);
        }

        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            if (col == 0) {
                return (true);
            } else {
                return (false);
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            Data[col].set(row, value);
            fireTableCellUpdated(row, col);
        }

        public void addNewRow() {
            for (int i = 0; i < columnNames.length; i++) {
                if (i == 0) {
                    Data[i].add(false);
                } else {
                    Data[i].add("");
                }
            }
            this.fireTableRowsInserted(0, Data[0].size() - 1);
        }

        public void removeNewRow() {
            for (int i = 0; i < columnNames.length; i++) {
                Data[i].remove(Data[i].size() - 1);
            }
            this.fireTableRowsDeleted(0, Data[0].size() - 1);
        }

        public void removeNewRow(int index) {
            for (int i = 0; i < columnNames.length; i++) {
                Data[i].remove(index);
            }
            this.fireTableRowsDeleted(0, Data[0].size() - 1);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.jidesoft.swing.JideButton BSubmit;
    private javax.swing.JCheckBox CBForm;
    private javax.swing.JLabel LabImageChecked;
    private javax.swing.JLabel LabImageDisplayed;
    private com.jidesoft.swing.CheckBoxList ListCategory;
    private com.jidesoft.swing.CheckBoxList ListYear;
    private javax.swing.JCheckBoxMenuItem MIChKMapPreview;
    private javax.swing.JMenuItem MIChecking;
    private javax.swing.JMenuItem MIChekAll;
    private javax.swing.JMenuItem MIClearTable;
    private javax.swing.JMenuItem MIExcel;
    private javax.swing.JMenuItem MIProperties;
    private javax.swing.JMenuItem MISubmit;
    private javax.swing.JScrollPane ScrlAll;
    private javax.swing.JPopupMenu TabPopupMenu;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JScrollPane scrollImage;
    // End of variables declaration//GEN-END:variables
    JImagePanel panImage;
    JXBusyLabel labBusyApercu;
    DefaultListModel category, year;
    ArrayList<Integer> IDList;
    SortableTable table;
    MyTableModel tableModel;
    String IDs = "";
    GeoResult geoResult;
    String pathPreviewFile = null;
    boolean headerChecked = false;
    DecimalFormat df = new DecimalFormat();
    private WKTDisplayHelper helper = new WKTDisplayHelper();
    ArrayList<UndoableCommand> commandList = new ArrayList<>();
    HashMap<Integer, Integer> commandKey = new HashMap<>();
    int currentCommand;
}
