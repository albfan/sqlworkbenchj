/*
 * ConnectionEditorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import workbench.db.*;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.BooleanPropertyEditor;
import workbench.gui.components.DelimiterDefinitionPanel;
import workbench.gui.components.FlatButton;
import workbench.gui.components.IntegerPropertyEditor;
import workbench.gui.components.PasswordPropertyEditor;
import workbench.gui.components.StringPropertyEditor;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbColorPicker;
import workbench.gui.components.WbTraversalPolicy;
import workbench.interfaces.SimplePropertyEditor;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.DelimiterDefinition;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class ConnectionEditorPanel
	extends JPanel
	implements PropertyChangeListener, ActionListener, ValidatingComponent
{
	private ConnectionProfile currentProfile;
	private ProfileListModel sourceModel;
	private boolean init;
	private List<SimplePropertyEditor> editors;

	public ConnectionEditorPanel()
	{
		super();
		this.initComponents();

		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(tfProfileName);
		policy.addComponent(cbDrivers);
		policy.addComponent(tfURL);
		policy.addComponent(tfUserName);
		policy.addComponent(asSysDBA);
		policy.addComponent(tfPwd);
		policy.addComponent(showPassword);
		policy.addComponent(tfFetchSize);
		policy.addComponent(cbAutocommit);
		policy.addComponent(extendedProps);
		policy.addComponent(cbStorePassword);
		policy.addComponent(rollbackBeforeDisconnect);
		policy.addComponent(cbSeparateConnections);
		policy.addComponent(confirmUpdates);
		policy.addComponent(readOnly);
		policy.addComponent(cbIgnoreDropErrors);
		policy.addComponent(includeNull);
		policy.addComponent(emptyStringIsNull);
		policy.addComponent(rememberExplorerSchema);
		policy.addComponent(trimCharData);
		policy.addComponent(removeComments);
		policy.addComponent(hideWarnings);
		policy.addComponent(editConnectionScriptsButton);
		policy.addComponent(altDelimiter.getTextField());
		policy.addComponent(altDelimiter.getCheckBox());
		policy.addComponent(tfWorkspaceFile);
		policy.addComponent(editConnectionScriptsButton);
		policy.addComponent(editFilterButton);

		policy.setDefaultComponent(tfProfileName);

		this.setFocusCycleRoot(false);
		this.setFocusTraversalPolicy(policy);

		this.initEditorList();

		this.selectWkspButton.addActionListener(this);
		this.showPassword.addActionListener(this);
		this.infoColor.setActionListener(this);
		this.confirmUpdates.addActionListener(this);
		this.readOnly.addActionListener(this);
	}

	public JComponent getInitialFocusComponent()
	{
		return tfProfileName;
	}

	public void setFocusToTitle()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				tfProfileName.requestFocusInWindow();
				tfProfileName.selectAll();
			}
		});
	}

	private void initEditorList()
	{
		this.editors = new LinkedList<SimplePropertyEditor>();
		initEditorList(this);
		altDelimiter.addPropertyChangeListener(DelimiterDefinitionPanel.PROP_SLD, this);
		altDelimiter.addPropertyChangeListener(DelimiterDefinitionPanel.PROP_DELIM, this);
	}

	private void initEditorList(Container parent)
	{
		for (int i = 0; i < parent.getComponentCount(); i++)
		{
			Component c = parent.getComponent(i);
			if (c instanceof SimplePropertyEditor)
			{
				SimplePropertyEditor ed = (SimplePropertyEditor)c;
				this.editors.add(ed);
				String name = c.getName();
				c.addPropertyChangeListener(name, this);
				ed.setImmediateUpdate(true);
			}
			else if (c instanceof JPanel && !(c instanceof DelimiterDefinitionPanel))
			{
				initEditorList((JPanel)c);
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tfProfileName = new StringPropertyEditor();
        cbDrivers = new javax.swing.JComboBox();
        tfURL = new StringPropertyEditor();
        tfUserName = new StringPropertyEditor();
        tfPwd = new PasswordPropertyEditor();
        asSysDBA = new BooleanPropertyEditor();
        lblUsername = new javax.swing.JLabel();
        lblPwd = new javax.swing.JLabel();
        lblDriver = new javax.swing.JLabel();
        lblUrl = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        showPassword = new FlatButton();
        wbOptionsPanel = new javax.swing.JPanel();
        cbStorePassword = new BooleanPropertyEditor();
        rollbackBeforeDisconnect = new BooleanPropertyEditor();
        cbIgnoreDropErrors = new BooleanPropertyEditor();
        cbSeparateConnections = new BooleanPropertyEditor();
        emptyStringIsNull = new BooleanPropertyEditor();
        includeNull = new BooleanPropertyEditor();
        removeComments = new BooleanPropertyEditor();
        rememberExplorerSchema = new BooleanPropertyEditor();
        trimCharData = new BooleanPropertyEditor();
        controlUpdates = new javax.swing.JPanel();
        confirmUpdates = new BooleanPropertyEditor();
        readOnly = new BooleanPropertyEditor();
        hideWarnings = new BooleanPropertyEditor();
        checkOpenTrans = new BooleanPropertyEditor();
        jPanel2 = new javax.swing.JPanel();
        tfFetchSize = new IntegerPropertyEditor();
        cbAutocommit = new BooleanPropertyEditor();
        extendedProps = new FlatButton();
        timeoutLabel = new javax.swing.JLabel();
        tfTimeout = new IntegerPropertyEditor();
        jLabel1 = new javax.swing.JLabel();
        fetchSizeLabel = new javax.swing.JLabel();
        propLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        workspaceFileLabel = new javax.swing.JLabel();
        infoColor = new WbColorPicker(true);
        infoColorLabel = new javax.swing.JLabel();
        altDelimiter = new workbench.gui.components.DelimiterDefinitionPanel();
        altDelimLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        tfWorkspaceFile = new StringPropertyEditor();
        selectWkspButton = new FlatButton();
        jSeparator3 = new javax.swing.JSeparator();
        timeoutpanel = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        editConnectionScriptsButton = new FlatButton();
        scriptLabel = new javax.swing.JLabel();
        filterPanel = new javax.swing.JPanel();
        editFilterButton = new FlatButton();
        filterLabel = new javax.swing.JLabel();

        FormListener formListener = new FormListener();

        setMinimumSize(new java.awt.Dimension(220, 200));
        setLayout(new java.awt.GridBagLayout());

        tfProfileName.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        tfProfileName.setName("name"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 5, 5, 5);
        add(tfProfileName, gridBagConstraints);

        cbDrivers.setMaximumSize(new java.awt.Dimension(32767, 20));
        cbDrivers.setMinimumSize(new java.awt.Dimension(40, 20));
        cbDrivers.setName("driverclass"); // NOI18N
        cbDrivers.setPreferredSize(new java.awt.Dimension(120, 20));
        cbDrivers.setVerifyInputWhenFocusTarget(false);
        cbDrivers.addItemListener(formListener);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 2, 6);
        add(cbDrivers, gridBagConstraints);

        tfURL.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        tfURL.setMaximumSize(new java.awt.Dimension(2147483647, 20));
        tfURL.setName("url"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 6, 2, 6);
        add(tfURL, gridBagConstraints);

        tfUserName.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        tfUserName.setMaximumSize(new java.awt.Dimension(2147483647, 20));
        tfUserName.setName("username"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 6, 2, 2);
        add(tfUserName, gridBagConstraints);

        tfPwd.setName("password"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 6, 4, 2);
        add(tfPwd, gridBagConstraints);

        asSysDBA.setText(ResourceMgr.getString("LblSysDba")); // NOI18N
        asSysDBA.setToolTipText(ResourceMgr.getString("d_LblSysDba")); // NOI18N
        asSysDBA.setBorder(null);
        asSysDBA.setName("oracleSysDBA");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 0);
        add(asSysDBA, gridBagConstraints);

        lblUsername.setLabelFor(tfUserName);
        lblUsername.setText(ResourceMgr.getString("LblUsername")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 0);
        add(lblUsername, gridBagConstraints);

        lblPwd.setLabelFor(tfPwd);
        lblPwd.setText(ResourceMgr.getString("LblPassword")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 0);
        add(lblPwd, gridBagConstraints);

        lblDriver.setLabelFor(cbDrivers);
        lblDriver.setText(ResourceMgr.getString("LblDriver")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 0);
        add(lblDriver, gridBagConstraints);

        lblUrl.setLabelFor(tfURL);
        lblUrl.setText(ResourceMgr.getString("LblDbURL")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 2, 0);
        add(lblUrl, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        add(jSeparator2, gridBagConstraints);

        showPassword.setText(ResourceMgr.getString("LblShowPassword")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 4, 6);
        add(showPassword, gridBagConstraints);

        wbOptionsPanel.setLayout(new java.awt.GridBagLayout());

        cbStorePassword.setSelected(true);
        cbStorePassword.setText(ResourceMgr.getString("LblSavePassword")); // NOI18N
        cbStorePassword.setToolTipText(ResourceMgr.getString("d_LblSavePassword")); // NOI18N
        cbStorePassword.setName("storePassword"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        wbOptionsPanel.add(cbStorePassword, gridBagConstraints);

        rollbackBeforeDisconnect.setText(ResourceMgr.getString("LblRollbackBeforeDisconnect")); // NOI18N
        rollbackBeforeDisconnect.setToolTipText(ResourceMgr.getString("d_LblRollbackBeforeDisconnect")); // NOI18N
        rollbackBeforeDisconnect.setMargin(new java.awt.Insets(2, 0, 2, 2));
        rollbackBeforeDisconnect.setName("rollbackBeforeDisconnect"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        wbOptionsPanel.add(rollbackBeforeDisconnect, gridBagConstraints);

        cbIgnoreDropErrors.setSelected(true);
        cbIgnoreDropErrors.setText(ResourceMgr.getString("LblIgnoreDropErrors")); // NOI18N
        cbIgnoreDropErrors.setToolTipText(ResourceMgr.getString("d_LblIgnoreDropErrors")); // NOI18N
        cbIgnoreDropErrors.setName("ignoreDropErrors"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        wbOptionsPanel.add(cbIgnoreDropErrors, gridBagConstraints);

        cbSeparateConnections.setText(ResourceMgr.getString("LblSeparateConnections")); // NOI18N
        cbSeparateConnections.setToolTipText(ResourceMgr.getString("d_LblSeparateConnections")); // NOI18N
        cbSeparateConnections.setName("useSeparateConnectionPerTab"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        wbOptionsPanel.add(cbSeparateConnections, gridBagConstraints);

        emptyStringIsNull.setText(ResourceMgr.getString("LblEmptyStringIsNull")); // NOI18N
        emptyStringIsNull.setToolTipText(ResourceMgr.getString("d_LblEmptyStringIsNull")); // NOI18N
        emptyStringIsNull.setName("emptyStringIsNull"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        wbOptionsPanel.add(emptyStringIsNull, gridBagConstraints);

        includeNull.setText(ResourceMgr.getString("LblIncludeNullInInsert")); // NOI18N
        includeNull.setToolTipText(ResourceMgr.getString("d_LblIncludeNullInInsert")); // NOI18N
        includeNull.setMargin(new java.awt.Insets(2, 0, 2, 2));
        includeNull.setName("includeNullInInsert"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        wbOptionsPanel.add(includeNull, gridBagConstraints);

        removeComments.setText(ResourceMgr.getString("LblRemoveComments")); // NOI18N
        removeComments.setToolTipText(ResourceMgr.getString("d_LblRemoveComments")); // NOI18N
        removeComments.setMargin(new java.awt.Insets(2, 0, 2, 2));
        removeComments.setName("removeComments"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        wbOptionsPanel.add(removeComments, gridBagConstraints);

        rememberExplorerSchema.setText(ResourceMgr.getString("LblRememberSchema")); // NOI18N
        rememberExplorerSchema.setToolTipText(ResourceMgr.getString("d_LblRememberSchema")); // NOI18N
        rememberExplorerSchema.setMargin(new java.awt.Insets(2, 0, 2, 2));
        rememberExplorerSchema.setName("storeExplorerSchema"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        wbOptionsPanel.add(rememberExplorerSchema, gridBagConstraints);

        trimCharData.setText(ResourceMgr.getString("LblTrimCharData")); // NOI18N
        trimCharData.setToolTipText(ResourceMgr.getString("d_LblTrimCharData")); // NOI18N
        trimCharData.setName("trimCharData"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        wbOptionsPanel.add(trimCharData, gridBagConstraints);

        controlUpdates.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        confirmUpdates.setText(ResourceMgr.getString("LblConfirmDbUpdates")); // NOI18N
        confirmUpdates.setToolTipText(ResourceMgr.getString("d_LblConfirmDbUpdates")); // NOI18N
        confirmUpdates.setMargin(new java.awt.Insets(2, 0, 2, 5));
        confirmUpdates.setName("confirmUpdates"); // NOI18N
        controlUpdates.add(confirmUpdates);

        readOnly.setText(ResourceMgr.getString("LblConnReadOnly")); // NOI18N
        readOnly.setToolTipText(ResourceMgr.getString("d_LblConnReadOnly")); // NOI18N
        readOnly.setMargin(new java.awt.Insets(2, 5, 2, 2));
        readOnly.setName("readOnly"); // NOI18N
        controlUpdates.add(readOnly);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 1, 0);
        wbOptionsPanel.add(controlUpdates, gridBagConstraints);

        hideWarnings.setText(ResourceMgr.getString("LblHideWarn")); // NOI18N
        hideWarnings.setToolTipText(ResourceMgr.getString("d_LblHideWarn")); // NOI18N
        hideWarnings.setName("hideWarnings"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        wbOptionsPanel.add(hideWarnings, gridBagConstraints);

        checkOpenTrans.setText(ResourceMgr.getString("LblCheckUncommitted")); // NOI18N
        checkOpenTrans.setToolTipText(ResourceMgr.getString("d_LblCheckUncommitted")); // NOI18N
        checkOpenTrans.setMargin(new java.awt.Insets(2, 0, 2, 2));
        checkOpenTrans.setName("detectOpenTransaction");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        wbOptionsPanel.add(checkOpenTrans, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 4, 6);
        add(wbOptionsPanel, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        tfFetchSize.setToolTipText(ResourceMgr.getString("d_LblFetchSize")); // NOI18N
        tfFetchSize.setMinimumSize(new java.awt.Dimension(20, 20));
        tfFetchSize.setName("defaultFetchSize"); // NOI18N
        tfFetchSize.setPreferredSize(new java.awt.Dimension(24, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        jPanel2.add(tfFetchSize, gridBagConstraints);

        cbAutocommit.setText("Autocommit");
        cbAutocommit.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        cbAutocommit.setName("autocommit"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cbAutocommit, gridBagConstraints);

        extendedProps.setText(ResourceMgr.getString("LblConnExtendedProps")); // NOI18N
        extendedProps.setToolTipText(ResourceMgr.getString("d_LblConnExtendedProps")); // NOI18N
        extendedProps.addMouseListener(formListener);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        jPanel2.add(extendedProps, gridBagConstraints);

        timeoutLabel.setLabelFor(tfTimeout);
        timeoutLabel.setText(ResourceMgr.getString("LblConnTimeout")); // NOI18N
        timeoutLabel.setToolTipText(ResourceMgr.getString("d_LblConnTimeout")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 16, 1, 0);
        jPanel2.add(timeoutLabel, gridBagConstraints);

        tfTimeout.setToolTipText(ResourceMgr.getString("d_LblConnTimeout")); // NOI18N
        tfTimeout.setMinimumSize(new java.awt.Dimension(20, 20));
        tfTimeout.setName("connectionTimeout"); // NOI18N
        tfTimeout.setPreferredSize(new java.awt.Dimension(24, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        jPanel2.add(tfTimeout, gridBagConstraints);

        jLabel1.setText(ResourceMgr.getString("LblSeconds")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        jPanel2.add(jLabel1, gridBagConstraints);

        fetchSizeLabel.setLabelFor(tfFetchSize);
        fetchSizeLabel.setText(ResourceMgr.getString("LblFetchSize")); // NOI18N
        fetchSizeLabel.setToolTipText(ResourceMgr.getString("d_LblFetchSize")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 15, 1, 0);
        jPanel2.add(fetchSizeLabel, gridBagConstraints);

        propLabel.setFocusable(false);
        propLabel.setIconTextGap(0);
        propLabel.setInheritsPopupMenu(false);
        propLabel.setMaximumSize(new java.awt.Dimension(16, 16));
        propLabel.setMinimumSize(new java.awt.Dimension(16, 16));
        propLabel.setPreferredSize(new java.awt.Dimension(16, 16));
        propLabel.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        jPanel2.add(propLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 0, 5);
        add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        workspaceFileLabel.setLabelFor(tfWorkspaceFile);
        workspaceFileLabel.setText(ResourceMgr.getString("LblOpenWksp")); // NOI18N
        workspaceFileLabel.setToolTipText(ResourceMgr.getString("d_LblOpenWksp")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel3.add(workspaceFileLabel, gridBagConstraints);

        infoColor.setToolTipText(ResourceMgr.getDescription("LblInfoColor"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 1, 0);
        jPanel3.add(infoColor, gridBagConstraints);

        infoColorLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        infoColorLabel.setText(ResourceMgr.getString("LblInfoColor")); // NOI18N
        infoColorLabel.setToolTipText(ResourceMgr.getDescription("LblInfoColor")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 1, 0);
        jPanel3.add(infoColorLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 4, 1, 0);
        jPanel3.add(altDelimiter, gridBagConstraints);

        altDelimLabel.setText(ResourceMgr.getString("LblAltDelimit")); // NOI18N
        altDelimLabel.setToolTipText(ResourceMgr.getString("d_LblAltDelimit")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 1, 0);
        jPanel3.add(altDelimLabel, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        tfWorkspaceFile.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        tfWorkspaceFile.setToolTipText(ResourceMgr.getDescription("LblOpenWksp"));
        tfWorkspaceFile.setMaximumSize(new java.awt.Dimension(2147483647, 20));
        tfWorkspaceFile.setName("workspaceFile"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(tfWorkspaceFile, gridBagConstraints);

        selectWkspButton.setText("...");
        selectWkspButton.setMaximumSize(new java.awt.Dimension(26, 22));
        selectWkspButton.setMinimumSize(new java.awt.Dimension(26, 22));
        selectWkspButton.setPreferredSize(new java.awt.Dimension(26, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        jPanel1.add(selectWkspButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 4, 0, 6);
        jPanel3.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 6, 6);
        add(jPanel3, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
        add(jSeparator3, gridBagConstraints);

        timeoutpanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 6, 0, 0);
        add(timeoutpanel, gridBagConstraints);

        jPanel6.setLayout(new java.awt.GridBagLayout());

        editConnectionScriptsButton.setText(ResourceMgr.getString("LblConnScripts")); // NOI18N
        editConnectionScriptsButton.setToolTipText(ResourceMgr.getString("d_LblConnScripts")); // NOI18N
        editConnectionScriptsButton.addActionListener(formListener);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel6.add(editConnectionScriptsButton, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel6.add(scriptLabel, gridBagConstraints);

        filterPanel.setLayout(new java.awt.GridBagLayout());

        editFilterButton.setText(ResourceMgr.getString("LblSchemaFilterBtn")); // NOI18N
        editFilterButton.setToolTipText(ResourceMgr.getString("d_LblSchemaFilterBtn")); // NOI18N
        editFilterButton.addActionListener(formListener);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        filterPanel.add(editFilterButton, gridBagConstraints);

        filterLabel.setMaximumSize(new java.awt.Dimension(16, 16));
        filterLabel.setMinimumSize(new java.awt.Dimension(16, 16));
        filterLabel.setPreferredSize(new java.awt.Dimension(16, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        filterPanel.add(filterLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 22, 0, 0);
        jPanel6.add(filterPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(7, 6, 0, 0);
        add(jPanel6, gridBagConstraints);
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.ItemListener, java.awt.event.MouseListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == editConnectionScriptsButton) {
                ConnectionEditorPanel.this.editConnectionScriptsButtonActionPerformed(evt);
            }
            else if (evt.getSource() == editFilterButton) {
                ConnectionEditorPanel.this.editFilterButtonActionPerformed(evt);
            }
        }

        public void itemStateChanged(java.awt.event.ItemEvent evt) {
            if (evt.getSource() == cbDrivers) {
                ConnectionEditorPanel.this.cbDriversItemStateChanged(evt);
            }
        }

        public void mouseClicked(java.awt.event.MouseEvent evt) {
            if (evt.getSource() == extendedProps) {
                ConnectionEditorPanel.this.extendedPropsMouseClicked(evt);
            }
        }

        public void mouseEntered(java.awt.event.MouseEvent evt) {
        }

        public void mouseExited(java.awt.event.MouseEvent evt) {
        }

        public void mousePressed(java.awt.event.MouseEvent evt) {
        }

        public void mouseReleased(java.awt.event.MouseEvent evt) {
        }
    }// </editor-fold>//GEN-END:initComponents

	private void extendedPropsMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_extendedPropsMouseClicked
	{//GEN-HEADEREND:event_extendedPropsMouseClicked
		this.editExtendedProperties();
	}//GEN-LAST:event_extendedPropsMouseClicked

	private void cbDriversItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbDriversItemStateChanged
	{//GEN-HEADEREND:event_cbDriversItemStateChanged
		if (this.init)
		{
			return;
		}

		if (evt.getStateChange() == ItemEvent.SELECTED)
		{
			String oldDriver = null;
			DbDriver newDriver = null;
			try
			{
				oldDriver = this.currentProfile.getDriverclass();
				newDriver = (DbDriver)this.cbDrivers.getSelectedItem();
				if (this.currentProfile != null)
				{
					this.currentProfile.setDriverclass(newDriver.getDriverClass());
					this.currentProfile.setDriverName(newDriver.getName());
				}
				if (oldDriver == null || !oldDriver.equals(newDriver.getDriverClass()))
				{
					this.tfURL.setText(newDriver.getSampleUrl());
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ConnectionProfilePanel.cbDriversItemStateChanged()", "Error changing driver", e);
			}

			if (!newDriver.canReadLibrary())
			{
				final Frame parent = (Frame)(SwingUtilities.getWindowAncestor(this).getParent());
				final DbDriver toSelect = newDriver;

				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						if (WbSwingUtilities.getYesNo(ConnectionEditorPanel.this, ResourceMgr.getString("MsgDriverLibraryNotReadable")))
						{
							DriverEditorDialog.showDriverDialog(parent, toSelect);
						}
					}
				});
			}
		}
	}//GEN-LAST:event_cbDriversItemStateChanged

	private void editConnectionScriptsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editConnectionScriptsButtonActionPerformed
		Dialog d = (Dialog)SwingUtilities.getWindowAncestor(this);
		EditConnectScriptsPanel.editScripts(d, this.getProfile());
		checkScripts();
	}//GEN-LAST:event_editConnectionScriptsButtonActionPerformed

	private void editFilterButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_editFilterButtonActionPerformed
	{//GEN-HEADEREND:event_editFilterButtonActionPerformed
		Dialog d = (Dialog)SwingUtilities.getWindowAncestor(this);
		EditConnectionFiltersPanel.editFilter(d, this.getProfile());
		checkFilters();
	}//GEN-LAST:event_editFilterButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JLabel altDelimLabel;
    protected workbench.gui.components.DelimiterDefinitionPanel altDelimiter;
    protected javax.swing.JCheckBox asSysDBA;
    protected javax.swing.JCheckBox cbAutocommit;
    protected javax.swing.JComboBox cbDrivers;
    protected javax.swing.JCheckBox cbIgnoreDropErrors;
    protected javax.swing.JCheckBox cbSeparateConnections;
    protected javax.swing.JCheckBox cbStorePassword;
    protected javax.swing.JCheckBox checkOpenTrans;
    protected javax.swing.JCheckBox confirmUpdates;
    protected javax.swing.JPanel controlUpdates;
    protected javax.swing.JButton editConnectionScriptsButton;
    protected javax.swing.JButton editFilterButton;
    protected javax.swing.JCheckBox emptyStringIsNull;
    protected javax.swing.JButton extendedProps;
    protected javax.swing.JLabel fetchSizeLabel;
    protected javax.swing.JLabel filterLabel;
    protected javax.swing.JPanel filterPanel;
    protected javax.swing.JCheckBox hideWarnings;
    protected javax.swing.JCheckBox includeNull;
    protected workbench.gui.components.WbColorPicker infoColor;
    protected javax.swing.JLabel infoColorLabel;
    protected javax.swing.JLabel jLabel1;
    protected javax.swing.JPanel jPanel1;
    protected javax.swing.JPanel jPanel2;
    protected javax.swing.JPanel jPanel3;
    protected javax.swing.JPanel jPanel6;
    protected javax.swing.JSeparator jSeparator2;
    protected javax.swing.JSeparator jSeparator3;
    protected javax.swing.JLabel lblDriver;
    protected javax.swing.JLabel lblPwd;
    protected javax.swing.JLabel lblUrl;
    protected javax.swing.JLabel lblUsername;
    protected javax.swing.JLabel propLabel;
    protected javax.swing.JCheckBox readOnly;
    protected javax.swing.JCheckBox rememberExplorerSchema;
    protected javax.swing.JCheckBox removeComments;
    protected javax.swing.JCheckBox rollbackBeforeDisconnect;
    protected javax.swing.JLabel scriptLabel;
    protected javax.swing.JButton selectWkspButton;
    protected javax.swing.JButton showPassword;
    protected javax.swing.JTextField tfFetchSize;
    protected javax.swing.JTextField tfProfileName;
    protected javax.swing.JPasswordField tfPwd;
    protected javax.swing.JTextField tfTimeout;
    protected javax.swing.JTextField tfURL;
    protected javax.swing.JTextField tfUserName;
    protected javax.swing.JTextField tfWorkspaceFile;
    protected javax.swing.JLabel timeoutLabel;
    protected javax.swing.JPanel timeoutpanel;
    protected javax.swing.JCheckBox trimCharData;
    protected javax.swing.JPanel wbOptionsPanel;
    protected javax.swing.JLabel workspaceFileLabel;
    // End of variables declaration//GEN-END:variables

	public void setDrivers(List<DbDriver> aDriverList)
	{
		if (aDriverList != null)
		{
			this.init = true;
			Object currentDriver = this.cbDrivers.getSelectedItem();
			try
			{
				Comparator<DbDriver> comparator = new Comparator<DbDriver>()
				{
					@Override
					public int compare(DbDriver o1, DbDriver o2)
					{
						return StringUtil.compareStrings(o1.getName(), o2.getName(), true);
					}
				};
				Collections.sort(aDriverList, comparator);
				this.cbDrivers.setModel(new DefaultComboBoxModel(aDriverList.toArray()));
				if (currentDriver != null)
				{
					this.cbDrivers.setSelectedItem(currentDriver);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ConnectionEditorPanel.setDrivers()", "Error when setting new driver list", e);
			}
			finally
			{
				this.init = false;
			}
		}
	}

	public void editExtendedProperties()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				ConnectionPropertiesEditor.editProperties(SwingUtilities.getWindowAncestor(ConnectionEditorPanel.this), currentProfile);
				checkExtendedProps();
			}
		});
	}

	public void selectWorkspace()
	{
		FileDialogUtil util = new FileDialogUtil();
		String filename = util.getWorkspaceFilename(SwingUtilities.getWindowAncestor(this), false, true);
		if (filename == null)
		{
			return;
		}
		this.tfWorkspaceFile.setText(filename);
	}

	void setSourceList(ProfileListModel aSource)
	{
		this.sourceModel = aSource;
	}

	public void updateProfile()
	{
		if (this.init) return;
		if (this.currentProfile == null) return;
		if (this.editors == null)	return;

		boolean changed = false;

		for (SimplePropertyEditor editor : editors)
		{
			changed = changed || editor.isChanged();
			editor.applyChanges();
		}

		if (altDelimiter.getDelimiter().isChanged())
		{
			changed = true;
			currentProfile.setAlternateDelimiter(altDelimiter.getDelimiter());
		}

		DbDriver current = getCurrentDriver();
		String driverName = currentProfile.getDriverName();
		if (!current.getName().equals(driverName))
		{
			// an alternate driver was chosen, because the original driver was not available.
			LogMgr.logDebug("ConnectionEditorPanel.updateProfile()", "Adjusting selected driver name for non-existing driver: " + currentProfile.getDriverName());
			currentProfile.setDriverName(current.getName());
			changed = true;
		}

		if (changed)
		{
			this.sourceModel.profileChanged(this.currentProfile);
		}
	}

	public DbDriver getCurrentDriver()
	{
		DbDriver drv = (DbDriver)cbDrivers.getSelectedItem();
		return drv;
	}

	public ConnectionProfile getProfile()
	{
		this.updateProfile();
		return this.currentProfile;
	}

	private void initPropertyEditors()
	{
		if (this.editors == null) return;
		if (this.currentProfile == null) return;

		for (SimplePropertyEditor editor : editors)
		{
			Component c = (Component)editor;
			String property = c.getName();
			if (property != null)
			{
				editor.setSourceObject(this.currentProfile, property);
			}
		}
	}

	private int getFilterSize(ObjectNameFilter f)
	{
		if (f == null) return 0;
		return f.getSize();
	}

	private void checkFilters()
	{
		int f1 = currentProfile == null ? 0 : getFilterSize(currentProfile.getSchemaFilter());
		int f2 = currentProfile == null ? 0 : getFilterSize(currentProfile.getCatalogFilter());
		boolean hasFilter = (f1 + f2) > 0;
		if (hasFilter)
		{
			filterLabel.setIcon(ResourceMgr.getPicture("tick"));
		}
		else
		{
			filterLabel.setIcon(null);
		}
	}

	private void checkScripts()
	{
		boolean hasScript = (currentProfile == null ? false : currentProfile.hasConnectScript());
		if (hasScript)
		{
			scriptLabel.setIcon(ResourceMgr.getPicture("tick"));
		}
		else
		{
			scriptLabel.setIcon(null);
		}
	}

	private void checkExtendedProps()
	{
		Properties props = (currentProfile == null ? null : currentProfile.getConnectionProperties());
		if (props != null && props.size() > 0)
		{
			propLabel.setIcon(ResourceMgr.getPicture("tick"));
		}
		else
		{
			propLabel.setIcon(null);
		}
	}

	public void setProfile(ConnectionProfile aProfile)
	{
		if (aProfile == null)
		{
			return;
		}

		this.currentProfile = aProfile;

		try
		{
			this.init = true;

			this.initPropertyEditors();

			String drvClass = aProfile.getDriverclass();
			DbDriver drv = null;
			if (drvClass != null)
			{
				String name = aProfile.getDriverName();
				drv = ConnectionMgr.getInstance().findDriverByName(drvClass, name);
			}

			this.altDelimiter.setDelimiter(this.currentProfile.getAlternateDelimiter());
			cbDrivers.setSelectedItem(drv);

			Color c = this.currentProfile.getInfoDisplayColor();
			this.infoColor.setSelectedColor(c);
			checkExtendedProps();
			checkScripts();
			checkFilters();
			checkOracle();
			checkUncommitted();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionEditorPanel.setProfile()", "Error setting profile", e);
		}
		finally
		{
			this.init = false;
		}
	}

	private void checkUncommitted()
	{
		String drvClass = getCurrentDriver().getDriverClass();
		boolean canCheck = TransactionChecker.Factory.supportsTransactionCheck(drvClass);
		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints hideCons = layout.getConstraints(hideWarnings);
		if (canCheck)
		{
			hideCons.weightx = 0;
			checkOpenTrans.setEnabled(true);
			checkOpenTrans.setVisible(true);
			checkOpenTrans.setSelected(currentProfile.getDetectOpenTransaction());
			if (drvClass.contains("oracle"))
			{
				checkOpenTrans.setToolTipText(ResourceMgr.getDescription("LblCheckUncommittedOra"));
			}
			else
			{
				checkOpenTrans.setToolTipText(ResourceMgr.getDescription("LblCheckUncommitted"));
			}
		}
		else
		{
			hideCons.weightx = 1;
			checkOpenTrans.setEnabled(false);
			checkOpenTrans.setVisible(false);
			checkOpenTrans.setSelected(false);
		}
	}

	private void checkOracle()
	{
		String url = this.tfURL.getText();
		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints cons = layout.getConstraints(tfUserName);
		if (url.startsWith("jdbc:oracle:"))
		{
			cons.gridwidth = 1;
			cons.insets = new Insets(0, 6, 2, 2);
			layout.setConstraints(tfUserName, cons);
			asSysDBA.setVisible(true);
			asSysDBA.setEnabled(true);
			asSysDBA.setSelected(currentProfile.getOracleSysDBA());
		}
		else
		{
			asSysDBA.setVisible(false);
			asSysDBA.setEnabled(false);
			asSysDBA.setSelected(false);
			cons.gridwidth = 2;
			cons.insets = new Insets(0, 6, 2, 6);
			layout.setConstraints(tfUserName, cons);
		}
	}

	/**
	 * This method gets called when a bound property is changed.
	 *
	 * @param evt A PropertyChangeEvent object describing the event source
	 * and the property that has changed.
	 *
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (!this.init)
		{
			if (evt.getSource() == this.altDelimiter)
			{
				// As the alternateDelimiter is a not attached to the profile itself,
				// we have to propagate any updated delimiter object to the profile
				this.currentProfile.setAlternateDelimiter(altDelimiter.getDelimiter());
			}
			this.sourceModel.profileChanged(this.currentProfile);
		}
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getSource() == this.readOnly)
		{
			if (readOnly.isSelected())
			{
				confirmUpdates.setSelected(false);
			}
		}
		else if (e.getSource() == this.confirmUpdates)
		{
			if (confirmUpdates.isSelected())
			{
				this.readOnly.setSelected(false);
			}
		}
		else if (e.getSource() == this.selectWkspButton)
		{
			this.selectWorkspace();
		}
		else if (e.getSource() == this.showPassword)
		{
			String pwd = this.getProfile().getInputPassword();
			String title = ResourceMgr.getString("LblCurrentPassword");
			title += " " + this.getProfile().getUsername();
			JTextField f = new JTextField();
			f.setDisabledTextColor(Color.BLACK);
			f.setEditable(false);
			f.setText(pwd);
			Border b = new CompoundBorder(new LineBorder(Color.LIGHT_GRAY), new EmptyBorder(2, 2, 2, 2));
			f.setBorder(b);
			TextComponentMouseListener l = new TextComponentMouseListener();
			f.addMouseListener(l);
			//WbSwingUtilities.showMessage(this, f);
			JOptionPane.showMessageDialog(this.getParent(), f, title, JOptionPane.PLAIN_MESSAGE);
		}
		else if (e.getSource() == this.infoColor && this.currentProfile != null)
		{
			this.currentProfile.setInfoDisplayColor(this.infoColor.getSelectedColor());
		}
	}

	@Override
	public boolean validateInput()
	{
		DelimiterDefinition delim = getProfile().getAlternateDelimiter();
		if (delim != null && delim.isStandard())
		{
			WbSwingUtilities.showErrorMessageKey(this, "ErrWrongAltDelim");
			return false;
		}
		return true;
	}

	@Override
	public void componentDisplayed()
	{
	// nothing to do
	}
}
