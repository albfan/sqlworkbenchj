/*
 * ProcedureListPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.FindPanel;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.WbThread;
import javax.swing.JLabel;
import workbench.gui.components.DataStoreTableModel;


/**
 *
 * @author  info@sql-workbench.net
 *
 */
public class ProcedureListPanel
	extends JPanel
	implements ListSelectionListener, Reloadable, ActionListener
{
	private WbConnection dbConnection;
	private JPanel listPanel;
	private FindPanel findPanel;
	private WbTable procList;
	private WbTable procColumns;
	private EditorPanel source;
	private JTabbedPane displayTab;
	private WbSplitPane splitPane;
	private Object retrieveLock = new Object();
	private String currentSchema;
	private String currentCatalog;
	private boolean shouldRetrieve;
	private WbMenuItem dropTableItem;
	private WbMenuItem recompileItem;
	private JLabel infoLabel;

	private static final String DROP_CMD = "drop-object";
	private static final String COMPILE_CMD = "compile-procedure";
	private EmptyBorder EMPTY = new EmptyBorder(1,1, 3, 1);

	public ProcedureListPanel() throws Exception
	{
		this.displayTab = new JTabbedPane();
		this.displayTab.setTabPlacement(JTabbedPane.BOTTOM);
		this.displayTab.setUI(TabbedPaneUIFactory.getBorderLessUI());
		this.displayTab.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.procColumns = new WbTable();
		this.procColumns.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.procColumns.setCellSelectionEnabled(false);
		this.procColumns.setColumnSelectionAllowed(false);
		this.procColumns.setRowSelectionAllowed(true);
		this.procColumns.getSelectionModel().addListSelectionListener(this);
		this.procColumns.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.procColumns.setAdjustToColumnLabel(false);
		JScrollPane scroll = new WbScrollPane(this.procColumns);

		this.source = EditorPanel.createSqlEditor();
		this.source.setEditable(false);
		this.source.showFindOnPopupMenu();

		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerSource"), this.source);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), scroll);

		this.listPanel = new JPanel();
		this.procList = new WbTable();
		this.procList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.procList.setCellSelectionEnabled(false);
		this.procList.setColumnSelectionAllowed(false);
		this.procList.setRowSelectionAllowed(true);
		this.procList.getSelectionModel().addListSelectionListener(this);
		this.procList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.procList.setAdjustToColumnLabel(false);

		this.findPanel = new FindPanel(this.procList);
		ReloadAction a = new ReloadAction(this);
		this.findPanel.addToToolbar(a, true, false);
		a.getToolbarButton().setToolTipText(ResourceMgr.getString("TxtRefreshProcedureList"));
		this.listPanel.setLayout(new BorderLayout());
		this.listPanel.add(findPanel, BorderLayout.NORTH);

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
		pol.setDefaultComponent(findPanel);
		pol.addComponent(this.procList);
		pol.addComponent(this.procColumns);
		this.setFocusTraversalPolicy(pol);
		this.reset();
		this.extendPopupMenu();
	}

	private void extendPopupMenu()
	{
		JPopupMenu popup = this.procList.getPopupMenu();
		popup.addSeparator();
		this.dropTableItem = new WbMenuItem(ResourceMgr.getString("MnuTxtDropDbObject"));
		this.dropTableItem.setActionCommand(DROP_CMD);
		this.dropTableItem.addActionListener(this);
		this.dropTableItem.setEnabled(false);
		this.dropTableItem.setIcon(ResourceMgr.getImage("blank"));
		popup.add(this.dropTableItem);
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
		this.source.getSqlTokenMarker().initDatabaseKeywords(aConnection.getSqlConnection());
		this.reset();

		if (this.dbConnection.getMetadata().isOracle())
		{
			this.recompileItem = new WbMenuItem(ResourceMgr.getString("MnuTxtRecompile"));
			this.recompileItem.setActionCommand(COMPILE_CMD);
			this.recompileItem.addActionListener(this);
			this.recompileItem.setEnabled(false);
			this.recompileItem.setIcon(ResourceMgr.getImage("blank"));
			JPopupMenu popup = this.procList.getPopupMenu();
			popup.add(this.recompileItem);
		}
		else
		{
			this.recompileItem = null;
		}
	}

	public void setCatalogAndSchema(String aCatalog, String aSchema)
		throws Exception
	{
		this.setCatalogAndSchema(aCatalog, aSchema, true);
	}

	public void setCatalogAndSchema(String aCatalog, String aSchema, boolean retrieve)
		throws Exception
	{
		this.reset();
		this.currentSchema = aSchema;
		this.currentCatalog = aCatalog;
		if (this.isVisible() && retrieve)
		{
			this.retrieve();
			this.procList.requestFocusInWindow();
		}
		else
		{
			this.shouldRetrieve = true;
		}

	}

	public void retrieveIfNeeded()
	{
		if (this.shouldRetrieve) this.retrieve();
	}

	public void startRetrieve()
	{
		Thread t = new WbThread("ProcedureListPanel retrieve thread")
		{
			public void run()
			{
				retrieve();
			}
		};
		t.start();
	}

	public void retrieve()
	{
		synchronized (retrieveLock)
		{
			try
			{
				DbMetadata meta = dbConnection.getMetadata();
				WbSwingUtilities.showWaitCursorOnWindow(this);
				DataStoreTableModel model = meta.getListOfProcedures(currentCatalog, currentSchema);
				int rows = model.getRowCount();
				String info = rows + " " + ResourceMgr.getString("TxtTableListObjects");
				this.infoLabel.setText(info);
				procList.setModel(model, true);
				procList.adjustColumns();
				shouldRetrieve = false;
			}
			catch (OutOfMemoryError mem)
			{
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("MsgOutOfMemoryError"));
			}
			catch (Throwable e)
			{
				LogMgr.logError("ProcedureListPanel.retrieve() thread", "Could not retrieve procedure list", e);
			}
			finally
			{
				WbSwingUtilities.showDefaultCursorOnWindow(this);
			}
		}
	}

	private void dropObjects()
	{
		if (this.procList.getSelectedRowCount() == 0) return;
		int rows[] = this.procList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return;

		ArrayList names = new ArrayList(count);
		ArrayList types = new ArrayList(count);

		this.readSelecteItems(names, types);

		ObjectDropperUI ui = new ObjectDropperUI();
		ui.setObjects(names, types);
		ui.setConnection(this.dbConnection);
		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(this);
		ui.showDialog(f);
		if (!ui.dialogWasCancelled())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					reload();
				}
			});
		}
	}

	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag && this.shouldRetrieve)
			this.retrieve();
	}

	public void saveSettings()
	{
		Settings.getInstance().setProperty(this.getClass().getName(), "divider", this.splitPane.getDividerLocation());
		Settings.getInstance().setProperty(this.getClass().getName(), "lastsearch", this.findPanel.getSearchString());
	}

	public void restoreSettings()
	{
		int loc = Settings.getInstance().getIntProperty(this.getClass().getName(), "divider");
		if (loc == 0) loc = 200;
		this.splitPane.setDividerLocation(loc);

		String s = Settings.getInstance().getProperty(this.getClass().getName(), "lastsearch", "");
		this.findPanel.setSearchString(s);
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		final Container parent = this.getParent();
		int row = this.procList.getSelectedRow();

		if (row < 0) return;
		this.dropTableItem.setEnabled(this.procList.getSelectedRowCount() > 0);
		if (this.recompileItem != null)
		{
			this.recompileItem.setEnabled(this.procList.getSelectedRowCount() > 0);
		}

		final String proc = this.procList.getValueAsString(row, DbMetadata.COLUMN_IDX_PROC_LIST_NAME);
		final String schema = this.procList.getValueAsString(row, DbMetadata.COLUMN_IDX_PROC_LIST_SCHEMA);
		final String catalog = this.procList.getValueAsString(row, DbMetadata.COLUMN_IDX_PROC_LIST_CATALOG);

		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				synchronized (retrieveLock)
				{
					DbMetadata meta = dbConnection.getMetadata();
					parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try
					{
						procColumns.setVisible(false);
						procColumns.setModel(meta.getProcedureColumns(catalog, schema, proc), true);
						procColumns.adjustColumns();
						procColumns.setVisible(true);
					}
					catch (Exception ex)
					{
						LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure definition", ex);
						procColumns.reset();
						procColumns.setVisible(true);
					}

					try
					{
						String sql = meta.getProcedureSource(catalog, schema, proc);
						source.setText(sql);
					}
					catch (Exception ex)
					{
						LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure source", ex);
						source.setText(ex.getMessage());
					}
					source.setCaretPosition(0);

					parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});
	}

	private void readSelecteItems(ArrayList names, ArrayList types)
	{
		if (this.procList.getSelectedRowCount() == 0) return;
		int rows[] = this.procList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return;

		for (int i=0; i < count; i ++)
		{
			String name = this.procList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_PROC_LIST_NAME);

			// MS SQL Server appends a semicolon at the end of the name...
			if (name.indexOf(';') > 0)
			{
				name = name.substring(0, name.indexOf(';'));
			}

			String schema = this.procList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_PROC_LIST_SCHEMA);
      if (schema != null && schema.length() > 0)
      {
  			name = SqlUtil.quoteObjectname(schema) + "." + SqlUtil.quoteObjectname(name);
      }
      else
      {
        name = SqlUtil.quoteObjectname(name);
      }

			String type = this.procList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_PROC_LIST_TYPE);
			if (this.dbConnection.getMetadata().isOracle())
			{
				// Oracle reports the type of the procedure in a rather strange way.
				// the only way to tell if it's a package, is to look at the CATALOG column
				// if that contains an entry, it's a packaged procedure
				String catalog = this.procList.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_PROC_LIST_CATALOG);
				if (catalog != null && catalog.length() > 0)
				{
					type = "PACKAGE";

					// the procedure itself cannot neither be dropped
					// nor recompiled. So we use the name of the package
					// as the object name
					name = catalog;
				}
      	else if ("RESULT".equalsIgnoreCase(type))
      	{
        	type = "FUNCTION";
				}
				else if ("NO RESULT".equalsIgnoreCase(type))
				{
					type = "PROCEDURE";
				}
			}
			else
			{
				if ("RESULT".equalsIgnoreCase(type))
      	{
        	type = "FUNCTION";
				}
				else if ("NO RESULT".equalsIgnoreCase(type))
				{
					type = "PROCEDURE";
				}
			}
			names.add(name);
			types.add(type);
		}
	}

	private void compileObjects()
	{
		if (this.procList.getSelectedRowCount() == 0) return;
		int rows[] = this.procList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return;

		ArrayList names = new ArrayList(count);
		ArrayList types = new ArrayList(count);

		this.readSelecteItems(names, types);

		try
		{
			ObjectCompilerUI ui = new ObjectCompilerUI(names, types, this.dbConnection);
			ui.show(SwingUtilities.getWindowAncestor(this));
		}
		catch (SQLException e)
		{
			LogMgr.logError("ProcedureListPanel.compileObjects()", "Error initializing ObjectCompilerUI", e);
		}

	}
	public void reload()
	{
		this.reset();
		this.retrieve();
	}

	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (DROP_CMD.equals(command))
		{
			this.dropObjects();
		}
		else if (COMPILE_CMD.equals(command))
		{
			this.compileObjects();
		}
	}
}