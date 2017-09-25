/*
 * RowHeightOptimizer.java
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
package workbench.gui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;

import workbench.util.StringUtil;


/**
 * A class to optimize the height of each table row according to their values.
 *
 * @author Thomas Kellerer
 */
public class RowHeightOptimizer
{
	private WbTable table;

	public RowHeightOptimizer(WbTable client)
	{
		this.table = client;
	}

	protected void notifyRowHeader(final int row)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				TableRowHeader header = TableRowHeader.getRowHeader(table);
				if (header != null)
				{
					header.rowHeightChanged(row);
				}
			}
		});
	}

	public void optimizeAllRows()
	{
		final int count = this.table.getRowCount();
		int maxLines = GuiSettings.getAutRowHeightMaxLines();
		if (count == 0) return;
		boolean ignore = GuiSettings.getIgnoreWhitespaceForAutoRowHeight();
		for (int row = 0; row < count; row ++)
		{
			optimizeRowHeight(row, maxLines, ignore);
		}

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				TableRowHeader header = TableRowHeader.getRowHeader(table);
				if (header != null)
				{
					header.rowHeightChanged();
				}
			}
		});
	}

	private int countWrappedLines(JTextArea edit, int colWidth)
	{
		// Using a LineBreakMeasurer seems to do wrapping differently than JTextArea.
		// The lines "counted" by this method are longer than those that are actually
		// displayed and thus the line count is wrong.
		//
		// But it is a somewhat safe fallback in case countVisibleLines() doesn't work
		// because the TextArea wasn't properly initialized.
		String content = edit.getText();
		if (StringUtil.isEmptyString(content)) return 0;

		int numLines = 0;
		try
		{
			AttributedString text = new AttributedString(content);
			FontRenderContext frc = edit.getFontMetrics(edit.getFont()).getFontRenderContext();

			AttributedCharacterIterator chars = text.getIterator();
			LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(chars, frc);
			lineMeasurer.setPosition(0);

			while (lineMeasurer.getPosition() < chars.getEndIndex())
			{
				lineMeasurer.nextLayout(colWidth);
				numLines++;
			}
		}
		catch (Throwable th)
		{
			LogMgr.logDebug("RowHeightOptimizer.countWrappedLines()", "Error when counting lines", th);
			numLines = 1;
		}
		return numLines;
	}

	public int countVisibleLines(JTextArea edit, FontMetrics fm, int colWidth)
	{
		String content = edit.getText();
		if (StringUtil.isEmptyString(content)) return 0;

		int fontHeight = fm.getHeight();
		int lineCount;
		try
		{
			int height = edit.modelToView(content.length() - 1).y;
			lineCount = height / fontHeight + 1;
		}
		catch (Throwable th)
		{
			// if something goes wrong because the TextArea wasn't initialized properly,
			// fall back to the less accurate method
			LogMgr.logDebug("RowHeightOptimizer.countVisibleLines()", "Error when counting lines", th);
			lineCount = countWrappedLines(edit, colWidth - 32);
		}
		return lineCount;
	}


	private void optimizeRowHeight(int row, int maxLines, boolean ignoreEmptyLines)
	{
		int colCount = this.table.getColumnCount();

		int defaultHeight = table.getRowHeight();
		int optHeight = defaultHeight;

		for (int col = 0; col < colCount; col++)
		{
			TableCellRenderer rend = this.table.getCellRenderer(row, col);

			// The renderer might not be identical to the componenent
			// that is used to draw the actual value (e.g. for TextAreaRenderer)
			// so we need to retrieve the real component in order to be able to get
			// the font that is used to render the text
			Component c = rend.getTableCellRendererComponent(this.table, table.getValueAt(row, col), false, false, row, col);
			Font f = c.getFont();
			FontMetrics fm = c.getFontMetrics(f);

			int lines = 1;

			// Calculating the lines for the value only makes sense
			// when a multi-line renderer is used which is essentially a JTextArea
			if (c instanceof JTextArea)
			{
				JTextArea text = (JTextArea) c;

				int colWidth = table.getColumnModel().getColumn(col).getPreferredWidth();

				// if the size and preferredSize is not set, counting lines doesn't work
				Dimension size = text.getPreferredSize();
				size.setSize(colWidth, size.height);
				text.setSize(size);
				text.setPreferredSize(size);
				text.setMaximumSize(size);

				if (ignoreEmptyLines)
				{
					lines = getLineCount(text);
				}
				else
				{
					lines = text.getLineCount();
					if (lines == 1 && text.getLineWrap())
					{
						lines = countVisibleLines(text, fm, colWidth);
					}
				}
			}

			if (lines > maxLines) lines = maxLines;
			int fheight = (fm == null ? 16 : fm.getHeight());
			int stringHeight = lines * fheight;// + addHeight;

			optHeight = Math.max(optHeight, stringHeight);
		}

		if (optHeight > 0 && optHeight != table.getRowHeight(row))
		{
			table.setRowHeight(row, optHeight);
		}
	}

	private int getLineCount(JTextArea text)
	{
		int lines = text.getLineCount();
		if (lines < 1) return lines;

		int line = lines;

		try
		{
			int len = 0;
			while (line > 0 && len == 0)
			{
				line --;
				int start = text.getLineStartOffset(line);
				int end = text.getLineEndOffset(line);
				String content = text.getText(start, end - start);
				if (StringUtil.isBlank(content))
				{
					len = 0;
				}
				else
				{
					len = 1;
				}
			}
		}
		catch (BadLocationException e)
		{
			LogMgr.logError("RowHeightOptimizer.getLineCount()", "Error when calculating lines", e);
			return lines;
		}
		return line + 1;
	}

}
