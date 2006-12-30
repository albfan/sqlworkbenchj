/*
 * ActionDisplayRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import workbench.gui.renderer.*;

/**
 * @author support@sql-workbench.net
 */
public class ActionDisplayRenderer
	extends DefaultTableCellRenderer
	implements WbRenderer
{
	
	public ActionDisplayRenderer()
	{
		super();
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			ActionDisplay d = (ActionDisplay)value;
			this.setToolTipText(d.tooltip);
			return super.getTableCellRendererComponent(table, d.text, isSelected, hasFocus, row, column);
		}
		catch (Exception e)
		{
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		
	}

	public int getDisplayWidth()
	{
		return getText().length();
	}
	
	public String getDisplayValue()
	{
		return getText();
	}
	
	public void setUseAlternatingColors(boolean flag)
	{
	}
	
}
