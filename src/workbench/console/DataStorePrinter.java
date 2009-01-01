/*
 * DataStorePrinter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import workbench.storage.*;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 * A class to print the contents of a {@link DataStore} to a PrintStream.
 * The column widths are optimized against the content of the DataStore
 * if column formatting {@link ConsolePrinter#setFormatColumns(boolean) }
 * is enabled
 * 
 * @author support@sql-workbench.net
 */
public class DataStorePrinter
	extends ConsolePrinter
{
	private DataStore data;
	private static final int MAX_WIDTH = 80;
	
	public DataStorePrinter(DataStore source)
	{
		this.data = source;
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
		Map<Integer, Integer> widths = new HashMap<Integer, Integer>();
		for (int i=0; i < data.getColumnCount(); i++)
		{
			int dataWidth = getMaxDataWidth(i);
			int width = Math.min(dataWidth, MAX_WIDTH);
			widths.put(Integer.valueOf(i), Integer.valueOf(width));
		}
		return widths;
	}

	private int getMaxDataWidth(int col)
	{
		int width = data.getColumnName(col).length();
		for (int row = 0; row < data.getRowCount(); row ++)
		{
			String value = data.getValueAsString(row, col);
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
		try
		{
			printHeader(pw);

			int count = data.getRowCount();
			for (int row=0; row < count; row++)
			{
				RowData rowData = data.getRow(row);
				printRow(pw, rowData);
			}
			if (doFormat)
			{
				pw.println(ResourceMgr.getFormattedString("MsgRows", count));
			}
			pw.flush();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConsolePrinter.printTo", "Error when printing DataStore contents", e);
		}
	}

}
