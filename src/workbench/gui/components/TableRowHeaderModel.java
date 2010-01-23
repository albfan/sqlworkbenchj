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

import javax.swing.AbstractListModel;
import javax.swing.JTable;

/**
 *
 * @author Thomas Kellerer
 */
public class TableRowHeaderModel
	extends AbstractListModel
{
	private JTable table;

	public TableRowHeaderModel(JTable table)
	{
		this.table = table;
	}

	public int getSize()
	{
		return table.getRowCount();
	}

	public Object getElementAt(int index)
	{
		return null;
	}

	public void fireModelChanged(int row)
	{
		fireContentsChanged(this, row, row);
	}
}
