/*
 * ToolTipRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.RowHighlighter;
import workbench.gui.components.WbTable;

import workbench.util.StringUtil;

/**
 * A renderer that automatically displays the value as a tooltip.
 * It also handles the highlighting of null values during display
 * and non-null columns in editing mode.
 * <br/>
 * It can also highlight values based on a ColumnExpression that is
 * provided by WbTable.
 * <br/>
 * For performance reasons the displayValue is drawn directly using the graphics
 * object.
 *
 * @author Thomas Kellerer
 */
public class ToolTipRenderer
	extends JComponent
	implements TableCellRenderer, WbRenderer, RequiredFieldHighlighter
{
	protected String displayValue = StringUtil.EMPTY_STRING;
	protected Object currentValue;
	protected String tooltip;

	protected int rightMargin;
	protected Color selectedForeground;
	protected Color selectedBackground;
	protected Color unselectedForeground;
	protected Color unselectedBackground;
	protected Color highlightBackground;
	protected Color filterHighlightColor = GuiSettings.getExpressionHighlightColor();

	protected RendererSetup rendererSetup;

	protected int maxTooltipSize = Settings.getInstance().getIntProperty("workbench.gui.renderer.maxtooltipsize", 1000);

	private static final int DEFAULT_BLEND = 0;
	protected int selectionBlendFactor = DEFAULT_BLEND;
	protected int alternateBlendFactor = DEFAULT_BLEND;

	protected int editingRow = -1;
	private boolean isEditing;
	private boolean[] highlightCols;
	private int currentColumn = -1;
	private int currentRow = -1;
	private String currentColumnName;

	private Rectangle paintIconR = new Rectangle();
	private Rectangle paintTextR = new Rectangle();
	private Rectangle paintViewR = new Rectangle();

	protected Insets focusedInsets;
	protected Insets regularInsets;

	protected boolean isSelected;
	protected boolean hasFocus;
	protected boolean isNull;

	protected RowHighlighter filter;

	private int valign = SwingConstants.TOP;
	private int halign = SwingConstants.LEFT;

	private boolean isAlternatingRow;
	private boolean isModifiedColumn;

	protected boolean showTooltip = true;
	protected Map renderingHints;

	public ToolTipRenderer()
	{
		super();
		int thick = WbSwingUtilities.FOCUSED_CELL_BORDER.getThickness();
		focusedInsets = new Insets(thick, thick, thick, thick);
		regularInsets = getDefaultInsets();
		Toolkit tk = Toolkit.getDefaultToolkit();
		renderingHints = (Map) tk.getDesktopProperty("awt.font.desktophints");
		showTooltip = Settings.getInstance().getBoolProperty("workbench.gui.renderer.showtooltip", true);
		selectionBlendFactor = retrieveBlendFactor("selection");
		alternateBlendFactor = retrieveBlendFactor("alternate");
	}

	private int retrieveBlendFactor(String type)
	{
		int value = Settings.getInstance().getIntProperty("workbench.gui.renderer.blend." + type, DEFAULT_BLEND);
		if (value < 0 || value > 256)
		{
			value = DEFAULT_BLEND;
		}
		return value;
	}

	static Insets getDefaultInsets()
	{
		Insets result = null;

		String prop = Settings.getInstance().getProperty("workbench.gui.renderer.insets", null);
		List<String> values = prop != null ? StringUtil.stringToList(prop, ",", true, true, false) : null;

		if (values != null && values.size() == 4)
		{
			try
			{
				int top = Integer.valueOf(values.get(0));
				int left = Integer.valueOf(values.get(1));
				int bottom = Integer.valueOf(values.get(2));
				int right = Integer.valueOf(values.get(3));
				result = new Insets(top,left,bottom,right);
			}
			catch (Exception e)
			{
				LogMgr.logError("ToolTipRenderer.getDefaultInsets()", "Error reading default insets from settings: " + prop, e);
				result = null;
			}
		}
		if (result == null)
		{
			result = new Insets(1,1,1,1);
		}
		return result;
	}

	@Override
	public void setEditingRow(int row)
	{
		this.editingRow = row;
	}

	@Override
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

	@Override
	public int getHorizontalAlignment()
	{
		return this.halign;
	}

	@Override
	public void setHighlightBackground(Color c)
	{
		this.highlightBackground = c;
	}

	private boolean doModificationHighlight(WbTable table, int row, int col)
	{
		if (this.rendererSetup.modifiedColor == null) return false;
		if (table == null) return false;
		DataStoreTableModel model = table.getDataStoreTableModel();
		if (model == null) return false;

		return model.isColumnModified(row, col);
	}

	protected void initDisplay(JTable table, Object value,	boolean selected,	boolean focus, int row, int col)
	{
		this.isNull = (value == null);
		this.hasFocus = focus;
		this.isEditing = (row == this.editingRow) && (this.highlightBackground != null);
		this.currentColumn = col;
		this.currentColumnName = table.getColumnName(col);
		this.currentRow = row;
		this.isSelected = selected;
		this.currentValue = value;

		try
		{
			WbTable wbtable = (WbTable)table;
			this.rendererSetup = wbtable.getRendererSetup();
			this.filter = wbtable.getHighlightExpression();
			this.isModifiedColumn = doModificationHighlight(wbtable, row, col);
		}
		catch (ClassCastException cce)
		{
			// ignore, should not happen
		}

		this.isAlternatingRow = rendererSetup.useAlternatingColors && ((row % 2) == 1);

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
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,	boolean selected,	boolean focus, int row, int col)
	{
		initDisplay(table, value, selected, focus, row, col);

    Font f = table.getFont();

		if (value != null)
		{
      setFont(f);
			prepareDisplay(value);
		}
		else
		{
      if (rendererSetup.nullFontStyle > 0 && rendererSetup.nullString != null)
      {
        f = f.deriveFont(rendererSetup.nullFontStyle);
      }
      setFont(f);
			displayValue = rendererSetup.nullString;
			setTooltip(null);
		}

		return this;
	}

	@Override
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
		if (col < 0 || col >= this.highlightCols.length) return false;
		return this.highlightCols[col];
	}

	protected Color getBackgroundColor()
	{
		Color c = getColumnHighlightColor(currentRow);
		if (isSelected)
		{
			return ColorUtils.blend(selectedBackground, c, selectionBlendFactor);
		}
		else if (isAlternatingRow)
		{
			return ColorUtils.blend(rendererSetup.alternateBackground, c, alternateBlendFactor);
		}
		if (c != null) return c;
		return unselectedBackground;
	}

	protected Color getColumnHighlightColor(int row)
	{
		if (isEditing)
		{
			if (isHighlightColumn(currentColumn))
			{
				return highlightBackground;
			}
			else
			{
				return unselectedBackground;
			}
		}

		if (checkHighlightExpression(row, currentValue))
		{
			return filterHighlightColor;
		}
		if (isModifiedColumn)
		{
			return rendererSetup.modifiedColor; // might be null which is OK
		}
		else if (displayValue == null || isNull)
		{
			return rendererSetup.nullColor; // might be null which is OK
		}
		// null means "default" color
		return null;
	}

	@Override
	public void paint(Graphics g)
	{
		int w = this.getWidth();
		int h = this.getHeight();

    Font f = getFont();
		FontMetrics fm = getFontMetrics(f);

		Insets insets;

		if (hasFocus)
		{
			insets = focusedInsets;
		}
		else
		{
			insets = regularInsets;
		}

		paintViewR.x = insets.left;
		paintViewR.y = insets.top;
		paintViewR.width = w - (insets.left + insets.right);
		paintViewR.height = h - (insets.top + insets.bottom);

		paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
		paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

		String clippedText = StringUtil.EMPTY_STRING;
		if (displayValue != null)
		{
			clippedText = SwingUtilities.layoutCompoundLabel(this, fm,
				this.displayValue, (Icon)null, this.valign, this.halign,
				SwingConstants.TOP, SwingConstants.RIGHT, paintViewR, paintIconR, paintTextR, 0);
		}

		int textX = paintTextR.x;
		textX -= rightMargin;
		if (textX < 0) textX = 0;
		int textY = paintTextR.y + fm.getAscent();
		if (textY < 0) textY = 0;

		Graphics2D g2d = (Graphics2D) g;
		if (renderingHints != null)
		{
			g2d.addRenderingHints(renderingHints);
		}

    g.setFont(f);
		g.setColor(getBackgroundColor());
		g.fillRect(0,0,w,h);
		g.setColor(getForegroundColor());
		g.drawString(clippedText, textX, textY);

		if (hasFocus)
		{
			WbSwingUtilities.FOCUSED_CELL_BORDER.paintBorder(this, g, 0, 0, w, h);
		}
	}

	@Override
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
	{
	}

	@Override
	public boolean isOpaque()
	{
		return true;
	}

	@Override
	public void prepareDisplay(Object value)
	{
		if (value == null)
		{
			displayValue = null;
		}
		else
		{
			displayValue = value.toString();
		}
		setTooltip(displayValue);
	}

	protected boolean checkHighlightExpression(int row, Object value)
	{
		if (this.filter == null)
		{
			return false;
		}
		return filter.hightlightColumn(row, currentColumnName, currentValue);
	}

	@Override
	public String getToolTipText()
	{
		return this.tooltip;
	}

	protected void setTooltip(String tip)
	{
		if (showTooltip && tip != null && tip.length() > 0)
		{
			tooltip = StringUtil.getMaxSubstring(tip, maxTooltipSize);
		}
		else
		{
			tooltip = null;
		}
	}

	@Override
	public String getDisplayValue()
	{
		return displayValue;
	}

}
