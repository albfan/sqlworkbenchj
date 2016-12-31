/*
 * DataStorePrinter.java
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
package workbench.console;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.storage.*;

import workbench.util.StringUtil;

/**
 * A class to print the contents of a {@link DataStore} to a PrintStream.
 *
 * The column widths are optimized against the content of the DataStore
 * if column formatting {@link ConsolePrinter#setFormatColumns(boolean) }
 * is enabled
 *
 * @author Thomas Kellerer
 */
public class DataStorePrinter
	extends ConsolePrinter
{
  private final String longValueSuffix = " (...)";
	private DataStore data;
  private int maxDataLength = Integer.MAX_VALUE;

	public DataStorePrinter(DataStore source)
	{
		super();
		this.data = source;
	}

  public void setMaxDataLength(int maxLength)
  {
    this.maxDataLength = maxLength;
  }

	@Override
	protected String getResultName()
	{
		if (data == null) return null;
		return data.getResultName();
	}

	@Override
	protected int getColumnType(int col)
	{
		return data.getColumnType(col);
	}

	@Override
	protected String getColumnName(int col)
	{
		return data.getColumnName(col);
	}

	@Override
	protected int getColumnCount()
	{
		return data.getColumnCount();
	}

	@Override
	protected Map<Integer, Integer> getColumnSizes()
	{
		if (!doFormat) return null;
		Map<Integer, Integer> widths = new HashMap<>();
		for (int i=0; i < data.getColumnCount(); i++)
		{
			int dataWidth = getMaxDataWidth(i);
			int width = Math.min(dataWidth, maxDataLength);
			widths.put(Integer.valueOf(i), Integer.valueOf(width));
		}
		return widths;
	}

	private int getMaxDataWidth(int col)
	{
		int width = data.getColumnName(col).length();
		for (int row = 0; row < data.getRowCount(); row ++)
		{
			RowData rowData = data.getRow(row);
			String value = getDisplayValue(rowData, col);
			if (value != null)
			{
				int len = value.length();
				if (value.indexOf('\n') > -1)
				{
					String line = StringUtil.getLongestLine(value, 25);
					len = line.length();
				}
				if (len > width) width = len;
			}
		}
		return Math.min(width, maxDataLength);
	}

  @Override
  protected String getDisplayValue(RowData row, int col)
  {
    String value = super.getDisplayValue(row, col);
    return StringUtil.getMaxSubstring(value, maxDataLength - longValueSuffix.length(), longValueSuffix);
  }

  /**
   * Print all rows to the specified stream.
   *
   * @param out    the print stream to use
   *
   * @see #printTo(java.io.PrintWriter)
   * @see #printTo(java.io.PrintWriter, int[])
   */
	public void printTo(PrintStream out)
	{
		PrintWriter pw = new PrintWriter(out);
		printTo(pw, null);
	}

  /**
   * Print all rows to the specified PrintWriter.
   *
   * @param pw    the PrintWriter to use
   * @see #printTo(java.io.PrintWriter, int[])
   */
	public void printTo(PrintWriter pw)
	{
		printTo(pw, null);
	}

  /**
   * Print rows to the specified PrintWriter.
   *
   * @param pw    the PrintWriter to use
   * @param rows  if <b>null</b> all rows are printed<br/>
   *              if <b>not null</b> only the selected rows are printed
   */
	public void printTo(PrintWriter pw, int[] rows)
	{
		int count = rows == null ? data.getRowCount() : rows.length;
		try
		{
			printHeader(pw);
			for (int i=0; i < count; i++)
			{
				int row = rows == null ? i : rows[i];
				RowData rowData = data.getRow(row);
				printRow(pw, rowData, row);
			}
			if (showRowCount)
			{
				pw.println();
				pw.println(ResourceMgr.getFormattedString("MsgRows", count));
			}
			pw.flush();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStorePrinter.printToSelected()", "Error when printing DataStore contents", e);
		}
	}

}
