package com.osfac.dmt.tools.search;

import com.jidesoft.grid.BooleanCheckBoxCellEditor;
import com.jidesoft.grid.EditorContext;
import com.jidesoft.grid.RowStripeTableStyleProvider;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TableUtils;
import com.jidesoft.grid.TreeTableModel;
import com.jidesoft.pane.FloorTabbedPane;
import com.jidesoft.pane.FloorTabbedPane.FloorButton;
import com.osfac.dmt.CheckBoxHeader;
import com.osfac.dmt.Config;
import com.osfac.dmt.I18N;
import com.osfac.dmt.JImagePanel;
import com.osfac.dmt.download.DownloadData;
import com.osfac.dmt.form.DataRequestForm;
import com.osfac.dmt.setting.SettingKeyFactory;
import com.osfac.dmt.tools.AdvancedSelection;
import com.osfac.dmt.workbench.DMTIconsFactory;
import com.osfac.dmt.workbench.DMTWorkbench;
import com.osfac.dmt.workbench.ui.WorkbenchFrame;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

public class QuerySearch extends javax.swing.JPanel {

    public QuerySearch() {
        exe = new RunSearch(null, true);
        tableModel = new MyTableModel();
        table = new SortableTable(tableModel);
//        table.setSortable(false);
//        table.setSortingEnabled(false);
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
        TableUtils.autoResizeAllColumns(table);
        TableUtils.autoResizeAllRows(table);
        Config.centerTableHeadAndBold(table);
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
        initComponents(I18N.DMTResourceBundle);
        showItemInScrollPane(null);
        table.requestFocus();
        panImage = new JImagePanel();
        panImage.setLayout(new BorderLayout());
        labBusyApercu = new JXBusyLabel();
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
        try {
            stat = Config.con.createStatement();
        } catch (SQLException ex) {
            Logger.getLogger(QuerySearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        scrollImage.getViewport().setBackground(Color.white);
        Timer timer = new Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (MainCriteria.CBCategory.getSelectedIndex() != -1 || MainCriteria.CBPath.getSelectedIndex() != -1
                        || MainCriteria.CBRow.getSelectedIndex() != -1 || MainCriteria.CBYear.getSelectedIndex() != -1
                        || MainCriteria.CBCountry.getSelectedIndex() != -1 || MainCriteria.CBMission.getSelectedIndex() != -1
                        || MainCriteria.CBOrtho.getSelectedIndex() != -1 || MainCriteria.CBSLC.getSelectedIndex() != -1
                        || !GeoCriteria.RBPoint.isSelected()) {
                    BReset.setEnabled(true);
                } else {
                    BReset.setEnabled(false);
                }
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

                enabledPopupItems();
                submitEnable();
            }
        });
        timer.start();
        timerShow = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (susp) {
                    if (vectThread.size() > ID) {
                        susp = false;
                        vectThread.get(ID).start();
                        ID++;
                    }
                    if (RunSearch.progression.getValue() == simulateNumber) {
                        exe.dispose();
                        WorkbenchFrame.progress.setProgress(100);
                        table.scrollRowToVisible(0);
                        timerShow.stop();
                        TableUtils.autoResizeAllColumns(table);
                        TableUtils.autoResizeAllRows(table);
                        susp = false;
                        BAddSearch.setEnabled(true);
                        showItemInScrollPane(table);
//                        WorkbenchFrame.dbprocessing.setVisible(false);
                        if (simulateNumber < 2000) {
                            JOptionPane.showMessageDialog(DMTWorkbench.frame, simulateNumber + " " + I18N.get("Search.images-found"));
                        }
                    }
                }
            }
        });
        if (Config.isLiteVersion() || Config.isSimpleUser()) {
            CBForm.setVisible(false);
        }
        panSwitcher.add(createTabbedPane());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TabPopupMenu = new javax.swing.JPopupMenu();
        MISubmit = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        MIExcel = new javax.swing.JMenuItem();
        MIChecking = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        MIClearTable = new javax.swing.JMenuItem();
        MIChekAll = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        MIProperties = new javax.swing.JMenuItem();
        BSubmit = new com.jidesoft.swing.JideButton();
        LabImageDisplayed = new javax.swing.JLabel();
        LabImageChecked = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        BSearch = new com.jidesoft.swing.JideButton();
        BReset = new com.jidesoft.swing.JideButton();
        BAddSearch = new com.jidesoft.swing.JideButton();
        ScrlAll = new javax.swing.JScrollPane();
        BLabLoading = new org.jdesktop.swingx.JXBusyLabel();
        scrollImage = new javax.swing.JScrollPane();
        CBForm = new javax.swing.JCheckBox();
        panSwitcher = new javax.swing.JPanel();

        MISubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/apply(5).png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("language/dmt_en"); // NOI18N
        MISubmit.setText(bundle.getString("Search.MISubmit.text")); // NOI18N
        MISubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MISubmitActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MISubmit);
        TabPopupMenu.add(jSeparator4);

        MIExcel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/excel.png"))); // NOI18N
        MIExcel.setText(bundle.getString("Search.MIExcel.text")); // NOI18N
        MIExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIExcelActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIExcel);

        MIChecking.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/ok.png"))); // NOI18N
        MIChecking.setText(bundle.getString("Search.MIChecking.text")); // NOI18N
        MIChecking.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MICheckingActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIChecking);
        TabPopupMenu.add(jSeparator3);

        MIClearTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/editclear(7).png"))); // NOI18N
        MIClearTable.setText(bundle.getString("Search.MIClearTable.text")); // NOI18N
        MIClearTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIClearTableActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIClearTable);

        MIChekAll.setText(bundle.getString("Search.MIChekAll.text")); // NOI18N
        MIChekAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIChekAllActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIChekAll);
        TabPopupMenu.add(jSeparator2);

        MIProperties.setText(bundle.getString("Search.MIProperties.text")); // NOI18N
        MIProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIPropertiesActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIProperties);

        setBackground(new java.awt.Color(255, 255, 255));

        BSubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/apply.png"))); // NOI18N
        BSubmit.setText(bundle.getString("QuerySearch.BSubmit.text")); // NOI18N
        BSubmit.setButtonStyle(1);
        BSubmit.setFocusable(false);
        BSubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BSubmitActionPerformed(evt);
            }
        });

        LabImageDisplayed.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        LabImageDisplayed.setForeground(new java.awt.Color(0, 0, 204));
        LabImageDisplayed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabImageDisplayed.setText(bundle.getString("QuerySearch.LabImageDisplayed.text")); // NOI18N
        LabImageDisplayed.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        LabImageChecked.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        LabImageChecked.setForeground(new java.awt.Color(51, 153, 0));
        LabImageChecked.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabImageChecked.setText(bundle.getString("QuerySearch.LabImageChecked.text")); // NOI18N
        LabImageChecked.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        BSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/search22x22.png"))); // NOI18N
        BSearch.setText(bundle.getString("QuerySearch.BSearch.text")); // NOI18N
        BSearch.setButtonStyle(1);
        BSearch.setFocusable(false);
        BSearch.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BSearchActionPerformed(evt);
            }
        });

        BReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/icon-48-clear.png"))); // NOI18N
        BReset.setText(bundle.getString("QuerySearch.BReset.text")); // NOI18N
        BReset.setButtonStyle(1);
        BReset.setEnabled(false);
        BReset.setFocusable(false);
        BReset.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BResetActionPerformed(evt);
            }
        });

        BAddSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/search_plus22x22.png"))); // NOI18N
        BAddSearch.setText(bundle.getString("QuerySearch.BAddSearch.text")); // NOI18N
        BAddSearch.setButtonStyle(1);
        BAddSearch.setEnabled(false);
        BAddSearch.setFocusable(false);
        BAddSearch.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BAddSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BAddSearchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(BSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(BAddSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(BReset, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(BAddSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(BReset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        ScrlAll.setBackground(new java.awt.Color(255, 255, 255));

        BLabLoading.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BLabLoading.setText(bundle.getString("Search.BLabLoading.text")); // NOI18N
        BLabLoading.setBusy(true);
        BLabLoading.setFocusable(false);
        ScrlAll.setViewportView(BLabLoading);

        scrollImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                scrollImageMouseClicked(evt);
            }
        });

        CBForm.setSelected(true);
        CBForm.setText(bundle.getString("QuerySearch.CBForm.text")); // NOI18N
        CBForm.setFocusable(false);
        CBForm.setOpaque(false);

        panSwitcher.setLayout(new javax.swing.BoxLayout(panSwitcher, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ScrlAll)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(LabImageDisplayed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(LabImageChecked, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(CBForm, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(BSubmit, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(panSwitcher, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scrollImage, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(5, 5, 5))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panSwitcher, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrollImage, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE))
                .addGap(4, 4, 4)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(ScrlAll, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE)
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(CBForm)
                    .addComponent(LabImageDisplayed)
                    .addComponent(BSubmit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LabImageChecked, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {LabImageChecked, LabImageDisplayed});

    }// </editor-fold>//GEN-END:initComponents

    private void initComponents(java.util.ResourceBundle bundle) {

        TabPopupMenu = new javax.swing.JPopupMenu();
        MISubmit = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        MIExcel = new javax.swing.JMenuItem();
        MIChecking = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        MIClearTable = new javax.swing.JMenuItem();
        MIChekAll = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        MIProperties = new javax.swing.JMenuItem();
        BSubmit = new com.jidesoft.swing.JideButton();
        LabImageDisplayed = new javax.swing.JLabel();
        LabImageChecked = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        BSearch = new com.jidesoft.swing.JideButton();
        BReset = new com.jidesoft.swing.JideButton();
        BAddSearch = new com.jidesoft.swing.JideButton();
        ScrlAll = new javax.swing.JScrollPane();
        BLabLoading = new org.jdesktop.swingx.JXBusyLabel();
        scrollImage = new javax.swing.JScrollPane();
        CBForm = new javax.swing.JCheckBox();
        panSwitcher = new javax.swing.JPanel();

        MISubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/apply(5).png"))); // NOI18N

        MISubmit.setText(bundle.getString("Search.MISubmit.text")); // NOI18N
        MISubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MISubmitActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MISubmit);
        TabPopupMenu.add(jSeparator4);

        MIExcel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/excel.png"))); // NOI18N
        MIExcel.setText(bundle.getString("Search.MIExcel.text")); // NOI18N
        MIExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIExcelActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIExcel);

        MIChecking.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/ok.png"))); // NOI18N
        MIChecking.setText(bundle.getString("Search.MIChecking.text")); // NOI18N
        MIChecking.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MICheckingActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIChecking);
        TabPopupMenu.add(jSeparator3);

        MIClearTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/editclear(7).png"))); // NOI18N
        MIClearTable.setText(bundle.getString("Search.MIClearTable.text")); // NOI18N
        MIClearTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIClearTableActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIClearTable);

        MIChekAll.setText(bundle.getString("Search.MIChekAll.text")); // NOI18N
        MIChekAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIChekAllActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIChekAll);
        TabPopupMenu.add(jSeparator2);

        MIProperties.setText(bundle.getString("Search.MIProperties.text")); // NOI18N
        MIProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIPropertiesActionPerformed(evt);
            }
        });
        TabPopupMenu.add(MIProperties);

        setBackground(new java.awt.Color(255, 255, 255));

        BSubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/apply.png"))); // NOI18N
        BSubmit.setText(bundle.getString("QuerySearch.BSubmit.text")); // NOI18N
        BSubmit.setButtonStyle(1);
        BSubmit.setFocusable(false);
        BSubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BSubmitActionPerformed(evt);
            }
        });

        LabImageDisplayed.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        LabImageDisplayed.setForeground(new java.awt.Color(0, 0, 204));
        LabImageDisplayed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabImageDisplayed.setText(bundle.getString("QuerySearch.LabImageDisplayed.text")); // NOI18N
        LabImageDisplayed.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        LabImageChecked.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        LabImageChecked.setForeground(new java.awt.Color(51, 153, 0));
        LabImageChecked.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LabImageChecked.setText(bundle.getString("QuerySearch.LabImageChecked.text")); // NOI18N
        LabImageChecked.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        BSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/search22x22.png"))); // NOI18N
        BSearch.setText(bundle.getString("QuerySearch.BSearch.text")); // NOI18N
        BSearch.setButtonStyle(1);
        BSearch.setFocusable(false);
        BSearch.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BSearchActionPerformed(evt);
            }
        });

        BReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/icon-48-clear.png"))); // NOI18N
        BReset.setText(bundle.getString("QuerySearch.BReset.text")); // NOI18N
        BReset.setButtonStyle(1);
        BReset.setEnabled(false);
        BReset.setFocusable(false);
        BReset.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BResetActionPerformed(evt);
            }
        });

        BAddSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/osfac/dmt/images/search_plus22x22.png"))); // NOI18N
        BAddSearch.setText(bundle.getString("QuerySearch.BAddSearch.text")); // NOI18N
        BAddSearch.setButtonStyle(1);
        BAddSearch.setEnabled(false);
        BAddSearch.setFocusable(false);
        BAddSearch.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BAddSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BAddSearchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(BSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BAddSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BReset, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)));
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(BSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(BAddSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(BReset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE));

        ScrlAll.setBackground(new java.awt.Color(255, 255, 255));

        BLabLoading.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BLabLoading.setText(bundle.getString("Search.BLabLoading.text")); // NOI18N
        BLabLoading.setBusy(true);
        BLabLoading.setFocusable(false);
        ScrlAll.setViewportView(BLabLoading);

        scrollImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                scrollImageMouseClicked(evt);
            }
        });

        CBForm.setSelected(true);
        CBForm.setText(bundle.getString("QuerySearch.CBForm.text")); // NOI18N
        CBForm.setFocusable(false);
        CBForm.setOpaque(false);

        panSwitcher.setLayout(new javax.swing.BoxLayout(panSwitcher, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addGap(5, 5, 5)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(ScrlAll)
                                                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                        .addComponent(LabImageDisplayed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(LabImageChecked, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(CBForm, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(BSubmit, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGroup(layout.createSequentialGroup()
                                        .addGap(4, 4, 4)
                                        .addComponent(panSwitcher, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(scrollImage, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(5, 5, 5)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(panSwitcher, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(scrollImage, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE))
                        .addGap(4, 4, 4)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(ScrlAll, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE)
                        .addGap(4, 4, 4)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                .addComponent(CBForm)
                                .addComponent(LabImageDisplayed)
                                .addComponent(BSubmit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(LabImageChecked, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(5, 5, 5)));

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[]{LabImageChecked, LabImageDisplayed});

    }// </editor-fold>

    private void BSubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BSubmitActionPerformed
        ArrayList list = getIDImageChecked();
        ArrayList list2 = getPathRowImageChecked();
        if (CBForm.isSelected()) {
            new DataRequestForm(DMTWorkbench.frame, true, list, getSizeOfImages(list), list2).setVisible(true);
        } else {
            if (WorkbenchFrame.BSConnect.isEnabled()) {
                JOptionPane.showMessageDialog(DMTWorkbench.frame, I18N.get("GeoResult.message-server-connection-error"
                        + ""), I18N.get("Text.Warning"), JOptionPane.WARNING_MESSAGE);
            } else {
                new DownloadData(list).setVisible(true);
            }
        }
    }//GEN-LAST:event_BSubmitActionPerformed

    private void BSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BSearchActionPerformed
        table.unsort();
        clearVector();
        cleanTable();
        searchData();
    }//GEN-LAST:event_BSearchActionPerformed

    private void BAddSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BAddSearchActionPerformed
        table.unsort();
        searchData();
    }//GEN-LAST:event_BAddSearchActionPerformed

    private void BResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BResetActionPerformed
        cleanTable();
        clearVector();
        MainCriteria.CBCategory.setSelectedIndex(-1);
        MainCriteria.CBCountry.setSelectedIndex(-1);
        MainCriteria.CBMission.setSelectedIndex(-1);
        MainCriteria.CBOrtho.setSelectedIndex(-1);
        MainCriteria.CBPath.setSelectedIndex(-1);
        MainCriteria.CBRow.setSelectedIndex(-1);
        MainCriteria.CBSLC.setSelectedIndex(-1);
        MainCriteria.CBYear.setSelectedIndex(-1);

        GeoCriteria.RBPoint.setSelected(true);
        for (int i = 0; i < GeoCriteria.table.getRowCount(); i++) {
            for (int j = 0; j < GeoCriteria.table.getColumnCount(); j++) {
                GeoCriteria.table.setValueAt("", i, j);
            }
        }
        //        BReset.setEnabled(false);
        BAddSearch.setEnabled(false);
        showItemInScrollPane(null);
        WorkbenchFrame.progress.setIndeterminate(false);
        WorkbenchFrame.progress.setProgress(100);
    }//GEN-LAST:event_BResetActionPerformed

    private void scrollImageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scrollImageMouseClicked
        if (table.getRowCount() > 0 && table.getSelectedRow() > -1) {
            if (evt.getClickCount() == 2 && !labBusyApercu.isVisible()) {
                new ImagePreview(DMTWorkbench.frame, true, pathPreviewFile, table.getValueAt(table.getSelectedRow(), 2).toString()).setVisible(true);
            }
        }
    }//GEN-LAST:event_scrollImageMouseClicked

    private void MISubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MISubmitActionPerformed
        BSubmitActionPerformed(evt);
    }//GEN-LAST:event_MISubmitActionPerformed

    private void MIExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MIExcelActionPerformed
        JFileChooser fc = new JFileChooser(Config.getDefaultDirectory());
        fc.setFileFilter(new FileNameExtensionFilter(I18N.get("GeoResult.Export-file-type"), "xls"));
        File file = new File(Config.defaultDirectory + File.separator + "data.xls");
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

    private void MIClearTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MIClearTableActionPerformed
        if (JOptionPane.showConfirmDialog(this, I18N.get("Text.Warning.before-clear-table"), I18N.get("Text.Warning"), 0) == 0) {
            cleanTable();
        }
    }//GEN-LAST:event_MIClearTableActionPerformed

    private void MIChekAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MIChekAllActionPerformed
        headerChecked = !headerChecked;
        for (int i = 0; i < table.getRowCount(); i++) {
            table.setValueAt(headerChecked, i, 0);
        }
    }//GEN-LAST:event_MIChekAllActionPerformed

    private void MIPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MIPropertiesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_MIPropertiesActionPerformed

    private static void showItemInScrollPane(JComponent component) {
        ScrlAll.setViewportView(component);
    }

    private FloorTabbedPane createTabbedPane() {
        _tabbedPane = new FloorTabbedPane() {
            @Override
            protected AbstractButton createButton(Action action) {
                return new FloorButton(action);
            }
        };
        _tabbedPane.setOrientation(SwingConstants.HORIZONTAL);
        addTabPanel(createTabPanel(I18N.get("QuerySearch.Main-criteria"), DMTIconsFactory.getImageIcon(DMTIconsFactory.DMTIcon.ICON), new MainCriteria()));
        addTabPanel(createTabPanel(I18N.get("QuerySearch.Geographic-criteria"), DMTIconsFactory.getImageIcon(DMTIconsFactory.DMTIcon.ICON), new GeoCriteria()));
        return _tabbedPane;
    }

    private void addTabPanel(TabPanel tabPanel) {
        _tabbedPane.addTab(tabPanel.getTitle(), tabPanel.getIcon(), tabPanel.getComponent(), tabPanel.getTitle());
    }

    private TabPanel createTabPanel(String title, Icon icon, JComponent component) {
        return new TabPanel(title, icon, component);
    }

    private class TabPanel extends JPanel {

        Icon _icon;
        String _title;
        JComponent _component;

        public TabPanel(String title, Icon icon, JComponent component) {
            _title = title;
            _icon = icon;
            _component = component;
        }

        public Icon getIcon() {
            return _icon;
        }

        public void setIcon(Icon icon) {
            _icon = icon;
        }

        public String getTitle() {
            return _title;
        }

        public void setTitle(String title) {
            _title = title;
        }

        public JComponent getComponent() {
            return _component;
        }

        public void setComponent(JComponent component) {
            _component = component;
        }
    }

    private void showItemInScrollPane(JComponent component, String message) {
        ScrlAll.setViewportView(component);
        BLabLoading.setText(message);
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

    private String getSizeOfImages(ArrayList list) {
        double totalSize = 0.0;
        try {
            ResultSet res = stat.executeQuery("select sum(size) from dmt_image where id_image in "
                    + "(" + manyCriteria(list.toArray()) + ")");
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

    private void showPreview(String path) {
        try {
            panImage.repaint();
            panImage.setAutoSize(true);
            String pathimg = Config.pref.get(SettingKeyFactory.OtherFeatures.HOST, "http://www.osfac.net") + path;
//            System.out.println(pathimg);
            panImage.setImage(new ImageIcon(new URL(pathimg)).getImage());
            labBusyApercu.setVisible(false);
            panImage.repaint();
            WorkbenchFrame.progress.setProgress(100);
        } catch (MalformedURLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error")
                    + "", ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }

    private void messagePreview(String text, boolean status) {
        panImage.setImage(null);
        panImage.repaint();
        panImage.setAutoSize(false);
        labBusyApercu.setVisible(true);
        labBusyApercu.setBusy(status);
        labBusyApercu.setText(text);
    }

    private void messagePreview() {
        labBusyApercu.setVisible(false);
        panImage.setImage(null);
        panImage.repaint();
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

    private void submitEnable() {
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

    private String criteriaSearch() {
        String where, category, path, row, year, country, mission, slc = "", ortho;

        if (MainCriteria.CBCategory.getSelectedObjects().length != 0) {
            category = " AND (category_name in (" + manyCriteria(MainCriteria.CBCategory.getSelectedObjects()) + "))";
        } else {
            category = "";
        }
        if (MainCriteria.CBPath.getSelectedObjects().length != 0) {
            path = " AND (path in (" + manyCriteria(MainCriteria.CBPath.getSelectedObjects()) + "))";
        } else {
            path = "";
        }
        if (MainCriteria.CBRow.getSelectedObjects().length != 0) {
            row = " AND (row in (" + manyCriteria(MainCriteria.CBRow.getSelectedObjects()) + "))";
        } else {
            row = "";
        }
        if (MainCriteria.CBCountry.getSelectedObjects().length != 0) {
            country = " AND (country_name in (" + manyCriteria(MainCriteria.CBCountry.getSelectedObjects()) + "))";
        } else {
            country = "";
        }
        if (MainCriteria.CBYear.getSelectedObjects().length != 0) {
            year = " AND (year(date) in (" + manyCriteria(MainCriteria.CBYear.getSelectedObjects()) + "))";
        } else {
            year = "";
        }
        if (MainCriteria.CBMission.getSelectedObjects().length != 0) {
            mission = " AND (mission in (" + manyCriteria(MainCriteria.CBMission.getSelectedObjects()) + "))";
        } else {
            mission = "";
        }
        if (MainCriteria.CBOrtho.getSelectedObjects().length != 0) {
            ortho = " AND (ortho in (" + manyCriteria(MainCriteria.CBOrtho.getSelectedObjects()) + "))";
        } else {
            ortho = "";
        }
        if (MainCriteria.CBSLC.isEnabled()) {
            if (MainCriteria.CBSLC.getSelectedObjects().length != 0) {
                slc = " AND (slc in (" + manyCriteria(MainCriteria.CBSLC.getSelectedObjects()) + "))";
            } else {
                slc = "";
            }
        }

        where = category + path + row + country + mission + slc + ortho + year + getShape();
        if (where.startsWith(" AND")) {
            where = where.substring(4);
        }
        if (!where.isEmpty()) {
            where = "WHERE " + where;
        }
//        System.err.println(where);
        return where;
    }

    private boolean isContainingValues() {
        boolean isEmpty = false;
        for (int i = 0; i < GeoCriteria.table.getRowCount(); i++) {
            if (GeoCriteria.table.getValueAt(i, 0).toString().equals("")
                    && GeoCriteria.table.getValueAt(i, 1).toString().equals("")) {
                isEmpty = true;
            }
        }
        return !isEmpty;
    }

    private String getShape() {
        String shape = "";
        if (isContainingValues()) {
            if (GeoCriteria.RBPoint.isSelected()) {
                shape = " AND (Intersects(GeomFromText('POINT(";
            } else if (GeoCriteria.RBLine.isSelected()) {
                shape = " AND (Intersects(GeomFromText('LINESTRING(";
            } else {
                shape = " AND (Intersects(GeomFromText('POLYGON((";
            }
            try {
                for (int i = 0; i < GeoCriteria.table.getRowCount(); i++) {
                    shape += Double.parseDouble(GeoCriteria.table.getValueAt(i, 0).toString()) + " "
                            + "" + Double.parseDouble(GeoCriteria.table.getValueAt(i, 1).toString()) + ",";
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, I18N.get("QuerySearch.Latitude-Longitude-values-are-no-valid"), I18N.get("com.osfac.dmt.Config.Error"), JOptionPane.ERROR_MESSAGE);
                return "";
            }
            if (GeoCriteria.RBPolygon.isSelected()) {
                shape = shape.substring(0, shape.length() - 1) + "))'), shape) = 1)";
            } else {
                shape = shape.substring(0, shape.length() - 1) + ")'), shape) = 1)";
            }
        }
        return shape;
    }

    private String manyCriteria(Object[] list) {
        String values = "";
        for (int i = 0; i < list.length; i++) {
            values += "\'" + list[i].toString() + "\',";
        }
        values = values.substring(0, values.length() - 1);
        return values;
    }

    private class MyTableModel extends TreeTableModel {

        private String[] columnNames = {"", I18N.get("Text.ID"), I18N.get("Text.IMAGES"), I18N.get("Text.PATH"
            + ""), I18N.get("Text.ROW"), I18N.get("Text.DATE"), I18N.get("Text.SIZE-IN-MO")};
        private ArrayList[] Data;

        public MyTableModel() {
            Data = new ArrayList[columnNames.length];
            for (int i = 0; i < columnNames.length; i++) {
                Data[i] = new ArrayList();
            }
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return Data[0].size();
        }

        @Override
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

        @Override
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

    public static void cancelExe() {
        if (JOptionPane.showConfirmDialog(DMTWorkbench.frame, I18N.get("Search.interrupt-search-by-user"
                + ""), I18N.get("Text.Confirm"), JOptionPane.YES_OPTION) == 0) {
            exe.dispose();
            WorkbenchFrame.progress.setProgress(100);
            TableUtils.autoResizeAllColumns(table);
            TableUtils.autoResizeAllRows(table);
            timerShow.stop();
            susp = false;
            BAddSearch.setEnabled(true);
            showItemInScrollPane(table);
//            WorkbenchFrame.dbprocessing.setVisible(false);
            if ((nbRow - 1) < 2000) {
                JOptionPane.showMessageDialog(DMTWorkbench.frame, I18N.get("Search.search-interrupt") + " "
                        + "" + (nbRow - 1) + " " + I18N.get("Search.images-found2"));
            }
        }
    }

    private void searchData() {
        table.unsort();
        panImage.setImage(null);
        panImage.repaint();
        final String WHERE = criteriaSearch();
        if (!WHERE.equals("")) {
            String query;
            if (MainCriteria.CBCountry.getSelectedObjects().length > 0) {
                query = "select distinct * from dmt_category join dmt_image on dmt_category.id_category = dmt_image.id_category join "
                        + "dmt_concern on dmt_concern.id_image = dmt_image.id_image join dmt_pathrow on dmt_pathrow.path_row = dmt_concern.path_row "
                        + "join dmt_include on dmt_include.path_row = dmt_pathrow.path_row join dmt_country on dmt_country.id_country = "
                        + "dmt_include.id_country " + WHERE + " order by dmt_image.id_image";
            } else {
                query = "select distinct * from dmt_category join dmt_image on dmt_category.id_category = dmt_image.id_category join "
                        + "dmt_concern on dmt_concern.id_image = dmt_image.id_image join dmt_pathrow on dmt_pathrow.path_row = dmt_concern.path_row "
                        + "" + WHERE + " order by dmt_image.id_image";
            }
//            System.out.println(query);
            simulateNumber = simulateQuery("select count(distinct dmt_image.id_image) " + query.substring(18));
            if (simulateNumber == 0) {
                WorkbenchFrame.progress.setProgress(100);
                showItemInScrollPane(BLabLoading, I18N.get("Search.no-image-has-been-found"));
                JOptionPane.showMessageDialog(DMTWorkbench.frame, I18N.get("Search.no-image-has-been-found"), ""
                        + I18N.get("Text.Warning"), JOptionPane.WARNING_MESSAGE);
                showItemInScrollPane(null);
            } else {
                if (simulateNumber >= 2000) {
                    if (JOptionPane.showConfirmDialog(DMTWorkbench.frame, simulateNumber + " " + I18N.get("Search.images-found") + " "
                            + I18N.get("Search.request-requires-time"), ""
                            + I18N.get("Text.Confirm"), JOptionPane.YES_OPTION) == 0) {
                        vectThread.clear();
                        ID = 0;
                        timerShow.start();
                        launghSearch(query);
                    } else {
                        timerShow.stop();
                        WorkbenchFrame.progress.setProgress(100);
                    }
                } else {
                    vectThread.clear();
                    ID = 0;
                    timerShow.start();
                    launghSearch(query);
                }
            }
        } else {
            JOptionPane.showMessageDialog(DMTWorkbench.frame, I18N.get("Search.no-criteria-has-been-made"), ""
                    + I18N.get("Text.Warning"), JOptionPane.WARNING_MESSAGE);
        }
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
        } catch (IOException | WriteException e) {
            JXErrorPane.showDialog(null, new ErrorInfo("Fatal error"
                    + "", e.getMessage(), null, null, e, Level.SEVERE, null));
        }
    }

    private void launghSearch(String query) {
        showItemInScrollPane(BLabLoading, I18N.get("Search.processing"));
        try {
            if (simulateNumber > 0) {
                progres = 0;
            }
            WorkbenchFrame.progress.setProgressStatus(I18N.get("Search.processing"));
            stat = Config.con.createStatement();
            ResultSet res = stat.executeQuery(query);
            while (res.next()) {
                if (IDIMAGE.equals(res.getString("dmt_image.id_image"))) {
                    continue;
                }
                startSearch(res);
            }
            WorkbenchFrame.progress.setProgressStatus(I18N.get("Search.displaying"));
            susp = true;
            exe = new RunSearch(DMTWorkbench.frame, true);
            exe.setVisible(true);
        } catch (SQLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error"), ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }

    private void startSearch(ResultSet res) {
        try {
            IDIMAGE = res.getString("dmt_image.id_image");
            vID.add(IDIMAGE);
            vImage.add(res.getString("image_name"));
            vPath.add(res.getString("path"));
            vRow.add(res.getString("row"));
            vDate.add(res.getDate("date"));
            vSize.add(res.getDouble("size"));
            Runnable runA = new Runnable() {
                @Override
                public void run() {
                    exeRecherche();
                }
            };
            Thread ta = new Thread(runA, "thread");
            vectThread.add(ta);
        } catch (SQLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error")
                    + "", ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }

    private void exeRecherche() {
        try {
            if (nbRow > tableModel.getRowCount()) {
                tableModel.addNewRow();
            }
            table.setValueAt(headerChecked, T, 0);
            table.setValueAt(vID.get(T), T, 1);
            table.setValueAt(vImage.get(T), T, 2);
            table.setValueAt(vPath.get(T), T, 3);
            table.setValueAt(vRow.get(T), T, 4);
            table.setValueAt(vDate.get(T), T, 5);
            table.setValueAt(vSize.get(T), T, 6);
            T++;
            nbRow++;
            progres++;
            RunSearch.progression.setValue(progres);
            RunSearch.progression.setMaximum(Max);
            RunSearch.progression.setString(progres + " / " + simulateNumber);
            susp = true;
        } catch (Exception ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error")
                    + "", ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }

    private int simulateQuery(String query) {
        int suite = 0;
        try {
            WorkbenchFrame.progress.setIndeterminate(true);
            WorkbenchFrame.progress.setProgressStatus(I18N.get("QuerySearch.Query-simulating"));
            showItemInScrollPane(BLabLoading, I18N.get("QuerySearch.Query-simulating"));
            stat = Config.con.createStatement();
            ResultSet res = stat.executeQuery(query);
            while (res.next()) {
                suite = res.getInt(1);
            }
            Max = suite;
            WorkbenchFrame.progress.setProgress(100);
        } catch (SQLException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error"
                    + ""), ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
        return suite;
    }

    private class MyItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                headerChecked = true;
                for (int i = 0; i < table.getRowCount(); i++) {
                    table.setValueAt(headerChecked, i, 0);
                }
            } else {
                headerChecked = false;
                for (int i = 0; i < table.getRowCount(); i++) {
                    table.setValueAt(headerChecked, i, 0);
                }
            }
        }
    }

    private void cleanTable() {
        table.unsort();
        table.setSortingEnabled(false);
        nbRow = 1;
        T = 0;
        int nbTable = table.getRowCount();
        while (nbTable > 0) {
            tableModel.removeNewRow(--nbTable);
        }
        LabImageDisplayed.setText(I18N.get("Text.Images-displayed") + " : 0");
        LabImageChecked.setText(I18N.get("Text.Images-checked") + " : 0");
        messagePreview();
    }

    private void clearVector() {
        vID.clear();
        vImage.clear();
        vPath.clear();
        vRow.clear();
        vDate.clear();
        vSize.clear();
        vectThread.clear();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static com.jidesoft.swing.JideButton BAddSearch;
    private org.jdesktop.swingx.JXBusyLabel BLabLoading;
    public static com.jidesoft.swing.JideButton BReset;
    private static com.jidesoft.swing.JideButton BSearch;
    private com.jidesoft.swing.JideButton BSubmit;
    private javax.swing.JCheckBox CBForm;
    private static javax.swing.JLabel LabImageChecked;
    private static javax.swing.JLabel LabImageDisplayed;
    private javax.swing.JMenuItem MIChecking;
    private javax.swing.JMenuItem MIChekAll;
    private javax.swing.JMenuItem MIClearTable;
    private javax.swing.JMenuItem MIExcel;
    private javax.swing.JMenuItem MIProperties;
    private javax.swing.JMenuItem MISubmit;
    private static javax.swing.JScrollPane ScrlAll;
    private javax.swing.JPopupMenu TabPopupMenu;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPanel panSwitcher;
    private javax.swing.JScrollPane scrollImage;
    // End of variables declaration//GEN-END:variables
    FloorTabbedPane _tabbedPane;
    static Timer timerShow;
    static SortableTable table;
    static MyTableModel tableModel;
    static Statement stat = null;
    static boolean susp = false;
    static int Max, nbRow = 1, T = 0, ID = 0, simulateNumber, progres = 0;
    static RunSearch exe;
    String IDIMAGE = "";
    JImagePanel panImage;
    JXBusyLabel labBusyApercu;
    String pathPreviewFile = null;
    boolean headerChecked = false;
    ArrayList<Thread> vectThread = new ArrayList<>();
    ArrayList<String> vID = new ArrayList<>();
    ArrayList<String> vImage = new ArrayList<>();
    ArrayList<String> vPath = new ArrayList<>();
    ArrayList<String> vRow = new ArrayList<>();
    ArrayList<Date> vDate = new ArrayList<>();
    ArrayList<Double> vSize = new ArrayList<>();
}
