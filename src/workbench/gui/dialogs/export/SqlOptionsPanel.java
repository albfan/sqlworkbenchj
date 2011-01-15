/*
 * SqlOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.gui.components.ColumnSelectorPanel;
import workbench.gui.components.KeyColumnSelectorPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class SqlOptionsPanel
	extends javax.swing.JPanel
	implements SqlOptions, ActionListener
{
	private List<String> keyColumns;
	private ColumnSelectorPanel columnSelectorPanel;
	private ResultInfo tableColumns;

	public SqlOptionsPanel(ResultInfo info)
	{
		super();
		initComponents();
		setResultInfo(info);
		List<String> types = Settings.getInstance().getLiteralTypeList();
		ComboBoxModel model = new DefaultComboBoxModel(types.toArray());
		literalTypes.setModel(model);
	}

	public void setResultInfo(ResultInfo info)
	{
		this.tableColumns = info;

		boolean hasColumns = tableColumns != null;
		boolean keysPresent = (info == null ? false : info.hasPkColumns());
		this.selectKeys.setEnabled(hasColumns);

		this.setIncludeDeleteInsert(keysPresent);
		this.setIncludeUpdate(keysPresent);

		if (info != null)
		{
			TableIdentifier table = info.getUpdateTable();
			if (table != null)
			{
				this.alternateTable.setText(table.getTableName());
			}
			else
			{
				this.alternateTable.setText("target_table");
			}
		}
	}

	public void saveSettings()
	{
		Settings s = Settings.getInstance();
		s.setProperty("workbench.export.sql.commitevery", this.getCommitEvery());
		s.setProperty("workbench.export.sql.createtable", this.getCreateTable());
		s.setProperty("workbench.export.sql.saveas.dateliterals", this.getDateLiteralType());
	}

	public void restoreSettings()
	{
		Settings s = Settings.getInstance();
		this.setCommitEvery(s.getIntProperty("workbench.export.sql.commitevery", 0));
		this.setCreateTable(s.getBoolProperty("workbench.export.sql.createtable"));
		String def = s.getProperty("workbench.export.sql.default.dateliterals", "dbms");
		String type = s.getProperty("workbench.export.sql.saveas.dateliterals", def);
		this.literalTypes.setSelectedItem(type);
	}

	public String getDateLiteralType()
	{
		return (String)literalTypes.getSelectedItem();
	}

	public String getAlternateUpdateTable()
	{
		String s = alternateTable.getText();
		if (StringUtil.isNonBlank(s)) return s.trim();
		return null;
	}

	public void setAlternateUpdateTable(String table)
	{
		this.alternateTable.setText((table == null ? "" : table.trim()));
	}

	public int getCommitEvery()
	{
		int result = -1;
		try
		{
			String value = this.commitCount.getText();
			if (value != null && value.length() > 0)
			{
				result = Integer.parseInt(value);
			}
			else
			{
				result = 0;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public boolean updateEnabled() { return useUpdate.isEnabled(); }
	public boolean deleteInsertEnabled() { return useDeleteInsert.isEnabled(); }

	public boolean isSqlAllowed()
	{
		return updateEnabled() || deleteInsertEnabled();
	}

	public void setIncludeUpdate(boolean flag)
	{
		useUpdate.setEnabled(flag);
	}

	public void setIncludeDeleteInsert(boolean flag)
	{
		useDeleteInsert.setEnabled(flag);
	}

	public boolean getCreateInsert()
	{
		if (useInsert.isSelected()) return true;
		return false;
	}

	public boolean getCreateUpdate()
	{
		if (useUpdate.isEnabled()) return useUpdate.isSelected();
		return false;
	}

	public boolean getCreateDeleteInsert()
	{
		if (useDeleteInsert.isEnabled()) return useDeleteInsert.isSelected();
		return false;
	}

	public boolean getCreateTable()
	{
		return createTable.isSelected();
	}

	public void setCommitEvery(int value)
	{
		if (value > 0)
		{
			this.commitCount.setText(Integer.toString(value));
		}
		else
		{
			this.commitCount.setText("");
		}
	}

	public void setCreateInsert()
	{
		this.useInsert.setSelected(true);
	}

	public void setCreateUpdate()
	{
		if (this.useUpdate.isEnabled()) this.useUpdate.setSelected(true);
	}

	public void setCreateDeleteInsert()
	{
		if (this.useDeleteInsert.isEnabled()) this.useDeleteInsert.setSelected(true);
	}

	public void setCreateTable(boolean flag)
	{
		this.createTable.setSelected(flag);
	}

	public List<String> getKeyColumns()
	{
		return keyColumns;
	}

	private void selectColumns()
	{
		if (this.tableColumns == null) return;

		if (this.columnSelectorPanel == null)
		{
			this.columnSelectorPanel = new KeyColumnSelectorPanel(tableColumns);
		}
		else
		{
			this.columnSelectorPanel.selectColumns(this.keyColumns);
		}

		int choice = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), this.columnSelectorPanel, ResourceMgr.getString("MsgSelectKeyColumnsWindowTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (choice == JOptionPane.OK_OPTION)
		{
			this.keyColumns = null;

			List selected = this.columnSelectorPanel.getSelectedColumns();
			int size = selected.size();
			this.keyColumns = new ArrayList<String>(size);
			for (int i=0; i < size; i++)
			{
				ColumnIdentifier col = (ColumnIdentifier)selected.get(i);
				this.keyColumns.add(col.getColumnName());
			}

			boolean keysPresent = (size > 0);
			this.setIncludeDeleteInsert(keysPresent);
			this.setIncludeUpdate(keysPresent);
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
		GridBagConstraints gridBagConstraints;

    typeGroup = new ButtonGroup();
    createTable = new JCheckBox();
    useUpdate = new JRadioButton();
    useInsert = new JRadioButton();
    alternateTable = new JTextField();
    jLabel1 = new JLabel();
    useDeleteInsert = new JRadioButton();
    selectKeys = new JButton();
    jPanel1 = new JPanel();
    jLabel2 = new JLabel();
    literalTypes = new JComboBox();
    jPanel2 = new JPanel();
    commitLabel = new JLabel();
    commitCount = new JTextField();

    setLayout(new GridBagLayout());

    createTable.setText(ResourceMgr.getString("LblExportIncludeCreateTable")); // NOI18N
    createTable.setToolTipText(ResourceMgr.getString("d_LblExportIncludeCreateTable")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 0, 0, 0);
    add(createTable, gridBagConstraints);

    typeGroup.add(useUpdate);
    useUpdate.setText(ResourceMgr.getString("LblExportSqlUpdate")); // NOI18N
    useUpdate.setToolTipText(ResourceMgr.getString("d_LblExportSqlUpdate")); // NOI18N
    useUpdate.setEnabled(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    add(useUpdate, gridBagConstraints);

    typeGroup.add(useInsert);
    useInsert.setSelected(true);
    useInsert.setText(ResourceMgr.getString("LblExportSqlInsert")); // NOI18N
    useInsert.setToolTipText(ResourceMgr.getString("d_LblExportSqlInsert")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(3, 0, 0, 0);
    add(useInsert, gridBagConstraints);

    alternateTable.setColumns(15);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 0);
    add(alternateTable, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblUseExportTableName")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 4, 0, 0);
    add(jLabel1, gridBagConstraints);

    typeGroup.add(useDeleteInsert);
    useDeleteInsert.setText(ResourceMgr.getString("LblExportSqlDeleteInsert")); // NOI18N
    useDeleteInsert.setToolTipText(ResourceMgr.getString("d_LblExportSqlDeleteInsert")); // NOI18N
    useDeleteInsert.setEnabled(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    add(useDeleteInsert, gridBagConstraints);

    selectKeys.setText(ResourceMgr.getString("LblSelectKeyColumns")); // NOI18N
    selectKeys.setToolTipText(ResourceMgr.getString("d_LblSelectKeyColumns")); // NOI18N
    selectKeys.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    add(selectKeys, gridBagConstraints);

    jPanel1.setLayout(new BorderLayout(10, 0));

    jLabel2.setText(ResourceMgr.getString("LblLiteralType")); // NOI18N
    jPanel1.add(jLabel2, BorderLayout.WEST);

    literalTypes.setToolTipText(ResourceMgr.getDescription("LblLiteralType"));
    jPanel1.add(literalTypes, BorderLayout.CENTER);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(6, 4, 0, 0);
    add(jPanel1, gridBagConstraints);

    jPanel2.setLayout(new BorderLayout(10, 0));

    commitLabel.setText(ResourceMgr.getString("LblExportCommitEvery")); // NOI18N
    jPanel2.add(commitLabel, BorderLayout.WEST);

    commitCount.setColumns(5);
    jPanel2.add(commitCount, BorderLayout.CENTER);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    add(jPanel2, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(java.awt.event.ActionEvent evt) {
    if (evt.getSource() == selectKeys) {
      SqlOptionsPanel.this.selectKeysActionPerformed(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

private void selectKeysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectKeysActionPerformed
	selectColumns();
}//GEN-LAST:event_selectKeysActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  public JTextField alternateTable;
  public JTextField commitCount;
  public JLabel commitLabel;
  public JCheckBox createTable;
  public JLabel jLabel1;
  public JLabel jLabel2;
  public JPanel jPanel1;
  public JPanel jPanel2;
  public JComboBox literalTypes;
  public JButton selectKeys;
  public ButtonGroup typeGroup;
  public JRadioButton useDeleteInsert;
  public JRadioButton useInsert;
  public JRadioButton useUpdate;
  // End of variables declaration//GEN-END:variables

}
