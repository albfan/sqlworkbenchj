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

import java.awt.Font;
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

/**
 * @author support@sql-workbench.net
 */
public class ReferenceTableNavigator
	implements ListSelectionListener, MenuListener, ActionListener
{
	private ResultReceiver receiver;
	private MainWindow container;
	private WbTable source;
	private WbMenu selectParentTables;
	private WbMenu selectChildTables;
	private boolean parentMenuInitialized = false;
	private boolean childMenuInitialized = false;
	private Font menuItemFont;
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
		parentMenuInitialized = false;
		
		selectChildTables = new WbMenu(ResourceMgr.getString("MnuTxtReferencingRows"));
		selectChildTables.addMenuListener(this);
		childMenuInitialized = false;
		
		this.source.addPopupMenu(selectParentTables, true);
		this.source.addPopupMenu(selectChildTables, false);
		
		Font menuFont = this.source.getPopupMenu().getFont();
		this.menuItemFont = new Font("Monospaced", Font.PLAIN, menuFont.getSize());
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
		if (evt.getSource() == this.selectParentTables && !parentMenuInitialized)
		{
			buildParentTablesMenu();
		}
		if (evt.getSource() == this.selectChildTables && !childMenuInitialized)
		{
			buildChildTablesMenu();
		}
	}
	
	public void menuDeselected(MenuEvent evts)
	{
		
	}
	
	public void menuCanceled(MenuEvent evt)
	{
		
	}

	private WbConnection getConnection()
	{
		return this.source.getDataStore().getOriginalConnection();
	}
	
	private void rebuildMenu()
	{
		if (parentMenuInitialized)
		{
			selectParentTables.removeAll();
			parentMenuInitialized = false;
		}
		
		if (childMenuInitialized)
		{
			selectChildTables.removeAll();
			childMenuInitialized = false;
		}
	}
	
	private void buildChildTablesMenu()
	{
		getUpdateTable();
		this.childNavigation = new ReferenceTableNavigation(this.baseTable, getConnection());
		this.childNavigation.readTreeForChildren();
		buildMenu(this.selectChildTables, this.childNavigation, "select-child");
		this.childMenuInitialized = true;
	}
	
	private void buildParentTablesMenu()
	{
		getUpdateTable();
		this.parentNavigation = new ReferenceTableNavigation(this.baseTable, getConnection());
		this.parentNavigation.readTreeForParents();
		buildMenu(this.selectParentTables, this.parentNavigation, "select-parent");
		parentMenuInitialized = true;
	}
	
	private void buildMenu(WbMenu menu, ReferenceTableNavigation navi, String cmd)
	{
		menu.setSelected(true);
		menu.repaint();
		WbSwingUtilities.showWaitCursor(menu.getParent());
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
					menu.add(item);
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
						item.setFont(menuItemFont);
						item.setVisible(true);
						item.setActionCommand(cmd);
						menu.add(item);
					}
				}
			}
			else
			{
				JMenuItem item = new JMenuItem(ResourceMgr.getString("MnuTxtNoUpdTbl"));
				item.setEnabled(false);
				menu.add(item);
			}
			menu.repaint();
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(menu);
			WbSwingUtilities.showDefaultCursor(menu.getParent());
		}
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

		String comment = null;
		if (WbAction.isShiftPressed(evt))
		{
			comment = "-- Related rows for " + getUpdateTable().getTableExpression(con);
		}
		
		if (this.container != null) 
		{
			PanelContentSender sender = new PanelContentSender(container);
			sender.showResult(sql, comment, containerIndex);
		}
		else if (receiver != null) 
		{
			receiver.showResult(sql, comment, ResultReceiver.ShowType.appendText);
		}
	}	
	
}
