/*
 * ProcedureListPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import workbench.WbManager;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.Reloadable;
import workbench.interfaces.WbSelectionModel;
import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DropType;
import workbench.db.GenericObjectDropper;
import workbench.db.JdbcProcedureReader;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.ReaderFactory;
import workbench.db.SourceStatementsHelp;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.dependency.DependencyReader;
import workbench.db.dependency.DependencyReaderFactory;
import workbench.db.objectcache.SourceCache;
import workbench.db.oracle.OraclePackageParser;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AlterProcedureAction;
import workbench.gui.actions.CompileDbObjectAction;
import workbench.gui.actions.CreateSnippetAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.ScriptDbObjectAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.FlatButton;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.ProcStatusRenderer;
import workbench.gui.renderer.SqlTypeRenderer;
import workbench.gui.settings.PlacementChooser;
import workbench.gui.sql.PanelContentSender;
import workbench.gui.sql.PasteType;

import workbench.storage.DataStore;

import workbench.sql.DelimiterDefinition;

import workbench.util.FilteredProperties;
import workbench.util.LowMemoryException;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;

/**
 * A panel that display the list of procedures/functions available in the database.
 * Essentially this is the list returned by ProcedureReader.getProcedures()
 *
 * @author Thomas Kellerer
 * @see workbench.db.ProcedureReader#getProcedures(java.lang.String, java.lang.String, java.lang.String)
 */
