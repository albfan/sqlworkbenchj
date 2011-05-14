/*
 * DbObjectTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import workbench.db.ColumnIdentifier;
import workbench.gui.actions.ResetColOrderAction;
import workbench.gui.components.ColumnOrderMgr;
import workbench.gui.components.WbTable;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;

/**
 * @author Thomas Kellerer
 */
public class DbObjectTable
	extends WbTable
{
	private boolean rememberColumnOrder;
	private List<String> columnOrder;
	private List<String> originalColumnOrder;
	private List<String> pendingOrder;

	public DbObjectTable()
	{
		super(true, false, false);
		setAutoResizeMode(AUTO_RESIZE_OFF);
		setCellSelectionEnabled(false);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public void setRememberColumnOrder(boolean flag)
	{
		rememberColumnOrder = flag;
	}

	public boolean getRememberColumnOrder()
	{
		return rememberColumnOrder;
	}

	@Override
	protected JPopupMenu getHeaderPopup()
	{
		JPopupMenu menu = super.getHeaderPopup();
		if (isColumnOrderChanged() && originalColumnOrder != null)
		{
			ResetColOrderAction resetColOrder = new ResetColOrderAction(null)
			{
				@Override
				public void executeAction(ActionEvent e)
				{
					ColumnOrderMgr.getInstance().applyColumnOrder(DbObjectTable.this, originalColumnOrder, false);
				}

				@Override
				public boolean isEnabled()
				{
					return true;
				}
			};
			menu.addSeparator();
			menu.add(resetColOrder);
		}
		return menu;
	}

	public void setOriginalOrder(DataStore objectList)
	{
		ColumnIdentifier[] columns = objectList.getColumns();
		originalColumnOrder = new ArrayList<String>(columns.length);
		for (ColumnIdentifier col : columns)
		{
			originalColumnOrder.add(col.getColumnName());
		}
	}

	public void setNewColumnOrder(List<String> newOrder)
	{
		if (CollectionUtil.isEmpty(newOrder))
		{
			this.pendingOrder = null;
		}
		else
		{
			this.pendingOrder = new ArrayList<String>(newOrder);
		}
	}

	public List<String> saveColumnOrder()
	{
		if (rememberColumnOrder)
		{
			columnOrder = ColumnOrderMgr.getInstance().getColumnOrder(this);
			return new ArrayList<String>(columnOrder);
		}
		return null;
	}

	public void restoreSavedColumnOrder()
	{
		if (pendingOrder != null)
		{
			if (canApplyColumnOrder(pendingOrder))
			{
				ColumnOrderMgr.getInstance().applyColumnOrder(this, pendingOrder, true);
			}
			pendingOrder = null;
		}
		else if (canApplyColumnOrder(columnOrder))
		{
			ColumnOrderMgr.getInstance().applyColumnOrder(this, columnOrder, true);
		}
	}

	private boolean canApplyColumnOrder(List<String> newColumns)
	{
		if (CollectionUtil.isEmpty(newColumns)) return false;
		if (getColumnCount() == 0) return false;

		Set<String> currentCols = CollectionUtil.caseInsensitiveSet();
		for (int i=0; i < getColumnCount(); i++)
		{
			currentCols.add(getColumnName(i));
		}

		Set<String> newCols = CollectionUtil.caseInsensitiveSet();
		newCols.addAll(newColumns);
		return currentCols.containsAll(newCols);
	}

	@Override
	public void setModel(TableModel aModel, boolean allowSort)
	{
		super.setModel(aModel, allowSort);
		restoreSavedColumnOrder();
	}
}
