/*
 * TableRowHeaderModel
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.components;

import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import workbench.util.NumberStringCache;

/**
 *
 * @author Thomas Kellerer
 */
public class TableRowHeaderModel
	implements TableModel
{
	private JTable table;

	public TableRowHeaderModel(JTable client)
	{
		this.table = client;
	}

	@Override
	public int getRowCount()
	{
		return table.getRowCount();
	}

	@Override
	public int getColumnCount()
	{
		return 1;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return NumberStringCache.getNumberString(rowIndex + 1);
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return "";
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
	}

	@Override
	public void addTableModelListener(TableModelListener l)
	{
	}

	@Override
	public void removeTableModelListener(TableModelListener l)
	{
	}
}
