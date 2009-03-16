/*
 * ResultSetPrinter.java
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

import java.sql.Types;
import workbench.sql.StatementRunnerResult;
import workbench.storage.*;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import workbench.interfaces.ResultSetConsumer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 * A class to print the contents of a ResultSet to a PrintStream.
 * The column widths are calculated by the suggested display size of the
 * columns of the ResultSet
 *
 * @see workbench.db.ColumnIdentifier#getDisplaySize()
 *
 * @author support@sql-workbench.net
 */
public class ResultSetPrinter
	extends ConsolePrinter
	implements ResultSetConsumer
{
	private static final int MAX_WIDTH = 80;
	private PrintWriter pw;
	private ResultInfo info;

	public ResultSetPrinter(PrintStream out)
		throws SQLException
	{
		pw = new PrintWriter(out);
	}

	public void cancel()
		throws SQLException
	{

	}

	public void done()
	{
	}

	@Override
	protected String getResultName()
	{
		return null;
	}

	@Override
	protected int getColumnType(int col)
	{
		return (info == null ? Types.OTHER : info.getColumnType(col));
	}

	@Override
	protected int getColumnCount()
	{
		return (info == null ? 0 : info.getColumnCount());
	}

	@Override
	protected String getColumnName(int col)
	{
		return (info == null ? "" : info.getColumnName(col));
	}

	@Override
	protected Map<Integer, Integer> getColumnSizes()
	{
		Map<Integer, Integer> widths = new HashMap<Integer, Integer>();
		for (int i=0; i < info.getColumnCount(); i++)
		{
			int nameWidth = info.getColumnName(i).length();
			int colSize = info.getColumn(i).getDisplaySize();

			int width = Math.max(nameWidth, colSize);
			width = Math.min(width, MAX_WIDTH);
			widths.put(Integer.valueOf(i), Integer.valueOf(width));
		}
		return widths;
	}

	public void consumeResult(StatementRunnerResult toConsume)
	{
		ResultSet data = toConsume.getResultSets().get(0);

		try
		{
			info = new ResultInfo(data.getMetaData(), null);
			printHeader(pw);

			//RowData row = new RowData(info);
			RowData row = RowDataFactory.createRowData(info, null);
			int count = 0;
			while (data.next())
			{
				row.read(data, info);
				printRow(pw, row, count);
				count ++;
			}
			if (toConsume.getShowRowCount())
			{
				pw.println();
				pw.println(ResourceMgr.getFormattedString("MsgRows", count));
			}
			pw.flush();
		}
		catch (Exception e)
		{
			LogMgr.logError("ResultSetPrinter.consumeResult", "Error when printing ResultSet", e);
		}
	}

}
