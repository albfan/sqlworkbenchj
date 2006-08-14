/*
 * TableListPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
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
import java.util.HashMap;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
import workbench.db.ColumnIdentifier;

import workbench.db.DbMetadata;
import workbench.db.ObjectScripter;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.report.SchemaReporter;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.actions.ToggleTableSourceAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.dialogs.export.ExportFileDialog;
import workbench.gui.menu.GenerateScriptMenuItem;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.ShareableDisplay;
import workbench.interfaces.Exporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;
import workbench.util.ExceptionUtil;
import java.awt.Component;
import java.util.Iterator;
import workbench.WbManager;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.sql.PanelContentSender;
import workbench.interfaces.CriteriaPanel;
import workbench.interfaces.Reloadable;
import workbench.interfaces.Resettable;
import workbench.util.WbWorkspace;
import workbench.util.WbProperties;


/**
 * @author  support@sql-workbench.net
 */
public class TableListPanel
	extends JPanel
	implements ActionListener, ChangeListener, ListSelectionListener, MouseListener,
						 ShareableDisplay, Exporter, PropertyChangeListener,
						 TableModelListener
{
	// <editor-fold defaultstate="collapsed" desc=" Variables ">
	private WbConnection dbConnection;
	private JPanel listPanel;
	private CriteriaPanel findPanel;
	private WbTable tableList;
	private TableDefinitionPanel tableDefinition;
	private WbTable indexes;
	private WbTable importedKeys;
	private WbTable exportedKeys;

	private TableDataPanel tableData;

	private TableDependencyTreeDisplay importedTableTree;
	private WbSplitPane importedPanel;

	private TableDependencyTreeDisplay exportedTableTree;
	private WbSplitPane exportedPanel;

	private JPanel indexPanel;
	//private WbScrollPane indexPanel;
	private TriggerDisplayPanel triggers;
	private DbObjectSourcePanel tableSource;
	private JTabbedPane displayTab;
	private WbSplitPane splitPane;

	private JComboBox tableTypes = new JComboBox();
	private String currentSchema;
	private String currentCatalog;
	private SpoolDataAction spoolData;

	private WbMenuItem dropTableItem;
	private WbMenuItem scriptTablesItem;
	private WbMenuItem deleteTableItem;

	private MainWindow parentWindow;

	private String selectedCatalog;
	private String selectedSchema;
	private String selectedTableName;
	private String selectedObjectType;

	private boolean shiftDown = false;
	private boolean shouldRetrieve;

	private boolean shouldRetrieveTable;
	private boolean shouldRetrieveTableSource;
	private boolean shouldRetrieveTriggers;
	private boolean shouldRetrieveIndexes;
	private boolean shouldRetrieveExportedKeys;
	private boolean shouldRetrieveImportedKeys;
	private boolean shouldRetrieveExportedTree;
	private boolean shouldRetrieveImportedTree;
	private boolean shouldRetrieveTableDataCount;

	private boolean busy;
	private boolean ignoreStateChanged = false;

	private static final String SCHEMA_REPORT_CMD = "create-schema-report";
	private static final String DROP_CMD = "drop-table";
	private static final String DELETE_TABLE_CMD = "delete-table-data";
	private static final String COMPILE_CMD = "compile-procedure";

	private EditorTabSelectMenu showDataMenu;
	private WbAction dropIndexAction;
	private WbAction createDummyInsertAction;
	private WbAction createDefaultSelect;

	private WbMenuItem recompileItem;

	private ToggleTableSourceAction toggleTableSource;

	// holds a reference to other WbTables which
	// need to display the same table list
	// e.g. the table search panel
	private List tableListClients;

	private JDialog infoWindow;
	private JLabel infoLabel;
	private JLabel tableInfoLabel;
	private String tableTypeToSelect;
	// </editor-fold>
	
	public TableListPanel(MainWindow aParent)
		throws Exception
	{
		this.parentWindow = aParent;
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.displayTab = new WbTabbedPane(JTabbedPane.BOTTOM);

		this.tableDefinition = new TableDefinitionPanel();
		this.tableDefinition.addPropertyChangeListener(this);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), tableDefinition);

		JPanel indexPanel = new JPanel();
		indexPanel.setLayout(new BorderLayout());
		
		Reloadable indexReload = new Reloadable()
		{
			public void reload()
			{
				shouldRetrieveIndexes = true;
				try { retrieveIndexes(); } catch (Throwable th) {}
			}
		};
		
		this.indexes = new WbTable();
		this.indexes.setAdjustToColumnLabel(false);
		this.indexPanel = new TableIndexPanel(this.indexes, indexReload);
			
		this.indexes.getSelectionModel().addListSelectionListener(this);

		Reloadable sourceReload = new Reloadable()
		{
			public void reload()
			{
				shouldRetrieveTable = true;
				retrieveTableSource();
			}
		};
		this.tableSource = new DbObjectSourcePanel(aParent, sourceReload);

		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerSource"), this.tableSource);
		this.tableData = new TableDataPanel();

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
		this.tableList = new WbTable(true, false);
		this.tableList.setSelectOnRightButtonClick(true);
		this.tableList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.tableList.setCellSelectionEnabled(false);
		this.tableList.setColumnSelectionAllowed(false);
		this.tableList.setRowSelectionAllowed(true);
		this.tableList.getSelectionModel().addListSelectionListener(this);
		this.tableList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.tableList.setAdjustToColumnLabel(false);
		this.tableList.addTableModelListener(this);

		this.spoolData = new SpoolDataAction(this);
		this.tableList.addPopupAction(spoolData, true);

		this.createDummyInsertAction = new WbAction(this, "create-dummy-insert");
		this.createDummyInsertAction.setEnabled(true);
		this.createDummyInsertAction.initMenuDefinition("MnuTxtCreateDummyInsert");

		this.createDefaultSelect = new WbAction(this, "create-default-select");
		this.createDefaultSelect.setEnabled(true);
		this.createDefaultSelect.initMenuDefinition("MnuTxtCreateDefaultSelect");

		this.extendPopupMenu();

		String[] s = new String[] { "NAME", "TYPE", "CATALOG", "SCHEMA", "REMARKS"};
		this.findPanel = new QuickFilterPanel(this.tableList, s, false, "tablelist");

		ReloadAction a = new ReloadAction(this);
		a.getToolbarButton().setToolTipText(ResourceMgr.getString("TxtRefreshTableList"));
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
		//this.reset();
		this.displayTab.addMouseListener(this);
		this.tableList.addMouseListener(this);

		this.dropIndexAction = new WbAction(this,"drop-index");
		this.dropIndexAction.setEnabled(false);
		this.indexes.setSelectOnRightButtonClick(true);
		this.dropIndexAction.initMenuDefinition("MnuTxtDropIndex");
		this.indexes.addPopupAction(this.dropIndexAction, true);

		this.toggleTableSource = new ToggleTableSourceAction(this);
		this.splitPane.setOneTouchTooltip(toggleTableSource.getTooltipTextWithKeys());
		setupActionMap();
	}

	private void extendPopupMenu()
	{
		if (this.parentWindow != null)
		{
			this.showDataMenu = new EditorTabSelectMenu(this, ResourceMgr.getString("MnuTxtShowTableData"), "LblShowDataInNewTab", "LblShowDataInTab", parentWindow);
			this.showDataMenu.setEnabled(false);
			this.tableList.addPopupMenu(this.showDataMenu, false);
		}

		this.tableList.addPopupAction(this.createDummyInsertAction, true);
		this.tableList.addPopupAction(this.createDefaultSelect, false);

		this.scriptTablesItem = new GenerateScriptMenuItem();
		this.scriptTablesItem.addActionListener(this);
		this.tableList.addPopupMenu(this.scriptTablesItem, false);

		WbMenuItem item = new WbMenuItem(ResourceMgr.getString("MnuTxtSchemaReport"));
		item.setToolTipText(ResourceMgr.getDescription("MnuTxtSchemaReport"));
		item.setActionCommand(SCHEMA_REPORT_CMD);
		item.addActionListener(this);
		item.setEnabled(true);
		tableList.addPopupMenu(item, false);

		this.dropTableItem = new WbMenuItem(ResourceMgr.getString("MnuTxtDropDbObject"));
		this.dropTableItem.setToolTipText(ResourceMgr.getDescription("MnuTxtDropDbObject"));
		this.dropTableItem.setActionCommand(DROP_CMD);
		this.dropTableItem.addActionListener(this);
		this.dropTableItem.setEnabled(false);
		tableList.addPopupMenu(this.dropTableItem, true);

		this.deleteTableItem = new WbMenuItem(ResourceMgr.getString("MnuTxtDeleteTableData"));
		this.deleteTableItem.setToolTipText(ResourceMgr.getDescription("MnuTxtDeleteTableData"));
		this.deleteTableItem.setActionCommand(DELETE_TABLE_CMD);
		this.deleteTableItem.addActionListener(this);
		this.deleteTableItem.setEnabled(true);
		tableList.addPopupMenu(this.deleteTableItem, false);
	}

	private void setupActionMap()
	{
		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		this.setInputMap(WHEN_IN_FOCUSED_WINDOW, im);
		this.setActionMap(am);

		this.toggleTableSource.addToInputMap(im, am);
	}

	private void addTablePanels()
	{
		try
		{
			if (this.displayTab.getComponentCount() > 3) return;
			this.ignoreStateChanged = true;
			if (this.displayTab.getComponentCount() == 2) this.addDataPanel();
			this.displayTab.add(ResourceMgr.getString("TxtDbExplorerIndexes"), this.indexPanel);
			this.displayTab.add(ResourceMgr.getString("TxtDbExplorerFkColumns"), this.importedPanel);
			this.displayTab.add(ResourceMgr.getString("TxtDbExplorerReferencedColumns"), this.exportedPanel);
			this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTriggers"), this.triggers);
		}
		finally
		{
			this.ignoreStateChanged = false;
		}
	}

	private void removeTablePanels(boolean includeDataPanel)
	{
		try
		{
			int index = this.displayTab.getSelectedIndex();
			this.ignoreStateChanged = true;

			this.displayTab.setSelectedIndex(0);

			int count = this.displayTab.getTabCount();

			if (count < 3 && includeDataPanel) return;

			if (count >= 3 && includeDataPanel) this.removeDataPanel();

			this.displayTab.remove(this.indexPanel);
			this.indexes.reset();
			this.displayTab.remove(this.importedPanel);
			this.importedKeys.reset();
			this.displayTab.remove(this.exportedPanel);
			this.exportedKeys.reset();
			this.displayTab.remove(this.triggers);
			this.triggers.reset();
			if (index < this.displayTab.getTabCount())
			{
				this.displayTab.setSelectedIndex(index);
			}
		}
		finally
		{
			this.ignoreStateChanged = false;
		}
	}

	private void addDataPanel()
	{
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerData"), this.tableData);
	}

	private void removeDataPanel()
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
		
		this.tableDefinition.reset();
		this.importedKeys.reset();
		this.exportedKeys.reset();
		this.indexes.reset();
		this.triggers.reset();
		this.tableSource.setText("");
		this.importedTableTree.reset();
		this.exportedTableTree.reset();
		this.tableData.reset();
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
	
	private void invalidateData()
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
		this.shouldRetrieveTableDataCount = true;
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.importedTableTree.setConnection(aConnection);
		this.exportedTableTree.setConnection(aConnection);
		this.tableData.setConnection(aConnection);
		this.tableDefinition.setConnection(aConnection);

		this.tableTypes.removeActionListener(this);
		//this.catalogs.removeActionListener(this);

		this.triggers.setConnection(aConnection);
		this.tableSource.setDatabaseConnection(aConnection);
		this.reset();
		try
		{
			Collection types = this.dbConnection.getMetadata().getTableTypes();
			this.tableTypes.removeAllItems();
			this.tableTypes.addItem("*");

			Iterator itr = types.iterator();
			String typeToSelect = null;
			
			while (itr.hasNext())
			{
				String type = (String)itr.next();
				this.tableTypes.addItem(type);
				if (type.equalsIgnoreCase(this.tableTypeToSelect))
				{
					typeToSelect = type;
				}
			}
			if (typeToSelect == null)
				this.tableTypes.setSelectedIndex(0);
			else
				this.tableTypes.setSelectedItem(typeToSelect);
			
			//this.tableTypeToSelect = null;
		}
		catch (Exception e)
		{
			LogMgr.logError("TableListPanel.setConnection()", "Error when setting up connection", e);
		}

		this.tableTypes.addActionListener(this);
		this.displayTab.addChangeListener(this);
		this.updateCompileMenu();
	}

	private void updateCompileMenu()
	{
		if (this.recompileItem != null)
		{
			this.recompileItem.removeActionListener(this);
		}

		if (this.dbConnection.getMetadata().isOracle())
		{
			this.recompileItem = new WbMenuItem(ResourceMgr.getString("MnuTxtRecompile"));
			this.recompileItem.setToolTipText(ResourceMgr.getDescription("MnuTxtRecompile"));
			this.recompileItem.setActionCommand(COMPILE_CMD);
			this.recompileItem.addActionListener(this);
			this.recompileItem.setEnabled(false);
			JPopupMenu popup = this.tableList.getPopupMenu();
			popup.add(this.recompileItem);
		}
		else
		{
			if (this.recompileItem != null)
			{
				JPopupMenu popup = this.tableList.getPopupMenu();
				popup.remove(this.recompileItem);
			}
			this.recompileItem = null;
		}
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
			this.shouldRetrieve = retrieve;
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
			this.shouldRetrieve = true;
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
			WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrConnectionGone"));
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
				types = new String[] { type };
			}
			DataStore ds = dbConnection.getMetadata().getTables(currentCatalog, currentSchema, types);
			DataStoreTableModel model = new DataStoreTableModel(ds);
			tableList.setModel(model, true);
			tableList.getExportAction().setEnabled(true);
			model.sortByColumn(0);
			tableList.adjustColumns();
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					updateDisplayClients();
				}
			});
			shouldRetrieve = false;
		}
		catch (OutOfMemoryError mem)
		{
			WbManager.getInstance().showOutOfMemoryError();
		}
		catch (Throwable e)
		{
			LogMgr.logError("TableListPanel.retrieve()", "Error retrieving table list", e);
			final String msg = ExceptionUtil.getDisplay(e);
			invalidateData();
			this.shouldRetrieve = true;
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
	public void startRetrieve()
	{
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
		//this.findPanel.saveSettings();
		this.tableDefinition.saveSettings();
		storeSettings(Settings.getInstance(), this.getClass().getName() + ".");
	}
	
	/**
	 *	Restore settings from global settings file.
	 */
	public void restoreSettings()
	{
		String prefix = this.getClass().getName() + ".";
		Settings s = Settings.getInstance();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int maxWidth = (int)(d.getWidth() - 50);
		readSettings(Settings.getInstance(), prefix);
		this.triggers.restoreSettings();
		this.tableData.restoreSettings();
		//this.findPanel.restoreSettings();
		this.tableDefinition.restoreSettings();
		if (!Settings.getInstance().getStoreExplorerObjectType())
		{
			this.tableTypeToSelect = Settings.getInstance().getDefaultObjectType();
		}
	}


	/**
	 * Save settings to a workspace
	 */
	public void saveToWorkspace(WbWorkspace w, int index)
	{
		tableData.saveToWorkspace(w, index);
		WbProperties props = w.getSettings();
		String prefix = getWorkspacePrefix(index);
		storeSettings(props, prefix);
		this.findPanel.saveSettings(props, prefix + "quickfilter.");
		if (Settings.getInstance().getStoreExplorerObjectType())
		{
			String type = (String)tableTypes.getSelectedItem();
			if (type != null) props.setProperty(prefix + "objecttype", type);
			else if (tableTypeToSelect != null) props.setProperty(prefix + "objecttype", tableTypeToSelect);
		}
		
	}
	
	/**
	 *	Read settings from a workspace
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
		findPanel.restoreSettings(props, prefix + "quickfilter.");
		if (Settings.getInstance().getStoreExplorerObjectType())
		{
			this.tableTypeToSelect = props.getProperty(prefix + "objecttype", Settings.getInstance().getDefaultObjectType());
		}
		else
		{
			this.tableTypeToSelect = Settings.getInstance().getDefaultObjectType();
		}
	}
	
	private void storeSettings(PropertyStorage props, String prefix)
	{
		try
		{
			String type = (String)tableTypes.getSelectedItem();
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

	private void checkCompileMenu()
	{
		if (this.recompileItem == null) return;
		int[] rows = this.tableList.getSelectedRows();
		int count = rows.length;
		if (count == 0)
		{
			this.recompileItem.setEnabled(false);
			return;
		}
		boolean enabled = true;
		for (int i=0;i<count; i++)
		{
			int row = rows[i];
			String type = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
			if (!"VIEW".equals(type))
			{
				enabled = false;
				break;
			}
		}
		this.recompileItem.setEnabled(enabled);
	}

	/**
	 * Invoked when the selection in the table list
	 * has changed
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		if (e.getSource() == this.tableList.getSelectionModel() && !this.suspendTableSelection)
		{
			checkCompileMenu();
			if (this.showDataMenu != null)
			{
				this.showDataMenu.setEnabled(this.tableList.getSelectedRowCount() == 1);
			}
			this.updateDisplay();
		}
		else if (e.getSource() == this.indexes.getSelectionModel())
		{
			this.dropIndexAction.setEnabled(this.indexes.getSelectedRowCount() > 0);
		}
	}

	public void updateDisplay()
	{
		int count = this.tableList.getSelectedRowCount();

		this.dropTableItem.setEnabled(count > 0);
		this.spoolData.setEnabled(count > 0);

		if (count > 1) return;

		int row = this.tableList.getSelectedRow();
		if (row < 0) return;

		this.selectedCatalog = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
		this.selectedSchema = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
		this.selectedTableName = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		this.selectedObjectType = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE).toLowerCase();

		this.invalidateData();

		if (isTable(this.selectedObjectType))
		{
			addTablePanels();
		}
		else
		{
			if (isTableType(this.selectedObjectType))
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
		this.tableData.setReadOnly(!maybeUpdateable(this.selectedObjectType));
		TableIdentifier id = new TableIdentifier(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
		id.setNeverAdjustCase(true);
		this.tableData.setTable(id);

		this.setShowDataMenuStatus(this.isTableType(selectedObjectType));

		this.startRetrieveCurrentPanel();
	}

	private void setShowDataMenuStatus(boolean flag)
	{
		if (this.showDataMenu != null) this.showDataMenu.setEnabled(flag);
	}

	private boolean maybeUpdateable(String aType)
	{
		if (aType == null) return false;
		return (aType.indexOf("table") > -1 || aType.indexOf("view") > -1);
	}

	private boolean isTable(String type)
	{
		return ((type.indexOf("table") > -1) || type.equalsIgnoreCase(DbMetadata.MVIEW_NAME));
	}
	
	private boolean isTableType(String type)
	{
		return this.dbConnection.getMetadata().objectTypeCanContainData(type);
	}

	private void retrieveTableSource()
	{
		tableSource.setText(ResourceMgr.getString("TxtRetrievingSourceCode"));

		try
		{
			WbSwingUtilities.showWaitCursor(this);
			String sql = "";

			DbMetadata meta = this.dbConnection.getMetadata();
			if (this.shouldRetrieveTable || tableDefinition.getRowCount() == 0)
			{
				this.retrieveTableDefinition();
				this.shouldRetrieveIndexes = true;
				this.shouldRetrieveImportedTree = true;
			}
			TableIdentifier tbl = new TableIdentifier(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
			tbl.setType(this.selectedObjectType);
			if (meta.isViewType(this.selectedObjectType))
			{
				sql = meta.getExtendedViewSource(tbl, tableDefinition.getDataStore(), true);
			}
			else if (meta.isSynonymType(this.selectedObjectType))
			{
				sql = meta.getSynonymSource(this.selectedSchema, this.selectedTableName);
				if (sql.length() == 0)
				{
					sql = ResourceMgr.getString("MsgSynonymSourceNotImplemented") + " " + meta.getProductName();
				}
			}
			else if ("sequence".equals(this.selectedObjectType))
			{
				sql = meta.getSequenceSource(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
				if (sql.length() == 0)
				{
					sql = ResourceMgr.getString("MsgSequenceSourceNotImplemented") + " " + meta.getProductName();
				}
			}
			else if (this.selectedObjectType.indexOf("table") > -1)
			{
				// the table information has to be retrieved before
				// the table source, because otherwise the DataStores
				// passed to getTableSource() would be empty
				if (this.shouldRetrieveIndexes) this.retrieveIndexes();
				if (this.shouldRetrieveImportedTree) this.retrieveImportedTables();
				sql = meta.getTableSource(tbl, tableDefinition.getDataStore(), indexes.getDataStore(), importedKeys.getDataStore(), true);
			}
			final String s = sql;
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					tableSource.setText(s);
					tableSource.setCaretPosition(0);
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
			TableIdentifier table = new TableIdentifier(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
			table.setType(this.selectedObjectType);
			this.tableDefinition.retrieve(table, this.selectedObjectType);
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

	private void showCancelMessage()
	{
		this.showPopupMessagePanel(ResourceMgr.getString("MsgTryCancelling"));
	}

	private void showWaitMessage()
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
		p.setBorder(WbSwingUtilities.BEVEL_BORDER_RAISED);
		p.setLayout(new BorderLayout());
		this.infoLabel = new JLabel(aMsg);
		this.infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		p.add(this.infoLabel, BorderLayout.CENTER);
		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(this);
		this.infoWindow = new JDialog(f, true);
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

	private Thread panelRetrieveThread;

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

	private void startRetrieveCurrentPanel()
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

	private void startRetrieveThread(final boolean withMessage)
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

	private void retrieveCurrentPanel(final boolean withMessage)
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
			this.dbConnection.executionStart(null, this);
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
			this.dbConnection.executionEnd(null, this);
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

	private synchronized void setBusy(boolean aFlag)
	{
		synchronized (busyLock)
		{
			this.busy = aFlag;
		}
	}

	private void retrieveTriggers()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showDefaultCursor(this);
			triggers.readTriggers(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
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

	private void retrieveIndexes()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			DbMetadata meta = this.dbConnection.getMetadata();
			TableIdentifier tbl = new TableIdentifier(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
			DataStore ds = meta.getTableIndexInformation(tbl);
			DataStoreTableModel model = new DataStoreTableModel(ds);
			indexes.setModel(model, true);
			indexes.adjustColumns();
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

	private void retrieveExportedTables()
		throws SQLException
	{
		try
		{
			DbMetadata meta = this.dbConnection.getMetadata();
			DataStoreTableModel model = new DataStoreTableModel(meta.getReferencedBy(this.selectedCatalog, this.selectedSchema, this.selectedTableName));
			exportedKeys.setModel(model, true);
			exportedKeys.adjustColumns();
			this.shouldRetrieveExportedKeys = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveExportedKeys = true;
			LogMgr.logError("TableListPanel.retrieveExportedTables()", "Error retrieving table references", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
	}

	private void retrieveImportedTables()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			DbMetadata meta = this.dbConnection.getMetadata();
			DataStoreTableModel model = new DataStoreTableModel(meta.getForeignKeys(this.selectedCatalog, this.selectedSchema, this.selectedTableName, false));
			importedKeys.setModel(model, true);
			importedKeys.adjustColumns();
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

	private void retrieveImportedTree()
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			TableIdentifier id = new TableIdentifier(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
			importedTableTree.readTree(id, false);
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

	private void retrieveExportedTree()
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			TableIdentifier id = new TableIdentifier(this.selectedCatalog, this.selectedSchema, this.selectedTableName);
			exportedTableTree.readTree(id, true);
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

	private void showTableData(final int panelIndex)
	{
		PanelContentSender sender = new PanelContentSender(this.parentWindow);
		sender.sendContent(buildSqlForTable(), panelIndex);
	}

	private String buildSqlForTable()
	{
		if (this.selectedTableName == null || this.selectedTableName.length() == 0) return null;

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
		String sql = tableDefinition.getSelectForTable();
		if (sql == null)
		{
			String msg = ResourceMgr.getString("ErrNoColumnsRetrieved").replaceAll("%table%", this.selectedTableName);
			WbSwingUtilities.showErrorMessage(this, msg);
		}
		return sql;
	}

	private void compileObjects()
	{
		if (this.tableList.getSelectedRowCount() == 0) return;
		int rows[] = this.tableList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return;

		ArrayList names = new ArrayList(count);
		ArrayList types = new ArrayList(count);

		for (int i=0; i < count; i++)
		{
			int row = rows[i];
			String table = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String schema = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			String type = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);

			if (!"VIEW".equalsIgnoreCase(type)) continue;

			TableIdentifier tbl = new TableIdentifier(schema,table);
			names.add(tbl.getTableExpression());
			types.add(type);
		}

		try
		{
			ObjectCompilerUI compilerUI = new ObjectCompilerUI(names, types, this.dbConnection);
			compilerUI.show(SwingUtilities.getWindowAncestor(this));
		}
		catch (SQLException e)
		{
			LogMgr.logError("ProcedureListPanel.compileObjects()", "Error initializing ObjectCompilerUI", e);
		}
	}

	/**
	 * Invoked when the type dropdown changes or one of the additional actions
	 * is invoked that are put into the context menu of the table list
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
			catch (Exception ex) {}
		}
		else
		{
			String command = e.getActionCommand();

			if (command.startsWith("panel-") && this.parentWindow != null)
			{
				try
				{
					final int panelIndex = Integer.parseInt(command.substring(6));
					// Allow the selection change to finish so that
					// we have the correct table name in the instance variables
					EventQueue.invokeLater(new Runnable()
					{
						public void run()
						{
							showTableData(panelIndex);
						}
					});
				}
				catch (Exception ex)
				{
					LogMgr.logError("TableListPanel().actionPerformed()", "Error when accessing editor tab", ex);
				}
			}
			else if (command.equals(COMPILE_CMD))
			{
				compileObjects();
			}
			else if (command.equals(SCHEMA_REPORT_CMD))
			{
				saveReport();
			}
			else if (command.equals(DELETE_TABLE_CMD))
			{
				this.deleteTables();
			}
			else if (command.equals(DROP_CMD))
			{
				this.dropTables();
			}
			else if (e.getSource() == scriptTablesItem)
			{
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						createScript();
					}
				});
			}
			else if (e.getSource() == this.dropIndexAction)
			{
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						dropIndexes();
					}
				});
			}
			else if (e.getSource() == this.createDummyInsertAction)
			{
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						createDummyInserts();
					}
				});
			}
			else if (e.getSource() == this.createDefaultSelect)
			{
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						createDefaultSelects();
					}
				});
			}
		}
	}

	private void dropIndexes()
	{
		if (this.indexes.getSelectedRowCount() == 0) return;
		int rows[] = this.indexes.getSelectedRows();
		int count = rows.length;
		if (count == 0) return;

		ArrayList names = new ArrayList(count);
		ArrayList types = new ArrayList(count);
		for (int i=0; i < count; i ++)
		{
			String name = this.indexes.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
			names.add(this.selectedSchema + "." + name);
			types.add("INDEX");
		}
		ObjectDropperUI dropperUI = new ObjectDropperUI();
		dropperUI.setObjects(names, types);
		dropperUI.setConnection(this.dbConnection);
		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(this);
		dropperUI.showDialog(f);
		if (!dropperUI.dialogWasCancelled())
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					try
					{
						retrieveIndexes();
					}
					catch (Exception e)
					{
						LogMgr.logError("TableListPanel.dropIndex()", "Error re-retrieving indexes", e);
					}
				}
			});
		}
	}

	private boolean isClientVisible()
	{
		if (this.tableListClients == null) return false;
		for (int i=0; i < this.tableListClients.size(); i++)
		{
			JTable table = (JTable)this.tableListClients.get(i);
			if (table.isVisible()) return true;
		}
		return false;
	}

	private void updateDisplayClients()
	{
		if (this.tableListClients == null) return;
		TableModel model = this.tableList.getModel();
		for (int i=0; i < this.tableListClients.size(); i++)
		{
			JTable table = (JTable)this.tableListClients.get(i);
			if (table != null && model != null)
			{
				table.setModel(model);
				if (table instanceof WbTable)
				{
					WbTable t = (WbTable)table;
					t.adjustColumns();
				}
				table.repaint();
			}
		}
	}

	public void addTableListDisplayClient(JTable aClient)
	{
		if (this.tableListClients == null) this.tableListClients = new ArrayList();
		if (!this.tableListClients.contains(aClient)) this.tableListClients.add(aClient);
	}

	public void removeTableListDisplayClient(JTable aClient)
	{
		if (this.tableListClients == null) return;
		this.tableListClients.remove(aClient);
	}

	private void deleteTables()
	{
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		if (this.tableList.getSelectedRowCount() == 0) return;
		int rows[] = this.tableList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return;

		ArrayList names = new ArrayList(count);

		for (int i=0; i < count; i ++)
		{
			String type = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
			if (!"table".equalsIgnoreCase(type) && !"view".equalsIgnoreCase(type)) continue;

			String name = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String schema = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			String catalog = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
			TableIdentifier tbl = new TableIdentifier(catalog, schema, name);
			names.add(tbl);
		}
		TableDeleterUI deleter = new TableDeleterUI();
		deleter.addDeleteListener(this.tableData);
		deleter.setObjects(names);
		deleter.setConnection(this.dbConnection);
		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(this);
		deleter.showDialog(f);
	}

	private void createScript()
	{
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		int[] rows = this.tableList.getSelectedRows();
		int count = rows.length;
		HashMap tables = new HashMap(count);
		for (int i=0; i < count; i++)
		{
			int row = rows[i];
			String owner = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			String table = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			TableIdentifier tbl = new TableIdentifier(owner, table);

			String type = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
			tables.put(tbl.getTableExpression(this.dbConnection), type.toLowerCase());
		}
		ObjectScripter s = new ObjectScripter(tables, this.dbConnection);
		ObjectScripterUI scripterUI = new ObjectScripterUI(s);
		scripterUI.show(SwingUtilities.getWindowAncestor(this));
	}

	private void createDummyInserts()
	{
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		int[] rows = this.tableList.getSelectedRows();
		int count = rows.length;
		HashMap tables = new HashMap(count);
		for (int i=0; i < count; i++)
		{
			int row = rows[i];
			String table = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String schema = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			if (schema != null)
			{
				tables.put(schema + "." + table, ObjectScripter.TYPE_INSERT);
			}
			else
			{
				tables.put(table, ObjectScripter.TYPE_INSERT);
			}
		}
		ObjectScripter s = new ObjectScripter(tables, this.dbConnection);
		ObjectScripterUI scripterUI = new ObjectScripterUI(s);
		scripterUI.show(SwingUtilities.getWindowAncestor(this));
	}

	private void createDefaultSelects()
	{
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		int[] rows = this.tableList.getSelectedRows();
		int count = rows.length;
		HashMap tables = new HashMap(count);
		for (int i=0; i < count; i++)
		{
			int row = rows[i];
			String table = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String schema = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			if (schema != null)
			{
				tables.put(schema + "." + table, ObjectScripter.TYPE_SELECT);
			}
			else
			{
				tables.put(table, ObjectScripter.TYPE_SELECT);
			}
		}
		ObjectScripter s = new ObjectScripter(tables, this.dbConnection);
		ObjectScripterUI scripterUI = new ObjectScripterUI(s);
		scripterUI.show(SwingUtilities.getWindowAncestor(this));
	}

	private void dropTables()
	{
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		if (this.tableList.getSelectedRowCount() == 0) return;
		int rows[] = this.tableList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return;

		ArrayList names = new ArrayList(count);
		ArrayList types = new ArrayList(count);

		for (int i=0; i < count; i ++)
		{
			String name = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String schema = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			String type = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);

			TableIdentifier id = new TableIdentifier(schema, name);
			String table = id.getTableExpression(this.dbConnection);

			names.add(table);
			types.add(type);
		}

		ObjectDropperUI dropperUI = new ObjectDropperUI();
		dropperUI.setObjects(names, types);
		dropperUI.setConnection(this.dbConnection);
		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(this);
		dropperUI.showDialog(f);
		if (!dropperUI.dialogWasCancelled())
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					reload();
				}
			});
		}
	}

	private TableIdentifier[] getSelectedTables()
	{
		int[] rows = this.tableList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return null;

		TableIdentifier[] result = new TableIdentifier[count];
		for (int i=0; i < count; i++)
		{
			int row = rows[i];
			String table = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String schema = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			result[i] = new TableIdentifier(schema, table);
		}
		return result;
	}


	public void saveReport()
	{
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		TableIdentifier[] tables = getSelectedTables();
		if (tables == null) return;

		FileDialogUtil dialog = new FileDialogUtil();

		String filename = dialog.getXmlReportFilename(this);
		if (filename == null) return;

		final SchemaReporter reporter = new SchemaReporter(this.dbConnection);
		final Component caller = this;
		reporter.setShowProgress(true, (JFrame)SwingUtilities.getWindowAncestor(this));
		reporter.setTableList(tables);
		reporter.setOutputFilename(filename);

		Thread t = new WbThread("Schema Report")
		{
			public void run()
			{
				try
				{
					dbConnection.setBusy(true);
					reporter.writeXml();
				}
				catch (Throwable e)
				{
					LogMgr.logError("TableListPanel.saveReport()", "Error writing schema report", e);
					final String msg = ExceptionUtil.getDisplay(e);
					EventQueue.invokeLater(new Runnable()
					{
						public void run()
						{
							WbSwingUtilities.showErrorMessage(caller, msg);
						}
					});
				}
				finally
				{
					dbConnection.setBusy(false);
				}
			}
		};
		t.start();
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
		String table = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		String schema = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
		TableIdentifier id = new TableIdentifier(schema, table);
		DataExporter exporter = new DataExporter(this.dbConnection);
		exporter.setProgressInterval(10);
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
				String table = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
				if (table == null) continue;

				String ttype = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
				if (ttype == null) continue;
				if (!this.isTableType(ttype.toLowerCase())) continue;
				String schema = this.tableList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
				TableIdentifier id = new TableIdentifier(schema, table);
				String fname = StringUtil.makeFilename(table);
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
			exporter.setProgressInterval(10);
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
