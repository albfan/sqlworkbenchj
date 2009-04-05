/*
 * SqlTypeRenderer.java
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import workbench.gui.WbSwingUtilities;
import workbench.resource.GuiSettings;
import workbench.util.SqlUtil;

/**
 * Displays the integer values from java.sql.Types as readable names.
 *
 * @see workbench.util.SqlUtil#getTypeName(int)
 * 
 * @author  support@sql-workbench.net
 */
public class SqlTypeRenderer
	extends DefaultTableCellRenderer
	implements WbRenderer
{
	private Color alternateColor = GuiSettings.getAlternateRowColor();
	private boolean useAlternatingColors = GuiSettings.getUseAlternateRowColor();
	private boolean isPrinting = false;
	private Font printFont;

	public void setUseAlternatingColors(boolean flag)
	{
		this.useAlternatingColors = flag;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			prepareDisplay(value);
			Font oldFont = null;
			if (isPrinting && printFont != null)
			{
				oldFont = getFont();
				this.setFont(printFont);
			}
			if (hasFocus)
			{
				this.setBorder(WbSwingUtilities.FOCUSED_CELL_BORDER);
			}
			else
			{
				this.setBorder(WbSwingUtilities.EMPTY_BORDER);
			}

			if (isSelected && !isPrinting)
			{
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			}
			else
			{
				setForeground(table.getForeground());
				if (useAlternatingColors && ((row % 2) == 1) && !isPrinting)
				{
					setBackground(this.alternateColor);
				}
				else
				{
					setBackground(table.getBackground());
				}
			}
			if (oldFont != null)
			{
				setFont(oldFont);
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

	public void print(Graphics g)
	{
		this.isPrinting = true;
		printFont = g.getFont();
		super.print(g);
		this.isPrinting = false;
	}

	public void prepareDisplay(Object value)
	{
		int type = ((Integer)value).intValue();
		String display = SqlUtil.getTypeName(type);
		this.setText(display);
		this.setToolTipText(display);
	}

}
