/*
 * EmptyTableModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;


/**
 *
 * @author Thomas Kellerer
 */
public class EmptyTableModel
	implements TableModel
{
	public static final TableModel EMPTY_MODEL = new EmptyTableModel();

	private EmptyTableModel()
	{
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		return "";
	}

	@Override
	public void setValueAt(Object aValue, int row, int column)
	{
	}

	@Override
	public int getColumnCount()
	{
		return 0;
	}

	@Override
	public int getRowCount()
	{
		return 0;
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		return false;
	}

	@Override
	public void addTableModelListener(TableModelListener l)
	{
	}

	@Override
	public Class getColumnClass(int columnIndex)
	{
		return String.class;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return "";
	}

	@Override
	public void removeTableModelListener(TableModelListener l)
	{
	}

}
