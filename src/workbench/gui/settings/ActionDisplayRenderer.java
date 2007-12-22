/*
 * ActionDisplayRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import workbench.gui.renderer.*;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class ActionDisplayRenderer
	extends DefaultTableCellRenderer
	implements WbRenderer
{
	private boolean useAlternateColors = false;
	private Color alternateBackground = null;
	
	public ActionDisplayRenderer()
	{
		super();
		alternateBackground = Settings.getInstance().getAlternateRowColor();
		useAlternateColors = Settings.getInstance().getUseAlternateRowColor();		
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			ActionDisplay d = (ActionDisplay)value;
			this.setToolTipText(d.tooltip);
			boolean isAlternatingRow = this.useAlternateColors && ((row % 2) == 1);
			if (isSelected)
			{
				this.setBackground(table.getSelectionBackground());
				this.setForeground(table.getSelectionForeground());
			}
			else
			{
				this.setForeground(table.getForeground());
				if (isAlternatingRow)
				{
					this.setBackground(alternateBackground);
				}
				else
				{
					this.setBackground(table.getBackground());
				}
			}
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
		this.useAlternateColors = flag;
	}
	
}
