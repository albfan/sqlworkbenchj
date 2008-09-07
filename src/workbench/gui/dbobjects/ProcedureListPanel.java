/*
 * ProcedureListPanel.java
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
import java.awt.Container;
import java.awt.EventQueue;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

import workbench.db.DbMetadata;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.ProcStatusRenderer;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import javax.swing.JLabel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.WbManager;
import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.TableIdentifier;
import workbench.db.oracle.OraclePackageParser;
import workbench.gui.MainWindow;
import workbench.gui.actions.CompileDbObjectAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.ScriptDbObjectAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.renderer.RendererFactory;
import workbench.interfaces.CriteriaPanel;
import workbench.storage.DataStore;
import workbench.util.WbWorkspace;

/**
 * @author  support@sql-workbench.net
 */
public class ProcedureListPanel
	extends JPanel
	implements ListSelectionListener, Reloadable, DbObjectList
{
	private WbConnection dbConnection;
	private JPanel listPanel;
	private CriteriaPanel findPanel;
	private WbTable procList;
	private WbTable procColumns;
	protected DbObjectSourcePanel source;
	private JTabbedPane displayTab;
	private WbSplitPane splitPane;
	private String currentSchema;
	private String currentCatalog;
	private boolean shouldRetrieve;
	private CompileDbObjectAction compileAction;
	
	private JLabel infoLabel;
	private boolean isRetrieving;
	protected ProcStatusRenderer statusRenderer;

	public ProcedureListPanel(MainWindow parent) 
		throws Exception
	{
		this.displayTab = new WbTabbedPane();
		this.displayTab.setTabPlacement(JTabbedPane.BOTTOM);

		this.procColumns = new DbObjectTable();
		
		JScrollPane scroll = new WbScrollPane(this.procColumns);

		Reloadable sourceReload = new Reloadable()
		{
			public void reload()
			{
				if (dbConnection.isBusy()) return;
				try
				{
					dbConnection.setBusy(true);
					retrieveCurrentProcedure();
				}
				finally
				{
					dbConnection.setBusy(false);
				}
			}
		};
		
		source = new DbObjectSourcePanel(parent, sourceReload);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerSource"), source);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), scroll);

		this.listPanel = new JPanel();
		this.statusRenderer = new ProcStatusRenderer();
		this.procList = new DbObjectTable()
		{
			public TableCellRenderer getCellRenderer(int row, int column) 
			{
				if (column == ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE) return statusRenderer;
				return super.getCellRenderer(row, column);
			}
		};
		
		this.procList.getSelectionModel().addListSelectionListener(this);
		this.procList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		String[] cols = new String[] {"PROCEDURE_NAME", "TYPE", "CATALOG", "SCHEMA", "REMARKS"};
		this.findPanel = new QuickFilterPanel(this.procList, cols, false, "procedurelist");
		
		ReloadAction a = new ReloadAction(this);
		
		this.findPanel.addToToolbar(a, true, false);
		a.getToolbarButton().setToolTipText(ResourceMgr.getString("TxtRefreshProcedureList"));
		this.listPanel.setLayout(new BorderLayout());
		this.listPanel.add((JPanel)findPanel, BorderLayout.NORTH);

		this.splitPane = new WbSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.splitPane.setOneTouchExpandable(true);
		this.splitPane.setDividerSize(6);
		scroll = new WbScrollPane(this.procList);

		this.listPanel.add(scroll, BorderLayout.CENTER);

		this.infoLabel = new JLabel("");
		EmptyBorder b = new EmptyBorder(1, 3, 0, 0);
		this.infoLabel.setBorder(b);
		this.listPanel.add(this.infoLabel, BorderLayout.SOUTH);

		this.splitPane.setLeftComponent(this.listPanel);
		this.splitPane.setRightComponent(displayTab);
		this.splitPane.setDividerBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setLayout(new BorderLayout());
		this.add(splitPane, BorderLayout.CENTER);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent((JPanel)findPanel);
		pol.addComponent((JPanel)findPanel);
		pol.addComponent(this.procList);
		pol.addComponent(this.procColumns);
		this.setFocusTraversalPolicy(pol);
		this.reset();
		this.extendPopupMenu();
	}

	private void extendPopupMenu()
	{
		ScriptDbObjectAction createScript = new ScriptDbObjectAction(this, procList.getSelectionModel());
		procList.addPopupAction(createScript, true);
		
		this.compileAction = new CompileDbObjectAction(this, this.procList.getSelectionModel());
		procList.addPopupAction(compileAction, false);
		
		DropDbObjectAction dropAction = new DropDbObjectAction(this, procList.getSelectionModel(), this);
		procList.addPopupAction(dropAction, false);
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.reset();
	}

	public void reset()
	{
		this.procList.reset();
		this.procColumns.reset();
		this.source.setText("");
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.source.setDatabaseConnection(aConnection);
		this.reset();
		this.compileAction.setConnection(aConnection);
	}

	public void setCatalogAndSchema(String aCatalog, String aSchema, boolean retrieve)
		throws Exception
	{
		this.currentSchema = aSchema;
		this.currentCatalog = aCatalog;
		if (this.isVisible() && retrieve)
		{
			this.retrieve();
		}
		else
		{
			this.reset();
			this.shouldRetrieve = true;
		}
	}

	public void panelSelected()
	{
		retrieveIfNeeded();
	}
	
	public void retrieveIfNeeded()
	{
		if (this.shouldRetrieve) this.retrieve();
	}

	public void retrieve()
	{
		if (this.isRetrieving) return;
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		
		try
		{
			this.reset();
			this.dbConnection.setBusy(true);
			this.infoLabel.setText(ResourceMgr.getString("MsgRetrieving"));

			this.isRetrieving = true;
			DbMetadata meta = dbConnection.getMetadata();
			WbSwingUtilities.showWaitCursorOnWindow(this);
			DataStore ds = meta.getProcedures(currentCatalog, currentSchema);
			final DataStoreTableModel model = new DataStoreTableModel(ds);
			
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					int rows = model.getRowCount();
					infoLabel.setText(rows + " " + ResourceMgr.getString("TxtTableListObjects"));
					procList.setModel(model, true);
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
			LogMgr.logError("ProcedureListPanel.retrieve() thread", "Could not retrieve procedure list", e);
		}
		finally
		{
			this.isRetrieving = false;
			this.dbConnection.setBusy(false);
			WbSwingUtilities.showDefaultCursorOnWindow(this);
		}

	}

	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag && this.shouldRetrieve)
			this.retrieve();
	}

	private String getWorkspacePrefix(int index)
	{
		return "dbexplorer" + index + ".procedurelist.";
	}
	
	public void saveSettings()
	{
		storeSettings(Settings.getInstance(), this.getClass().getName() + ".");
		findPanel.saveSettings();
	}
	
	public void saveToWorkspace(WbWorkspace w, int index)
	{
		String prefix = getWorkspacePrefix(index);
		storeSettings(w.getSettings(), prefix);
		findPanel.saveSettings(w.getSettings(), prefix);
	}
	
	private void storeSettings(PropertyStorage props, String prefix)
	{
		props.setProperty(prefix + "divider", this.splitPane.getDividerLocation());
	}
	
	public void restoreSettings()
	{
		readSettings(Settings.getInstance(), this.getClass().getName() + ".");
		findPanel.restoreSettings();
	}
	
	public void readFromWorkspace(WbWorkspace w, int index)
	{
		String prefix = getWorkspacePrefix(index);
		readSettings(w.getSettings(), prefix);
		this.findPanel.restoreSettings(w.getSettings(), prefix);
	}
	
	private void readSettings(PropertyStorage props, String prefix)
	{
		int loc = props.getIntProperty(prefix + "divider", 200);
		this.splitPane.setDividerLocation(loc);
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getSource() != this.procList.getSelectionModel()) return;
		if (e.getValueIsAdjusting()) return;
		retrieveCurrentProcedure();
	}
	
	protected void retrieveCurrentProcedure()
	{
		int row = this.procList.getSelectedRow();

		if (row < 0) return;

		final String proc = this.procList.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
		final String schema = this.procList.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
		final String catalog = this.procList.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
		final int type = this.procList.getDataStore().getValueAsInt(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureResultUnknown);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				retrieveProcDefinition(catalog, schema, proc, type);
			}
		});
	}

	private void retrieveProcDefinition(String catalog, String schema, String proc, int type)
	{
		if (this.dbConnection == null) return;
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		
		DbMetadata meta = dbConnection.getMetadata();
		Container parent = this.getParent();
		WbSwingUtilities.showWaitCursor(parent);
		CharSequence sql = null;
		try
		{
			dbConnection.setBusy(true);
			try
			{
				DataStoreTableModel model = new DataStoreTableModel(meta.getProcedureColumns(catalog, schema, proc));
				procColumns.setModel(model, true);
				
				TableColumnModel colmod = procColumns.getColumnModel();
				// Assign the correct renderer to display java.sql.Types values
				TableColumn col = colmod.getColumn(ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE);
				if (col != null)
				{
					col.setCellRenderer(RendererFactory.getSqlTypeRenderer());
				}
			}
			catch (Exception ex)
			{
				LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure definition", ex);
				procColumns.reset();
			}

			try
			{
				sql = meta.getProcedureSource(catalog, schema, proc, type);
				source.setText(sql == null ? "" : sql.toString());
			}
			catch (Throwable ex)
			{
				sql = null;
				LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure source", ex);
				source.setText(ex.getMessage());
			}
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(parent);
			dbConnection.setBusy(false);
		}
		// The package name is stored in the catalog field if this is an Oracle database
		final int pos = findOracleProcedureInPackage(sql, catalog, proc);
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				source.setCaretPosition(pos,(pos > 0));
				source.requestFocusInWindow();
			}
		});
	}
	
	private int findOracleProcedureInPackage(CharSequence sql, String packageName, String procName)
	{
		if (sql == null) return 0;
		if (this.dbConnection == null) return 0;
		if (!this.dbConnection.getMetadata().isOracle()) return 0;
		
		if (StringUtil.isEmptyString(packageName)) return 0;
		int pos = OraclePackageParser.findProcedurePosition(sql, procName);
		
		return (pos < 0 ? 0 : pos);
	}
	
	public TableIdentifier getObjectTable()
	{
		return null;
	}
	
	public Component getComponent()
	{
		return this;
	}

	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	public List<? extends DbObject> getSelectedObjects()
	{
		if (this.procList.getSelectedRowCount() == 0) return null;
		int rows[] = this.procList.getSelectedRows();
		int count = rows.length;
		List<ProcedureDefinition> result = new ArrayList<ProcedureDefinition>(count);
		if (count == 0) return result;

		for (int i=0; i < count; i ++)
		{
			String name = this.procList.getValueAsString(rows[i], ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);

			// MS SQL Server appends a semicolon at the end of the name...
			if (name.indexOf(';') > 0)
			{
				name = name.substring(0, name.indexOf(';'));
			}

			String proc = this.procList.getValueAsString(rows[i], ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
			String schema = this.procList.getValueAsString(rows[i], ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
			String catalog = this.procList.getValueAsString(rows[i], ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
			int type = this.procList.getDataStore().getValueAsInt(rows[i], ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureResultUnknown);
			ProcedureDefinition def = new ProcedureDefinition(catalog, schema, proc, type, this.dbConnection.getMetadata().isOracle());
			result.add(def);
		}
		return result;
	}

	public void reload()
	{
		this.reset();
		this.retrieve();
	}
	
}
