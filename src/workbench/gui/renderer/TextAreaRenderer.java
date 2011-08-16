/*
 * TextAreaRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * A renderer to display multi-line character data.
 * <br/>
 * The renderer uses a JTextArea internally which is a lot slower than the own
 * drawing of the text implemented in ToolTipRender. But ToolTipRenderer
 * cannot cope with line breaks
 *
 * @author Thomas Kellerer
 */
public class TextAreaRenderer
	extends ToolTipRenderer
	implements TableCellRenderer, WbRenderer
{
	public static final Insets AREA_INSETS = new Insets(1,0,0,0);
	protected JTextArea textDisplay;

	public TextAreaRenderer()
	{
		super();
		textDisplay = new JTextArea()
		{
			@Override
			public Insets getInsets()
			{
				return AREA_INSETS;
			}

			@Override
			public Insets getMargin()
			{
				return WbSwingUtilities.EMPTY_INSETS;
			}

		};

		textDisplay.setWrapStyleWord(false);
		textDisplay.setLineWrap(false);
		textDisplay.setAutoscrolls(false);
		textDisplay.setTabSize(Settings.getInstance().getEditorTabWidth());
		textDisplay.setBorder(WbSwingUtilities.EMPTY_BORDER);
	}


	@Override
	public int getHorizontalAlignment()
	{
		return SwingConstants.LEFT;
	}

	@Override
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

	@Override
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
			try
			{
				this.displayValue = (String)value;
			}
			catch (ClassCastException cce)
			{
				this.displayValue = value.toString();
			}
			this.textDisplay.setText(this.displayValue);
			if (showTooltip)
			{
				this.textDisplay.setToolTipText(StringUtil.getMaxSubstring(this.displayValue, maxTooltipSize));
			}
		}
	}

}
