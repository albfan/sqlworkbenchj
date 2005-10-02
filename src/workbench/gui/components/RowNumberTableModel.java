/*
 * RowNumberDataModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author support@sql-workbench.net
 */
public class RowNumberTableModel
	extends AbstractTableModel
{
	private JTable referenceTable;
	
	public RowNumberTableModel(JTable reference)
	{
		this.referenceTable = reference;
	}
	
	public int getRowCount()
	{
		return this.referenceTable.getRowCount();
	}
	
	public Class getColumnClass(int columnIndex)
	{
		return Integer.class;
	}
	
	public int getColumnCount()
	{
		return 1;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return new Integer(rowIndex + 1);
	}
	
}
