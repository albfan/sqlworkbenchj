/*
 * ProcStatusRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import workbench.db.JdbcProcedureReader;

/**
 * Displays the return type of a stored procedure as a readable text.
 * <br/>
 * @see workbench.db.JdbcProcedureReader#convertProcType(int) 
 * @author  support@sql-workbench.net
 */
public class ProcStatusRenderer
	extends DefaultTableCellRenderer
	implements WbRenderer
{
	
	public ProcStatusRenderer()
	{
		super();
		this.setHorizontalAlignment(JLabel.LEFT);
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			Integer status = (Integer)value;
			String display = JdbcProcedureReader.convertProcType(status.intValue());
			return super.getTableCellRendererComponent(table, display, isSelected, hasFocus, row, column);
		}
		catch (Exception e)
		{
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}

	}

	public void setUseAlternatingColors(boolean flag)
	{
		// not implemented
	}

	public String getDisplayValue()
	{
		return getText();
	}

	public void prepareDisplay(Object value)
	{
		// nothing to do
	}

}
