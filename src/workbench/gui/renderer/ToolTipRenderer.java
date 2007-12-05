/*
 * ToolTipRenderer.java
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import workbench.gui.WbSwingUtilities;
import workbench.resource.Settings;
import workbench.storage.filter.ColumnExpression;
import workbench.util.StringUtil;

/**
 * Displays a string in a table cell and shows a tool
 * tip if the string is too long to fit in the cell.
 */
public class ToolTipRenderer
	extends JComponent
	implements TableCellRenderer, WbRenderer, RequiredFieldHighlighter, FilterHighlighter
{
	protected String displayValue = StringUtil.EMPTY_STRING;
	protected String tooltip = null;

	protected Color selectedForeground;
	protected Color selectedBackground;
	protected Color unselectedForeground;
	protected Color unselectedBackground;
	protected Color highlightBackground;
	protected Color filterHighlightColor = Color.YELLOW;
	
	private Color alternateBackground = Settings.getInstance().getAlternateRowColor();
	private boolean useAlternatingColors = Settings.getInstance().getUseAlternateRowColor();
	private Color nullColor = Settings.getInstance().getNullColor();

	protected int maxTooltipSize = Settings.getInstance().getIntProperty("workbench.gui.renderer.maxtooltipsize", 1000);
	protected int editingRow = -1;
	private boolean isEditing = false;
	private boolean[] highlightCols;
	private int currentColumn = -1;
	
	private Rectangle paintIconR = new Rectangle();
	private Rectangle paintTextR = new Rectangle();
	private Rectangle paintViewR = new Rectangle();

	private boolean isPrinting = false;
	
	private Insets focusedInsets;

	protected boolean isSelected;
	protected boolean hasFocus;
	
	protected boolean filterMatches;
	protected ColumnExpression filter;
	
	private int valign = SwingConstants.TOP; 
	private int halign = SwingConstants.LEFT;
	
	private boolean isAlternatingRow = false;
	
	public ToolTipRenderer()
	{
		int thick = WbSwingUtilities.FOCUSED_CELL_BORDER.getThickness();
		focusedInsets = new Insets(thick, thick, thick, thick);
	}

	public void setUseAlternatingColors(boolean flag)
	{
		this.useAlternatingColors = flag;
	}
	
	public void setEditingRow(int row) 
	{ 
		this.editingRow = row; 
	}
	
	public void setHighlightColumns(boolean[] cols) 
	{ 
		this.highlightCols = cols; 
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

	public void setHighlightBackground(Color c)
	{
		this.highlightBackground = c;
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value,	boolean selected,	boolean focus, int row, int col)
	{
		this.hasFocus = focus;
		this.isEditing = (row == this.editingRow) && (this.highlightBackground != null);
		this.currentColumn = col;
		this.setFont(table.getFont());
		
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

		this.isSelected = selected;
		this.isAlternatingRow = this.useAlternatingColors && ((row % 2) == 1);
		
		filterMatches = false;
		
		if (value == null)
		{
			displayValue = null;
			tooltip = null;
		}
		else
		{
			this.prepareDisplay(value);
			if (this.filter != null)
			{
				filterMatches = filter.evaluate(displayValue != null ? displayValue : value.toString());
			}
		}
		
		return this;
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		FontMetrics fm = getFontMetrics(getFont());
		
		d.setSize(d.getWidth(), fm.getHeight());
		return d;
	}
	
	protected Color getForegroundColor()
	{
		if (isSelected) 
		{
			return selectedForeground;
		}
		
		return unselectedForeground;
	}
	
	private boolean isHighlightColumn(int col)
	{
		if (this.highlightCols == null) return false;
		if (col < 0 || col > this.highlightCols.length) return false;
		return this.highlightCols[col];
	}
	
	protected Color getBackgroundColor(boolean isNull)
	{
		if (isPrinting)
		{
			return unselectedBackground;
		}
		
		if (isEditing)
		{
			if (isHighlightColumn(currentColumn))
			{
				return this.highlightBackground;
			}
			else
			{
				return unselectedBackground;
			}
		}
		
		if (isSelected)
		{
			return selectedBackground;
		}

		if (filterMatches && filterHighlightColor != null)
		{
			return filterHighlightColor;
		}
		
		if (isNull && nullColor != null)
		{
			return nullColor;
		}
		else
		{
			if (isAlternatingRow)
			{
				return alternateBackground;
			}
			else
			{
				return unselectedBackground;
			}
		}
	}
	
	public void paint(Graphics g)
	{
		int w = this.getWidth();
		int h = this.getHeight();
		
		FontMetrics fm = getFontMetrics(getFont());

		Insets insets;
		
		if (hasFocus)
		{
			insets = focusedInsets;
		}
		else
		{
			insets = WbSwingUtilities.EMPTY_INSETS;
		}
			
		paintViewR.x = insets.left;
		paintViewR.y = insets.top;
		paintViewR.width = w - (insets.left + insets.right);
		paintViewR.height = h - (insets.top + insets.bottom);
		
		paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
		paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;
		
		boolean isNull = (displayValue == null);
		
		String clippedText = StringUtil.EMPTY_STRING;
		if (!isNull)
		{
			clippedText = 
        SwingUtilities.layoutCompoundLabel(this,fm,this.displayValue,(Icon)null
						,this.valign
						,this.halign
						,SwingConstants.TOP
						,SwingConstants.RIGHT
						,paintViewR, paintIconR, paintTextR, 0);
		}
	
		int textX = paintTextR.x;
		if (textX < 0) textX = 0;
		int textY = paintTextR.y + fm.getAscent();
		if (textY < 0) textY = 0;

		g.setColor(getBackgroundColor(isNull));
		g.fillRect(0,0,w,h);
		g.setColor(getForegroundColor());
		g.drawString(clippedText, textX, textY);

		if (hasFocus) 
		{
			WbSwingUtilities.FOCUSED_CELL_BORDER.paintBorder(this, g, 0, 0, w, h);
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
		displayValue = aValue.toString();
		setTooltip(displayValue);
	}

	public String getToolTipText()
	{
		return this.tooltip;
	}
	
	protected void setTooltip(String tip)
	{
		if (tip != null && tip.length() > 0)
			tooltip = StringUtil.getMaxSubstring(tip, maxTooltipSize);
		else 
			tooltip = null;
	}
	
	public String getDisplayValue() 
	{ 
		return displayValue; 
	}
	
	public void setFilterHighlightColor(Color highlight)
	{
		this.filterHighlightColor = highlight;
	}
	
	public void setFilterHighlighter(ColumnExpression columnFilter)
	{
		this.filter = columnFilter;
	}
	
	public void clearFilterHighligher()
	{
		this.filter = null;
	}
}
