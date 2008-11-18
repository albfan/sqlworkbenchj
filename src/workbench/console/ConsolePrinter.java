/*
 * ConsolePrinter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import workbench.storage.*;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import workbench.db.exporter.TextRowDataConverter;
import workbench.log.LogMgr;
import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to print results to the console
 * Concrete classes either print a DataStore or a ResultSet
 * 
 * @author support@sql-workbench.net
 */
public abstract class ConsolePrinter
{
	protected Map<Integer, Integer> columnWidths;
	protected TextRowDataConverter converter = new TextRowDataConverter();

	protected abstract Map<Integer, Integer> getColumnSizes();
	protected abstract int getColumnCount();
	protected abstract String getColumnName(int col);
	protected abstract int getColumnType(int col);
	protected boolean doFormat = true;

	/**
	 * If formatting of columns is enabled, the width of each column
	 * is adjusted to fit the data. How could the optimization is, depends
	 * on the concrete implementation of this class.
	 *
	 * If formatting is disabled, values are printed in the width they need
	 * with control characters escaped to ensure a single-output line per row
	 *
	 * @see DataStorePrinter
	 * @see ResultSetPrinter
	 * @see workbench.util.StringUtil#escapeUnicode(java.lang.String, workbench.util.CharacterRange)
	 *
	 * @param flag
	 */
	public void setFormatColumns(boolean flag)
	{
		doFormat = flag;
	}

	protected void printHeader(PrintWriter pw)
	{
		if (columnWidths == null && doFormat)
		{
			columnWidths = getColumnSizes();
		}

		int headerWidth = 0;
		for (int col = 0; col < getColumnCount(); col ++)
		{
			if (col > 0) pw.print(" | ");
			
			if (doFormat)
			{
				int colWidth = columnWidths.get(Integer.valueOf(col));
				writePadded(pw, getColumnName(col), colWidth);
				headerWidth += colWidth;
			}
			else
			{
				pw.print(getColumnName(col));
			}
			
		}
		pw.println();

		if (headerWidth > 0 && doFormat)
		{
			// Print divider line
			for (int i=0; i < getColumnCount(); i++)
			{
				if (i > 0) pw.print("-+-");
				pw.print(StringUtil.padRight("-", columnWidths.get(Integer.valueOf(i)), '-'));
			}
			pw.println();
		}
	}

	protected void printRow(PrintWriter pw, RowData row)
	{
		int colcount = row.getColumnCount();
		try
		{
			Map<Integer, String[]> continuationLines = new HashMap<Integer, String[]>(colcount);

			for (int col = 0; col < colcount; col ++)
			{
				if (col > 0) pw.print(" | ");

				int type = getColumnType(col);
				String value = "";
				if (SqlUtil.isBlobType(type))
				{
					value = "(BLOB)";
				}
				else if (value != null)
				{
					value = converter.getValueAsFormattedString(row, col);
				}
				if (value == null) value = "";
				
				if (doFormat)
				{
					int colwidth = columnWidths.get(Integer.valueOf(col));
					String[] lines = value.split(StringUtil.REGEX_CRLF);
					writePadded(pw, lines[0], colwidth);
					if (lines.length > 1)
					{
						continuationLines.put(col, lines);
					}
				}
				else
				{
					pw.print(StringUtil.escapeUnicode(value, CharacterRange.RANGE_CONTROL));
				}
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
		while (printed)
		{
			printed = false;
			int currentpos = 0;
			for (int col = 0; col < colcount; col ++)
			{
				String[] lines = lineMap.get(col);
				if (lines == null) continue;
				if (lines.length <= currentLine) continue;
				int colstart = getColStartColumn(col) - currentpos;
				writePadded(pw, "", colstart);
				if (col > 0) 
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
		return;
	}

	private int writePadded(PrintWriter out, String value, int width)
	{
		StringBuffer result = new StringBuffer(width);
		if (value != null) result.append(value);
		
		if (width > 0)
		{
			while (result.length() < width)
			{
				result.append(' ');
			}
		}
		out.print(result.toString());
		return result.length();
	}

}
