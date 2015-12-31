/*
 * ConsolePrinter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.console;


import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingConstants;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.exporter.TextRowDataConverter;

import workbench.storage.RowData;

import workbench.util.CharacterRange;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to print results to the console
 * Concrete classes either print a DataStore or a ResultSet
 *
 * @author Thomas Kellerer
 */
public abstract class ConsolePrinter
{
	protected Map<Integer, Integer> columnWidths;
	protected TextRowDataConverter converter = new TextRowDataConverter();
	protected boolean doFormat = true;
	protected boolean showRowCount = true;
	protected boolean printRowAsLine = true;
	protected Set<String> includedColumns;

	protected abstract String getResultName();
	protected abstract Map<Integer, Integer> getColumnSizes();
	protected abstract int getColumnCount();
	protected abstract String getColumnName(int col);
	protected abstract int getColumnType(int col);
  protected String nullString;

	public ConsolePrinter()
	{
		converter.setNullString(ConsoleSettings.getNullString());
		converter.setDefaultNumberFormatter(Settings.getInstance().createDefaultDecimalFormatter());
    nullString = ConsoleSettings.getNullString();
	}

  public void setNullString(String displayValue)
  {
    nullString = displayValue;
  }

	/**
	 * If set to true (the default) one row is printed per console line.
	 * If set to false, one row is displayed as a "form", i.e. one row per column,
	 * rows are divided by a divider line.
	 */
	public void setPrintRowsAsLine(boolean flag)
	{
		printRowAsLine = flag;
	}

	/**
	 * If formatting of columns is enabled, the width of each column
	 * is adjusted to fit the data. How good the optimization is, depends
	 * on the concrete implementation of this class.
	 * <br/>
	 * If formatting is disabled, values are printed in the width they need
	 * with control characters escaped to ensure a single-output line per row
	 *
	 * @see DataStorePrinter
	 * @see ResultSetPrinter
	 * @see workbench.util.StringUtil#escapeText(java.lang.String, workbench.util.CharacterRange, java.lang.String)
	 *
	 * @param flag
	 */
	public void setFormatColumns(boolean flag)
	{
		doFormat = flag;
	}

	public void setPrintRowCount(boolean flag)
	{
		this.showRowCount = flag;
	}

	protected void printHeader(PrintWriter pw)
	{
		if (!printRowAsLine) return;

		if (columnWidths == null && doFormat)
		{
			columnWidths = getColumnSizes();
		}

		String resultName = getResultName();

		if (StringUtil.isNonBlank(resultName))
		{
			pw.println("---- " + resultName);
		}
		int headerWidth = 0;
		int currentCol = 0;
		for (int col = 0; col < getColumnCount(); col ++)
		{
			String colName = getColumnName(col);
			if (!isColumnIncluded(colName)) continue;

			if (currentCol > 0) pw.print(" | ");

			if (doFormat)
			{
				int colWidth = columnWidths.get(Integer.valueOf(col));
				writePadded(pw, colName, colWidth, false);
				headerWidth += colWidth;
			}
			else
			{
				pw.print(colName);
			}
			currentCol ++;
		}
		pw.println();

		if (headerWidth > 0 && doFormat)
		{
			currentCol = 0;
			// Print divider line
			for (int i=0; i < getColumnCount(); i++)
			{
				if (!isColumnIncluded(i)) continue;
				if (i > 0) pw.print("-+-");
				pw.print(StringUtil.padRight("-", columnWidths.get(Integer.valueOf(i)), '-'));
				currentCol ++;
			}
			pw.println();
		}
	}

	public void setColumnsToPrint(Collection<String> columns)
	{
		if (CollectionUtil.isEmpty(columns))
		{
			includedColumns = null;
		}
		includedColumns = CollectionUtil.caseInsensitiveSet();
		includedColumns.addAll(columns);
	}

	protected void printRow(PrintWriter pw, RowData row, int rowNumber)
	{
		if (printRowAsLine)
		{
			printAsLine(pw, row);
		}
		else
		{
			printAsRecord(pw, row, rowNumber);
		}
	}

	protected boolean isColumnIncluded(String colName)
	{
		if (includedColumns == null) return true;
		return includedColumns.contains(colName);
	}

	protected boolean isColumnIncluded(int index)
	{
		return isColumnIncluded(getColumnName(index));
	}

