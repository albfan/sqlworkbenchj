/*
 * TableSelectorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import workbench.log.LogMgr;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.ObjectNameSorter;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.FlatButton;

import workbench.util.StringUtil;

/**
 * A Panel to select a table.
 *
 * A list of available schemas and tables is displayed using two dropdowns.
 * If the selected table has changed, a PropertyChangeListener
 * is notified.
 *
 * @author  Thomas Kellerer
 */
public class TableSelectorPanel
	extends JPanel
	implements ItemListener, ActionListener
{
	private WbConnection dbConnection;
	private String currentSchema;
	protected TableIdentifier currentTable;
	private PropertyChangeListener client;
	private String clientPropName;
	private boolean allowNewTable = false;
	private TableIdentifier newTableId = new TableIdentifier();

	public TableSelectorPanel()
	{
		super();
		initComponents();
		this.schemaLabel.setText(ResourceMgr.getString("LblSchema"));
		this.tableLabel.setText(ResourceMgr.getString("LblTable"));
		this.tableSelector.setMaximumRowCount(15);
		this.editNewTableNameButton.setVisible(false);
		autoSync.setText(ResourceMgr.getString("LblDPAutoSyncTarget"));
		autoSync.setToolTipText(ResourceMgr.getDescription("LblDPAutoSyncTarget"));
		WbSwingUtilities.makeEqualSize(refreshButton, editNewTableNameButton);
		WbSwingUtilities.makeEqualHeight(refreshButton, tableSelector, schemaSelector);
	}

	public void reset()
	{
		this.schemaSelector.removeItemListener(this);
		this.tableSelector.removeItemListener(this);
		this.schemaSelector.removeAllItems();
		this.tableSelector.removeAllItems();
	}

	public boolean isAutoSyncSelected()
	{
		if (autoSync.isVisible())
		{
			return autoSync.isSelected();
		}
		return false;
	}

	public void setAutoSyncSelected(boolean flag)
	{
		if (autoSync.isVisible())
		{
			autoSync.setSelected(flag);
		}
	}

	public void setAutoSyncVisible(boolean flag)
	{
		autoSync.setVisible(flag);
		autoSync.setEnabled(flag);
	}

	public void resetNewTableItem()
	{
		if (this.newTableId != null)
		{
			this.newTableId.setNewTable(true);
			this.newTableId.parseTableIdentifier(null);
			this.newTableId.setSchema(null);
			WbSwingUtilities.repaintNow(this);
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
		this.refreshButton.setEnabled(this.dbConnection != null);
		if (this.dbConnection != null)
		{
			retrieveSchemas();
		}
		else
		{
			this.reset();
		}
	}

	/**
	 * Sets the name of the JComboBox component
	 * that contains the table list.
	 * Needed for automated GUI testing.
	 * @param name
	 */
	public void setTableDropDownName(String name)
	{
		this.tableSelector.setName(name);
	}

	public void retrieveSchemas()
	{
		WbSwingUtilities.invoke(this::_retrieveSchemas);
	}

	private void _retrieveSchemas()
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			StringBuilder s = new StringBuilder(this.dbConnection.getMetadata().getSchemaTerm().toLowerCase());
			s.setCharAt(0, Character.toUpperCase(s.charAt(0)));
			this.schemaLabel.setText(s.toString());

			this.schemaSelector.removeItemListener(this);
			this.tableSelector.removeItemListener(this);

			this.schemaSelector.removeAllItems();
			this.tableSelector.removeAllItems();

			schemaSelector.addItem("*");
			currentSchema = null;

			List<String> schemas = this.dbConnection.getMetadata().getSchemas();
			String current = this.dbConnection.getMetadata().getCurrentSchema();

			for (String schema : schemas)
			{
				if (StringUtil.isEmptyString(schema)) continue;
				schemaSelector.addItem(schema);
				if (current != null && schema.equalsIgnoreCase(current)) this.currentSchema = schema;
			}

			if (currentSchema != null)
			{
				schemaSelector.setSelectedItem(currentSchema);
			}
			else
			{
				schemaSelector.setSelectedIndex(0);
			}
			retrieveTables();
			schemaSelector.addItemListener(this);
		}
		catch (Exception e)
		{
			LogMgr.logError("TableSelectorPanel.retrieveSchemas()", "Could not retrieve schema list", e);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	@Override
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
			tableSelector.removeItemListener(this);
			List<TableIdentifier> tables = dbConnection.getMetadata().getSelectableObjectsList(null, currentSchema);

			tables.sort(new ObjectNameSorter());
			tableSelector.removeAllItems();

			if (allowNewTable)
			{
				tableSelector.addItem(newTableId);
			}

			for (TableIdentifier table : tables)
			{
				if (dbConnection.getMetadata().isSequenceType(table.getType())) continue;
				table.setShowTablenameOnly(true);
				tableSelector.addItem(table);
			}

			editNewTableNameButton.setEnabled(false);
			tableSelector.setSelectedItem(null);
			TableIdentifier old = currentTable;
			currentTable = null;
			firePropertyChange(old, null);
		}
		catch (Exception e)
		{
			LogMgr.logError("TableSelectorPanel.retrieveTables()", "Could not retrieve table list", e);
		}
		finally
		{
			tableSelector.addItemListener(this);
		}
	}

	public TableIdentifier getSelectedTable()
	{
		if (!this.isEnabled()) return null;
		Object selected = this.tableSelector.getSelectedItem();
		if (selected == null) return null;

		TableIdentifier tbl = (TableIdentifier)selected;
		if (!"*".equals(currentSchema))
		{
			String schema = dbConnection.getMetadata().quoteObjectname(currentSchema);
			tbl.setSchema(schema);
		}
		return tbl;
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

	@Override
	public void itemStateChanged(ItemEvent e)
	{
		if (e.getStateChange() != ItemEvent.SELECTED) return;

		if (e.getSource() == this.schemaSelector)
		{
			this.currentSchema = (String)this.schemaSelector.getSelectedItem();
			if (this.currentSchema != null)
			{
				retrieveTables();
			}
			newTableId.setSchema(this.currentSchema);
		}
		else if (e.getSource() == this.tableSelector)
		{
			final TableIdentifier old = this.currentTable;
			currentTable = this.getSelectedTable();
			if (currentTable != null)
			{
				editNewTableNameButton.setEnabled(currentTable.isNewTable());
			}
			else
			{
				editNewTableNameButton.setEnabled(false);
			}
			EventQueue.invokeLater(() ->
      {
        firePropertyChange(old, currentTable);
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

	@Override
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
					id.parseTableIdentifier(name);
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
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    schemaSelector = new JComboBox();
    tableSelector = new JComboBox();
    schemaLabel = new JLabel();
    editNewTableNameButton = new FlatButton();
    refreshButton = new FlatButton();
    jPanel1 = new JPanel();
    tableLabel = new JLabel();
    autoSync = new JCheckBox();

    setMinimumSize(new Dimension(68, 65));
    setPreferredSize(new Dimension(68, 65));
    setLayout(new GridBagLayout());

    schemaSelector.setAlignmentX(0.0F);
    schemaSelector.setAlignmentY(0.0F);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.3;
    add(schemaSelector, gridBagConstraints);

    tableSelector.setMaximumRowCount(0);
    tableSelector.setAlignmentX(0.0F);
    tableSelector.setAlignmentY(0.0F);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.7;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    add(tableSelector, gridBagConstraints);

    schemaLabel.setText("jLabel1");
    schemaLabel.setVerticalAlignment(SwingConstants.TOP);
    schemaLabel.setMaximumSize(new Dimension(32768, 21));
    schemaLabel.setMinimumSize(new Dimension(34, 21));
    schemaLabel.setPreferredSize(new Dimension(34, 21));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(3, 0, 2, 0);
    add(schemaLabel, gridBagConstraints);

    editNewTableNameButton.setIcon(IconMgr.getInstance().getLabelIcon("rename"));
    editNewTableNameButton.setToolTipText(ResourceMgr.getString("LblEditNewTableName"));
    editNewTableNameButton.setEnabled(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    add(editNewTableNameButton, gridBagConstraints);

    refreshButton.setIcon(IconMgr.getInstance().getLabelIcon("Refresh"));
    refreshButton.setToolTipText("");
    refreshButton.setEnabled(false);
    refreshButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
        refreshButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    add(refreshButton, gridBagConstraints);

    jPanel1.setLayout(new GridBagLayout());

    tableLabel.setText("jLabel1");
    tableLabel.setVerticalAlignment(SwingConstants.TOP);
    tableLabel.setMaximumSize(new Dimension(32768, 21));
    tableLabel.setMinimumSize(new Dimension(34, 21));
    tableLabel.setPreferredSize(new Dimension(34, 21));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(4, 0, 0, 0);
    jPanel1.add(tableLabel, gridBagConstraints);

    autoSync.setText("jCheckBox1");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    jPanel1.add(autoSync, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 4, 0, 0);
    add(jPanel1, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

	private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_refreshButtonActionPerformed
	{//GEN-HEADEREND:event_refreshButtonActionPerformed
		retrieveSchemas();
	}//GEN-LAST:event_refreshButtonActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JCheckBox autoSync;
  private JButton editNewTableNameButton;
  private JPanel jPanel1;
  private JButton refreshButton;
  private JLabel schemaLabel;
  private JComboBox schemaSelector;
  private JLabel tableLabel;
  private JComboBox tableSelector;
  // End of variables declaration//GEN-END:variables

}
