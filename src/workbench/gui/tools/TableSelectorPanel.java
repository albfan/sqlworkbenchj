/*
 * TableSelectorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.tools;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JPanel;

import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TableSelectorPanel
	extends JPanel
	implements ItemListener, ActionListener
{
	private WbConnection dbConnection;
	private String currentSchema;
	private TableIdentifier currentTable;
	private PropertyChangeListener client;
	private String clientPropName;
	//private boolean tablesOnly = false;
	private boolean allowNewTable = false;
	private TableIdentifier newTableId = new TableIdentifier();
	//private WbMenuItem editNewTableNameItem;
	//private JPopupMenu popupMenu;

	/** Creates new form TableSelectorPanel */
	public TableSelectorPanel()
	{
		initComponents();
		this.schemaLabel.setText(ResourceMgr.getString("LabelSchema"));
		this.tableLabel.setText(ResourceMgr.getString("LabelTable"));
		this.tableSelector.setMaximumRowCount(15);
		this.editNewTableNameButton.setVisible(false);
	}

	public void reset()
	{
		this.schemaSelector.removeItemListener(this);
		this.tableSelector.removeItemListener(this);
		this.schemaSelector.removeAllItems();
		this.tableSelector.removeAllItems();
	}
	
//	public void setTablesOnly(boolean tablesOnly) { this.tablesOnly = tablesOnly; }
//	public boolean getTablesOnly() { return this.tablesOnly; }

	public void resetNewTableItem()
	{
		if (this.newTableId != null)
		{
			this.newTableId.setNewTable(true);
			this.newTableId.setTable(null);
			this.newTableId.setSchema(null);
			this.repaint();
		}
	}

	public void allowNewTable(boolean flag)
	{
		this.allowNewTable = flag;
		this.editNewTableNameButton.removeActionListener(this);
		int count = this.tableSelector.getItemCount();
		if (count > 0)
		{
			this.tableSelector.removeItemListener(this);
			if (this.allowNewTable)
			{
				int newTableIndex = -1;

				for (int i=0; i < count; i++)
				{
					Object item = this.tableSelector.getItemAt(i);
					if (item instanceof TableIdentifier)
					{
						newTableIndex = i;
						break;
					}
				}
				if (newTableIndex == -1)
				{
					this.tableSelector.addItem(this.newTableId);
				}
			}
			else
			{
				this.tableSelector.removeItem(this.newTableId);
			}
			this.tableSelector.addItemListener(this);
		}
		this.editNewTableNameButton.setVisible(flag);
		if (flag)
		{
			this.editNewTableNameButton.addActionListener(this);
		}
	}

	public void removeChangeListener()
	{
		this.client = null;
		this.clientPropName = null;
	}

	public void setChangeListener(PropertyChangeListener l, String propName)
	{
		this.client = l;
		this.clientPropName = propName;
	}

	public void setConnection(WbConnection conn)
	{
		this.dbConnection = conn;
		if (this.dbConnection != null)
		{
			this.retrieveSchemas();
		}
		else
		{
			this.reset();
		}
	}

	public void retrieveSchemas()
	{
		try
		{
			StringBuffer s = new StringBuffer(this.dbConnection.getMetadata().getSchemaTerm().toLowerCase());
			s.setCharAt(0, Character.toUpperCase(s.charAt(0)));
			this.schemaLabel.setText(s.toString());

			this.schemaSelector.removeItemListener(this);
			this.tableSelector.removeItemListener(this);

			this.schemaSelector.removeAllItems();
			this.tableSelector.removeAllItems();

			this.schemaSelector.addItem("*");
			this.currentSchema = null;

			List schemas = this.dbConnection.getMetadata().getSchemas();
			String current = this.dbConnection.getMetadata().getCurrentSchema();
			
			for (int i=0; i < schemas.size(); i++)
			{
				String schema = (String)schemas.get(i);
				if (StringUtil.isEmptyString(schema)) continue;
				this.schemaSelector.addItem(schema);
				if (current != null && schema.equalsIgnoreCase(current)) this.currentSchema = schema;
			}
			if (this.currentSchema != null)
			{
				schemaSelector.setSelectedItem(this.currentSchema);
			}
			else 
			{
				this.schemaSelector.setSelectedIndex(0);
			}
			this.retrieveTables();
			this.schemaSelector.addItemListener(this);
		}
		catch (Exception e)
		{
			LogMgr.logError("TableSelectorPanel.retrieveSchemas()", "Could not retrieve schema list", e);
		}
	}

	public void setEnabled(boolean enable)
	{
		super.setEnabled(enable);
		this.schemaSelector.setEnabled(enable);
		this.tableSelector.setEnabled(enable);

		if (enable)
		{
			this.tableLabel.setForeground(Color.BLACK);
			this.schemaLabel.setForeground(Color.BLACK);
		}
		else
		{
			this.tableLabel.setForeground(Color.DARK_GRAY);
			this.schemaLabel.setForeground(Color.DARK_GRAY);
		}
		if (!enable)
		{
			this.tableSelector.setSelectedItem(null);
			this.schemaSelector.setSelectedItem(null);
		}
		this.repaint();
	}

	public void retrieveTables()
	{
		try
		{
			this.tableSelector.removeItemListener(this);
			List tables = this.dbConnection.getMetadata().getSelectableObjectsList(this.currentSchema);
			this.tableSelector.removeAllItems();
			if (this.allowNewTable)
			{
				this.tableSelector.addItem(this.newTableId);
			}
			int count = tables.size();
			for (int i=0; i < count; i++)
			{
				this.tableSelector.addItem(tables.get(i));
			}
			this.editNewTableNameButton.setEnabled(false);
			tableSelector.setSelectedItem(null);
			TableIdentifier old = this.currentTable;
			this.currentTable = null;
			this.firePropertyChange(old, null);
		}
		catch (Exception e)
		{
			LogMgr.logError("TableSelectorPanel.retrieveTables()", "Could not retrieve table list", e);
		}
		finally
		{
			this.tableSelector.addItemListener(this);
		}
	}

	public TableIdentifier getSelectedTable()
	{
		if (!this.isEnabled()) return null;
		Object selected = this.tableSelector.getSelectedItem();
		if (selected == null) return null;

		return (TableIdentifier)selected;
	}

	public void findAndSelectTable(String aTable)
	{
		if (aTable == null) return;

		int count = this.tableSelector.getItemCount();
		for (int i=0; i < count; i++)
		{
			String table = null;
			Object item = this.tableSelector.getItemAt(i);
			if (item instanceof TableIdentifier)
			{
				table = ((TableIdentifier)item).getTableName();
			}
			if (table == null) continue;

			if (aTable.equalsIgnoreCase(table))
			{
				this.tableSelector.setSelectedIndex(i);
				break;
			}
		}
	}

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getStateChange() != ItemEvent.SELECTED) return;

		if (e.getSource() == this.schemaSelector)
		{
			this.currentSchema = (String)this.schemaSelector.getSelectedItem();
			if (this.currentSchema != null)
			{
				this.retrieveTables();
			}
			this.newTableId.setSchema(this.currentSchema);
		}
		else if (e.getSource() == this.tableSelector)
		{
			final TableIdentifier old = this.currentTable;
			this.currentTable = this.getSelectedTable();
			if (this.currentTable != null)
			{
				this.editNewTableNameButton.setEnabled(currentTable.isNewTable());
			}
			else
			{
				this.editNewTableNameButton.setEnabled(false);
			}
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					firePropertyChange(old, currentTable);
				}
			});
		}
	}

	private void firePropertyChange(TableIdentifier oldTable, TableIdentifier newTable)
	{
		if (this.client == null) return;
		if (oldTable == null && newTable == null) return;
		if (oldTable != null && newTable != null && oldTable.equals(newTable)) return;

		PropertyChangeEvent evt = new PropertyChangeEvent(this, this.clientPropName, oldTable, newTable);
		this.client.propertyChange(evt);
	}

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getSource() == this.editNewTableNameButton)
		{
			Object item = this.tableSelector.getSelectedItem();
			if (item instanceof TableIdentifier)
			{
				TableIdentifier id = (TableIdentifier)item;
				String name = id.getTableName();
				name = WbSwingUtilities.getUserInput(this, ResourceMgr.getString("TxtEnterNewTableName"), name);
				if (name != null)
				{
					id.setTable(name);
					this.tableSelector.repaint();
				}
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    schemaSelector = new javax.swing.JComboBox();
    tableSelector = new javax.swing.JComboBox();
    schemaLabel = new javax.swing.JLabel();
    tableLabel = new javax.swing.JLabel();
    editNewTableNameButton = new javax.swing.JButton();

    setLayout(new java.awt.GridBagLayout());

    setMinimumSize(new java.awt.Dimension(68, 65));
    setPreferredSize(new java.awt.Dimension(68, 65));
    schemaSelector.setMaximumRowCount(0);
    schemaSelector.setAlignmentX(0.0F);
    schemaSelector.setAlignmentY(0.0F);
    schemaSelector.setMaximumSize(new java.awt.Dimension(200, 25));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.3;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
    add(schemaSelector, gridBagConstraints);

    tableSelector.setMaximumRowCount(0);
    tableSelector.setAlignmentX(0.0F);
    tableSelector.setAlignmentY(0.0F);
    tableSelector.setMaximumSize(new java.awt.Dimension(80, 25));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.7;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
    add(tableSelector, gridBagConstraints);

    schemaLabel.setText("jLabel1");
    schemaLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
    schemaLabel.setMaximumSize(new java.awt.Dimension(32768, 21));
    schemaLabel.setMinimumSize(new java.awt.Dimension(34, 21));
    schemaLabel.setPreferredSize(new java.awt.Dimension(34, 21));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
    add(schemaLabel, gridBagConstraints);

    tableLabel.setText("jLabel1");
    tableLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
    tableLabel.setMaximumSize(new java.awt.Dimension(32768, 21));
    tableLabel.setMinimumSize(new java.awt.Dimension(34, 21));
    tableLabel.setPreferredSize(new java.awt.Dimension(34, 21));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
    add(tableLabel, gridBagConstraints);

    editNewTableNameButton.setIcon(ResourceMgr.getImage("Rename"));
    editNewTableNameButton.setToolTipText(ResourceMgr.getString("LabelEditNewTableName"));
    editNewTableNameButton.setBorder(new javax.swing.border.EtchedBorder());
    editNewTableNameButton.setMaximumSize(new java.awt.Dimension(24, 24));
    editNewTableNameButton.setMinimumSize(new java.awt.Dimension(24, 24));
    editNewTableNameButton.setPreferredSize(new java.awt.Dimension(24, 24));
    editNewTableNameButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 4, 0);
    add(editNewTableNameButton, gridBagConstraints);

  }
  // </editor-fold>//GEN-END:initComponents



  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton editNewTableNameButton;
  private javax.swing.JLabel schemaLabel;
  private javax.swing.JComboBox schemaSelector;
  private javax.swing.JLabel tableLabel;
  private javax.swing.JComboBox tableSelector;
  // End of variables declaration//GEN-END:variables

}
