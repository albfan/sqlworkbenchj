/*
 * ReferenceTableNavigator.java
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
package workbench.gui.sql;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DependencyNode;
import workbench.db.ReferenceTableNavigation;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.EditorTabSelectMenu;
import workbench.interfaces.ResultReceiver;
import workbench.resource.GuiSettings;

import workbench.storage.ColumnData;
import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * A class to manage the popup menu for the reference table navigation.
 * This class populates the popup menu according to the passed in base table
 * to generate the SELECT statements for retrieving referenced database rows.
 *
 * It uses several other classes to build the GUI and retrieve the necessary
 * database information
 * @see workbench.db.ReferenceTableNavigation
 * @see workbench.db.TableDependency
 * @see workbench.gui.dbobjects.EditorTabSelectMenu
 * @author Thomas Kellerer
 */
public class ReferenceTableNavigator
	implements ListSelectionListener, ActionListener, PopupMenuListener, MenuListener,
					   PropertyChangeListener
{
	private MainWindow container;
	private WbTable source;
	private WbMenu selectParentTables;
	private WbMenu selectChildTables;
	private boolean parentMenuInitialized;
	private boolean childMenuInitialized;
	private TableIdentifier baseTable;
	private ReferenceTableNavigation parentNavigation;
	private ReferenceTableNavigation childNavigation;
	private final Object parentLock = new Object();
	private final Object childLock = new Object();
	private final Object connectionLock = new Object();

	public ReferenceTableNavigator(DwPanel data, MainWindow win)
	{
		this.setSourceTable(data.getTable());
		data.addPropertyChangeListener(DwPanel.PROP_UPDATE_TABLE, this);
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

	public void removeFromPopup()
	{
		if (this.source == null) return;
		this.source.removeSubmenu(selectChildTables);
		this.source.removeSubmenu(selectParentTables);
	}

	@Override
	public void valueChanged(ListSelectionEvent evt)
	{
    if (evt.getValueIsAdjusting()) return;
		boolean selected = this.source.getSelectedRowCount() > 0;
		selectParentTables.setEnabled(selected);
		selectChildTables.setEnabled(selected);
	}

	private TableIdentifier getUpdateTable()
	{
		if (this.baseTable != null) return this.baseTable;
		TableIdentifier tbl = this.source.getDataStore().getUpdateTable();
		if (tbl == null)
		{
			if (this.source.getDataStore().checkUpdateTable())
			{
				tbl = this.source.getDataStore().getUpdateTable();
			}
		}
		if (tbl != null)
		{
			this.baseTable = tbl.createCopy();
			if (this.baseTable.getSchema() == null)
			{
				String schema = this.getConnection().getMetadata().getSchemaToUse();
				this.baseTable.setSchema(schema);
			}
		}
		return this.baseTable;
	}

	@Override
	public void menuSelected(MenuEvent evt)
	{
	}

	@Override
	public void menuDeselected(MenuEvent evt)
	{
		closePopup(evt);
	}

	@Override
	public void menuCanceled(MenuEvent evt)
	{
		selectParentTables.getPopupMenu().setVisible(false);
		selectChildTables.getPopupMenu().setVisible(false);
	}

	@Override
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

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent evt)
	{

	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent evt)
	{
		closePopup(evt);
	}

	private void closePopup(EventObject evt)
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

	private JMenuItem createLoadingItem()
	{
		JMenuItem item = new JMenuItem(ResourceMgr.getString("MsgLoadDependency"));
		item.setVisible(true);
		item.setEnabled(false);
		return item;
	}

	private void rebuildMenu()
	{
		synchronized (parentLock)
		{
			selectParentTables.removeAll();
			selectParentTables.add(createLoadingItem());
			parentMenuInitialized = false;
		}

		synchronized (childLock)
		{
			selectChildTables.removeAll();
			selectChildTables.add(createLoadingItem());
			childMenuInitialized = false;
		}
	}

	private void buildChildTablesMenu()
	{
		WbThread init = new WbThread("InitChildren")
		{
			@Override
			public void run()
			{
				synchronized (connectionLock)
				{
					childNavigation = new ReferenceTableNavigation(getUpdateTable(), getConnection());
					childNavigation.readTreeForChildren();
				}
				synchronized (childLock)
				{
					buildMenu(selectChildTables, childNavigation, "select-child");
					childMenuInitialized = true;
				}
			}
		};
		init.start();
	}

	private void buildParentTablesMenu()
	{
		WbThread init = new WbThread("InitParents")
		{
			@Override
			public void run()
			{
				synchronized (connectionLock)
				{
					parentNavigation = new ReferenceTableNavigation(getUpdateTable(), getConnection());
					parentNavigation.readTreeForParents();
				}

				synchronized (parentLock)
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
		List<JMenuItem> itemsToAdd = new LinkedList<>();

		TableIdentifier tbl = getUpdateTable();
		if (tbl != null)
		{
			TableDependency dep = navi.getTree();
			List<DependencyNode> tables = dep.getLeafs();

			if (CollectionUtil.isEmpty(tables))
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
					Map<String, String> columnMap = node.getColumns();
					Collection<String> cols = node.getColumns().values();
					StringBuilder display = new StringBuilder(cols.size() * 10);
					StringBuilder tooltip = new StringBuilder(cols.size() * 20);

					display.append(node.getTable().getTableName());
					display.append(" (");
					int index = 0;
					for (Map.Entry<String, String> entry : columnMap.entrySet())
					{
						if (index > 0)
						{
							display.append(',');
							tooltip.append(',');
						}
						display.append(entry.getKey());
						tooltip.append(this.getUpdateTable().getTableName());
						tooltip.append('.');
						tooltip.append(entry.getValue());
						tooltip.append(" > ");
						tooltip.append(node.getTable().getTableName());
						tooltip.append('.');
						tooltip.append(entry.getKey());
						index++;
					}
					display.append(')');
					EditorTabSelectMenu item = new EditorTabSelectMenu(display.toString(), "LblShowDataInNewTab", "MsgRelatedTabHint", container);
          item.setActionListener(this);
					item.setDependencyNode(node);
					item.setVisible(true);
					item.setToolTipText(tooltip.toString());
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
			@Override
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
						// The popup menu is not repainted correctly
						// if not made invisible. Neither doLayout() or updateUI()
						// adjust the height and width of the popup menu correctly
						pop.setVisible(false);
						pop.setVisible(true);
					}
				}
			}
		});
	}

	private List<List<ColumnData>> getColumnData(DependencyNode node)
	{
		List<List<ColumnData>> rows = new LinkedList<>();
		int[] selectedRows = this.source.getSelectedRows();
		int rowCount = selectedRows.length;

		Map<String, String> columns = node.getColumns();
		DataStore ds = this.source.getDataStore();
		for (int i = 0; i < rowCount; i++)
		{
			List<ColumnData> rowData = new LinkedList<>();
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

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JMenuItem item = (JMenuItem)evt.getSource();
		String cmd = item.getActionCommand();
		int containerIndex = -42;

    ResultReceiver.ShowType showType = GuiSettings.getDefaultShowType();

		if (cmd.startsWith(EditorTabSelectMenu.PANEL_CMD_PREFIX))
		{
			containerIndex = StringUtil.getIntValue(cmd.substring(EditorTabSelectMenu.PANEL_CMD_PREFIX.length()), -1);
			JPopupMenu popup = (JPopupMenu)item.getParent();
			item = (JMenuItem)popup.getInvoker();
			cmd = item.getActionCommand();
		}

    if (containerIndex == PanelContentSender.NEW_PANEL)
    {
      showType = GuiSettings.getDefaultShowTypeNewTab();
    }

		TableIdentifier tbl = null;
		String fkName = null;
		String error = null;

		if (item instanceof EditorTabSelectMenu)
		{
			DependencyNode node = ((EditorTabSelectMenu)item).getDependencyNode();
			tbl = node.getTable();
			fkName = node.getFkName();
		}
		else
		{
			WbSwingUtilities.showErrorMessage(this.container, "Menu was incorrectly initialized!");
			return;
		}

		String sql = null;

		List<List<ColumnData>> rowData = null;
		try
		{
			if ("select-child".equals(cmd))
			{
				DependencyNode node = this.childNavigation.getNodeForTable(tbl, fkName);
				if (node == null)
				{
					error = "Could not find child table from menu item!";
					LogMgr.logError("ReferenceTableNavigator.actionPerformed()", error, null);
				}
				else
				{
					rowData = getColumnData(node);
					sql = this.childNavigation.getSelectForChild(tbl, fkName, rowData);
				}
			}
			else if ("select-parent".equals(cmd))
			{
				DependencyNode node = this.parentNavigation.getNodeForTable(tbl, fkName);
				if (node == null)
				{
					error = "Could not find parent table from menu item!";
					LogMgr.logError("ReferenceTableNavigator.actionPerformed()", error, null);
				}
				else
				{
					rowData = getColumnData(node);
					sql = this.parentNavigation.getSelectForParent(tbl, fkName, rowData);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("ReferenceTableNavigator.actionPerformed", "Error when creating SQL", e);
			error = ExceptionUtil.getDisplay(e);
		}

		if (sql == null || error != null)
		{
			WbSwingUtilities.showErrorMessage(this.container, error);
			return;
		}

		String comment = ResourceMgr.getFormattedString("MsgLoadRelatedComment", tbl.getTableName(), getUpdateTable().getTableName(), fkName);

		if (this.container != null)
		{
			PanelContentSender sender = new PanelContentSender(container, null);
			sender.showResult(sql, comment, containerIndex, showType);
		}
	}

	/**
	 * Reset the internal dependency tree and popup menu.
	 * The next time the menu should be displayed, the
	 * source table will then abe queried for the new
	 * update table.
	 */
	public void reset()
	{
		this.baseTable = null;
		this.rebuildMenu();
	}

	public void dispose()
	{
		selectParentTables.dispose();
		parentMenuInitialized = false;
		selectChildTables.dispose();
		childMenuInitialized = false;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(DwPanel.PROP_UPDATE_TABLE))
		{
			reset();
		}
	}

}
