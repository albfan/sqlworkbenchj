/*
 * TextAreaRenderer.java
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
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;
import workbench.gui.WbSwingUtilities;
import workbench.resource.Settings;
import workbench.storage.NullValue;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class TextAreaRenderer
	extends JTextArea
	implements TableCellRenderer, WbRenderer
{
	protected Color selectedForeground;
	protected Color selectedBackground;
	protected Color unselectedForeground;
	protected Color unselectedBackground;
	protected Color highlightBackground;
	
	private Color alternateBackground = Settings.getInstance().getAlternateRowColor();
	private boolean useAlternatingColors = Settings.getInstance().getUseAlternateRowColor();
	protected int maxTooltipSize = Settings.getInstance().getIntProperty("workbench.gui.renderer.maxtooltipsize", 1000);

	protected int editingRow = -1;
	private boolean isEditing = false;
	private boolean[] highlightCols;

	private boolean isPrinting = false;
	
	private boolean isAlternatingRow = false;
	
	public TextAreaRenderer()
	{
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value,	boolean isSelected,	boolean hasFocus, int row, int col)
	{
		this.isEditing = (row == this.editingRow) && (this.highlightBackground != null);
		
		this.setFont(table.getFont());
		
		if (hasFocus)
		{
			this.setBorder(WbSwingUtilities.FOCUSED_CELL_BORDER);
		}
		else
		{
			this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		}
		
		if (selectedForeground == null)
		{
			selectedForeground = table.getSelectionForeground();
			selectedBackground = table.getSelectionBackground();
		}
		
		if (unselectedForeground == null)
		{
			unselectedForeground = table.getForeground();
			unselectedBackground = table.getBackground();
		}
		
		this.isAlternatingRow = this.useAlternatingColors && ((row % 2) == 1);
		if (!this.isEditing)
		{
			if (isSelected)
			{
				setBackground(selectedBackground);
				setForeground(selectedForeground);
			}
			else 
			{
				setForeground(unselectedForeground);
				if (isAlternatingRow && !isPrinting)
				{
					setBackground(alternateBackground);
				}
				else
				{
					setBackground(unselectedBackground);
				}		
			}
		}
		else
		{
			try
			{
				if (this.highlightCols[col])
				{
					setBackground(this.highlightBackground);
				}
				else
				{
					setBackground(unselectedBackground);
				}
			}
			catch (Throwable th)
			{
				setBackground(unselectedBackground);
			}
		}
		
		boolean isNull = (value == null) || (value instanceof NullValue);
		
		if (isNull)
		{
			this.setText("");
			this.setToolTipText(null);
		}
		else
		{
			String s = (String)value;
			this.setText(s);
			this.setToolTipText(StringUtil.getMaxSubstring(s, maxTooltipSize));
		}
		return this;
	}

	public Insets getInsets()
	{
		return WbSwingUtilities.EMPTY_INSETS;
	}
	
	public void setHighlightColumns(boolean[] cols) 
	{ 
		this.highlightCols = cols; 
	}	
	
	public void setHighlightBackground(Color c)
	{
		this.highlightBackground = c;
	}
	
	public void print(Graphics g)
	{
		this.isPrinting = true;
		super.print(g);
		this.isPrinting = false;
	}
	
	public void setEditingRow(int row) 
	{ 
		this.editingRow = row; 
	}

	/**
	 * This is used by WbTable to calculate the optimal column
	 * width. Assuming that most of the time only the first line
	 * is visible, only that is returned.
	 */ 
	public String getDisplayValue()
	{
		String s = getText();
		if (s == null) return s;
		int pos = s.indexOf('\n');
		if (pos > 0)
		{
			return s.substring(0, pos);
		}
		return s;
	}
	
	public void setUseAlternatingColors(boolean flag)
	{
		this.useAlternatingColors = flag;
	}
	
}
