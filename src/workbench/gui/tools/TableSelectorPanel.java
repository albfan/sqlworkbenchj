/*
 * TableSelectorPanel.java
 *
 * Created on December 20, 2003, 9:54 PM
 */

package workbench.gui.tools;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JPanel;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;

import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class TableSelectorPanel 
	extends JPanel
	implements ActionListener
{
	private WbConnection dbConnection;
	private String currentSchema;
	private TableIdentifier currentTable;
	private PropertyChangeListener client;
	private String clientPropName;
	private boolean tablesOnly = false;
	
	/** Creates new form TableSelectorPanel */
	public TableSelectorPanel()
	{
		this.tablesOnly = tablesOnly;
		initComponents();
		this.schemaLabel.setText(ResourceMgr.getString("LabelSchema"));
		this.tableLabel.setText(ResourceMgr.getString("LabelTable"));
	}
	
	public void setTablesOnly(boolean tablesOnly) { this.tablesOnly = tablesOnly; }
	public boolean getTablesOnly() { return this.tablesOnly; }

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
			this.schemaSelector.removeActionListener(this);
			this.tableSelector.removeActionListener(this);
			this.schemaSelector.removeAllItems();
			this.tableSelector.removeAllItems();
		}
	}

	public void retrieveSchemas()
	{
		try
		{
			StringBuffer s = new StringBuffer(this.dbConnection.getMetadata().getSchemaTerm().toLowerCase());
			s.setCharAt(0, Character.toUpperCase(s.charAt(0)));
			this.schemaLabel.setText(s.toString());
			
			this.schemaSelector.removeActionListener(this);
			this.schemaSelector.removeAllItems();
			this.tableSelector.removeAllItems();
			this.schemaSelector.addItem(ResourceMgr.getString("LabelLoadingProgress"));
			this.schemaSelector.setSelectedIndex(0);
			
			List schemas = this.dbConnection.getMetadata().getSchemas();
			String user = this.dbConnection.getMetadata().getUserName();
			
			this.schemaSelector.removeAllItems();
			this.schemaSelector.addItem("*");
			int numSchemas = 0;
			this.currentSchema = null;
			
			for (int i=0; i < schemas.size(); i++)
			{
				String schema = (String)schemas.get(i);
				if (schema == null || schema.trim().length() == 0) continue;
				this.schemaSelector.addItem(schema);
				numSchemas++;
				if (user.equalsIgnoreCase(schema)) this.currentSchema = schema;
			}
			if (numSchemas == 0)
			{
				this.schemaSelector.setSelectedIndex(0);
				this.retrieveTables();
			}
			else if (this.currentSchema != null)
			{
				schemaSelector.setSelectedItem(this.currentSchema);
				this.retrieveTables();
			}
			this.schemaSelector.addActionListener(this);
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
			String[] types;
			this.tableSelector.removeActionListener(this);
			if (this.tablesOnly) types = new String[] { "TABLE"};
			else types = new String[] { "TABLE", "VIEW" };
			
			DataStore tables = this.dbConnection.getMetadata().getTables(null, this.currentSchema, types);
			tables.sortByColumn(DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, true);
			this.tableSelector.removeAllItems();
			int count = tables.getRowCount();
			for (int i=0; i < count; i++)
			{
				String table = tables.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
				this.tableSelector.addItem(table);
			}
			tableSelector.setSelectedItem(null);
			TableIdentifier old = this.currentTable;
			this.currentTable = null;
			this.firePropertyChange(old, null);
			this.tableSelector.addActionListener(this);
		}
		catch (Exception e)
		{
			LogMgr.logError("TableSelectorPanel.retrieveTables()", "Could not retrieve table list", e);
		}
	}

	public TableIdentifier getSelectedTable()
	{
		if (!this.isEnabled()) return null;
		String schema = (String)this.schemaSelector.getSelectedItem();
		if ("*".equals(schema) || (schema != null && schema.length() == 0)) schema = null;
		String table = (String)this.tableSelector.getSelectedItem();
		
		if (table == null || table.trim().length() == 0) return null;
		return new TableIdentifier(null, schema, table);
	}
	
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getSource() == this.schemaSelector)
		{
			this.currentSchema = (String)this.schemaSelector.getSelectedItem();
			if (this.currentSchema != null)
			{
				this.retrieveTables();
			}
		}
		else if (e.getSource() == this.tableSelector)
		{
			TableIdentifier old = this.currentTable;
			this.currentTable = this.getSelectedTable();
			firePropertyChange(old, this.currentTable);
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
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  private void initComponents()//GEN-BEGIN:initComponents
  {
    java.awt.GridBagConstraints gridBagConstraints;

    schemaSelector = new javax.swing.JComboBox();
    tableSelector = new javax.swing.JComboBox();
    schemaLabel = new javax.swing.JLabel();
    tableLabel = new javax.swing.JLabel();

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
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.3;
    add(schemaSelector, gridBagConstraints);

    tableSelector.setMaximumRowCount(0);
    tableSelector.setAlignmentX(0.0F);
    tableSelector.setAlignmentY(0.0F);
    tableSelector.setMaximumSize(new java.awt.Dimension(80, 25));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.7;
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

  }//GEN-END:initComponents
	
	
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel schemaLabel;
  private javax.swing.JComboBox schemaSelector;
  private javax.swing.JLabel tableLabel;
  private javax.swing.JComboBox tableSelector;
  // End of variables declaration//GEN-END:variables
	
}
