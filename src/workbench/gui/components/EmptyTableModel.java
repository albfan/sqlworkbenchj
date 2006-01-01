/*
 * EmptyTableModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;


public class EmptyTableModel 
	implements TableModel
{
	public static final TableModel EMPTY_MODEL = new EmptyTableModel();
	
	private EmptyTableModel()
	{
	}
	
	public Object getValueAt(int row, int col)
	{
		return "";
	}	

	public void setValueAt(Object aValue, int row, int column)
	{
		return;
	}
	
	public int getColumnCount()
	{
		return 0;
	}

	public int getRowCount() 
	{ 
		return 0;
	}

	public boolean isCellEditable(int row, int column)
	{
		return false;
	}

	public void addTableModelListener(TableModelListener l)
	{
	}	

	public Class getColumnClass(int columnIndex)
	{
		return String.class;
	}
	
	public String getColumnName(int columnIndex)
	{
		return "";
	}
	
	public void removeTableModelListener(TableModelListener l)
	{
	}
	
}
