/*
 * RowNumberTableModel.java
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

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import workbench.resource.ResourceMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class RowNumberTableModel
	extends AbstractTableModel
{
	private JTable referenceTable;
	private String label;
	
	public RowNumberTableModel(JTable reference)
	{
		this.referenceTable = reference;
		label = ResourceMgr.getString("LblRowColumn");
	}
	
	public int getRowCount()
	{
		return this.referenceTable.getRowCount();
	}
	
	public Class getColumnClass(int columnIndex)
	{
		return Integer.class;
	}
	
	public String getColumnName(int column)
	{
		return label;
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
