/*
 * TriggerListPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import javax.swing.JLabel;
import workbench.WbManager;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.gui.MainWindow;
import workbench.gui.actions.CompileDbObjectAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.QuickFilterPanel;
import workbench.interfaces.CriteriaPanel;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.WbWorkspace;

/**
 *
 * @author  support@sql-workbench.net
 *
 */
public class TriggerListPanel
	extends JPanel
	implements ListSelectionListener, Reloadable, DbObjectList
{
	private WbConnection dbConnection;
	private JPanel listPanel;
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
	
	public TriggerListPanel(MainWindow parent) 
		throws Exception
	{
		Reloadable sourceReload = new Reloadable()
		{
			public void reload()
			{
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
		
		this.source = new DbObjectSourcePanel(parent, sourceReload);

		this.listPanel = new JPanel();
		this.statusRenderer = new ProcStatusRenderer();
		this.triggerList = new WbTable(true, false, false)
		{
			public TableCellRenderer getCellRenderer(int row, int column) 
			{
				if (column == ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE) return statusRenderer;
				return super.getCellRenderer(row, column);
			}
		};
		
		this.triggerList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.triggerList.setCellSelectionEnabled(false);
		this.triggerList.setColumnSelectionAllowed(false);
		this.triggerList.setRowSelectionAllowed(true);
		this.triggerList.getSelectionModel().addListSelectionListener(this);
		this.triggerList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		String[] cols = new String[] {"NAME", "TYPE", "EVENT"};
		this.findPanel = new QuickFilterPanel(this.triggerList, cols, false, "triggerlist");
		
		ReloadAction a = new ReloadAction(this);
		
		this.findPanel.addToToolbar(a, true, false);
		a.getToolbarButton().setToolTipText(ResourceMgr.getString("TxtRefreshTriggerList"));
		this.listPanel.setLayout(new BorderLayout());
		this.listPanel.add((JPanel)findPanel, BorderLayout.NORTH);

		this.splitPane = new WbSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.splitPane.setOneTouchExpandable(true);
		this.splitPane.setDividerSize(6);
		WbScrollPane scroll = new WbScrollPane(this.triggerList);

		this.listPanel.add(scroll, BorderLayout.CENTER);

		this.infoLabel = new JLabel("");
		EmptyBorder b = new EmptyBorder(1, 3, 0, 0);
		this.infoLabel.setBorder(b);
		this.listPanel.add(this.infoLabel, BorderLayout.SOUTH);

		this.splitPane.setLeftComponent(this.listPanel);
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
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.reset();
	}

	public void reset()
	{
		this.triggerList.reset();
		this.source.setText("");
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.source.setDatabaseConnection(aConnection);
		this.compileAction.setConnection(aConnection);
		this.reset();
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
			this.isRetrieving = true;
			DbMetadata meta = dbConnection.getMetadata();
			WbSwingUtilities.showWaitCursorOnWindow(this);
			
			DataStore ds = meta.getTriggers(currentCatalog, currentSchema);
			final DataStoreTableModel model = new DataStoreTableModel(ds);
			
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					int rows = model.getRowCount();
					infoLabel.setText(rows + " " + ResourceMgr.getString("TxtTableListObjects"));
					triggerList.setModel(model, true);
					triggerList.adjustOrOptimizeColumns();
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
		if (e.getSource() != this.triggerList.getSelectionModel()) return;
		if (e.getValueIsAdjusting()) return;
		retrieveCurrentTrigger();
	}
	
	protected void retrieveCurrentTrigger()
	{
		int row = this.triggerList.getSelectedRow();

		if (row < 0) return;

		final String trigger = this.triggerList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				retrieveTriggerSource(trigger);
			}
		});
	}

	protected void retrieveTriggerSource(String triggerName)
	{
		if (this.dbConnection == null) return;
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		
		DbMetadata meta = dbConnection.getMetadata();
		Container parent = this.getParent();
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try
		{
			dbConnection.setBusy(true);

			try
			{
				String sql = meta.getTriggerSource(currentCatalog, currentSchema, triggerName);
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
		if (this.triggerList.getSelectedRowCount() == 0) return null;
		int rows[] = this.triggerList.getSelectedRows();
		int count = rows.length;
		List<DbObject> objects = new ArrayList<DbObject>(count);
		if (count == 0) return objects;
		
		for (int i=0; i < count; i ++)
		{
			String name = this.triggerList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);

			// MS SQL Server appends a semicolon at the end of the name...
			int pos = name.indexOf(';');
			if (pos > 0)
			{
				name = name.substring(0, pos);
			}

			// To build the correct schema, catalog and trigger name 
			// we use the functionality built into TableIdentifier
			// The name of a trigger should follow the same rules as a table
			// name. So it should be save to apply the same algorithm to 
			// build a correctly qualified name
			TriggerDefinition trg = new TriggerDefinition(currentCatalog, currentSchema, name);
			objects.add(trg);
		}
		return objects;
	}

	public void reload()
	{
		this.reset();
		this.retrieve();
	}
	
}
