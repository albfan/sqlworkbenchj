/*
 * TableListPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.actions.ToggleTableSourceAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.dialogs.export.ExportFileDialog;
import workbench.interfaces.ProgressReporter;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.ShareableDisplay;
import workbench.interfaces.Exporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.StringUtil;
import workbench.util.WbThread;
import workbench.util.ExceptionUtil;
import workbench.WbManager;
import workbench.db.DbObject;
import workbench.db.IndexDefinition;
import workbench.db.SequenceDefinition;
import workbench.gui.actions.CompileDbObjectAction;
import workbench.gui.actions.CreateDummySqlAction;
import workbench.gui.actions.DeleteTablesAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.SchemaReportAction;
import workbench.gui.actions.ScriptDbObjectAction;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.sql.PanelContentSender;
import workbench.interfaces.CriteriaPanel;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.Reloadable;
import workbench.util.WbWorkspace;
import workbench.util.WbProperties;


/**
 * @author  support@sql-workbench.net
 */
public class TableListPanel
	extends JPanel
	implements ActionListener, ChangeListener, ListSelectionListener, MouseListener,
						 ShareableDisplay, Exporter, PropertyChangeListener,
						 TableModelListener, DbObjectList
{
	// <editor-fold defaultstate="collapsed" desc=" Variables ">
	protected WbConnection dbConnection;
	protected JPanel listPanel;
	protected CriteriaPanel findPanel;
	protected WbTable tableList;
	protected TableDefinitionPanel tableDefinition;
	protected WbTable indexes;
	protected WbTable importedKeys;
	protected WbTable exportedKeys;

	protected TableDataPanel tableData;

	private TableDependencyTreeDisplay importedTableTree;
	private WbSplitPane importedPanel;

	private TableDependencyTreeDisplay exportedTableTree;
	private WbSplitPane exportedPanel;

	private JPanel indexPanel;
	private TriggerDisplayPanel triggers;
	protected DbObjectSourcePanel tableSource;
	private JTabbedPane displayTab;
	private WbSplitPane splitPane;

	private JComboBox tableTypes = new JComboBox();
	private String currentSchema;
	private String currentCatalog;
	private SpoolDataAction spoolData;

	private CompileDbObjectAction compileAction;
	
	private MainWindow parentWindow;

	private TableIdentifier selectedTable;
	
	// For synonym resolution
	private TableIdentifier realTable;
	
	private boolean shiftDown = false;
	protected boolean shouldRetrieve;

	protected boolean shouldRetrieveTable;
	protected boolean shouldRetrieveTableSource;
	protected boolean shouldRetrieveTriggers;
	protected boolean shouldRetrieveIndexes;
	protected boolean shouldRetrieveExportedKeys;
	protected boolean shouldRetrieveImportedKeys;
	protected boolean shouldRetrieveExportedTree;
	protected boolean shouldRetrieveImportedTree;
	protected boolean shouldRetrieveTableDataCount;

	protected boolean busy;
	protected boolean ignoreStateChanged = false;

	private EditorTabSelectMenu showDataMenu;

	private ToggleTableSourceAction toggleTableSource;

	// holds a reference to other WbTables which
	// need to display the same table list
	// e.g. the table search panel
	private List<JTable> tableListClients;

	protected JDialog infoWindow;
	private JLabel infoLabel;
	private JLabel tableInfoLabel;
	private String tableTypeToSelect;
	// </editor-fold>
	
	public TableListPanel(MainWindow aParent)
		throws Exception
	{
		this.parentWindow = aParent;
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		String tabLocation = Settings.getInstance().getProperty("workbench.gui.dbobjects.tabletabs", "bottom");
		int location = JTabbedPane.BOTTOM;
		if (tabLocation.equalsIgnoreCase("top"))
		{
			location = JTabbedPane.TOP;
		}
		else if (tabLocation.equalsIgnoreCase("left"))
		{
			location = JTabbedPane.LEFT;
		}
		else if (tabLocation.equalsIgnoreCase("right"))
		{
			location = JTabbedPane.RIGHT;
		}
		this.displayTab = new WbTabbedPane(location);

		this.tableDefinition = new TableDefinitionPanel();
		this.tableDefinition.addPropertyChangeListener(TableDefinitionPanel.INDEX_PROP, this);
		this.tableDefinition.addPropertyChangeListener(TableDefinitionPanel.DEFINITION_PROP, this);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), tableDefinition);

		Reloadable indexReload = new Reloadable()
		{
			public void reload()
			{
				shouldRetrieveIndexes = true;
				if (dbConnection.isBusy()) return;
				try 
				{ 
					dbConnection.setBusy(true);
					retrieveIndexes(); 
				} 
				catch (SQLException e)
				{
					LogMgr.logError("TableListPanel.indexReloader", "Error retrieving indexes", e);
				}
				finally
				{
					dbConnection.setBusy(false);
				}
			}
		};
		
		this.indexes = new WbTable();
		this.indexes.setAdjustToColumnLabel(false);
		this.indexes.setSelectOnRightButtonClick(true);
		this.indexPanel = new TableIndexPanel(this.indexes, indexReload);
			
		Reloadable sourceReload = new Reloadable()
		{
			public void reload()
			{
				shouldRetrieveTable = true;
				if (dbConnection.isBusy()) return;
				
				try
				{
					dbConnection.setBusy(true);
					retrieveTableSource();
				}
				finally
				{
					dbConnection.setBusy(false);
				}
			}
		};
		
		this.tableSource = new DbObjectSourcePanel(aParent, sourceReload);

		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerSource"), this.tableSource);
		this.tableData = new TableDataPanel();
		this.tableData.setResultContainer(aParent);
		
		this.importedKeys = new WbTable();
		this.importedKeys.setAdjustToColumnLabel(false);
		WbScrollPane scroll = new WbScrollPane(this.importedKeys);
		this.importedPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.importedPanel.setDividerLocation(100);
		this.importedPanel.setDividerSize(6);
		this.importedPanel.setTopComponent(scroll);
		this.importedTableTree = new TableDependencyTreeDisplay();
		this.importedPanel.setBottomComponent(this.importedTableTree);

		this.exportedKeys = new WbTable();
		this.exportedKeys.setAdjustToColumnLabel(false);
		scroll = new WbScrollPane(this.exportedKeys);
		this.exportedPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.exportedPanel.setDividerLocation(100);
		this.exportedPanel.setDividerSize(5);
		this.exportedPanel.setTopComponent(scroll);
		this.exportedTableTree = new TableDependencyTreeDisplay();
		this.exportedPanel.setBottomComponent(this.exportedTableTree);

		this.triggers = new TriggerDisplayPanel();

		this.listPanel = new JPanel();
		this.tableList = new DbObjectTable();
		this.tableList.setSelectOnRightButtonClick(true);
		this.tableList.getSelectionModel().addListSelectionListener(this);
		this.tableList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.tableList.setAdjustToColumnLabel(true);
		this.tableList.addTableModelListener(this);

		this.spoolData = new SpoolDataAction(this);
		this.tableList.addPopupAction(spoolData, true);

		this.extendPopupMenu();

		String[] s = new String[] { "NAME", "TYPE", "CATALOG", "SCHEMA", "REMARKS"};
		this.findPanel = new QuickFilterPanel(this.tableList, s, false, "tablelist");

		ReloadAction a = new ReloadAction(this);
		a.getToolbarButton().setToolTipText(ResourceMgr.getString("TxtRefreshTableList"));
		a.addToInputMap(tableList);
		
		this.findPanel.addToToolbar(a, true, false);

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.gridx = 0;

		topPanel.add(this.tableTypes, constr);

		constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.weightx = 1.0;
		topPanel.add((JPanel)this.findPanel, constr);

		this.listPanel.setLayout(new BorderLayout());
		this.listPanel.add(topPanel, BorderLayout.NORTH);

		this.tableInfoLabel = new JLabel("");

		EmptyBorder b = new EmptyBorder(1, 3, 0, 0);
		this.tableInfoLabel.setBorder(b);
		this.listPanel.add(this.tableInfoLabel, BorderLayout.SOUTH);

		this.splitPane = new WbSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		scroll = new WbScrollPane(this.tableList);

		this.listPanel.add(scroll, BorderLayout.CENTER);
		this.listPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.splitPane.setLeftComponent(this.listPanel);
		this.splitPane.setRightComponent(displayTab);
		this.splitPane.setDividerSize(5);
		this.splitPane.setDividerBorder(WbSwingUtilities.EMPTY_BORDER);
		this.splitPane.setOneTouchExpandable(true);
		this.splitPane.setContinuousLayout(true);

		this.setLayout(new BorderLayout());
		this.add(splitPane, BorderLayout.CENTER);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent((JPanel)findPanel);
		pol.addComponent((JPanel)findPanel);
		pol.addComponent(tableList);
		pol.addComponent(tableDefinition);
		this.setFocusTraversalPolicy(pol);
		this.setFocusCycleRoot(false);
		this.displayTab.addMouseListener(this);
		this.tableList.addMouseListener(this);

		initIndexDropper(indexReload);
		
		this.toggleTableSource = new ToggleTableSourceAction(this);
		this.splitPane.setOneTouchTooltip(toggleTableSource.getTooltipTextWithKeys());
		setupActionMap();

		if (Settings.getInstance().showFocusInDbExplorer())
		{
			EventQueue.invokeLater(new Runnable() 
			{
				public void run()
				{
					tableDefinition.showFocusBorder();
					indexes.showFocusBorder();
					tableList.showFocusBorder();
					tableData.showFocusBorder();
				}
			});
		}
	}

	private void initIndexDropper(Reloadable indexReload)
	{
		DbObjectList indexList = new DbObjectList()
		{
			public Component getComponent()
			{
				return TableListPanel.this;
			}

			public WbConnection getConnection()
			{
				return dbConnection;
			}

			public TableIdentifier getObjectTable()
			{
				return TableListPanel.this.getObjectTable();
			}

			public List<DbObject> getSelectedObjects()
			{
				int[] rows = indexes.getSelectedRows();
				if (rows == null) return null;
				
				ArrayList<DbObject> objects = new ArrayList<DbObject>(rows.length);

				TableIdentifier tbl = getObjectTable();
				String schema = (tbl == null ? null : tbl.getSchema());

				for (int i = 0; i < rows.length; i++)
				{
					String name = indexes.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
					IndexDefinition index = new IndexDefinition(tbl, schema, name, null);
					objects.add(index);
				}
				return objects;
			}
		};
		
		DropDbObjectAction dropAction = new DropDbObjectAction("MnuTxtDropIndex", indexList, indexes.getSelectionModel(), indexReload);
		this.indexes.addPopupAction(dropAction, true);
	}
	
	public void dispose()
	{
		this.reset();
		this.tableDefinition.removePropertyChangeListener(this);		
		this.tableData.dispose();
	}
	
	private void extendPopupMenu()
	{
		if (this.parentWindow != null)
		{
			this.showDataMenu = new EditorTabSelectMenu(this, ResourceMgr.getString("MnuTxtShowTableData"), "LblShowDataInNewTab", "LblShowDataInTab", parentWindow);
			this.showDataMenu.setEnabled(false);
			this.tableList.addPopupMenu(this.showDataMenu, false);
		}

		this.tableList.addPopupAction(CreateDummySqlAction.createDummyInsertAction(this, tableList.getSelectionModel()), true);
		this.tableList.addPopupAction(CreateDummySqlAction.createDummySelectAction(this, tableList.getSelectionModel()), false);

		ScriptDbObjectAction createScript = new ScriptDbObjectAction(this, tableList.getSelectionModel());
		this.tableList.addPopupAction(createScript, false);

		SchemaReportAction action = new SchemaReportAction(this);
		tableList.addPopupMenu(action.getMenuItem(), false);

		compileAction = new CompileDbObjectAction(this, tableList.getSelectionModel());
		tableList.addPopupAction(compileAction, false);
		
		DropDbObjectAction dropAction = new DropDbObjectAction(this, this.tableList.getSelectionModel(), this);
		tableList.addPopupAction(dropAction, true);

		tableList.addPopupAction(new DeleteTablesAction(this, tableList.getSelectionModel(), this.tableData), false);
	}

	public void setDbExecutionListener(DbExecutionListener l)
	{
		if (l != null)
		{
			tableData.addDbExecutionListener(l);
		}		
	}

	private void setDirty(boolean flag)
	{
		this.shouldRetrieve = flag;
	}
	
	private void setupActionMap()
	{
		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		this.setInputMap(WHEN_IN_FOCUSED_WINDOW, im);
		this.setActionMap(am);

		this.toggleTableSource.addToInputMap(im, am);
	}

	protected void addTablePanels()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				try
				{
					if (displayTab.getComponentCount() > 3) return;
					ignoreStateChanged = true;
					if (displayTab.getComponentCount() == 2) addDataPanel();
					displayTab.add(ResourceMgr.getString("TxtDbExplorerIndexes"), indexPanel);
					displayTab.add(ResourceMgr.getString("TxtDbExplorerFkColumns"), importedPanel);
					displayTab.add(ResourceMgr.getString("TxtDbExplorerReferencedColumns"), exportedPanel);
					displayTab.add(ResourceMgr.getString("TxtDbExplorerTriggers"), triggers);
				}
				finally
				{
					ignoreStateChanged = false;
				}
			}
		});
	}

	private void removeTablePanels(final boolean includeDataPanel)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				try
				{
					int index = displayTab.getSelectedIndex();
					ignoreStateChanged = true;

					displayTab.setSelectedIndex(0);

					int count = displayTab.getTabCount();

					if (count < 3 && includeDataPanel) return;

					if (count >= 3 && includeDataPanel) removeDataPanel();

					displayTab.remove(indexPanel);
					indexes.reset();
					displayTab.remove(importedPanel);
					importedKeys.reset();
					displayTab.remove(exportedPanel);
					exportedKeys.reset();
					displayTab.remove(triggers);
					triggers.reset();
					if (index < displayTab.getTabCount())
					{
						displayTab.setSelectedIndex(index);
					}
				}
				finally
				{
					ignoreStateChanged = false;
				}
			}
		});
	}

	protected void addDataPanel()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				displayTab.add(ResourceMgr.getString("TxtDbExplorerData"), tableData);
			}
		});
	}

	protected void removeDataPanel()
	{
		this.displayTab.remove(this.tableData);
		this.tableData.reset();
	}

	private boolean sourceExpanded = false;

	public void toggleExpandSource()
	{
		if (sourceExpanded)
		{
			int last = this.splitPane.getLastDividerLocation();
			this.splitPane.setDividerLocation(last);
		}
		else
		{
			int current = this.splitPane.getDividerLocation();
			this.splitPane.setLastDividerLocation(current);
			this.splitPane.setDividerLocation(0);
		}
		sourceExpanded = !sourceExpanded;
	}

	public void setInitialFocus()
	{
		findPanel.setFocusToEntryField();
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.tableTypes.removeActionListener(this);
		this.displayTab.removeChangeListener(this);
		this.tableTypes.removeAllItems();
		this.tableDefinition.setConnection(null);
		this.reset();
	}

	public void reset()
	{
		this.invalidateData();
		if (this.isBusy())
		{
			return;
		}
		
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				tableDefinition.reset();
				importedKeys.reset();
				exportedKeys.reset();
				indexes.reset();
				triggers.reset();
				tableSource.setText("");
				importedTableTree.reset();
				exportedTableTree.reset();
				tableData.reset();
			}
		});
	}

	private void resetCurrentPanel()
	{
		int index = this.displayTab.getSelectedIndex();
		switch (index)
		{
			case 0:
				this.tableDefinition.reset();
				break;
			case 1:
				this.tableSource.setText("");
				break;
			case 2:
				this.tableData.reset();
				break;
			case 3:
				this.indexes.reset();
				break;
			case 4:
				this.importedKeys.reset();
				this.importedTableTree.reset();
				break;
			case 5:
				this.exportedKeys.reset();
				this.exportedTableTree.reset();
				break;
			case 6:
				this.triggers.reset();
		}
	}
	
	protected void invalidateData()
	{
		this.shouldRetrieveTable = true;
		this.shouldRetrieveTableDataCount = true;
		this.shouldRetrieveTableSource = true;
		this.shouldRetrieveTriggers = true;
		this.shouldRetrieveIndexes = true;
		this.shouldRetrieveExportedKeys = true;
		this.shouldRetrieveImportedKeys = true;
		this.shouldRetrieveExportedTree = true;
		this.shouldRetrieveImportedTree = true;
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.importedTableTree.setConnection(aConnection);
		this.exportedTableTree.setConnection(aConnection);
		this.tableData.setConnection(aConnection);
		this.tableDefinition.setConnection(aConnection);

		this.tableTypes.removeActionListener(this);

		this.triggers.setConnection(aConnection);
		this.tableSource.setDatabaseConnection(aConnection);
		this.reset();
		try
		{
			Collection<String> types = this.dbConnection.getMetadata().getTableTypes();
			this.tableTypes.removeAllItems();
			this.tableTypes.addItem("*");

			for (String type : types)
			{
				this.tableTypes.addItem(type);
			}
			
			String tableView = this.dbConnection.getMetadata().getTableTypeName() + "," + this.dbConnection.getMetadata().getViewTypeName();
			String add = Settings.getInstance().getProperty("workbench.dbexplorer.typefilter.additional", tableView);
			List<String> userFilter = StringUtil.stringToList(add, ";", true, true);
			
			for (String t : userFilter)
			{
				this.tableTypes.addItem(t);
			}

			this.tableTypes.setSelectedIndex(0);
			if (tableTypeToSelect != null)
			{
				this.tableTypes.setSelectedItem(this.tableTypeToSelect);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("TableListPanel.setConnection()", "Error when setting up connection", e);
		}

		this.tableTypes.addActionListener(this);
		this.displayTab.addChangeListener(this);
		this.compileAction.setConnection(aConnection);
	}

	public boolean isReallyVisible()
	{
		if (!this.isVisible()) return false;
		Window w = SwingUtilities.getWindowAncestor(this);
		return (w.isActive() && w.isFocused() && w.isVisible());

	}
	
	public void setCatalogAndSchema(String aCatalog, String aSchema)
		throws Exception
	{
		this.setCatalogAndSchema(aCatalog, aSchema, true);
	}

	public void setCatalogAndSchema(String aCatalog, String aSchema, boolean retrieve)
		throws Exception
	{
		this.currentSchema = aSchema;
		this.currentCatalog = aCatalog;
		this.invalidateData();

		if (this.isBusy())
		{
			setDirty(retrieve);
			return;
		}
		
		this.reset();

		if (!retrieve) return;
		if (this.dbConnection == null) return;

		if (this.isReallyVisible() || this.isClientVisible())
		{
			this.retrieve();
			this.setFocusToTableList();
		}
		else
		{
			setDirty(true);
		}
	}
	
	public void tableChanged(TableModelEvent e)
	{
		String info = tableList.getRowCount() + " " + ResourceMgr.getString("TxtTableListObjects");
		this.tableInfoLabel.setText(info);
		
	}
	private void setFocusToTableList()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				listPanel.requestFocus();
				tableList.requestFocus();
			}
		});
	}

	public void retrieve()
	{
		if (this.isBusy())
		{
			this.invalidateData();
			return;
		}

		if (dbConnection == null || dbConnection.isClosed())
		{
			LogMgr.logDebug("TableListPanel.retrieve()", "Connection object not accessible", new Exception());
			WbSwingUtilities.showErrorMessageKey(this, "ErrConnectionGone");
			return;
		}

		try
		{
			WbSwingUtilities.showWaitCursor(this);
			reset();
			
			// do not call setBusy() before reset() because
			// reset will do nothing if the panel is busy
			setBusy(true);
			
			String[] types = null;
			String type = (String)tableTypes.getSelectedItem();
			
			if (!"*".equals(type))
			{
				List<String> typeList = StringUtil.stringToList(type);
				types = new String[typeList.size()];
				for (int i=0; i < typeList.size(); i++)
				{
					types[i] = typeList.get(i);
				}
			}
			
			DataStore ds = dbConnection.getMetadata().getTables(currentCatalog, currentSchema, types);
			final DataStoreTableModel model = new DataStoreTableModel(ds);
			model.sortByColumn(0);
			
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					tableList.setModel(model, true);
					tableList.getExportAction().setEnabled(true);
					tableList.adjustOrOptimizeColumns();
					updateDisplayClients();
				}
			});
			
			setDirty(false);
		}
		catch (OutOfMemoryError mem)
		{
			reset();
			setDirty(true);
			WbManager.getInstance().showOutOfMemoryError();
		}
		catch (Throwable e)
		{
			LogMgr.logError("TableListPanel.retrieve()", "Error retrieving table list", e);
			final String msg = ExceptionUtil.getDisplay(e);
			invalidateData();
			setDirty(true);
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					WbSwingUtilities.showErrorMessage(TableListPanel.this, msg);
				}
			});
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
			setBusy(false);
		}
	}

	/**
	 *	Starts the retrieval of the tables in a background thread
	 */
	private void startRetrieve()
	{
		if (dbConnection == null || dbConnection.isClosed()) 
		{
			LogMgr.logDebug("TableListPanel.startRetrieve()", "startRetrieve() called, but no connection available", new Exception());
			return;
		}
		
		Thread t = new WbThread("TableListPanel retrieve() thread")
		{
			public void run()
			{
				retrieve();
			}
		};
		t.start();
	}

	public void panelSelected()
	{
		if (this.shouldRetrieve) startRetrieve();
	}
	
	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag && this.shouldRetrieve)
			this.retrieve();
	}

	private String getWorkspacePrefix(int index)
	{
		return "dbexplorer" + index + ".tablelist.";
	}

	/** 
	 * Save settings to global settings file
	 */
	public void saveSettings()
	{
		this.triggers.saveSettings();
		this.tableData.saveSettings();
		this.tableDefinition.saveSettings();
		String prefix = this.getClass().getName() + ".";
		storeSettings(Settings.getInstance(), prefix);
		findPanel.saveSettings(Settings.getInstance(), "workbench.quickfilter.");
	}
	
	/**
	 *	Restore settings from global settings file.
	 */
	public void restoreSettings()
	{
		String prefix = this.getClass().getName() + ".";
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		readSettings(Settings.getInstance(), prefix);
		findPanel.restoreSettings(Settings.getInstance(), "workbench.quickfilter.");
		this.triggers.restoreSettings();
		this.tableData.restoreSettings();
		this.tableDefinition.restoreSettings();
	}


	/**
	 * Save settings to a workspace
	 * 
	 * @param w the Workspace into which the settings should be saved
	 * @param index the index to be used in the Workspace
	 */
	public void saveToWorkspace(WbWorkspace w, int index)
	{
		tableData.saveToWorkspace(w, index);
		WbProperties props = w.getSettings();
		String prefix = getWorkspacePrefix(index);
		storeSettings(props, prefix);
		this.findPanel.saveSettings(props, "workbench.quickfilter.");
	}
	
	/**
	 *	Read settings from a workspace
	 * 
	 * @param w the Workspace from which to read the settings
	 * @param index the index inside the workspace
	 */
	public void readFromWorkspace(WbWorkspace w, int index)
	{
		// first we read the global settings, then we'll let
		// the settings in the workspace override the global ones
		restoreSettings(); 
		tableData.readFromWorkspace(w, index);
		WbProperties props = w.getSettings();
		String prefix = getWorkspacePrefix(index);
		readSettings(props, prefix);
		findPanel.restoreSettings(props, "workbench.quickfilter.");
	}
	
	private void storeSettings(PropertyStorage props, String prefix)
	{
		try
		{
			String type = null;
			if (tableTypes != null && tableTypes.getModel().getSize() > 0)
			{
				type = (String)tableTypes.getSelectedItem();
			}
			else
			{
				// if tableTypes does not contain any items, this panel was never
				// displayed and we should use the value of the tableTypeToSelect
				// variable that was retrieved when the settings were read from 
				// the workspace.
				type = tableTypeToSelect;
			}
			LogMgr.logDebug("TableListPanel.storeSettings()", "Saved object type: '" + type + "' (workspace prefix " + prefix + ")");
			if (type != null) props.setProperty(prefix + "objecttype", type);
			
			props.setProperty(prefix + "divider", Integer.toString(this.splitPane.getDividerLocation()));
			props.setProperty(prefix + "exportedtreedivider", Integer.toString(this.exportedPanel.getDividerLocation()));
			props.setProperty(prefix + "importedtreedivider", Integer.toString(this.exportedPanel.getDividerLocation()));
		}
		catch (Throwable th)
		{
			LogMgr.logError("TableListPanel.storeSettings()", "Error storing settings", th);
		}
	}

	private void readSettings(PropertyStorage props, String prefix)
	{
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int maxWidth = (int)(d.getWidth() - 50);

		int loc = props.getIntProperty(prefix + "divider",-1);
		if (loc != -1)
		{
			if (loc == 0 || loc > maxWidth) loc = 200;
			this.splitPane.setDividerLocation(loc);
		}

		loc = props.getIntProperty(prefix + "exportedtreedivider",-1);
		if (loc != -1)
		{
			if (loc == 0 || loc > maxWidth) loc = 200;
			this.exportedPanel.setDividerLocation(loc);
		}

		loc = props.getIntProperty(prefix + "importedtreedivider",-1);
		if (loc != -1)
		{
			if (loc == 0 || loc > maxWidth) loc = 200;
			this.importedPanel.setDividerLocation(loc);
		}
		
		if (Settings.getInstance().getStoreExplorerObjectType())
		{
			this.tableTypeToSelect = props.getProperty(prefix + "objecttype", null);
			//LogMgr.logDebug("TableListPanel.readSettings()", "Retrieved objecttype: '" + tableTypeToSelect + "' (workspace prefix: " + prefix + ")");
		}
		else
		{
			this.tableTypeToSelect = null;
		}
	}

	private boolean suspendTableSelection = false;

	public void suspendTableSelection(boolean flag)
	{
		boolean wasSuspended = this.suspendTableSelection;
		this.suspendTableSelection = flag;
		if (wasSuspended && !this.suspendTableSelection)
		{
			this.updateDisplay();
		}
	}

	/**
	 * Invoked when the selection in the table list has changed
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		if (e.getSource() == this.tableList.getSelectionModel() && !this.suspendTableSelection)
		{
			if (this.showDataMenu != null)
			{
				this.showDataMenu.setEnabled(this.tableList.getSelectedRowCount() == 1);
			}
			this.updateDisplay();
		}
	}

	public void updateDisplay()
	{
		int count = this.tableList.getSelectedRowCount();

		this.spoolData.setEnabled(count > 0);

		if (count > 1) return;

		int row = this.tableList.getSelectedRow();
		if (row < 0) return;

		this.selectedTable = createTableIdentifier(row);
		this.realTable = null;

		this.invalidateData();

		boolean isTable = isTable();
		boolean hasData = isTable;
		if (!isTable)
		{
			hasData = canContainData();
		}
		if (isTable)
		{
			addTablePanels();
		}
		else
		{
			if (hasData)
			{
				if (this.displayTab.getTabCount() == 2)
				{
					this.addDataPanel();
				}
				else
				{
					this.removeTablePanels(false);
				}
			}
			else
			{
				removeTablePanels(true);
			}
		}

		this.tableData.reset();
		this.tableData.setReadOnly(!isTableType(this.selectedTable.getType()));
		this.tableData.setTable(this.selectedTable);

		this.setShowDataMenuStatus(hasData);

		this.startRetrieveCurrentPanel();
	}

	private void setShowDataMenuStatus(boolean flag)
	{
		if (this.showDataMenu != null) this.showDataMenu.setEnabled(flag);
	}

	private boolean isTableType(String type)
	{
		if (type == null) return false;
		return (type.indexOf("TABLE") > -1 || type.indexOf("table") > -1 || type.equalsIgnoreCase(DbMetadata.MVIEW_NAME));
	}

	private boolean isSynonym(TableIdentifier table)
	{
		if (table == null) return false;
		DbMetadata meta = this.dbConnection.getMetadata();
		DbSettings dbs = this.dbConnection.getDbSettings();
		return (meta.supportsSynonyms() && dbs.isSynonymType(table.getType()));
	}
	
	private boolean isTable()
	{
		if (this.selectedTable == null) return false;
		DbMetadata meta = this.dbConnection.getMetadata();
		DbSettings dbs = this.dbConnection.getDbSettings();
		String type = selectedTable.getType();
		if (isTableType(type)) return true;
		if (meta.supportsSynonyms() && dbs.isSynonymType(type))
		{
			TableIdentifier rt = getObjectTable();
			if (rt == null) return false;
			return isTableType(realTable.getType());
		}
		return false;
	}
	
	private boolean canContainData()
	{
		if (selectedTable == null) return false;
		String type = selectedTable.getType();
		DbMetadata meta = this.dbConnection.getMetadata();
		DbSettings dbs = this.dbConnection.getDbSettings();
		if (meta.supportsSynonyms() && dbs.isSynonymType(type))
		{
			TableIdentifier rt = getObjectTable();
			if (rt == null) return false;
			type = rt.getType();
		}
		return meta.objectTypeCanContainData(type);
	}

	protected void retrieveTableSource()
	{
		tableSource.setPlainText(ResourceMgr.getString("TxtRetrievingSourceCode"));

		try
		{
			WbSwingUtilities.showWaitCursor(this);
			CharSequence sql = "";

			DbMetadata meta = this.dbConnection.getMetadata();
			DbSettings dbs = this.dbConnection.getDbSettings();
			if (this.shouldRetrieveTable || tableDefinition.getRowCount() == 0)
			{
				this.retrieveTableDefinition();
				this.shouldRetrieveIndexes = true;
				this.shouldRetrieveImportedTree = true;
			}
			String type = this.selectedTable.getType();
			
			if (dbs.isViewType(type))
			{
				sql = meta.getExtendedViewSource(this.selectedTable, tableDefinition.getDataStore(), true);
			}
			else if (dbs.isSynonymType(type))
			{
				sql = meta.getSynonymSource(this.selectedTable);
				if (sql.length() == 0)
				{
					sql = ResourceMgr.getString("MsgSynonymSourceNotImplemented") + " " + meta.getProductName();
				}
				else
				{
					try
					{
						TableIdentifier tbl = meta.getSynonymTable(this.selectedTable);
						String tableSql = meta.getTableSource(tbl, false, true);
						if (!StringUtil.isEmptyString(tableSql))
						{
							StringBuilder sb = new StringBuilder(sql.length() + tableSql.length() + 50);
							String nl = Settings.getInstance().getInternalEditorLineEnding();
							sb.append(sql);
							sb.append(nl);
							sb.append(nl);
							sb.append("-------------- ");
							sb.append(tbl.getTableExpression(dbConnection));
							sb.append(" --------------");
							sb.append(nl);
							sb.append(nl);
							sb.append(tableSql);
							sql = sb;
						}
					}
					catch (Exception e)
					{
						LogMgr.logError("TableListPanel.retrieveTableSource()", "Error when retrieving source for synonym table", e);
					}
				}
			}
			else if ("sequence".equalsIgnoreCase(type))
			{
				sql = meta.getSequenceSource(this.selectedTable.getCatalog(), this.selectedTable.getSchema(), this.selectedTable.getTableName());
				if (sql.length() == 0)
				{
					sql = ResourceMgr.getString("MsgSequenceSourceNotImplemented") + " " + meta.getProductName();
				}
			}
			else if (isTableType(type))
			{
				// the table information has to be retrieved before
				// the table source, because otherwise the DataStores
				// passed to getTableSource() would be empty
				if (this.shouldRetrieveIndexes) this.retrieveIndexes();
				if (this.shouldRetrieveImportedTree) this.retrieveImportedTables();
				sql = meta.getTableSource(selectedTable, tableDefinition.getDataStore(), indexes.getDataStore(), importedKeys.getDataStore(), true, null);
			}
			final String s = (sql == null ? "" : sql.toString());
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					tableSource.setText(s);
					tableSource.setCaretPosition(0, false);
				}
			});
			shouldRetrieveTableSource = false;
		}
		catch (Exception e)
		{
			LogMgr.logError("TableListPanel.retrieveTableSource()", "Error retrieving table source", e);
			final String msg = ExceptionUtil.getDisplay(e);
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					tableSource.setText(msg);
				}
			});
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	private void retrieveTableDefinition()
		throws SQLException
	{

		try
		{
			WbSwingUtilities.showWaitCursor(this);
			this.tableDefinition.retrieve(selectedTable);
			this.shouldRetrieveTable = false;
			shouldRetrieveTable = false;
		}
		catch (SQLException e)
		{
			shouldRetrieveTable = true;
			throw e;
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
			tableDefinition.setIgnoreRepaint(false);
		}
	}

	protected void showCancelMessage()
	{
		this.showPopupMessagePanel(ResourceMgr.getString("MsgTryCancelling"));
	}

	protected void showWaitMessage()
	{
		this.showPopupMessagePanel(ResourceMgr.getString("MsgWaitRetrieveEnded"));
	}

	private void showRetrieveMessage()
	{
		this.showPopupMessagePanel(ResourceMgr.getString("MsgRetrieving"));
	}

	private void showPopupMessagePanel(String aMsg)
	{
		if (this.infoWindow != null)
		{
			this.infoLabel.setText(aMsg);
			this.infoWindow.invalidate();
			Thread.yield();
			return;
		}
		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.getBevelBorderRaised());
		p.setLayout(new BorderLayout());
		this.infoLabel = new JLabel(aMsg);
		this.infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		p.add(this.infoLabel, BorderLayout.CENTER);
		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(this);
		this.infoWindow = new JDialog(f, true);
		this.infoWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.infoWindow.getContentPane().setLayout(new BorderLayout());
		this.infoWindow.getContentPane().add(p, BorderLayout.CENTER);
		this.infoWindow.setUndecorated(true);
		this.infoWindow.setSize(260,50);
		WbSwingUtilities.center(this.infoWindow, f);
		WbThread t = new WbThread("Info display")
		{
			public void run()
			{
				infoWindow.setVisible(true);
			}
		};
		t.start();
		Thread.yield();
	}

	private void closeInfoWindow()
	{
		if (this.infoWindow != null)
		{
			this.infoLabel = null;
			this.infoWindow.getOwner().setEnabled(true);
			this.infoWindow.setVisible(false);
			this.infoWindow.dispose();
			this.infoWindow = null;
		}
	}

	protected Thread panelRetrieveThread;

	private void startCancelThread()
	{
		Thread t = new WbThread("TableListPanel Cancel")
		{
			public void run()
			{
				try
				{
					if (tableData.isRetrieving())
					{
						showCancelMessage();
						tableData.cancelRetrieve();
					}
					else
					{
						showWaitMessage();
					}

					if (panelRetrieveThread != null)
					{
						panelRetrieveThread.join();
						panelRetrieveThread = null;
					}
				}
				catch (InterruptedException e)
				{
				}
				setBusy(false);
				invalidateData();
				startRetrieveThread(true);
			}
		};
		t.start();
	}

	protected void startRetrieveCurrentPanel()
	{
		if (isBusy())
		{
			startCancelThread();
		}
		else
		{
			startRetrieveThread(false);
		}
	}

	protected void startRetrieveThread(final boolean withMessage)
	{
		panelRetrieveThread = new WbThread("TableListPanel RetrievePanel")
		{
			public void run()
			{
				try
				{
					retrieveCurrentPanel(withMessage);
				}
				finally
				{
					panelRetrieveThread = null;
				}
			}
		};
		panelRetrieveThread.start();
	}

	protected void retrieveCurrentPanel(final boolean withMessage)
	{
		if (this.isBusy() || this.dbConnection.isBusy())
		{
			this.invalidateData();
			this.resetCurrentPanel();
			return;
		}

		if (this.tableList.getSelectedRowCount() <= 0) return;
		int index = this.displayTab.getSelectedIndex();

		if (withMessage) showRetrieveMessage();

		this.setBusy(true);

		try
		{
			synchronized (this.dbConnection)
			{
				switch (index)
				{
					case 0:
						if (this.shouldRetrieveTable) this.retrieveTableDefinition();
						break;
					case 1:
						if (this.shouldRetrieveTableSource) this.retrieveTableSource();
						break;
					case 2:
						if (this.shouldRetrieveTableDataCount)
						{
							this.tableData.showData(!this.shiftDown);
							this.shouldRetrieveTableDataCount = false;
						}
						break;
					case 3:
						if (this.shouldRetrieveIndexes) this.retrieveIndexes();
						break;
					case 4:
						if (this.shouldRetrieveImportedKeys) this.retrieveImportedTables();
						if (this.shouldRetrieveImportedTree) this.retrieveImportedTree();
						break;
					case 5:
						if (this.shouldRetrieveExportedKeys) this.retrieveExportedTables();
						if (this.shouldRetrieveExportedTree) this.retrieveExportedTree();
						break;
					case 6:
						if (this.shouldRetrieveTriggers) this.retrieveTriggers();
				}
			}
		}
		catch (Throwable ex)
		{
			LogMgr.logError("TableListPanel.retrieveCurrentPanel()", "Error retrieving panel " + index, ex);
		}
		finally
		{
			if (this.dbConnection.selectStartsTransaction() && !this.dbConnection.getAutoCommit())
			{
				try { this.dbConnection.commit(); } catch (Throwable th) {}
			}
			this.setBusy(false);
			this.repaint();
			closeInfoWindow();
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	private final Object busyLock = new Object();

	private boolean isBusy()
	{
		synchronized (busyLock)
		{
			return this.busy;
		}
	}

	protected void setBusy(boolean aFlag)
	{
		synchronized (busyLock)
		{
			this.busy = aFlag;
			this.dbConnection.setBusy(aFlag);
		}
	}

	public TableIdentifier getObjectTable()
	{
		if (this.selectedTable == null) return null;
		if (!isSynonym(selectedTable)) return selectedTable;
		
		if (realTable == null)
		{
			realTable = getRealTable(this.selectedTable);
		}
		return realTable;
	}
	
	protected TableIdentifier getRealTable(TableIdentifier tbl)
	{
		return this.dbConnection.getMetadata().resolveSynonym(tbl);
	}
	
	protected void retrieveTriggers()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showDefaultCursor(this);
			triggers.readTriggers(getObjectTable());
			this.shouldRetrieveTriggers = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveTriggers = true;
			LogMgr.logError("TableListPanel.retrieveTriggers()", "Error retrieving triggers", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	protected void retrieveIndexes()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			DbMetadata meta = this.dbConnection.getMetadata();
			DataStore ds = meta.getTableIndexInformation(getObjectTable());
			DataStoreTableModel model = new DataStoreTableModel(ds);
			indexes.setModel(model, true);
			indexes.adjustOrOptimizeColumns();
			
			this.shouldRetrieveIndexes = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveIndexes = true;
			LogMgr.logError("TableListPanel.retrieveIndexes()", "Error retrieving indexes", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	protected void retrieveExportedTables()
		throws SQLException
	{
		try
		{
			DbMetadata meta = this.dbConnection.getMetadata();
			DataStoreTableModel model = new DataStoreTableModel(meta.getReferencedBy(getObjectTable()));
			exportedKeys.setModel(model, true);
			exportedKeys.adjustOrOptimizeColumns();
			this.shouldRetrieveExportedKeys = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveExportedKeys = true;
			LogMgr.logError("TableListPanel.retrieveExportedTables()", "Error retrieving table references", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
	}

	protected void retrieveImportedTables()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			DbMetadata meta = this.dbConnection.getMetadata();
			DataStoreTableModel model = new DataStoreTableModel(meta.getForeignKeys(getObjectTable(), false));
			importedKeys.setModel(model, true);
			importedKeys.adjustOrOptimizeColumns();
			this.shouldRetrieveImportedKeys = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveImportedKeys = true;
			LogMgr.logError("TableListPanel.retrieveImportedTables()", "Error retrieving table references", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	protected void retrieveImportedTree()
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			importedTableTree.readReferencedTables(getObjectTable());
			this.shouldRetrieveImportedTree = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveImportedTree = true;
			LogMgr.logError("TableListPanel.retrieveImportedTree()", "Error retrieving table references", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	protected void retrieveExportedTree()
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			exportedTableTree.readReferencingTables(getObjectTable());
			this.shouldRetrieveExportedTree = false;
		}
		catch (Throwable th)
		{
			LogMgr.logError("TableListPanel.retrieveImportedTree()", "Error retrieving table references", th);
			this.shouldRetrieveExportedTree = true;
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	public void reload()
	{
		this.reset();
		this.retrieve();
	}

	private void showTableData(final int panelIndex, final boolean appendText)
	{
		PanelContentSender sender = new PanelContentSender(this.parentWindow);
		sender.sendContent(buildSqlForTable(), panelIndex, appendText);
	}

	private String buildSqlForTable()
	{
		if (this.selectedTable == null) return null;

		if (this.shouldRetrieveTable || this.tableDefinition.getRowCount() == 0)
		{
			try
			{
				this.retrieveTableDefinition();
			}
			catch (Exception e)
			{
				LogMgr.logError("TableListPanel.buidlSqlForTable()", "Error retrieving table definition", e);
				String msg = ExceptionUtil.getDisplay(e);
				WbSwingUtilities.showErrorMessage(this, msg);
				return null;
			}
		}
		String sql = tableDefinition.getSelectForTable() + ";";
		if (sql == null)
		{
			String msg = ResourceMgr.getString("ErrNoColumnsRetrieved").replaceAll("%table%", this.selectedTable.getTableName());
			WbSwingUtilities.showErrorMessage(this, msg);
		}
		return sql;
	}

	/**
	 * Invoked when the type dropdown changes or one of the additional actions
	 * is invoked that are put into the context menu of the table list
	 * 
	 * @param e the Event that ocurred
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.tableTypes)
		{
			try
			{
				this.removeTablePanels(true);
				this.retrieve();
				this.setFocusToTableList();
			}
			catch (Exception ex) 
			{
				LogMgr.logError("TableListPanel.actionPerformed()", "Error while retrieving", ex);
			}
		}
		else
		{
			String command = e.getActionCommand();

			if (command.startsWith(EditorTabSelectMenu.PANEL_CMD_PREFIX) && this.parentWindow != null)
			{
				try
				{
					final int panelIndex = Integer.parseInt(command.substring(EditorTabSelectMenu.PANEL_CMD_PREFIX.length()));
					final boolean appendText = WbAction.isCtrlPressed(e);
					// Allow the selection change to finish so that
					// we have the correct table name in the instance variables
					EventQueue.invokeLater(new Runnable()
					{
						public void run()
						{
							showTableData(panelIndex, appendText);
						}
					});
				}
				catch (Exception ex)
				{
					LogMgr.logError("TableListPanel().actionPerformed()", "Error when accessing editor tab", ex);
				}
			}
		}
	}

	private boolean isClientVisible()
	{
		if (this.tableListClients == null) return false;
		for (JTable table : tableListClients)
		{
			if (table.isVisible()) return true;
		}
		return false;
	}

	protected void updateDisplayClients()
	{
		if (this.tableListClients == null) return;
		TableModel model = this.tableList.getModel();
		for (JTable table : tableListClients)
		{
			if (table != null && model != null)
			{
				table.setModel(model);
				if (table instanceof WbTable)
				{
					WbTable t = (WbTable)table;
					t.adjustOrOptimizeColumns();
				}
				table.repaint();
			}
		}
	}

	public void addTableListDisplayClient(JTable aClient)
	{
		if (this.tableListClients == null) this.tableListClients = new ArrayList<JTable>();
		if (!this.tableListClients.contains(aClient)) this.tableListClients.add(aClient);
	}

	public void removeTableListDisplayClient(JTable aClient)
	{
		if (this.tableListClients == null) return;
		this.tableListClients.remove(aClient);
	}

	/**
	 * Return a TableIdentifier for the given row number in the table list.
	 * @param row the row from the tableList Table
	 * @return a TableIdentifier for that row
	 */
	private TableIdentifier createTableIdentifier(int row)
	{
		String name = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		String schema = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
		String catalog = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
		String type = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
		TableIdentifier tbl = new TableIdentifier(catalog, schema, name);
		tbl.setType(type);
		tbl.setNeverAdjustCase(true);
		return tbl;
	}

	public WbConnection getConnection()
	{
		return this.dbConnection;
	}
	
	public Component getComponent()
	{
		return this;
	}
	
	public List<DbObject> getSelectedObjects()
	{
		int[] rows = this.tableList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return null;

		List<DbObject> result = new ArrayList<DbObject>(count);
		for (int i=0; i < count; i++)
		{
			TableIdentifier table = createTableIdentifier(rows[i]);
			table.checkQuotesNeeded(dbConnection);
			if (table.getType().equalsIgnoreCase("SEQUENCE"))
			{
				result.add(new SequenceDefinition(table.getSchema(), table.getTableName()));
			}
			else
			{
				result.add(table);
			}
		}
		return result;
	}

	public void exportData()
	{
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		int rowCount = this.tableList.getSelectedRowCount();
		if (rowCount <= 0) return;

		if (rowCount > 1)
		{
			this.exportTables();
			return;
		}

		int row = this.tableList.getSelectedRow();
		if (row < 0) return;
		TableIdentifier id = createTableIdentifier(row);
		DataExporter exporter = new DataExporter(this.dbConnection);
		exporter.setReportInterval(ProgressReporter.DEFAULT_PROGRESS_INTERVAL);
		exporter.exportTable(SwingUtilities.getWindowAncestor(this), id);
	}

	private void exportTables()
	{
		ExportFileDialog dialog = new ExportFileDialog(this);
		dialog.setIncludeSqlInsert(true);
		dialog.setIncludeSqlUpdate(false);
		dialog.setSelectDirectoryOnly(true);
		dialog.restoreSettings();

		String title = ResourceMgr.getString("LblSelectDirTitle");
		DbMetadata meta = this.dbConnection.getMetadata();
		boolean answer = dialog.selectOutput(title);
		if (answer)
		{
			String fdir = dialog.getSelectedFilename();

			DataExporter exporter = new DataExporter(this.dbConnection);
			dialog.setExporterOptions(exporter);
			exporter.setShowProgressWindow(true);
			String ext = null;
			int type = dialog.getExportType();

			if (type == DataExporter.EXPORT_SQL)
			{
				ext = ".sql";
			}
			else if (type == DataExporter.EXPORT_XML)
			{
				ext = ".xml";
			}
			else if (type == DataExporter.EXPORT_TXT)
			{
				ext = ".txt";
			}
			else if (type == DataExporter.EXPORT_HTML)
			{
				ext = ".html";
			}

			int[] rows = this.tableList.getSelectedRows();
			for (int i = 0; i < rows.length; i ++)
			{
				if (rows[i] < 0) continue;
				TableIdentifier id = createTableIdentifier(rows[i]);

				String ttype = id.getType();
				if (ttype == null) continue;
				if (!meta.objectTypeCanContainData(ttype)) continue;
				String fname = StringUtil.makeFilename(id.getTableName());
				File f = new File(fdir, fname + ext);
				try
				{
					exporter.addTableExportJob(f.getAbsolutePath(), id);
				}
				catch (SQLException e)
				{
					LogMgr.logError("TableListPanel.exportTables()", "Error adding ExportJob", e);
					WbSwingUtilities.showMessage(this, e.getMessage());
				}
			}
			exporter.setReportInterval(ProgressReporter.DEFAULT_PROGRESS_INTERVAL);
			exporter.startExportJobs((Frame)SwingUtilities.getWindowAncestor(this));
		}
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
		if (this.ignoreStateChanged) return;
		if (e.getSource() == this.displayTab)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					startRetrieveCurrentPanel();
				}

			});
		}
	}

	/**
	 * If an index is created in the TableDefinitionPanel it
	 * sends a PropertyChange event. This will invalidate
	 * the currently retrieved index list
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (TableDefinitionPanel.INDEX_PROP.equals(evt.getPropertyName()))
		{
			this.shouldRetrieveIndexes = true;
		}
		else if (TableDefinitionPanel.DEFINITION_PROP.equals(evt.getPropertyName()))
		{
			invalidateData();
			this.shouldRetrieveTable = false;
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		this.shiftDown = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}


}
