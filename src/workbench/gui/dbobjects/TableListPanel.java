/*
 * Created on 5. August 2002, 21:06
 */
package workbench.gui.dbobjects;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.components.*;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.Reloadable;
import workbench.interfaces.Spooler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;


/**
 *
 * @author  workbench@kellerer.org
 *
 */
public class TableListPanel 
	extends JPanel 
	implements ActionListener, ChangeListener, ListSelectionListener, Reloadable, Spooler
{
	private WbConnection dbConnection;
	private JPanel listPanel;
	private FindPanel findPanel;
	private WbTable tableList;
	private WbTable tableDefinition;
	private WbTable indexes;
	private WbTable importedKeys;
	private WbTable exportedKeys;
	private WbScrollPane importedPanel;
	private WbScrollPane exportedPanel;
	private WbScrollPane indexPanel;
	private TriggerDisplayPanel triggers;
	private EditorPanel tableSource;
	private JTabbedPane displayTab;
	private JSplitPane splitPane;
	private DbMetadata meta;
	private Object retrieveLock = new Object();
	private JComboBox tableTypes = new JComboBox();
	private String currentSchema;
	private String currentCatalog;
	private SpoolDataAction spoolData;

	private MainWindow parentWindow;
	
	private String selectedCatalog;
	private String selectedSchema;
	private String selectedTableName;
	private String selectedObjectType;
	
	private boolean shouldRetrieve;
	
	private boolean shouldRetrieveTable;
	private boolean shouldRetrieveTriggers;
	private boolean shouldRetrieveIndexes;
	private boolean shouldRetrieveKeys;
	private boolean busy;

	private JMenu showDataMenu;
	
	public TableListPanel(MainWindow aParent)
		throws Exception
	{
		this.parentWindow = aParent;
		this.displayTab = new JTabbedPane();
		this.displayTab.setTabPlacement(JTabbedPane.BOTTOM);
		this.tableDefinition = new WbTable();
		this.tableDefinition.setAdjustToColumnLabel(false);

		JScrollPane scroll = new WbScrollPane(this.tableDefinition);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), scroll);

		this.indexes = new WbTable();
		this.indexes.setAdjustToColumnLabel(false);
		this.indexPanel = new WbScrollPane(this.indexes);

		this.tableSource = new EditorPanel();
		this.tableSource.setEditable(false);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerSource"), this.tableSource);

		this.importedKeys = new WbTable();
		this.importedKeys.setAdjustToColumnLabel(false);
		this.importedPanel = new WbScrollPane(this.importedKeys);

		this.exportedKeys = new WbTable();
		this.exportedKeys.setAdjustToColumnLabel(false);
		this.exportedPanel = new WbScrollPane(this.exportedKeys);

		this.triggers = new TriggerDisplayPanel();

		this.listPanel = new JPanel();
		this.tableList = new WbTable();
		this.tableList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.tableList.setCellSelectionEnabled(false);
		this.tableList.setColumnSelectionAllowed(false);
		this.tableList.setRowSelectionAllowed(true);
		this.tableList.getSelectionModel().addListSelectionListener(this);
		this.tableList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.tableList.setAdjustToColumnLabel(false);

		this.spoolData = new SpoolDataAction(this);
		this.tableList.addPopupAction(spoolData, true);
		this.initShowDataMenu();
		JPanel topPanel = new JPanel();
		this.findPanel = new FindPanel(this.tableList);
		this.findPanel.addToToolbar(new ReloadAction(this), true, false);

		topPanel.setLayout(new GridBagLayout());
		this.tableTypes.setMaximumSize(new Dimension(32768, 18));
		this.tableTypes.setMaximumSize(new Dimension(80, 18));
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		topPanel.add(this.tableTypes, constr);
		
		constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.weightx = 1.0;
		topPanel.add(this.findPanel, constr);

		this.listPanel.setLayout(new BorderLayout());
		this.listPanel.add(topPanel, BorderLayout.NORTH);

		this.splitPane = new WbSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		scroll = new WbScrollPane(this.tableList);

		this.listPanel.add(scroll, BorderLayout.CENTER);

		this.splitPane.setLeftComponent(this.listPanel);
		this.splitPane.setRightComponent(displayTab);
		this.setLayout(new BorderLayout());
		this.add(splitPane, BorderLayout.CENTER);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(findPanel);
		pol.addComponent(findPanel);
		pol.addComponent(tableList);
		pol.addComponent(tableDefinition);
		this.setFocusTraversalPolicy(pol);
		this.reset();
	}

	private void initShowDataMenu()
	{
		this.showDataMenu = new WbMenu(ResourceMgr.getString("MnuTxtShowTableData"));
		this.showDataMenu.setEnabled(false);
		JPopupMenu popup = this.tableList.getPopupMenu();
		String[] panels = this.parentWindow.getPanelLabels();
		for (int i=0; i < panels.length; i++)
		{
			JMenuItem item = new WbMenuItem(panels[i]);
			item.setActionCommand("panel-" + i);
			item.addActionListener(this);
			this.showDataMenu.add(item);
		}
		popup.addSeparator();
		popup.add(showDataMenu);
	}
	
	private void addTablePanels()
	{
		if (this.displayTab.getComponentCount() > 2) return;
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerIndexes"), this.indexPanel);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerFkColumns"), this.importedPanel);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerReferencedColumns"), this.exportedPanel);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTriggers"), this.triggers);
	}

	private void removeTablePanels()
	{
		if (this.displayTab.getComponentCount() == 2) return;
		this.displayTab.setSelectedIndex(0);
		this.displayTab.remove(this.indexPanel);
		this.displayTab.remove(this.importedPanel);
		this.displayTab.remove(this.exportedPanel);
		this.displayTab.remove(this.triggers);
	}

	public void setInitialFocus()
	{
		this.findPanel.setFocusToEntryField();
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.meta = null;
		this.reset();
	}

	private void reset()
	{
		this.tableList.reset();
		this.resetDetails();
		this.invalidateData();
	}
	
	private void resetDetails()
	{
		this.tableDefinition.reset();
		this.importedKeys.reset();
		this.exportedKeys.reset();
		this.indexes.reset();
		this.triggers.reset();
		this.tableSource.setText("");
		this.invalidateData();
	}
	
	private void invalidateData()
	{
		this.shouldRetrieveTable = true;
		this.shouldRetrieveTriggers = true;
		this.shouldRetrieveIndexes = true;
		this.shouldRetrieveKeys = true;
	}
	
	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.meta = aConnection.getMetadata();
		this.tableTypes.removeActionListener(this);
		this.triggers.setConnection(aConnection);
		this.tableSource.getSqlTokenMarker().initDatabaseKeywords(aConnection.getSqlConnection());
		this.reset();
		try
		{
			List types = this.meta.getTableTypes();
			this.tableTypes.removeAllItems();
			this.tableTypes.addItem("*");
			for (int i=0; i < types.size(); i++)
			{
				this.tableTypes.addItem(types.get(i));
			}
			this.tableTypes.setSelectedItem(null);
		}
		catch (Exception e)
		{
		}
		this.tableTypes.addActionListener(this);
		this.displayTab.addChangeListener(this);
	}

	public void setCatalogAndSchema(String aCatalog, String aSchema)
		throws Exception
	{
		this.reset();
		this.currentSchema = aSchema;
		this.currentCatalog = aCatalog;
		if (this.isVisible())
		{
			this.retrieve();
		}
		else
		{
			this.shouldRetrieve = true;
		}
	}

	public void retrieve()
	{
		final Container parent = this.getParent();
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					synchronized (retrieveLock)
					{
						busy = true;
						parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						reset();
						String table = (String)tableTypes.getSelectedItem();
						DataStoreTableModel rs = meta.getListOfTables(currentCatalog, currentSchema, table);
						tableList.setModel(rs, true);
						tableList.adjustColumns();
						parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						shouldRetrieve = false;
					}
				}
				catch (Throwable e)
				{
					LogMgr.logError("TableListPanel.retrieve()", "Error retrieving table list", e);
				}
				busy = false;
			}
		}).start();
	}

	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (this.shouldRetrieve)
			this.retrieve();
	}

	public void saveSettings()
	{
		this.triggers.saveSettings();
		WbManager.getSettings().setProperty(this.getClass().getName(), "divider", this.splitPane.getDividerLocation());
	}

	public void restoreSettings()
	{
		int loc = WbManager.getSettings().getIntProperty(this.getClass().getName(), "divider");
		if (loc == 0) loc = 200;
		this.splitPane.setDividerLocation(loc);
		this.triggers.restoreSettings();
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		int row = this.tableList.getSelectedRow();
		if (row < 0) return;
		
		synchronized (retrieveLock)
		{
			this.selectedCatalog = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
			this.selectedSchema = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			this.selectedTableName = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			this.selectedObjectType = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE).toLowerCase();
			this.resetDetails();
			if (this.selectedObjectType.indexOf("table") > -1)
			{
				addTablePanels();
			}
			else
			{
				removeTablePanels();
			}
			if (this.selectedObjectType.indexOf("table") > -1 ||
			    this.selectedObjectType.indexOf("view") > -1 ||
					this.selectedObjectType.indexOf("synonym") > -1)
			{
				this.showDataMenu.setEnabled(true);
			}
			else
			{
				this.showDataMenu.setEnabled(false);
			}
			
			this.startRetrieveCurrentPanel();
		}
	}

	private void retrieveTableDefinition()
		throws SQLException, WbException
	{
		tableDefinition.setModel(meta.getTableDefinitionModel(this.selectedCatalog, this.selectedSchema, this.selectedTableName), true);
		tableDefinition.adjustColumns();
		if (this.selectedObjectType.indexOf("view") > -1)
		{
			String viewSource = meta.getViewSource(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
			tableSource.setText(viewSource);
			tableSource.setCaretPosition(0);
		}
		else if (this.selectedObjectType.indexOf("table") > -1)
		{
			// the table information has to be retrieved before
			// the table source, because otherwise the DataStores
			// passed to getTableSource() would be empty
			this.retrieveIndexes();
			this.retrieveFkInformation();
			String sql = meta.getTableSource(this.selectedTableName, tableDefinition.getDataStore(), indexes.getDataStore(), importedKeys.getDataStore());
			tableSource.setText(sql);
			tableSource.setCaretPosition(0);
		}
		shouldRetrieveTable = false;
	}

	private void startRetrieveCurrentPanel()
	{
		final Component caller = this;
		new Thread(new Runnable()
		{
			public void run()
			{
				WbSwingUtilities.showWaitCursor(caller);
				retrieveCurrentPanel();	
				WbSwingUtilities.showDefaultCursor(caller);
			}
		}).start();
	}

	private void retrieveCurrentPanel()
	{
		synchronized (retrieveLock)
		{
			this.busy = true;
			try
			{
				int index = this.displayTab.getSelectedIndex();

				switch (index)
				{
					case 0:
					case 1:
						if (this.shouldRetrieveTable) this.retrieveTableDefinition();
						break;
					case 2:
						if (this.shouldRetrieveIndexes) this.retrieveIndexes();
						break;
					case 3:
					case 4:
						if (this.shouldRetrieveKeys) this.retrieveFkInformation();
						break;
					case 5:
						if (this.shouldRetrieveTriggers) this.retrieveTriggers();
				}
			}
			catch (Throwable ex)
			{
				ex.printStackTrace();
			}
			finally
			{
				this.busy = false;
			}
		}
	}
	
	private void retrieveTriggers()
		throws SQLException
	{
		triggers.readTriggers(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
		this.shouldRetrieveTriggers = false;
	}
	
	private void retrieveIndexes()
		throws SQLException
	{
		indexes.setModel(meta.getTableIndexes(this.selectedCatalog, this.selectedSchema, this.selectedTableName), true);
		indexes.adjustColumns();
		this.shouldRetrieveIndexes = false;
	}
	
	private void retrieveFkInformation()
		throws SQLException
	{
		DataStoreTableModel model = new DataStoreTableModel(meta.getForeignKeys(this.selectedCatalog, this.selectedSchema, this.selectedTableName));
		importedKeys.setModel(model, true);
		importedKeys.adjustColumns();
		model = new DataStoreTableModel(meta.getReferencedBy(this.selectedCatalog, this.selectedSchema, this.selectedTableName));
		exportedKeys.setModel(model, true);
		exportedKeys.adjustColumns();
		this.shouldRetrieveKeys = false;
	}

	public void reload()
	{
		this.reset();
		this.retrieve();
	}
	
	private String buildSqlForTable()
	{
		if (this.selectedTableName == null || this.selectedTableName.length() == 0) return null;
		if (this.shouldRetrieveTable) 
		{
			try
			{
				this.retrieveTableDefinition();
			}
			catch (Exception e)
			{
				return null;
			}
		}
		int colCount = this.tableDefinition.getRowCount();
		if (colCount == 0) return null;
		StringBuffer sql = new StringBuffer(colCount * 80);
		sql.append("SELECT ");
		for (int i=0; i < colCount; i++)
		{
			String column = this.tableDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			if (i > 0 && i < colCount) sql.append(",\n");
			if (i > 0) sql.append("       ");
			sql.append(column);
		}
		sql.append("\nFROM ");
		sql.append(this.selectedTableName);
		return sql.toString();
	}
	/**
	 *	Invoked when the type dropdown changes or the "Show data" item is selected
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.tableTypes)
		{
			try { this.retrieve(); } catch (Exception ex) {}
		}
		else
		{
			String command = e.getActionCommand();
			if (command.startsWith("panel-"))
			{
				int panelIndex = 0;
				try 
				{ 
					panelIndex = Integer.parseInt(command.substring(6)); 
					final SqlPanel panel = (SqlPanel)this.parentWindow.getSqlPanel(panelIndex);
					String sql = this.buildSqlForTable();
					if (sql != null)
					{
						panel.setStatementText(sql);
						this.parentWindow.show();
						this.parentWindow.selectTab(panelIndex);
						EventQueue.invokeLater(new Runnable()
						{
							public void run()
							{
								panel.selectEditor();
							}
						});
					}
				}
				catch (Exception ex) 
				{
					ex.printStackTrace();
				}
			}
		}
	}
	public static void main(String args[])
	{
		Connection con = null;
		try
		{
			//JFrame f = new JFrame("Test");
			//Class.forName("com.inet.tds.TdsDriver");
			//Class.forName("oracle.jdbc.OracleDriver");
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			//final Connection con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
			//final Connection con = DriverManager.getConnection("jdbc:oracle:thin:@DEMRDB34:1521:SBL1", "sadmin", "sadmin");
			//con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "auto", "auto");
			con = DriverManager.getConnection("jdbc:odbc:Auto", "auto", "auto");

			DatabaseMetaData meta = con.getMetaData();
			System.out.println(meta.getDatabaseProductName());
			System.out.println(meta.getCatalogTerm());
			/*
			ResultSet rs;
			rs = meta.getImportedKeys(null, "AUTO", "EXPENSE");
			while (rs.next())
			{
				System.out.println("column=" + rs.getString(4) + ", fk_table=" + rs.getString(7) + ", fk_col=" + rs.getString(8));
			}
			rs.close();
			*/
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Throwable th) {}
		}
	}

	public void spoolData()
	{
		int row = this.tableList.getSelectedRow();
		if (row < 0) return;
		String table = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		String sql = "SELECT * FROM " + table;
		this.spoolData.execute(this.dbConnection, sql);
	}

	public Window getParentWindow()
	{
		return SwingUtilities.getWindowAncestor(this);
	}
	
	/** Invoked when the displayed tab has changed.
	 *	Retrieve table detail information here.
	 */
	public void stateChanged(ChangeEvent e)
	{
		this.startRetrieveCurrentPanel();
	}
	
}
