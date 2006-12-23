/*
 * ReferenceTableNavigator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuListener;
import workbench.db.ColumnIdentifier;
import workbench.db.DependencyNode;
import workbench.db.ReferenceTableNavigation;
import workbench.db.ReferenceTableNavigation;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.EditorTabSelectMenu;
import workbench.interfaces.ResultReceiver;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.ColumnData;
import workbench.storage.DataStore;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * @author support@sql-workbench.net
 */
public class ReferenceTableNavigator
	implements ListSelectionListener, ActionListener, PopupMenuListener, MenuListener
{
	private ResultReceiver receiver;
	private MainWindow container;
	private WbTable source;
	private WbMenu selectParentTables;
	private WbMenu selectChildTables;
	private boolean parentMenuInitialized = false;
	private boolean childMenuInitialized = false;
	private TableIdentifier baseTable;
	private ReferenceTableNavigation parentNavigation;
	private ReferenceTableNavigation childNavigation;
	
	public ReferenceTableNavigator(WbTable data)
	{
		this.setSourceTable(data);
	}
	
	public void setReceiver(ResultReceiver target)
	{
		this.receiver = target;
		this.container = null;
		rebuildMenu();
	}
	
	public void setTargetContainer(MainWindow win)
	{
		this.receiver = null;
		this.container = win;
		rebuildMenu();
	}
	
	private void setSourceTable(WbTable data)
	{
		if (this.source != null)
		{
			this.source.getSelectionModel().removeListSelectionListener(this);
		}
		this.source = data;
		this.source.getSelectionModel().addListSelectionListener(this);

		selectParentTables = new WbMenu(ResourceMgr.getString("MnuTxtReferencedRows"));
		selectParentTables.addMenuListener(this);
		selectParentTables.getPopupMenu().addPopupMenuListener(this);
		selectParentTables.setEnabled(false);
		parentMenuInitialized = false;
		
		selectChildTables = new WbMenu(ResourceMgr.getString("MnuTxtReferencingRows"));
		selectChildTables.addMenuListener(this);
		selectChildTables.getPopupMenu().addPopupMenuListener(this);
		selectChildTables.setEnabled(false);
		childMenuInitialized = false;
		
		this.source.addPopupMenu(selectParentTables, true);
		this.source.addPopupMenu(selectChildTables, false);
	}
	
	public void valueChanged(ListSelectionEvent evt)
	{
		boolean selected = this.source.getSelectedRowCount() > 0;
		selectParentTables.setEnabled(selected);
		selectChildTables.setEnabled(selected);
	}
	
	private TableIdentifier getUpdateTable()
	{
		if (this.baseTable != null) return this.baseTable;
		if (this.source.getDataStore().checkUpdateTable())
		{
			TableIdentifier tbl = this.source.getDataStore().getUpdateTable();
			this.baseTable = tbl.createCopy();
			if (this.baseTable.getSchema() == null)
			{
				String schema = this.getConnection().getMetadata().getSchemaToUse();
				this.baseTable.setSchema(schema);
			}
		}
		return this.baseTable;
	}

	public void menuSelected(MenuEvent evt)
	{
	}
	
	public void menuDeselected(MenuEvent evt)
	{
		if (evt.getSource() == selectParentTables)
		{
			selectParentTables.getPopupMenu().setVisible(false);
		}
		if (evt.getSource() == selectChildTables)
		{
			selectChildTables.getPopupMenu().setVisible(false);
		}
	}
	
	public void menuCanceled(MenuEvent arg0)
	{
		selectParentTables.getPopupMenu().setVisible(false);
		selectChildTables.getPopupMenu().setVisible(false);
	}
	
	
	public void popupMenuWillBecomeVisible(PopupMenuEvent evt)
	{
		JPopupMenu pop = (JPopupMenu)evt.getSource();
		if (pop == this.selectParentTables.getPopupMenu() && !parentMenuInitialized)
		{
			buildParentTablesMenu();
		}
		if (pop == this.selectChildTables.getPopupMenu() && !childMenuInitialized)
		{
			buildChildTablesMenu();
		}
	}
	
	public void popupMenuWillBecomeInvisible(PopupMenuEvent evt)
	{
		
	}
	
	public void popupMenuCanceled(PopupMenuEvent evt)
	{
		if (evt.getSource() == this.selectParentTables.getPopupMenu())
		{
			selectParentTables.getPopupMenu().setVisible(false);
		}
		if (evt.getSource() == this.selectChildTables.getPopupMenu())
		{
			selectChildTables.getPopupMenu().setVisible(false);
		}
	}

	private WbConnection getConnection()
	{
		return this.source.getDataStore().getOriginalConnection();
	}
	
	private void rebuildMenu()
	{
		synchronized(selectParentTables)
		{
			selectParentTables.removeAll();
			JMenuItem item = new JMenuItem(ResourceMgr.getString("MsgLoadDependency"));
			item.setVisible(true);
			selectParentTables.add(item);
			parentMenuInitialized = false;
		}

		synchronized(selectChildTables)
		{
			selectChildTables.removeAll();
			JMenuItem item = new JMenuItem(ResourceMgr.getString("MsgLoadDependency"));
			item.setVisible(true);
			selectChildTables.add(item);
			childMenuInitialized = false;
		}
	}
	
	private void buildChildTablesMenu()
	{
		WbThread init = new WbThread("InitChildren")
		{
			public void run()
			{
				synchronized (getConnection())
				{
					childNavigation = new ReferenceTableNavigation(getUpdateTable(), getConnection());
					childNavigation.readTreeForChildren();
				}
				synchronized (childNavigation)
				{
					buildMenu(selectChildTables, childNavigation, "select-child");
					childMenuInitialized = true;
				}
				synchronized (source.getPopupMenu())
				{
					source.getPopupMenu().repaint();
				}
			}
		};
		init.start();
	}
	
	private void buildParentTablesMenu()
	{
		WbThread init = new WbThread("InitParents")
		{
			public void run()
			{
				synchronized (getConnection())
				{
					parentNavigation = new ReferenceTableNavigation(getUpdateTable(), getConnection());
					parentNavigation.readTreeForParents();
				}
				synchronized (parentNavigation)
				{
					buildMenu(selectParentTables, parentNavigation, "select-parent");
					parentMenuInitialized = true;
				}
			}
		};
		init.start();
	}
	
	private void buildMenu(WbMenu menu, ReferenceTableNavigation navi, String cmd)
	{
		List<JMenuItem> itemsToAdd = new LinkedList<JMenuItem>();

		WbSwingUtilities.showWaitCursor(menu);
		
		try
		{
			TableIdentifier tbl = getUpdateTable();
			if (tbl != null)
			{
				TableDependency dep = navi.getTree();
				List<DependencyNode> tables = dep.getLeafs();
				WbConnection con = getConnection();

				if (tables == null || tables.size() == 0)
				{
					JMenuItem item = new JMenuItem(ResourceMgr.getString("MnuTxtNoTables"));
					item.setEnabled(false);
					item.setVisible(true);
					itemsToAdd.add(item);
				}
				else
				{
					for (DependencyNode node : tables)
					{
						JMenuItem item = null;
						if (this.container == null)
						{
							item = new JMenuItem(node.getTable().getTableExpression(con));
							item.addActionListener(this);
						}
						else
						{ 
							item = new EditorTabSelectMenu(this, node.getTable().getTableExpression(con), "LblShowDataInNewTab", "LblShowDataInTab", container);
						}
						item.setVisible(true);
						boolean hasColumns = hasColumns(node);
						item.setEnabled(hasColumns);
						if (!hasColumns)
						{
							item.setToolTipText(ResourceMgr.getString("MsgRelatedNoColumns"));
						}
						item.setActionCommand(cmd);
						
						itemsToAdd.add(item);
					}
				}
			}
			else
			{
				JMenuItem item = new JMenuItem(ResourceMgr.getString("MnuTxtNoUpdTbl"));
				item.setEnabled(false);
				itemsToAdd.add(item);
			}
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(menu);
		}
		addMenuItems(menu, itemsToAdd);
	}

	/**
	 * Check if our source DataStore has all necessary columns to 
	 * be able to retrieve the table referenced by the given DependencyNode
	 */
	private boolean hasColumns(DependencyNode node)
	{
		Map<String, String> cols = node.getColumns();
		DataStore ds = this.source.getDataStore();
		for (String col : cols.values())
		{
			if (ds.getColumnIndex(col) == -1) return false;
		}
		return true;
	}
	
	private void addMenuItems(final WbMenu menu, final List<JMenuItem> items)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				synchronized (menu)
				{
					menu.removeAll();
					for (JMenuItem item : items)
					{
						menu.add(item);
					}
					JPopupMenu pop = menu.getPopupMenu();
					if (pop.isVisible())
					{
						pop.invalidate();
						pop.setVisible(false);
						pop.setVisible(true);
					}
				}
			}
		});
	}
	
	private List<List<ColumnData>> getColumnData(DependencyNode node)
	{
		List<List<ColumnData>> rows = new LinkedList<List<ColumnData>>();
		int[] selectedRows = this.source.getSelectedRows();
		int rowCount = selectedRows.length;
		
		Map<String, String> columns = node.getColumns();
		DataStore ds = this.source.getDataStore();
		for (int i = 0; i < rowCount; i++)
		{
			List<ColumnData> rowData = new LinkedList<ColumnData>();
			//for (Map.Entry<String, String> entry : columns.entrySet())
			for (String col : columns.values())
			{
				int colIndex = ds.getColumnIndex(col);
				if (colIndex > -1)
				{
					Object data = ds.getValue(selectedRows[i], colIndex);
					ColumnData cdata = new ColumnData(data, new ColumnIdentifier(col));
					rowData.add(cdata);
				}
			}
			if (rowData.size() > 0)
			{
				rows.add(rowData);
			}
		}
		
		return rows;
	}
	
	public void actionPerformed(ActionEvent evt)
	{
		JMenuItem item = (JMenuItem)evt.getSource();
		String cmd = item.getActionCommand();
		int containerIndex = -42;
		
		if (cmd.startsWith("panel-"))
		{
			containerIndex = StringUtil.getIntValue(cmd.substring(6), -1);
			JPopupMenu popup = (JPopupMenu)item.getParent();
			item = (JMenuItem)popup.getInvoker();
			cmd = item.getActionCommand();
		}
		
		WbConnection con = getConnection();
		TableIdentifier tbl = new TableIdentifier(item.getText());
		
		ReferenceTableNavigation navi = null;
		String sql = null;
		List<List<ColumnData>> rowData = null;
		if ("select-child".equals(cmd))
		{
			DependencyNode node = this.childNavigation.getNodeForTable(tbl);
			if (node == null) 
			{
				LogMgr.logError("ReferenceTableNavigator.actionPerformed", "Could not find child table from menu item!", null);
				return;
			}
			
			rowData = getColumnData(node);
			sql = this.childNavigation.getSelectForChild(tbl, rowData);
		}
		else if ("select-parent".equals(cmd))
		{
			DependencyNode node = this.parentNavigation.getNodeForTable(tbl);
			if (node == null) 
			{
				LogMgr.logError("ReferenceTableNavigator.actionPerformed", "Could not find parent table from menu item!", null);
				return;
			}
			rowData = getColumnData(node);
			sql = this.parentNavigation.getSelectForParent(tbl, rowData);
		}		
		if (sql == null)
		{
			LogMgr.logError("ReferenceTableNavigator.actionPerformed", "Could not create sql", null);
		}

		String comment = comment = ResourceMgr.getString("MsgLoadRelatedComment") + " " + getUpdateTable().getTableExpression(con);
		
		if (this.container != null) 
		{
			PanelContentSender sender = new PanelContentSender(container);
			sender.showResult(sql, comment, containerIndex);
		}
		else if (receiver != null) 
		{
			boolean shiftPressed = WbAction.isShiftPressed(evt);
			ResultReceiver.ShowType how = (shiftPressed ? ResultReceiver.ShowType.appendText : ResultReceiver.ShowType.logText); 
			receiver.showResult(sql, comment, how);
		}
	}	
	
}
