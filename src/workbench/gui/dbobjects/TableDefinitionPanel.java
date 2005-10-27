/*
 * TableDefinitionPanel.java
 *
 * Created on 8. September 2005, 19:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.FindPanel;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.renderer.SqlTypeRenderer;
import workbench.gui.sql.ExecuteSqlDialog;
import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;
import workbench.util.WbThread;

/**
 *
 * @author thomas
 */
public class TableDefinitionPanel
	extends JPanel
	implements Reloadable, ActionListener, ListSelectionListener
{
	public static final String INDEX_PROP = "index";
	private WbTable tableDefinition;
	private QuickFilterPanel columnFilter;
	private FindPanel columnSearcher;
	private WbAction createIndexAction;
	private TableIdentifier currentTable;
	private String currentObjectType;
	private WbConnection dbConnection;
	private List changeListener = new LinkedList();
	private WbAction reloadAction;
	private boolean busy;

	/** Creates a new instance of TableDefinitionPanel */
	public TableDefinitionPanel()
	{
		this.tableDefinition = new WbTable();
		this.tableDefinition.setAdjustToColumnLabel(false);
		this.tableDefinition.setSelectOnRightButtonClick(true);

		this.reloadAction = new ReloadAction(this);
		this.reloadAction.setEnabled(false);

		JPanel bar = new JPanel(new GridBagLayout());
		columnSearcher = new FindPanel(this.tableDefinition, 20);

		String[] cols = new String[] {"COLUMN_NAME", "DATA_TYPE", "PK", "NULLABLE", "DEFAULT", "REMARKS", "JAVA_TYPE"};
		columnFilter  = new QuickFilterPanel(this.tableDefinition, cols, true, "columnlist");
		columnFilter.addToToolbar(reloadAction, 0);
		//columnFilter.setToolbarBorder(new EtchedBorder());

		GridBagConstraints cc = new GridBagConstraints();
		cc.anchor = GridBagConstraints.WEST;
		cc.gridx = 0;
		cc.weightx = 0.6;
		cc.ipadx = 0;
		cc.ipady = 0;
		cc.insets = new Insets(0, 0, 0, 5);
		cc.fill = GridBagConstraints.HORIZONTAL;
		bar.add(columnFilter, cc);

		cc.gridx ++;
		cc.anchor = GridBagConstraints.WEST;
		cc.fill = GridBagConstraints.NONE;
		cc.weightx = 0.4;
		cc.insets = new Insets(0, 0, 0, 0);
		columnSearcher.setBorder(new DividerBorder(DividerBorder.LEFT));
		bar.add(columnSearcher, cc);

		WbScrollPane scroll = new WbScrollPane(this.tableDefinition);
		this.setLayout(new BorderLayout());
		this.add(bar, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);

		this.createIndexAction = new WbAction(this, "create-index");
		this.createIndexAction.setEnabled(false);
		this.createIndexAction.initMenuDefinition("MnuTxtCreateIndex");

		this.tableDefinition.getSelectionModel().addListSelectionListener(this);
		this.tableDefinition.addPopupAction(this.createIndexAction, true);

	}

	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		this.changeListener.add(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l)
	{
		this.changeListener.remove(l);
	}

	private void fireIndexChanged(String indexName)
	{
		if (this.changeListener.size() == 0) return;

		Iterator itr = this.changeListener.iterator();
		PropertyChangeEvent evt = new PropertyChangeEvent(this, INDEX_PROP, null, indexName);
		while (itr.hasNext())
		{
			PropertyChangeListener l = (PropertyChangeListener)itr.next();
			l.propertyChange(evt);
		}
	}

	private final Object busyLock = new Object();
	public boolean isBusy()
	{
		synchronized (this.busyLock)
		{
			return busy;
		}
	}

	private void setBusy(boolean flag)
	{
		synchronized (this.busyLock)
		{
			busy = flag;
		}
	}

	public void retrieve(TableIdentifier table, String objectType)
		throws SQLException
	{
		this.currentTable = table;
		this.currentObjectType = objectType;
		retrieveTableDefinition();
	}

	private void retrieveTableDefinition()
		throws SQLException
	{
		if (this.isBusy()) return;
		synchronized (this.dbConnection)
		{
			try
			{
				tableDefinition.setSuspendRepaint(true);
				DbMetadata meta = this.dbConnection.getMetadata();
				DataStore def = meta.getTableDefinition(this.currentTable, false);
				DataStoreTableModel model = new DataStoreTableModel(def);
				tableDefinition.setPrintHeader(this.currentTable.getTableName());
				tableDefinition.setAutoCreateColumnsFromModel(true);
				tableDefinition.setModel(model, true);
				if ("SEQUENCE".equalsIgnoreCase(this.currentObjectType))
				{
					tableDefinition.optimizeAllColWidth(true);
				}
				else
				{
					tableDefinition.adjustColumns();
				}

				// remove the last three columns if we are not displaying a SEQUENCE
				// these columns are "SCALE/SIZE", "PRECISION" and "POSITION"
				// they don't need to be displayed as this is "included" in the
				// displayed (DBMS) data type already
				if (!"SEQUENCE".equalsIgnoreCase(this.currentObjectType))
				{
					TableColumnModel colmod = tableDefinition.getColumnModel();
					
					// Assign the correct renderer to display java.sql.Types values
					TableColumn col = colmod.getColumn(DbMetadata.COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE);
					col.setCellRenderer(new SqlTypeRenderer());

					try
					{
						if (colmod.getColumnCount() == DbMetadata.TABLE_DEFINITION_COLS.length)
						{
							for (int i = 0; i < 3; i++)
							{
								col = colmod.getColumn(colmod.getColumnCount() - 1);
								colmod.removeColumn(col);
							}
						}
					}
					catch (Throwable e)
					{
						// ignore it
					}
				}
				
			}
			catch (SQLException e)
			{
				throw e;
			}
			finally
			{
				tableDefinition.setSuspendRepaint(false);
				reloadAction.setEnabled(true);
				setBusy(false);
			}
		}
	}

	public void reset()
	{
		this.currentTable = null;
		this.currentObjectType = null;
		this.tableDefinition.reset();
		reloadAction.setEnabled(false);
	}

	public void setConnection(WbConnection conn)
	{
		this.dbConnection = conn;
		this.createIndexAction.setEnabled(this.dbConnection != null);
		this.reloadAction.setEnabled(this.dbConnection != null);
	}

	public void reload()
	{
		if (this.currentTable == null) return;
		if (this.currentObjectType == null) return;
		if (this.dbConnection == null) return;

		WbThread t = new WbThread("TableDefinition Retrieve")
		{
			public void run()
			{
				try
				{
					retrieveTableDefinition();
				}
				catch (SQLException ex)
				{
					LogMgr.logError("TableDefinitionPanel.reload()", "Error loading table definition", ex);
				}
			}
		};
		t.start();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.createIndexAction)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					createIndex();
				}
			});
		}
	}

	private void createIndex()
	{
		if (this.tableDefinition.getSelectedRowCount() <= 0) return;
		int rows[] = this.tableDefinition.getSelectedRows();
		int count = rows.length;
		String[] columns = new String[count];

		String msg = ResourceMgr.getString("LabelInputIndexName");
		String indexName = ResourceMgr.getString("TxtNewIndexName");
		//String indexName = WbSwingUtilities.getUserInput(this, msg, defaultName);
		if (indexName == null || indexName.trim().length() == 0) return;

		for (int i=0; i < count; i++)
		{
			columns[i] = this.tableDefinition.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME).toLowerCase();
		}
		String sql = this.dbConnection.getMetadata().buildIndexSource(this.currentTable, indexName, false, columns);
		String title = ResourceMgr.getString("TxtWindowTitleCreateIndex");
		Window parent = SwingUtilities.getWindowAncestor(this);
		Frame owner = null;
		if (parent != null && parent instanceof Frame)
		{
			owner = (Frame)parent;
		}
		ExecuteSqlDialog dialog = new ExecuteSqlDialog(owner, title, sql, indexName, this.dbConnection);
		dialog.setStartButtonText(ResourceMgr.getString("TxtCreateIndex"));
		dialog.show();
		fireIndexChanged(indexName);
	}

	/**
	 * Invoked when the selection in the table list
	 * has changed
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		if (e.getSource() == this.tableDefinition.getSelectionModel())
		{
			boolean rowsSelected = (this.tableDefinition.getSelectedRowCount() > 0);
			this.createIndexAction.setEnabled(rowsSelected);
		}
	}

	public String getSelectForTable()
	{
		int colCount = this.tableDefinition.getRowCount();
		if (colCount == 0) return null;

		StrBuffer sql = new StrBuffer(colCount * 80);

		sql.append("SELECT ");
		boolean quote = false;
		DbMetadata meta = this.dbConnection.getMetadata();
		for (int i=0; i < colCount; i++)
		{
			String column = this.tableDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			column = meta.quoteObjectname(column);
			if (i > 0 && i < colCount) sql.append(",\n");
			if (i > 0) sql.append("       ");
			sql.append(column);
		}
		sql.append("\nFROM ");
		sql.append(this.currentTable.getTableExpression(this.dbConnection));
		return sql.toString();
	}

	public int getRowCount()
	{
		return this.tableDefinition.getRowCount();
	}

	public DataStore getDataStore()
	{
		return this.tableDefinition.getDataStore();
	}

	public void restoreSettings()
	{
		this.columnSearcher.restoreSettings();
		this.columnFilter.restoreSettings();
	}

	public void saveSettings()
	{
		this.columnSearcher.saveSettings();
		this.columnFilter.saveSettings();
	}
}