public class ProcedureListPanel
	extends JPanel
	implements ListSelectionListener, Reloadable, DbObjectList, ActionListener, PropertyChangeListener, TableModelListener
{
	private WbConnection dbConnection;
	private JPanel listPanel;
	private QuickFilterPanel findPanel;
	private DbObjectTable procList;
	private WbTable procColumns;
	private DbObjectSourcePanel source;
  private ObjectDependencyPanel dependencyPanel;
	private JTabbedPane displayTab;
	private JPanel statusPanel;
	private FlatButton alterButton;

	private WbSplitPane splitPane;
	private String currentSchema;
	private String currentCatalog;
	private boolean shouldRetrieve;
	private CompileDbObjectAction compileAction;

	private SummaryLabel infoLabel;
	private boolean isRetrieving;
	protected ProcStatusRenderer statusRenderer;

	private MainWindow parentWindow;
	private boolean initialized;
	private FilteredProperties workspaceSettings;

  private EditorTabSelectMenu generateWbCall;
	private SourceCache cache;

	private IsolationLevelChanger levelChanger = new IsolationLevelChanger();
	private ProcedureChangeValidator validator = new ProcedureChangeValidator();
	private AlterProcedureAction renameAction;

	public ProcedureListPanel(MainWindow window)
	{
		super();
		parentWindow = window;
	}

	private void initGui()
	{
		if (initialized) return;

		WbSwingUtilities.invoke(this::_initGui);
	}

	private void _initGui()
	{
		if (initialized) return;

		this.displayTab = new WbTabbedPane();
		int location = PlacementChooser.getPlacementLocation();
		this.displayTab.setTabPlacement(location);

		this.procColumns = new DbObjectTable();

		Reloadable sourceReload = () ->
    {
      if (dbConnection.isBusy()) return;
      try
      {
        dbConnection.setBusy(true);
        retrieveCurrentProcedure(false);
      }
      finally
      {
        dbConnection.setBusy(false);
      }
    };

		source = new DbObjectSourcePanel(parentWindow, sourceReload);
    if (DbExplorerSettings.allowSourceEditing())
    {
      source.allowEditing(true);
    }

		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerSource"), source);

		JPanel p = new JPanel(new BorderLayout());
		p.add(new WbScrollPane(this.procColumns), BorderLayout.CENTER);
		this.displayTab.add(ResourceMgr.getString("TxtDbExpProcParams"), p);

		this.listPanel = new JPanel(new BorderLayout());
		this.statusRenderer = new ProcStatusRenderer();
		this.procList = new DbObjectTable()
		{
			@Override
			public TableCellRenderer getCellRenderer(int row, int column)
			{
				if (column == convertColumnIndexToView(ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE)) return statusRenderer;
				return super.getCellRenderer(row, column);
			}
		};
		procList.setReadOnly(!DbExplorerSettings.allowAlterInDbExplorer());
		this.procList.getSelectionModel().addListSelectionListener(this);
		this.procList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.procList.setRememberColumnOrder(DbExplorerSettings.getRememberMetaColumnOrder("procedurelist"));
		this.procList.addTableModelListener(this);
		this.findPanel = new QuickFilterPanel(this.procList, false, "procedurelist");

    configureFindPanel();

		ReloadAction a = new ReloadAction(this);
    a.setUseLabelIconSize(true);

		this.findPanel.addToToolbar(a, true, false);
		a.getToolbarButton().setToolTipText(ResourceMgr.getString("TxtRefreshProcedureList"));
		this.listPanel.add((JPanel)findPanel, BorderLayout.NORTH);

		this.splitPane = new WbSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.splitPane.setOneTouchExpandable(true);
		this.splitPane.setDividerSize(6);

		this.listPanel.add(new WbScrollPane(this.procList), BorderLayout.CENTER);

		renameAction = new AlterProcedureAction(procList);
		renameAction.setReloader(this);
		renameAction.setEnabled(false);
		renameAction.addPropertyChangeListener(this);

		this.statusPanel = new JPanel(new BorderLayout());
		this.alterButton = new FlatButton(this.renameAction);
		this.alterButton.setResourceKey("MnuTxtRunAlter");

		this.infoLabel = new SummaryLabel("");
		this.statusPanel.add(infoLabel, BorderLayout.CENTER);

		this.listPanel.add(statusPanel, BorderLayout.SOUTH);

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

		source.setDatabaseConnection(dbConnection);
		compileAction.setConnection(dbConnection);

		this.validator.setConnection(dbConnection);
		this.renameAction.setConnection(dbConnection);

		initialized = true;
		restoreSettings();
		if (workspaceSettings != null)
		{
			readSettings(workspaceSettings, workspaceSettings.getFilterPrefix());
			workspaceSettings = null;
		}

    addDependencyPanelIfSupported();

    Settings.getInstance().addPropertyChangeListener(this, DbExplorerSettings.PROP_ALLOW_SOURCE_EDITING);
	}

	public void dispose()
	{
		reset();
		if (this.generateWbCall != null) this.generateWbCall.removeAll();
		if (procList != null)
		{
			procList.dispose();
		}
		if (source != null) source.dispose();
		WbAction.dispose(compileAction, renameAction);
		if (findPanel != null) findPanel.dispose();
    if (dependencyPanel != null)
    {
      dependencyPanel.dispose();
    }
    Settings.getInstance().removePropertyChangeListener(this);
	}

  private void configureFindPanel()
  {
    if (findPanel == null) return;
    findPanel.setFilterOnType(DbExplorerSettings.getFilterDuringTyping());
    findPanel.setAlwaysUseContainsFilter(DbExplorerSettings.getUsePartialMatch());
  }

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == renameAction)
		{
			checkAlterButton();
		}
    else if (evt.getPropertyName().equals(DbExplorerSettings.PROP_INSTANT_FILTER) || evt.getPropertyName().equals(DbExplorerSettings.PROP_ASSUME_WILDCARDS))
    {
      configureFindPanel();
    }
    else if (DbExplorerSettings.PROP_ALLOW_SOURCE_EDITING.equals(evt.getPropertyName()))
    {
      if (source != null) source.allowEditing(DbExplorerSettings.allowSourceEditing());
    }
	}

	protected void checkAlterButton()
	{
		WbSwingUtilities.invoke(() ->
    {
      if (renameAction.isEnabled() && !WbSwingUtilities.containsComponent(statusPanel, alterButton))
      {
        statusPanel.add(alterButton, BorderLayout.EAST);
        statusPanel.validate();
      }
      else
      {
        statusPanel.remove(alterButton);
      }
    });
	}

	private void extendPopupMenu()
	{
		if (this.parentWindow != null)
		{
			this.generateWbCall = new EditorTabSelectMenu(ResourceMgr.getString("MnuTxtShowProcData"), "LblWbCallInNewTab", "LblWbCallInTab", parentWindow, true);
      generateWbCall.setActionListener(this);
			this.generateWbCall.setEnabled(false);
			this.procList.addPopupMenu(this.generateWbCall, true);
		}

    WbSelectionModel list = WbSelectionModel.Factory.createFacade(procList.getSelectionModel());
    ScriptDbObjectAction createScript = new ScriptDbObjectAction(this, list);
		procList.addPopupAction(createScript, true);

		this.compileAction = new CompileDbObjectAction(this, list);
		procList.addPopupAction(compileAction, false);

		DropDbObjectAction dropAction = new DropDbObjectAction(this, list, this);
		procList.addPopupAction(dropAction, false);
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.reset();
	}

	public void reset()
	{
		if (!initialized) return;
		procList.saveColumnOrder();

		if (this.cache != null)
		{
			this.cache.clear();
		}

    if (dbConnection != null)
    {
      dbConnection.getMetadata().getProcedureReader().clearCache();
    }

		WbSwingUtilities.invoke(() ->
    {
      procList.reset();
      procColumns.reset();
      source.reset();
      if (dependencyPanel != null)
      {
        dependencyPanel.reset();
      }
    });
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		if (source != null) this.source.setDatabaseConnection(aConnection);
		this.reset();
		if (compileAction != null) this.compileAction.setConnection(aConnection);
		if (this.dbConnection != null)
		{
			cache = new SourceCache(dbConnection.getDbId());
		}
		if (validator != null) this.validator.setConnection(dbConnection);
		if (renameAction != null) this.renameAction.setConnection(dbConnection);
    addDependencyPanelIfSupported();
	}

  private void addDependencyPanelIfSupported()
  {
    if (dbConnection == null) return;
    if (!initialized) return;

    DependencyReader reader = DependencyReaderFactory.getReader(dbConnection);
    if (reader != null)
    {
      WbSwingUtilities.invoke( () ->
      {
        if (dependencyPanel == null)
        {
          dependencyPanel = new ObjectDependencyPanel();
        }
        dependencyPanel.setConnection(dbConnection);
        displayTab.add(ResourceMgr.getString("TxtDeps"), dependencyPanel);
      });
    }
  }

	public void setCatalogAndSchema(String aCatalog, String aSchema, boolean retrieve)
		throws Exception
	{
		this.currentSchema = aSchema;
		this.currentCatalog = aCatalog;
		if (initialized && this.isVisible() && retrieve)
		{
			this.startRetrieve();
		}
		else
		{
			this.reset();
			this.shouldRetrieve = true;
		}
	}

	public void panelSelected()
	{
    if (this.shouldRetrieve)
    {
      this.startRetrieve();
    }
	}

	public void startRetrieve()
	{
		initGui();

		if (this.isRetrieving) return;
		if (!WbSwingUtilities.isConnectionIdle(this, this.dbConnection)) return;

    reset();

    WbThread retrieve = new WbThread("RetrieveProcedures")
    {
      @Override
      public void run()
      {
        _retrieve();
      }
    };
    retrieve.start();
  }

  private void _retrieve()
  {
		final Container parent = this.getParent();
		try
		{
			this.dbConnection.setBusy(true);
			WbSwingUtilities.showWaitCursor(parent);

			WbSwingUtilities.invoke(() ->
      {
        infoLabel.setText(ResourceMgr.getString("MsgRetrieving"));
      });

      if (dbConnection.getProfile().getUseSeparateConnectionPerTab())
      {
        levelChanger.changeIsolationLevel(dbConnection);
      }
			DbMetadata meta = dbConnection.getMetadata();
			DataStore ds = meta.getProcedureReader().getProcedures(currentCatalog, currentSchema, null);
			procList.setOriginalOrder(ds);
			dbConnection.getObjectCache().addProcedureList(ds, currentSchema);
			final DataStoreTableModel model = new DataStoreTableModel(ds);
			model.setSortDefinition(JdbcProcedureReader.getProcedureListSort());

			model.setValidator(validator);

			WbSwingUtilities.invoke(() ->
      {
        infoLabel.showObjectListInfo(model);
        procList.setModel(model, true);
      });
      if (displayTab.getSelectedComponent() == dependencyPanel)
      {
        dependencyPanel.doLoad();
      }
			shouldRetrieve = false;
		}
		catch (LowMemoryException mem)
		{
			WbManager.getInstance().showLowMemoryError();
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
			levelChanger.restoreIsolationLevel(dbConnection);
			WbSwingUtilities.showDefaultCursor(parent);
		}
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		this.infoLabel.showObjectListInfo(procList.getDataStoreTableModel());
	}

	@Override
	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag && this.shouldRetrieve)
		{
			initGui();
			WbSwingUtilities.invokeLater(this::startRetrieve);
		}
	}

	private String getWorkspacePrefix(int index)
	{
		return "dbexplorer" + index + ".procedurelist.";
	}

	public void saveSettings()
	{
		if (initialized)
		{
			storeSettings(Settings.getInstance(), this.getClass().getName() + ".");
			findPanel.saveSettings();
		}
	}

	public void saveToWorkspace(WbWorkspace w, int index)
	{
		String prefix = getWorkspacePrefix(index);
		if (initialized)
		{
			storeSettings(w.getSettings(), prefix);
			findPanel.saveSettings(w.getSettings(), prefix);
		}
		else if (workspaceSettings != null)
		{
			workspaceSettings.copyTo(w.getSettings(), prefix);
		}
	}

	private void storeSettings(PropertyStorage props, String prefix)
	{
		props.setProperty(prefix + "divider", this.splitPane.getDividerLocation());
		List<String> objectListColumnOrder = procList.saveColumnOrder();
		if (objectListColumnOrder != null)
		{
			props.setProperty(prefix + "columnorder", StringUtil.listToString(objectListColumnOrder, ','));
		}
	}

	public void restoreSettings()
	{
		if (initialized)
		{
			readSettings(Settings.getInstance(), this.getClass().getName() + ".");
			findPanel.restoreSettings();
		}
	}

	public void readFromWorkspace(WbWorkspace w, int index)
	{
		String prefix = getWorkspacePrefix(index);
		if (!initialized)
		{
			workspaceSettings = new FilteredProperties(w.getSettings(), prefix);
		}
		else
		{
			readSettings(w.getSettings(), prefix);
		}
	}

	private void readSettings(PropertyStorage props, String prefix)
	{
		int loc = props.getIntProperty(prefix + "divider", 200);
		splitPane.setDividerLocation(loc);
		findPanel.restoreSettings(props, prefix);
		String colString = props.getProperty(prefix + "columnorder", null);
		if (StringUtil.isNonEmpty(colString))
		{
			procList.setNewColumnOrder(StringUtil.stringToList(colString, ","));
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (!initialized) return;

		if (e.getSource() != this.procList.getSelectionModel()) return;
		if (e.getValueIsAdjusting()) return;

		retrieveCurrentProcedure(true);

		if (e.getSource() == this.procList.getSelectionModel())
		{
			if (this.generateWbCall != null)
			{
				this.generateWbCall.setEnabled(this.procList.getSelectedRowCount() == 1);
			}
		}
	}

	protected void retrieveCurrentProcedure(final boolean useCache)
	{
		final ProcedureDefinition def = getCurrentProcedureDefinition();
		if (def == null) return;

		EventQueue.invokeLater(() ->
    {
      retrieveProcDefinition(def, useCache);
    });
	}

   /**
    * Returns the current defintion.
    * @return The {@link ProcedureDefinition} or null if no row is selected.
    */
	private ProcedureDefinition getCurrentProcedureDefinition()
	{
		int row = this.procList.getSelectedRow();

		ProcedureDefinition def = null;

		if (row > -1)
		{
			def = getDefinition(row);
		}
		return def;
	}

	private String getCurrentProcedureName()
	{
		ProcedureDefinition def = getCurrentProcedureDefinition();
		if (def == null) return null;
		return def.getProcedureName();
	}

	private String getCurrentProcedureType()
	{
		ProcedureDefinition def = getCurrentProcedureDefinition();
		if (def == null) return null;
		return def.getObjectType();
	}

	private ProcedureDefinition getDefinition(int row)
	{
		ProcedureDefinition def = null;
		Object obj = procList.getDataStore().getRow(row).getUserObject();
		if (obj instanceof ProcedureDefinition)
		{
			def = (ProcedureDefinition)obj;
		}
		else
		{
			def = buildDefinitionFromDataStore(dbConnection, procList.getDataStore(), row, true);
		}
		return def;
	}

	public static ProcedureDefinition buildDefinitionFromDataStore(WbConnection conn, DataStore data, int row, boolean currentValue)
	{
		String proc = null;
		String schema = null;
		String catalog = null;
		String comment = null;
		String specificName = null;
		int type = DatabaseMetaData.procedureNoResult;
		boolean useSpecificName = data.getColumnIndex("SPECIFIC_NAME") > 1;

		if (currentValue)
		{
			proc = data.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
			schema = data.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
			catalog = data.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
			comment = data.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS);
			specificName = useSpecificName ? data.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SPECIFIC_NAME) : null;
			type = data.getValueAsInt(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureNoResult);
		}
		else
		{
			proc = (String)data.getOriginalValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
			schema = (String)data.getOriginalValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
			catalog = (String)data.getOriginalValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
			comment = (String)data.getOriginalValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS);
			specificName = useSpecificName ?  (String)data.getOriginalValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SPECIFIC_NAME) : null;
			Integer iType = (Integer)data.getOriginalValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE);
			if (iType != null)
			{
				type = iType.intValue();
			}
		}
		ProcedureDefinition def = null;
		if (conn.getMetadata().isOracle() && catalog != null)
		{
			def = ProcedureDefinition.createOracleDefinition(schema, proc, catalog, type, comment);
		}
		else
		{
			def = new ProcedureDefinition(catalog, schema, proc, type);
			def.setComment(comment);
		}
		def.setSpecificName(specificName);
		return def;
	}

	private String getCacheKey(ProcedureDefinition def)
	{
		if (def.isPackageProcedure())
		{
			return def.getSchema() + "." + def.getPackageName();
		}
		return def.getObjectNameForDrop(dbConnection);
	}

	private CharSequence getSourceFromCache(ProcedureDefinition def)
	{
		if (def == null) return null;
		return cache.getSource(def.getObjectType(), getCacheKey(def));
	}

	private void putSourceToCache(ProcedureDefinition def, CharSequence source)
	{
		if (source == null) return;
		if (cache.addSource(def.getObjectType(), getCacheKey(def), source))
		{
			// make sure the source code is not stored twice
			def.setSource(null);
		}
	}

	private void retrieveProcDefinition(ProcedureDefinition def, boolean useCache)
	{
		if (this.dbConnection == null) return;
		if (!WbSwingUtilities.isConnectionIdle(this, this.dbConnection)) return;

		DbMetadata meta = dbConnection.getMetadata();
		Container parent = this.getParent();
		WbSwingUtilities.showWaitCursor(parent);
		CharSequence sql = null;

		try
		{
      if (dbConnection.getProfile().getUseSeparateConnectionPerTab())
      {
        levelChanger.changeIsolationLevel(dbConnection);
      }
      dbConnection.setBusy(true);
			try
			{
				DataStoreTableModel model = new DataStoreTableModel(meta.getProcedureReader().getProcedureColumns(def));
				procColumns.setModel(model, true);

				TableColumnModel colmod = procColumns.getColumnModel();
				// Assign the correct renderer to display java.sql.Types values
				TableColumn col = colmod.getColumn(ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE);
				if (col != null)
				{
					col.setCellRenderer(new SqlTypeRenderer());
				}
			}
			catch (Exception ex)
			{
				LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure definition", ex);
				procColumns.reset();
			}

			if (useCache)
			{
				sql = getSourceFromCache(def);
			}
			else
			{
				// make sure the cached source in the instance is cleared to force a reload from the database
				def.setSource(null);
			}

			try
			{
				if (sql == null)
				{
					ProcedureReader reader = ReaderFactory.getProcedureReader(dbConnection.getMetadata());
					reader.readProcedureSource(def);
					sql = def.getSource();
					putSourceToCache(def, sql);
				}
				source.setText(addDropStatement(sql, def), this.getCurrentProcedureName(), getCurrentProcedureType());
			}
			catch (NoConfigException nce)
			{
				SourceStatementsHelp help = new SourceStatementsHelp(this.dbConnection.getMetadata().getMetaDataSQLMgr());
				String msg = help.explainMissingProcSourceSql();
				source.setPlainText(msg);
			}
			catch (Throwable ex)
			{
				sql = null;
				LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure source", ex);
				source.setPlainText(ex.getMessage());
			}
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(parent);
			levelChanger.restoreIsolationLevel(dbConnection);
			dbConnection.setBusy(false);
		}

    source.appendDelimiter(dbConnection);

    if (dependencyPanel != null)
    {
      dependencyPanel.setCurrentObject(def);
    }

		final int pos = findOracleProcedureInPackage(sql, def);

		if (procList.getSelectedRowCount() == 1)
		{
			EventQueue.invokeLater(() ->
      {
        source.setCaretPosition(pos,(pos > 0));
        if (DbExplorerSettings.getSelectSourcePanelAfterRetrieve())
        {
          source.requestFocusInWindow();
        }
      });
		}
	}

  private String addDropStatement(CharSequence sql, ProcedureDefinition proc)
  {
    if (sql == null || proc == null) return "";
    if (DbExplorerSettings.getDropTypeToGenerate() == DropType.none) return sql.toString();
    ProcedureReader reader = ReaderFactory.getProcedureReader(dbConnection.getMetadata());
    if (reader.isRecreateStatement(sql))
    {
      return sql.toString();
    }
    String drop = proc.getDropStatement(dbConnection, true);
    if (drop == null)
    {
      GenericObjectDropper dropper = new GenericObjectDropper();
      dropper.setConnection(dbConnection);
      CharSequence ddl = dropper.getDropForObject(proc);
      if (ddl != null)
      {
        drop = ddl.toString();
      }
    }
    drop = StringUtil.trim(SqlUtil.trimSemicolon(drop));

    if (StringUtil.isEmptyString(drop)) return sql.toString();

    DelimiterDefinition delim = Settings.getInstance().getAlternateDelimiter(dbConnection, DelimiterDefinition.STANDARD_DELIMITER);

    if (delim.isSingleLine())
    {
      drop += "\n";
    }
    drop += delim.getDelimiter() + "\n";

    return drop + "\n" + sql;
  }

	private List<String> getParameterNames()
	{
		int rows = procColumns.getRowCount();
		List<String> names = new ArrayList<>(rows);
		for (int row = 0; row < rows; row ++)
		{
			String name = procColumns.getValueAsString(row, 0);
			if (name != null)
			{
				names.add(name);
			}
		}
		return names;
	}

	private int findOracleProcedureInPackage(CharSequence sql, ProcedureDefinition def)
	{
		if (sql == null) return 0;
		if (this.dbConnection == null) return 0;
		if (!this.dbConnection.getMetadata().isOracle()) return 0;

		if (StringUtil.isEmptyString(def.getPackageName())) return 0;
		int pos = OraclePackageParser.findProcedurePosition(sql, def, getParameterNames());

		return (pos < 0 ? 0 : pos);
	}

  @Override
  public int getSelectionCount()
  {
    return procList.getSelectedRowCount();
  }

  @Override
  public TableDefinition getCurrentTableDefinition()
  {
    return null;
  }

	@Override
	public TableIdentifier getObjectTable()
	{
		return null;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	@Override
	public List<DbObject> getSelectedObjects()
	{
		if (!initialized) return null;

		if (this.procList.getSelectedRowCount() == 0) return null;
		int[] rows = this.procList.getSelectedRows();
		int count = rows.length;
		List<DbObject> result = new ArrayList<>(count);
		if (count == 0) return result;

		for (int i=0; i < count; i ++)
		{
			String name = this.procList.getValueAsString(rows[i], ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);

			// MS SQL Server appends a semicolon at the end of the name...
			if (name.indexOf(';') > 0)
			{
				name = name.substring(0, name.indexOf(';'));
			}
			ProcedureDefinition def = getDefinition(rows[i]);
			result.add(def);
		}
		return result;
	}

	@Override
	public void reload()
	{
		if (!WbSwingUtilities.isConnectionIdle(this, dbConnection)) return;
		this.startRetrieve();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (EditorTabSelectMenu.CMD_CLIPBOARD.equals(command))
		{
			boolean ctrlPressed = WbAction.isCtrlPressed(e);
			String sql = buildProcedureCallForTable();
			if (sql != null)
			{
				if (ctrlPressed)
				{
					sql = CreateSnippetAction.makeJavaString(sql, true);
				}
				StringSelection sel = new StringSelection(sql);
				Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
				clp.setContents(sel, sel);
			}
		}
		if (command.startsWith(EditorTabSelectMenu.PANEL_CMD_PREFIX) && this.parentWindow != null)
		{
			try
			{
				final int panelIndex = Integer.parseInt(command.substring(EditorTabSelectMenu.PANEL_CMD_PREFIX.length()));
				final PasteType type = WbAction.isCtrlPressed(e) ? PasteType.append : PasteType.overwrite;

				// Allow the selection change to finish so that
				// we have the correct table name in the instance variables
				EventQueue.invokeLater(() ->
        {
          showProcedureCallData(panelIndex, type);
        });
			}
			catch (Exception ex)
			{
				LogMgr.logError("ProcedureListPanel().actionPerformed()", "Error when accessing editor tab", ex);
			}
		}
	}

	private void showProcedureCallData(int panelIndex, PasteType type)
	{
		PanelContentSender sender = new PanelContentSender(this.parentWindow, null);
		String sql = buildProcedureCallForTable();
		if (sql != null)
		{
			sender.sendContent(sql, panelIndex, type);
		}
	}

	private String buildProcedureCallForTable()
	{
		ProcedureDefinition currentDefinition = this.getCurrentProcedureDefinition();

		if (currentDefinition == null)
		{
			return null;
		}

		return currentDefinition.createSql(this.getConnection());
	}

}
