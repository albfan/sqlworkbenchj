/*
 * ToolTipRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import workbench.gui.components.WbTable;
import workbench.util.StringUtil;

/**
 * Displays a string in a table cell and shows a tool
 * tip if the string is too long to fit in the cell.
 */
public class ToolTipRenderer
	extends JComponent
	implements TableCellRenderer
{
	protected String displayValue = StringUtil.EMPTY_STRING;
	protected String tooltip = null;

	private Color selectedForeground;
	private Color selectedBackground;
	private Color unselectedForeground;
	private Color unselectedBackground;

	private Rectangle paintIconR = new Rectangle();
	private Rectangle paintTextR = new Rectangle();
	private Rectangle paintViewR = new Rectangle();
	private static Insets paintViewInsets = new Insets(0, 0, 0, 0);
	private static Insets emptyInsets = new Insets(0, 0, 0, 0);

	private boolean isPrinting = false;
	
	public static final ToolTipRenderer DEFAULT_TEXT_RENDERER = new ToolTipRenderer();
	
	private static Insets focusedInsets;
	static
	{
		int thick = WbTable.FOCUSED_CELL_BORDER.getThickness();
		focusedInsets = new Insets(thick, thick, thick, thick);
	}

	private boolean selected;
	private boolean focus;
	private int valign = SwingConstants.TOP; 
	private int halign = SwingConstants.LEFT;
	
	public static final String[] EMPTY_DISPLAY = new String[] { StringUtil.EMPTY_STRING, null };
	
	public boolean debug = false;
	
	public ToolTipRenderer()
	{
	}
	
	public void setVerticalAlignment(int align)
	{
		this.valign = align;
	}
	
	public void setHorizontalAlignment(int align)
	{
		this.halign = align;
	}
	public int getHorizontalAlignment()
	{
		return this.halign;
	}

	public Component getTableCellRendererComponent(	JTable table,
																									Object value,
																									boolean isSelected,
																									boolean hasFocus,
																									int row,
																									int col)
	{
		this.focus = hasFocus;
		if (isSelected)
		{
			if (selectedForeground == null)
			{
				selectedForeground = table.getSelectionForeground();
				selectedBackground = table.getSelectionBackground();
			}
		}
		else
		{
			if (selectedForeground == null)
			{
				unselectedForeground = table.getForeground();
				unselectedBackground = table.getBackground();
			}
		}
		this.selected = isSelected;
		
		if (value != null)
		{
			this.prepareDisplay(value);
			this.setToolTipText(this.tooltip);
		}
		else
		{
			displayValue = StringUtil.EMPTY_STRING;
			tooltip = null;
		}
		return this;
	}
	
	public void paint(Graphics g)
	{
		int w = this.getWidth();
		int h = this.getHeight();
		
		FontMetrics fm = g.getFontMetrics();

		Insets insets;
		
		if (focus)
		{
			insets = focusedInsets;
		}
		else
		{
			insets = emptyInsets;
		}
			
		paintViewR.x = insets.left;
		paintViewR.y = insets.top;
		paintViewR.width = w - (insets.left + insets.right);
		paintViewR.height = h - (insets.top + insets.bottom);
		
		
		paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
		paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;
		
		Icon ic = null;
		
		String clippedText = 
        SwingUtilities.layoutCompoundLabel(this,fm,this.displayValue,ic
						,this.valign
						,this.halign
						,SwingConstants.TOP
						,SwingConstants.RIGHT
						,paintViewR, paintIconR, paintTextR, 0);
		
		int textX = paintTextR.x;
		if (textX < 0) textX = 0;
		int textY = paintTextR.y + fm.getAscent();
		if (textY < 0) textY = 0;

		if (this.selected)
		{
			g.setColor(selectedBackground);
			g.fillRect(0,0, w, h);
			g.setColor(selectedForeground);
		}
		else 
		{
			g.setColor(unselectedBackground);
			g.fillRect(0,0, w, h);
			g.setColor(unselectedForeground);
		}
		g.drawString(clippedText, textX, textY);

		if (focus) 
		{
			WbTable.FOCUSED_CELL_BORDER.paintBorder(this, g, 0, 0, w, h);
		}
	}

	public void print(Graphics g)
	{
		this.isPrinting = true;
		super.print(g);
		this.isPrinting = false;
	}
	
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
  public boolean isOpaque() { return true; }
	
	public void prepareDisplay(Object aValue)
	{
		// this method will not be called with a null value, so we do not need
		// to check it here!
		displayValue = aValue.toString();
		// this is the tooltip
		tooltip = null;
		if (displayValue.length() > 0) tooltip = displayValue;
	}

	public String getDisplayValue() { return displayValue; }
}
