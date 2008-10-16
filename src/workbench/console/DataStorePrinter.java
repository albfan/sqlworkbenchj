/*
 * DataPrinter.java
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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.util.CharacterRange;
import workbench.util.StringUtil;

/**
 * A class to print the contents of a {@link DataStore} to a PrintStream
 * 
 * @author support@sql-workbench.net
 */
public class DataStorePrinter
{
	private DataStore data;
	private Map<Integer, Integer> columnWidths;
	private static final int MAX_WIDTH = 80;
	private int lineLength = 0;
	
	public DataStorePrinter(DataStore source)
	{
		this.data = source;
		checkColumnSizes();
	}

	private void checkColumnSizes()
	{
		columnWidths = new HashMap<Integer, Integer>();
		for (int i=0; i < data.getColumnCount(); i++)
		{
			int dataWidth = getMaxDataWidth(i);
			int width = Math.min(dataWidth, MAX_WIDTH);
			columnWidths.put(Integer.valueOf(i), Integer.valueOf(width));
			lineLength += width;
		}
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
				if (len > width) width = len;
			}
		}
		return width;
	}
	
	public void printTo(PrintStream out)
	{
		PrintWriter pw = new PrintWriter(out);
		int colcount = data.getColumnCount();
		try
		{
			StringBuffer line = new StringBuffer(lineLength);
			for (int i=0; i < colcount; i++)
			{
				if (i > 0) line.append("-+-");
				line.append(StringUtil.padRight("-", columnWidths.get(Integer.valueOf(i)), '-'));
			}
			
			for (int col = 0; col < colcount; col ++)
			{
				if (col > 0) pw.print(" | ");
				writePadded(pw, data.getColumnName(col), columnWidths.get(Integer.valueOf(col)));
			}
			pw.println();

			pw.println(line);
			pw.flush();
			
			for (int row=0; row < data.getRowCount(); row++)
			{
				for (int col = 0; col < colcount; col ++)
				{
				if (col > 0) pw.print(" | ");
					String value = data.getValueAsString(row, col);
					value = StringUtil.escapeUnicode(value, CharacterRange.RANGE_CONTROL, null);
					writePadded(pw, value, columnWidths.get(Integer.valueOf(col)));
				}
				pw.println();
				pw.flush();
			}
			pw.flush();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConsolePrinter.printTo", "Error when printing DataStore contents", e);
		}
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
