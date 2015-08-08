/*
 * DataStorePrinter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
 * The column widths are optimized against the content of the DataStore
 * if column formatting {@link ConsolePrinter#setFormatColumns(boolean) }
 * is enabled
 *
 * @author Thomas Kellerer
 */
public class DataStorePrinter
	extends ConsolePrinter
{
	private DataStore data;

	public DataStorePrinter(DataStore source)
	{
		super();
		this.data = source;
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
			int width = Math.min(dataWidth, ConsoleSettings.getMaxColumnDataWidth());
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
		return width;
	}

	public void printTo(PrintStream out)
	{
		PrintWriter pw = new PrintWriter(out);
		printTo(pw, null);
	}

	public void printTo(PrintWriter pw)
	{
		printTo(pw, null);
	}

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
