/*
 * OneLineTableModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
 * @author support@sql-workbench.net
 */
public class OneLineTableModel
	implements TableModel
{
	private String columnTitle;
	private String message;

	public OneLineTableModel(String colTitle, String msg)
	{
		this.columnTitle = colTitle;
		this.message = msg;
	}

	public void setMessage(String aMessage)
	{
		this.message = aMessage;
	}

	public Object getValueAt(int row, int col)
	{
		return message;
	}

	public void setValueAt(Object aValue, int row, int column)
	{
	}

	public int getColumnCount()
	{
		return 1;
	}

	public int getRowCount()
	{
		return 1;
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
		return this.columnTitle;
	}

	public void removeTableModelListener(TableModelListener l)
	{
	}

}
