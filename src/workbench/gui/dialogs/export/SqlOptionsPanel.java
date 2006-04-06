/*
 * SqlOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import workbench.db.ColumnIdentifier;
import workbench.gui.components.ColumnSelectorPanel;
import workbench.gui.components.KeyColumnSelectorPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SqlOptionsPanel 
	extends javax.swing.JPanel
	implements SqlOptions
{
	private List keyColumns;
	private ColumnSelectorPanel columnSelectorPanel;	
	private ResultInfo tableColumns;
	
	/** Creates new form SqlOptionsPanel */
	public SqlOptionsPanel(ResultInfo info)
	{
		initComponents();
		this.tableColumns = info;
		if (info == null)
		{
			this.selectKeys.setEnabled(false);
		}
	}

	public void saveSettings()
	{
		Settings s = Settings.getInstance();
		s.setProperty("workbench.export.sql.commitevery", this.getCommitEvery());
		s.setProperty("workbench.export.sql.createtable", this.getCreateTable());
	}
	
	public void restoreSettings()
	{
		Settings s = Settings.getInstance();
		this.setCommitEvery(s.getIntProperty("workbench.export.sql.commitevery", 0));
		this.setCreateTable(s.getBoolProperty("workbench.export.sql.createtable"));
	}
	
	public String getAlternateUpdateTable()
	{
		String s = alternateTable.getText();
		if (s != null && s.trim().length() > 0) return s.trim();
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

	//public boolean insertEnabled() { return useInsert.isEnabled(); }
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

	public List getKeyColumns()
	{
		return keyColumns;
	}

	private void selectColumns()
	{
		if (this.tableColumns == null) return;
		if (this.columnSelectorPanel == null) 
		{
			this.columnSelectorPanel = new KeyColumnSelectorPanel(this.tableColumns.getColumns(), this.tableColumns.getUpdateTable().getTableName());
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
			this.keyColumns = new ArrayList(size);
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
  private void initComponents()//GEN-BEGIN:initComponents
  {
    java.awt.GridBagConstraints gridBagConstraints;

    typeGroup = new javax.swing.ButtonGroup();
    commitLabel = new javax.swing.JLabel();
    commitCount = new javax.swing.JTextField();
    createTable = new javax.swing.JCheckBox();
    useUpdate = new javax.swing.JRadioButton();
    useInsert = new javax.swing.JRadioButton();
    alternateTable = new javax.swing.JTextField();
    jLabel1 = new javax.swing.JLabel();
    useDeleteInsert = new javax.swing.JRadioButton();
    selectKeys = new javax.swing.JButton();

    setLayout(new java.awt.GridBagLayout());

    commitLabel.setText(ResourceMgr.getString("LblExportCommitEvery"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(commitLabel, gridBagConstraints);

    commitCount.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
    add(commitCount, gridBagConstraints);

    createTable.setText(ResourceMgr.getString("LblExportIncludeCreateTable"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(createTable, gridBagConstraints);

    typeGroup.add(useUpdate);
    useUpdate.setText(ResourceMgr.getString("LblExportSqlUpdate"));
    useUpdate.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    add(useUpdate, gridBagConstraints);

    typeGroup.add(useInsert);
    useInsert.setSelected(true);
    useInsert.setText(ResourceMgr.getString("LblExportSqlInsert"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
    add(useInsert, gridBagConstraints);

    alternateTable.setColumns(15);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 0);
    add(alternateTable, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblUseExportTableName"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
    add(jLabel1, gridBagConstraints);

    typeGroup.add(useDeleteInsert);
    useDeleteInsert.setText(ResourceMgr.getString("LblExportSqlDeleteInsert"));
    useDeleteInsert.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    add(useDeleteInsert, gridBagConstraints);

    selectKeys.setText(ResourceMgr.getString("LblSelectKeyColumns"));
    selectKeys.addMouseListener(new java.awt.event.MouseAdapter()
    {
      public void mouseClicked(java.awt.event.MouseEvent evt)
      {
        selectKeysMouseClicked(evt);
      }
    });

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    add(selectKeys, gridBagConstraints);

  }//GEN-END:initComponents

	private void selectKeysMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_selectKeysMouseClicked
	{//GEN-HEADEREND:event_selectKeysMouseClicked
		this.selectColumns();
	}//GEN-LAST:event_selectKeysMouseClicked
	
	
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextField alternateTable;
  private javax.swing.JTextField commitCount;
  private javax.swing.JLabel commitLabel;
  private javax.swing.JCheckBox createTable;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JButton selectKeys;
  private javax.swing.ButtonGroup typeGroup;
  private javax.swing.JRadioButton useDeleteInsert;
  private javax.swing.JRadioButton useInsert;
  private javax.swing.JRadioButton useUpdate;
  // End of variables declaration//GEN-END:variables
	
}
