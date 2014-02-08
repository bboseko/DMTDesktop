package com.osfac.dmt.workbench.ui.plugin.datastore;

import com.osfac.dmt.I18N;
import com.osfac.dmt.util.Block;
import com.osfac.dmt.util.CollectionUtil;
import com.osfac.dmt.workbench.WorkbenchContext;
import com.osfac.dmt.workbench.datastore.ConnectionDescriptor;
import com.osfac.dmt.workbench.model.LayerManager;
import com.osfac.dmt.workbench.ui.RecordPanel;
import com.osfac.dmt.workbench.ui.RecordPanelModel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * The panel used to choose a connection and input a SQL query.
 */
public class RunDatastoreQueryPanel extends ConnectionPanel
        implements ActionListener, RecordPanelModel {

    private JTextField layerNameTextField;
    //private JTextField maxFeaturesTextField;
    private HashMap queryMap = new HashMap();
    private ArrayList currentConnectionQueries = new ArrayList();
    private int currentIndex = -1;
    private RecordPanel recordPanel = new RecordPanel(this);
    private LayerManager layerManager;

    public RunDatastoreQueryPanel(WorkbenchContext context) {
        super(context);
        layerManager = context.getLayerManager();
        initialize();
    }

    public int getRecordCount() {
        int num = 0;
        if (currentConnectionQueries != null) {
            num = currentConnectionQueries.size();
        }
        return num;
    }

    public void setCurrentIndex(int index) {
        currentIndex = index;
        String query = null;
        if (index > -1) {
            query = (String) currentConnectionQueries.get(index);
        }
        getQueryTextArea().setText(query);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    private void initialize() {
        JButton jbView = new JButton(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.View"));
        jbView.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                queryTextArea.insert("${view:-1}", queryTextArea.getCaretPosition());
            }
        });
        JButton jbFence = new JButton(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Fence"));
        jbFence.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                queryTextArea.insert("${fence:-1}", queryTextArea.getCaretPosition());
            }
        });
        JPanel jpButtons = new JPanel(new java.awt.GridLayout(2, 1));
        jpButtons.add(jbView);
        jpButtons.add(jbFence);
        addRow(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Layer-Name"), getLayerNameTextField(), null, false);
        //addRow(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Max-Features"), getMaxFeaturesTextField(), null, false);
        addRow(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Query"), new JScrollPane(getQueryTextArea()) {
            {
                setPreferredSize(new Dimension(MAIN_COLUMN_WIDTH, 100));
            }
        }, jpButtons, true);

        // We are not using addRow because we want the widgets centered over the
        // OK/Cancel buttons.
        add(recordPanel,
                new GridBagConstraints(0, //x
                3, // y
                3, // width
                1, // height
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                INSETS,
                0,
                0));

        getConnectionComboBox().addActionListener(this);
    }

    private JTextField getLayerNameTextField() {
        if (layerNameTextField == null) {
            layerNameTextField = new JTextField(
                    layerManager.uniqueLayerName(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.New-Query-Layer")));
        }
        return layerNameTextField;
    }

    //private JTextField getMaxFeaturesTextField() {
    //    if (maxFeaturesTextField == null) {
    //        maxFeaturesTextField = new ValidatingTextField("", 10,
    //                new ValidatingTextField.BoundedIntValidator(1,
    //                        Integer.MAX_VALUE));
    //    }
    //    return maxFeaturesTextField;
    //}
    private JTextArea getQueryTextArea() {
        if (queryTextArea == null) {
            queryTextArea = new JTextArea();
        }
        return queryTextArea;
    }
    private JTextArea queryTextArea;

    public String validateInput() {
        String errMsg = super.validateInput();
        if (errMsg == null) {
            if (getQuery().length() == 0) {
                errMsg = I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Required-field-missing-Query");
            }
        }
        return errMsg;
    }

    public String getLayerName() {
        return layerManager.uniqueLayerName(layerNameTextField.getText().trim());
    }

    public void setLayerName(String layerName) {
        layerNameTextField.setText(layerName);
    }

    public String getQuery() {
        return queryTextArea.getText().trim();
    }

    public void setQuery(String query) {
        getQueryTextArea().setText(query);
    }

    public void saveQuery() {
        String query = getQuery();
        // maybe we should check for duplicates
        currentConnectionQueries.add(query);
        currentIndex = currentConnectionQueries.size() - 1;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        ConnectionDescriptor cd = getConnectionDescriptor();
        if (cd != null) {
            if (queryMap.containsKey(cd)) {
                ArrayList prevQueries = (ArrayList) queryMap.get(cd);
                currentConnectionQueries = prevQueries;
            } else {
                currentConnectionQueries = new ArrayList();
                queryMap.put(cd, currentConnectionQueries);
            }
            setCurrentIndex(currentConnectionQueries.size() - 1);
            recordPanel.updateAppearance();
        }
    }

    /**
     * @return null if the user has left the Max Features text field blank.
     */
    //public Integer getMaxFeatures() {
    //    return maxFeaturesTextField.getText().trim().length() > 0 ? new Integer(
    //            maxFeaturesTextField.getText().trim()) : null;
    //}
    protected Collection connectionDescriptors() {
        return CollectionUtil.select(super.connectionDescriptors(),
                new Block() {
                    public Object yield(Object connectionDescriptor) {
                        try {
                            return Boolean.valueOf(connectionManager()
                                    .getDriver(
                                    ((ConnectionDescriptor) connectionDescriptor)
                                    .getDataStoreDriverClassName())
                                    .isAdHocQuerySupported());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }
}