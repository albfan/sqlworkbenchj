/*
 * TextAreaRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Component;
import java.awt.Insets;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import workbench.gui.WbSwingUtilities;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class TextAreaRenderer
	extends ToolTipRenderer
	implements TableCellRenderer, WbRenderer
{
	protected JTextArea textDisplay;
	
	public TextAreaRenderer()
	{
		super();
		textDisplay = new JTextArea()
		{
			public Insets getInsets()
			{
				return WbSwingUtilities.EMPTY_INSETS;
			}
		};
	}
	
	public int getHorizontalAlignment()
	{
		return SwingConstants.LEFT;
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value,	boolean isSelected,	boolean hasFocus, int row, int col)
	{
		initDisplay(table, value, isSelected, hasFocus, row, col);
		
		this.textDisplay.setFont(table.getFont());
		
		if (hasFocus)
		{
			this.textDisplay.setBorder(WbSwingUtilities.FOCUSED_CELL_BORDER);
		}
		else
		{
			this.textDisplay.setBorder(WbSwingUtilities.EMPTY_BORDER);
		}

		prepareDisplay(value);
		
		this.textDisplay.setBackground(getBackgroundColor());
		this.textDisplay.setForeground(getForegroundColor());
		
		return textDisplay;
	}

	public Insets getInsets()
	{
		return WbSwingUtilities.EMPTY_INSETS;
	}

	public void prepareDisplay(Object value)
	{
		if (value == null)
		{
			this.displayValue = null;
			this.textDisplay.setText("");
			this.textDisplay.setToolTipText(null);
		}
		else
		{
			this.displayValue = value.toString();
			this.textDisplay.setText(this.displayValue);
			this.textDisplay.setToolTipText(StringUtil.getMaxSubstring(this.displayValue, maxTooltipSize));
		}
	}

	/**
	 * This is used by WbTable to calculate the optimal column
	 * width. Assuming that most of the time only the first line
	 * is visible, only that is returned.
	 */ 
	public String getDisplayValue()
	{
		if (displayValue == null) return null;
		int pos = displayValue.indexOf('\n');
		if (pos > 0)
		{
			return displayValue.substring(0, pos);
		}
		return displayValue;
	}
	
}
