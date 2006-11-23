/*
 * SqlTypeRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import workbench.gui.WbSwingUtilities;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 * @author  support@sql-workbench.net
 */
public class SqlTypeRenderer 
	extends DefaultTableCellRenderer
	implements WbRenderer
{
	private Color selectedForeground;
	private Color selectedBackground;
	private Color unselectedForeground;
	private Color unselectedBackground;
	
	private Color alternateColor = Settings.getInstance().getAlternateRowColor();
	private boolean useAlternatingColors = Settings.getInstance().getUseAlternateRowColor();
	
	public SqlTypeRenderer()
	{
	}
	
	public void setUseAlternatingColors(boolean flag)
	{
		this.useAlternatingColors = flag;
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			int type = ((Integer)value).intValue();
			String display = SqlUtil.getTypeName(type);
			this.setText(display);
			this.setToolTipText(display);
			
			if (hasFocus)
			{
				this.setBorder(WbSwingUtilities.FOCUSED_CELL_BORDER);
			}
			else
			{
				this.setBorder(WbSwingUtilities.EMPTY_BORDER);
			}

			if (isSelected)
			{
				if (selectedForeground == null)
				{
					this.selectedForeground = table.getSelectionForeground();
					this.selectedBackground = table.getSelectionBackground();
				}
				super.setForeground(this.selectedForeground);
				super.setBackground(this.selectedBackground);
			}
			else
			{
				if (selectedForeground == null)
				{
					this.unselectedForeground = table.getForeground();
					this.unselectedBackground = table.getBackground();
				}
				super.setForeground(this.unselectedForeground);
				if (useAlternatingColors && ((row % 2) == 1))
				{
					super.setBackground(this.alternateColor);
				}
				else
				{
					super.setBackground(this.unselectedBackground);
				}
			}
			
		}
		catch (Exception e)
		{
		}
		return this;
	}

	public String getDisplayValue()
	{
		return getText();
	}
	
}
