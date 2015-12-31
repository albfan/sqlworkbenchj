/*
 * TriggerListPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import workbench.WbManager;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.Reloadable;
import workbench.interfaces.WbSelectionModel;
import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.DbObject;
import workbench.db.DropType;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CompileDbObjectAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererSetup;
import workbench.sql.DelimiterDefinition;
import workbench.sql.parser.ScriptParser;

import workbench.storage.DataStore;

import workbench.util.ExceptionUtil;
import workbench.util.FilteredProperties;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;

/**
 * A Panel that display a list of triggers defined in the database
 *
 * @author Thomas Kellerer
 * @see workbench.db.TriggerReader#getTriggerList(java.lang.String, java.lang.String, java.lang.String)
 */
public class TriggerListPanel
	extends JPanel
  implements ListSelectionListener, Reloadable, DbObjectList, TableModelListener, PropertyChangeListener
{
  private static final String TRG_TYPE_NAME = "TRIGGER";
  private WbConnection dbConnection;
	private TriggerReader reader;
	private QuickFilterPanel findPanel;
	private WbTable triggerList;

	protected DbObjectSourcePanel source;
	private WbSplitPane splitPane;
	private String currentSchema;
	private String currentCatalog;
	private boolean shouldRetrieve;
	private SummaryLabel infoLabel;
	private boolean isRetrieving;

	private CompileDbObjectAction compileAction;
	private DropDbObjectAction dropAction;

	private MainWindow parentWindow;
	private boolean initialized;
	private FilteredProperties workspaceProperties;
	private IsolationLevelChanger levelChanger = new IsolationLevelChanger();

	public TriggerListPanel(MainWindow window)
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

		Reloadable sourceReload = new Reloadable()
		{
			@Override
			public void reload()
			{
				if (dbConnection == null) return;
				if (dbConnection.isBusy()) return;
        retrieveCurrentTrigger();
			}
		};

		this.source = new DbObjectSourcePanel(parentWindow, sourceReload);
    if (DbExplorerSettings.allowSourceEditing())
    {
      source.allowEditing(true);
    }

		JPanel listPanel = new JPanel();
		this.triggerList = new WbTable(true, false, false);

		this.triggerList.setRendererSetup(RendererSetup.getBaseSetup());
		this.triggerList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.triggerList.setCellSelectionEnabled(false);
		this.triggerList.setColumnSelectionAllowed(false);
		this.triggerList.setRowSelectionAllowed(true);
		this.triggerList.getSelectionModel().addListSelectionListener(this);
		this.triggerList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.triggerList.addTableModelListener(this);
		triggerList.setReadOnly(true);

		findPanel = new QuickFilterPanel(this.triggerList, false, "triggerlist");
		findPanel.setFilterOnType(DbExplorerSettings.getFilterDuringTyping());
		findPanel.setAlwaysUseContainsFilter(DbExplorerSettings.getUsePartialMatch());

		ReloadAction a = new ReloadAction(this);
    a.setUseLabelIconSize(true);
		this.findPanel.addToToolbar(a, true, false);
		a.getToolbarButton().setToolTipText(ResourceMgr.getString("TxtRefreshTriggerList"));
		listPanel.setLayout(new BorderLayout());
		listPanel.add((JPanel)findPanel, BorderLayout.NORTH);

		this.splitPane = new WbSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.splitPane.setOneTouchExpandable(true);
		this.splitPane.setDividerSize(6);
		WbScrollPane scroll = new WbScrollPane(this.triggerList);

		listPanel.add(scroll, BorderLayout.CENTER);

		infoLabel = new SummaryLabel("");
		listPanel.add(infoLabel, BorderLayout.SOUTH);

		this.splitPane.setLeftComponent(listPanel);
		this.splitPane.setRightComponent(source);
		this.splitPane.setDividerBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setLayout(new BorderLayout());
		this.add(splitPane, BorderLayout.CENTER);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent((JPanel)findPanel);
		pol.addComponent((JPanel)findPanel);
		pol.addComponent(this.triggerList);
		this.setFocusTraversalPolicy(pol);
		this.reset();

    WbSelectionModel list = WbSelectionModel.Factory.createFacade(triggerList.getSelectionModel());
    this.dropAction = new DropDbObjectAction(this, list, this);
		triggerList.addPopupAction(dropAction, true);
		this.compileAction = new CompileDbObjectAction(this, list);
		triggerList.addPopupAction(compileAction, false);

		if (dbConnection != null)
		{
			setConnection(dbConnection);
		}
		this.splitPane.setDividerLocation(0.5d);

		initialized = true;

		restoreSettings();
		if (workspaceProperties != null)
		{
			readSettings(workspaceProperties, workspaceProperties.getFilterPrefix());
			workspaceProperties = null;
		}
    Settings.getInstance().addPropertyChangeListener(this, DbExplorerSettings.PROP_ALLOW_SOURCE_EDITING);
	}

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    if (source != null)
    {
      source.allowEditing(DbExplorerSettings.allowSourceEditing());
    }
  }

	public void dispose()
	{
		reset();
		WbAction.dispose(dropAction, compileAction);
		if (source != null) source.dispose();
		if (findPanel != null) findPanel.dispose();
		if (triggerList != null)
		{
			triggerList.dispose();
		}
    Settings.getInstance().removePropertyChangeListener(this);
	}

	public void disconnect()
	{
		this.reader = null;
		this.dbConnection = null;
		this.reset();
	}

	public void reset()
	{
		if (!initialized) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				triggerList.reset();
				source.reset();
			}
		});
	}

	public void setConnection(WbConnection aConnection)
	{
		shouldRetrieve = true;
		reset();

		this.dbConnection = aConnection;
		this.reader = TriggerReaderFactory.createReader(dbConnection);
		if (source != null) source.setDatabaseConnection(aConnection);
		if (compileAction != null) compileAction.setConnection(aConnection);
	}

	public void setCatalogAndSchema(String aCatalog, String aSchema, boolean retrieve)
		throws Exception
	{
		this.currentSchema = aSchema;
		this.currentCatalog = aCatalog;
		if (initialized && this.isVisible() && retrieve)
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
		if (this.shouldRetrieve) this.retrieve();
	}

	public void retrieve()
	{
		initGui();

		if (this.dbConnection == null) return;
		if (this.reader == null) return;
		if (this.isRetrieving) return;

		if (!WbSwingUtilities.isConnectionIdle(this, this.dbConnection)) return;

		try
		{
			this.reset();
			this.dbConnection.setBusy(true);
			this.isRetrieving = true;
			this.infoLabel.setText(ResourceMgr.getString("MsgRetrieving"));
			WbSwingUtilities.showWaitCursorOnWindow(this);

			DataStore ds = reader.getTriggers(currentCatalog, currentSchema);
			final DataStoreTableModel model = new DataStoreTableModel(ds);

			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					infoLabel.showObjectListInfo(model);
					triggerList.setModel(model, true);
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
			LogMgr.logError("ProcedureListPanel.retrieve() thread", "Could not retrieve trigger list", e);
		}
		finally
		{
			this.isRetrieving = false;
			this.dbConnection.setBusy(false);
			WbSwingUtilities.showDefaultCursorOnWindow(this);
		}
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		this.infoLabel.showObjectListInfo(triggerList.getDataStoreTableModel());
	}

	@Override
	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag && this.shouldRetrieve)
		{
			this.retrieve();
		}
	}

	private String getWorkspacePrefix(int index)
	{
		return "dbexplorer" + index + ".triggerlist.";
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
		else if (workspaceProperties != null)
		{
			workspaceProperties.copyTo(w.getSettings(), prefix);
		}
	}

	private void storeSettings(PropertyStorage props, String prefix)
	{
		props.setProperty(prefix + "divider", this.splitPane.getDividerLocation());
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
		if (initialized)
		{
			readSettings(w.getSettings(), prefix);
		}
		else
		{
			workspaceProperties = new FilteredProperties(w.getSettings(), prefix);
		}
	}

	private void readSettings(PropertyStorage props, String prefix)
	{
		if (initialized)
		{
			int loc = props.getIntProperty(prefix + "divider", 200);
			splitPane.setDividerLocation(loc);
			findPanel.restoreSettings(props, prefix);
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (!initialized) return;

		if (e.getSource() != this.triggerList.getSelectionModel()) return;
		if (e.getValueIsAdjusting()) return;
    WbThread retrieve = new WbThread("TriggerSourceRetrieve")
    {
      @Override
      public void run()
      {
        retrieveCurrentTrigger();
      }
    };
    retrieve.start();
	}

  private String getDelimiterForDrop()
  {
    if (dbConnection == null) return ";";

    DelimiterDefinition delim = dbConnection.getAlternateDelimiter();
    if (delim == null) return ";";

    ScriptParser parser = ScriptParser.createScriptParser(dbConnection);
    if (parser.supportsMixedDelimiter())
    {
      return ";";
    }

    return "\n" + delim.getDelimiter() + "\n";
  }

	protected void retrieveCurrentTrigger()
	{
		if (this.dbConnection == null || this.reader == null) return;
		int row = this.triggerList.getSelectedRow();
		if (!WbSwingUtilities.isConnectionIdle(this, this.dbConnection)) return;

		if (row < 0) return;

		final String triggerName = this.triggerList.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);
		final String tableName = this.triggerList.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TABLE);
		final String comment = this.triggerList.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_COMMENT);

		Container parent = this.getParent();
    WbSwingUtilities.showWaitCursor(parent);

		try
		{
			levelChanger.changeIsolationLevel(dbConnection);
			dbConnection.setBusy(true);

			try
			{
				TableIdentifier tbl = null;
				if (tableName != null)
				{
					tbl = new TableIdentifier(tableName, dbConnection);
					if (tbl.getCatalog() == null) tbl.setCatalog(currentCatalog);
					if (tbl.getSchema() == null) tbl.setSchema(currentSchema);
				}

        DropType dropType = DbExplorerSettings.getDropTypeToGenerate(TriggerDefinition.TRIGGER_TYPE_NAME);

				String sql = reader.getTriggerSource(currentCatalog, currentSchema, triggerName, tbl, comment, true);
        Object obj = triggerList.getUserObject(row);

        boolean isReplace = SqlUtil.isReplaceDDL(sql, dbConnection, dropType);

        if (dropType != DropType.none && obj instanceof TriggerDefinition && sql != null && !isReplace)
        {
          TriggerDefinition trg = (TriggerDefinition)obj;
          String drop = trg.getDropStatement(dbConnection, dropType == DropType.cascaded);
          if (StringUtil.isNonBlank(drop))
          {
            sql = drop + getDelimiterForDrop() + "\n\n" + sql;
          }
        }

        final String sourceSql = sql;
        WbSwingUtilities.invoke(new Runnable()
        {
          @Override
          public void run()
          {
            source.setText(sourceSql == null ? "" : sourceSql, triggerName, TRG_TYPE_NAME);
          }
        });

			}
			catch (Throwable ex)
			{
				LogMgr.logError("TriggerListPanel.retrieveTriggerSource() thread", "Could not read trigger source", ex);
				source.setPlainText(ExceptionUtil.getDisplay(ex));
			}
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(parent);
			levelChanger.restoreIsolationLevel(dbConnection);
			dbConnection.setBusy(false);
		}

		if (this.triggerList.getSelectedRowCount() == 1)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					source.setCaretPosition(0, false);
					if (DbExplorerSettings.getSelectSourcePanelAfterRetrieve())
					{
						source.requestFocusInWindow();
					}
				}
			});
		}
	}

  @Override
  public int getSelectionCount()
  {
    return triggerList.getSelectedRowCount();
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

		if (this.triggerList.getSelectedRowCount() == 0) return null;
		int[] rows = this.triggerList.getSelectedRows();
		int count = rows.length;
		List<DbObject> objects = new ArrayList<>(count);
		if (count == 0) return objects;

		for (int i=0; i < count; i ++)
		{
			String name = this.triggerList.getValueAsString(rows[i], TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);

			// MS SQL Server appends a semicolon at the end of the name...
			int pos = name.indexOf(';');
			if (pos > 0)
			{
				name = name.substring(0, pos);
			}

			// To build the correct schema, catalog and trigger name
			// we use the functionality built into TableIdentifier
			// The name of a trigger should follow the same rules as a table
			// name. So it should be safe to apply the same algorithm to
			// build a correctly qualified name
			TriggerDefinition trg = new TriggerDefinition(currentCatalog, currentSchema, name);
			String tableName = triggerList.getValueAsString(rows[i], TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TABLE);
			if (tableName != null)
			{
				trg.setRelatedTable(new TableIdentifier(tableName, dbConnection));
			}
			objects.add(trg);
		}
		return objects;
	}

	@Override
	public void reload()
	{
		if (!WbSwingUtilities.isConnectionIdle(this, dbConnection)) return;
		this.reset();
		this.retrieve();
	}

}
