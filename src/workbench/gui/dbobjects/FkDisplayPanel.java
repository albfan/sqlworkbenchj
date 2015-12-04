/*
 * FkDisplayPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import workbench.interfaces.Interruptable;
import workbench.interfaces.Reloadable;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.ResourceMgr;

import workbench.db.DependencyNode;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DropForeignKeyAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.StopAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.renderer.RendererSetup;
import workbench.resource.IconMgr;

import workbench.storage.DataStore;

import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class FkDisplayPanel
	extends JPanel
	implements Resettable, Reloadable, Interruptable, ActionListener
{
	protected WbTable keys;
	private final TableDependencyTreeDisplay dependencyTree;
	private final WbSplitPane splitPanel;

	private final ReloadAction reloadTree;
	private final StopAction cancelAction;
	private final JCheckBox retrieveAll;

	private final boolean showImportedKeys;
	private WbConnection dbConnection;
	private boolean isRetrieving;
	private boolean isTreeRetrieving;

	private TableIdentifier currentTable;
	private final JMenuItem selectTableItem;
	private DropForeignKeyAction dropFK;
	private final TableLister tables;

	public FkDisplayPanel(TableLister lister, boolean showImported)
	{
		super(new BorderLayout());
		this.keys = new WbTable();
		this.keys.setAdjustToColumnLabel(false);
		this.keys.setRendererSetup(RendererSetup.getBaseSetup());
		WbScrollPane scroll = new WbScrollPane(this.keys);
		this.splitPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.splitPanel.setDividerLocation(100);
		this.splitPanel.setDividerSize((int)(IconMgr.getInstance().getSizeForLabel() / 2));
    splitPanel.setDividerBorder(WbSwingUtilities.EMPTY_BORDER);
		this.splitPanel.setTopComponent(scroll);
		tables = lister;
		this.dependencyTree = new TableDependencyTreeDisplay(lister);
		this.dependencyTree.reset();
		JPanel treePanel = new JPanel(new BorderLayout());
		treePanel.add(this.dependencyTree, BorderLayout.CENTER);

		WbToolbar toolbar = new WbToolbar();
		toolbar.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		reloadTree = new ReloadAction(this);
		cancelAction = new StopAction(this);
		cancelAction.setEnabled(false);
		toolbar.add(reloadTree);
		toolbar.add(cancelAction);
		toolbar.addSeparator();

		treePanel.add(toolbar, BorderLayout.NORTH);
		this.splitPanel.setBottomComponent(treePanel);
		this.add(splitPanel, BorderLayout.CENTER);
		showImportedKeys = showImported;

		retrieveAll = new JCheckBox(ResourceMgr.getString("LblRefAllLevel"));
		retrieveAll.setToolTipText(ResourceMgr.getDescription("LblRefAllLevel"));
		retrieveAll.setBorder(new EmptyBorder(0, 5, 0,0));
		retrieveAll.setSelected(true);
		toolbar.add(retrieveAll);

		selectTableItem = new JMenuItem(ResourceMgr.getString("MnuTextSelectInList"));
		selectTableItem.addActionListener(this);

		dropFK = new DropForeignKeyAction(this);
		keys.addPopupAction(dropFK, true);
		keys.addPopupMenu(selectTableItem, false);
	}

	public boolean getRetrieveAll()
	{
		return retrieveAll.isSelected();
	}

	public void setRetrieveAll(boolean flag)
	{
		retrieveAll.setSelected(flag);
	}

	public void setConnection(WbConnection conn)
	{
		dbConnection = conn;
		dependencyTree.setConnection(conn);
	}

	public int getDividerLocation()
	{
		return splitPanel.getDividerLocation();
	}

	public void setDividerLocation(int location)
	{
		splitPanel.setDividerLocation(location);
	}

	public TableDependencyTreeDisplay getTree()
	{
		return dependencyTree;
	}

	public WbTable getKeyDisplay()
	{
		return keys;
	}

	public TableIdentifier getCurrentTable()
	{
		return currentTable;
	}

	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	public Map<TableIdentifier, String> getSelectedForeignKeys()
	{
		int[] rows = keys.getSelectedRows();

		DataStore ds = keys.getDataStore();

		Map<TableIdentifier, String> result = new HashMap<>();

		for (int i=0; i < rows.length; i++)
		{
			int row = rows[i];
			DependencyNode node = (DependencyNode)ds.getRow(row).getUserObject();
			String fkName = node.getFkName();
			TableIdentifier constraintTable = null;
			if (showImportedKeys)
			{
				constraintTable = currentTable;
			}
			else
			{
				constraintTable = node.getTable();
			}
			if (constraintTable != null)
			{
				result.put(constraintTable, fkName);
			}
		}
		return result;
	}

	@Override
	public void reset()
	{
		keys.reset();
		dependencyTree.reset();
	}

	public boolean isRetrieving()
	{
		return isRetrieving || isTreeRetrieving;
	}

	public void cancel()
	{
		dependencyTree.cancelRetrieve();
	}

	public void reloadTable()
	{
		try
		{
			reset();
			retrieve(currentTable);
		}
		catch (SQLException sql)
		{
			LogMgr.logError("FkDisplayPanel.reloadTable()", "Could not reload constraints", sql);
		}
	}

	protected void retrieve(TableIdentifier table)
		throws SQLException
	{
		try
		{
			currentTable = table;
			isRetrieving = true;

			final DataStoreTableModel model = new DataStoreTableModel(getFKDataStore(table));

			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					keys.setModel(model, true);
					keys.adjustRowsAndColumns();
					dependencyTree.reset();
				}
			});

			if (DbExplorerSettings.getAutoRetrieveFKTree())
			{
				reload();
			}
		}
		finally
		{
			isRetrieving = false;
		}
	}

	private DataStore getFKDataStore(TableIdentifier table)
	{
		TableDependency deps = new TableDependency(dbConnection, table);
		return deps.getDisplayDataStore(showImportedKeys);
	}

	protected void retrieveTree(TableIdentifier table)
	{
		dependencyTree.setRetrieveAll(retrieveAll.isSelected());
		if (showImportedKeys)
		{
			dependencyTree.readReferencedTables(table);
		}
		else
		{
			dependencyTree.readReferencingTables(table);
		}
	}

	@Override
	public void reload()
	{
		WbThread t = new WbThread("DependencyTreeRetriever")
		{
			@Override
			public void run()
			{
				try
				{
					isTreeRetrieving = true;
					retrieveTree(currentTable);
				}
				finally
				{
					isTreeRetrieving = false;
					reloadTree.setEnabled(true);
					cancelAction.setEnabled(false);
					WbSwingUtilities.showDefaultCursor(FkDisplayPanel.this);
				}
			}
		};
		reloadTree.setEnabled(false);
		cancelAction.setEnabled(true);
		WbSwingUtilities.showWaitCursor(this);
		t.start();
	}

	@Override
	public void cancelExecution()
	{
		if (isTreeRetrieving)
		{
			dependencyTree.cancelRetrieve();
			cancelAction.setEnabled(false);
		}
	}

	@Override
	public boolean confirmCancel()
	{
		return true;
	}

	private TableIdentifier getReferencedTable(int row)
	{
		if (row < 0) return null;

		DataStore ds = keys.getDataStore();
		DependencyNode node = (DependencyNode)ds.getRow(row).getUserObject();
		return node.getTable();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (tables == null) return;
		if (e.getSource() != selectTableItem) return;

		int selected = keys.getSelectedRow();
		if (selected < 0) return;

		final TableIdentifier tbl = getReferencedTable(selected);
		if (tbl == null) return;

		LogMgr.logDebug("FkDisplayPanel.actionPerformed()", "Trying to select table: " + tbl.getTableName());
		WbSwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				tables.selectTable(tbl);
			}
		});
	}
}
