/*
 * TriggerListPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.util.ArrayList;

import java.util.List;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
import javax.swing.JLabel;
import workbench.WbManager;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.gui.MainWindow;
import workbench.gui.actions.CompileDbObjectAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.QuickFilterPanel;
import workbench.interfaces.CriteriaPanel;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.FilteredProperties;
import workbench.util.WbWorkspace;

/**
 * A Panel that display a list of triggers defined in the database
 *
 * @author Thomas Kellerer
 * @see workbench.db.TriggerReader#getTriggerList(java.lang.String, java.lang.String, java.lang.String)
 */
public class TriggerListPanel
	extends JPanel
	implements ListSelectionListener, Reloadable, DbObjectList
{
	private WbConnection dbConnection;
	private TriggerReader reader;
	private CriteriaPanel findPanel;
	private WbTable triggerList;

	protected DbObjectSourcePanel source;
	private WbSplitPane splitPane;
	private String currentSchema;
	private String currentCatalog;
	private boolean shouldRetrieve;
	private JLabel infoLabel;
	private boolean isRetrieving;
	protected ProcStatusRenderer statusRenderer;

	private CompileDbObjectAction compileAction;
	private DropDbObjectAction dropAction;

	private MainWindow parentWindow;
	private boolean initialized;
	private FilteredProperties workspaceProperties;

	public TriggerListPanel(MainWindow window)
	{
		super();
		parentWindow = window;
	}

	private void initGui()
	{
		if (initialized) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				_initGui();
			}
		});
	}

	private void _initGui()
	{
		if (initialized) return;

		Reloadable sourceReload = new Reloadable()
		{
			public void reload()
			{
				if (dbConnection == null) return;
				if (dbConnection.isBusy()) return;
				try
				{
					dbConnection.setBusy(true);
					retrieveCurrentTrigger();
				}
				finally
				{
					dbConnection.setBusy(false);
				}
			}
		};

		this.source = new DbObjectSourcePanel(parentWindow, sourceReload);

		JPanel listPanel = new JPanel();
		this.statusRenderer = new ProcStatusRenderer();
		this.triggerList = new WbTable(true, false, false);

		this.triggerList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.triggerList.setCellSelectionEnabled(false);
		this.triggerList.setColumnSelectionAllowed(false);
		this.triggerList.setRowSelectionAllowed(true);
		this.triggerList.getSelectionModel().addListSelectionListener(this);
		this.triggerList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		triggerList.setReadOnly(true);

		this.findPanel = new QuickFilterPanel(this.triggerList, false, "triggerlist");

		ReloadAction a = new ReloadAction(this);

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

		this.dropAction = new DropDbObjectAction(this, triggerList.getSelectionModel(), this);
		triggerList.addPopupAction(dropAction, true);
		this.compileAction = new CompileDbObjectAction(this, this.triggerList.getSelectionModel());
		triggerList.addPopupAction(compileAction, false);

		if (dbConnection != null)
		{
			setConnection(dbConnection);
		}

		initialized = true;

		restoreSettings();
		if (workspaceProperties != null)
		{
			readSettings(workspaceProperties, workspaceProperties.getFilterPrefix());
			workspaceProperties = null;
		}
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
			public void run()
			{
				triggerList.reset();
				source.setText("");
			}
		});
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.reader = TriggerReaderFactory.createReader(dbConnection);
		if (source != null) source.setDatabaseConnection(aConnection);
		if (compileAction != null) compileAction.setConnection(aConnection);
		this.reset();
		shouldRetrieve = true;
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

		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;

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
				public void run()
				{
					int rows = model.getRowCount();
					infoLabel.setText(rows + " " + ResourceMgr.getString("TxtTableListObjects"));
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

	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag && this.shouldRetrieve)
			this.retrieve();
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

	public void valueChanged(ListSelectionEvent e)
	{
		if (!initialized) return;

		if (e.getSource() != this.triggerList.getSelectionModel()) return;
		if (e.getValueIsAdjusting()) return;
		retrieveCurrentTrigger();
	}

	protected void retrieveCurrentTrigger()
	{
		int row = this.triggerList.getSelectedRow();

		if (row < 0) return;

		final String trigger = this.triggerList.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);
		final String table = this.triggerList.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TABLE);
		final String comment = this.triggerList.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_COMMENT);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				retrieveTriggerSource(trigger, table, comment);
			}
		});
	}

	protected void retrieveTriggerSource(String triggerName, String tableName, String comment)
	{
		if (this.dbConnection == null || this.reader == null) return;
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;

		Container parent = this.getParent();
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try
		{
			dbConnection.setBusy(true);

			try
			{
				TableIdentifier tbl = null;
				if (tableName != null)
				{
					tbl = new TableIdentifier(tableName);
					if (tbl.getCatalog() == null) tbl.setCatalog(currentCatalog);
					if (tbl.getSchema() == null) tbl.setSchema(currentSchema);
				}

				String sql = reader.getTriggerSource(currentCatalog, currentSchema, triggerName, tbl, comment, true);
				source.setText(sql == null ? "" : sql);
			}
			catch (Throwable ex)
			{
				LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure source", ex);
				source.setText(ExceptionUtil.getDisplay(ex));
			}
		}
		finally
		{
			parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			dbConnection.setBusy(false);
		}

		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				source.setCaretPosition(0, false);
				source.requestFocusInWindow();
			}
		});
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

	public List<DbObject> getSelectedObjects()
	{
		if (!initialized) return null;

		if (this.triggerList.getSelectedRowCount() == 0) return null;
		int[] rows = this.triggerList.getSelectedRows();
		int count = rows.length;
		List<DbObject> objects = new ArrayList<DbObject>(count);
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
				trg.setRelatedTable(new TableIdentifier(tableName));
			}
			objects.add(trg);
		}
		return objects;
	}

	public void reload()
	{
		if (!WbSwingUtilities.checkConnection(this, dbConnection)) return;
		this.reset();
		this.retrieve();
	}

}