	public void printAsRecord(PrintWriter pw, RowData row, int rowNum)
	{
		int colcount = row.getColumnCount();
		int colwidth = 0;

		pw.println("---- [" + ResourceMgr.getString("TxtRow") + " " + (rowNum + 1) + "] -------------------------------");

		// Calculate max. colname width
		for (int col=0; col < colcount; col++)
		{
			String colname = getColumnName(col);
			if (colname.length() > colwidth) colwidth = colname.length();
		}

		for (int col=0; col < colcount; col++)
		{
			String colname = getColumnName(col);
			if (!isColumnIncluded(colname)) continue;

			String value = getDisplayValue(row, col);
			writePadded(pw, colname, colwidth + 1, false);
			pw.print(": ");
			if (doFormat)
			{
				String[] lines = value.split(StringUtil.REGEX_CRLF);
				pw.println(lines[0]);
				if (lines.length > 1)
				{
					for (int i=1; i < lines.length; i++)
					{
						writePadded(pw, " ", colwidth + 3, false);
						pw.println(lines[i]);
					}
				}
			}
			else
			{
				pw.println(value);
			}
		}

	}

	protected String getDisplayValue(RowData row, int col)
	{
		int type = getColumnType(col);
		String value = null;

		if (SqlUtil.isBlobType(type))
		{
			Object data = row.getValue(col);

			// In case the BLOB data was converted to a string by a DataConverter
			if (data instanceof String)
			{
				value = (String)row.getValue(col);
			}
			else if (data != null)
			{
				value = "(BLOB)";
			}
		}
		else
		{
			value = converter.getValueAsFormattedString(row, col);
		}

		if (value == null)
		{
			value = nullString;
		}

		return value;
	}

	protected void printAsLine(PrintWriter pw, RowData row)
	{
		int colcount = row.getColumnCount();
		try
		{
			Map<Integer, String[]> continuationLines = new HashMap<>(colcount);

			int realColCount = 0;
			for (int col = 0; col < colcount; col ++)
			{
				if (!isColumnIncluded(col)) continue;
				if (realColCount > 0) pw.print(" | ");

				String value = getDisplayValue(row, col);

				if (doFormat)
				{
					int colwidth = columnWidths.get(Integer.valueOf(col));
					String[] lines = value.split(StringUtil.REGEX_CRLF);
					if (lines.length == 0)
					{
						// the value only contained newlines --> treat as an empty string (thus a single line)
						writePadded(pw, value.trim(), colwidth, alignRight(col));
					}
					else
					{
						writePadded(pw, lines[0], colwidth, alignRight(col));
						if (lines.length > 1)
						{
							continuationLines.put(col, lines);
						}
					}
				}
				else
				{
					pw.print(StringUtil.escapeText(value, CharacterRange.RANGE_CONTROL));
				}
				realColCount ++;
			}
			pw.println();
			printContinuationLines(pw, continuationLines);

			pw.flush();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConsolePrinter.printRow", "Error when printing DataStore contents", e);
		}
	}

	private boolean alignRight(int col)
	{
		if (GuiSettings.getNumberDataAlignment() == SwingConstants.LEFT) return false;
		int type = getColumnType(col);
		return SqlUtil.isNumberType(type);
	}

	private int getColStartColumn(int col)
	{
		if (columnWidths == null) return 0;
		int colstart = 0;
		for (int i=0; i < col; i++)
		{
			colstart += columnWidths.get(i);
			if (i > 0) colstart += 3;
		}
		return colstart;
	}

	private void printContinuationLines(PrintWriter pw, Map<Integer, String[]> lineMap)
	{
		boolean printed = true;
		int colcount = getColumnCount();
		int currentLine = 1; // line 0 has already been printed
		int printedColNr = 0;
		while (printed)
		{
			printed = false;
			int currentpos = 0;
			for (int col = 0; col < colcount; col ++)
			{
				if (!isColumnIncluded(col)) continue;
				String[] lines = lineMap.get(col);
				printedColNr ++;

				if (lines == null) continue;
				if (lines.length <= currentLine) continue;

				int colstart = getColStartColumn(col) - currentpos;
				writePadded(pw, "", colstart, false);
				if (printedColNr > 1)
				{
					pw.print(" : ");
				}
				pw.print(lines[currentLine]);
				currentpos = colstart + lines[currentLine].length() + (col * 3);
				printed = true;
			}
			currentLine++;
			if (printed) pw.println();
		}
	}

	private int writePadded(PrintWriter out, String value, int width, boolean rightAligned)
	{
		StringBuilder result = new StringBuilder(width);
		if (value != null) result.append(value);

		if (width > 0)
		{
			if (result.length() < width)
			{
				int delta = width - result.length();
				StringBuilder pad = new StringBuilder(delta);
				for (int i=0; i < delta; i++)
				{
					pad.append(' ');
				}
				if (rightAligned)
				{
					result.insert(0, pad);
				}
				else
				{
					result.append(pad);
				}
			}
		}
		out.print(result.toString());
		return result.length();
	}

}
