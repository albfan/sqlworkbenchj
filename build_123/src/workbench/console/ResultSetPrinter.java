/*
 * ResultSetPrinter.java
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import workbench.interfaces.ResultSetConsumer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.storage.*;

import workbench.sql.StatementRunnerResult;

/**
 * A class to print the contents of a ResultSet to a PrintStream.
 * The column widths are calculated by the suggested display size of the
 * columns of the ResultSet
 *
 * @see workbench.db.ColumnIdentifier#getDisplaySize()
 *
 * @author Thomas Kellerer
 */
public class ResultSetPrinter
	extends ConsolePrinter
	implements ResultSetConsumer, PropertyChangeListener
{
	private static final int MAX_WIDTH = 80;
	private PrintWriter pw;
	private ResultInfo info;

	public ResultSetPrinter(PrintStream out)
		throws SQLException
	{
		super();
		pw = new PrintWriter(out);
	}

	@Override
	public boolean ignoreMaxRows()
	{
		return false;
	}

	@Override
	public void cancel()
		throws SQLException
	{

	}

	@Override
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
		Map<Integer, Integer> widths = new HashMap<>();
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

	@Override
	public void consumeResult(StatementRunnerResult toConsume)
	{
		ResultSet data = toConsume.getResultSets().get(0);

		try
		{
			info = new ResultInfo(data.getMetaData(), null);
			printHeader(pw);

			//RowData row = new RowData(info);
			RowDataReader reader = RowDataReaderFactory.createReader(info, null);
			int count = 0;
			while (data.next())
			{
				RowData row = reader.read(data, false);
				printRow(pw, row, count);
				reader.closeStreams();
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

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() != ConsoleSettings.getInstance()) return;

    if (evt.getPropertyName().equals(ConsoleSettings.EVT_PROPERTY_ROW_DISPLAY))
    {
      RowDisplay newDisplay = ConsoleSettings.getInstance().getNextRowDisplay();
      setPrintRowsAsLine(newDisplay == RowDisplay.SingleLine);
    }
    else if (evt.getPropertyName().equals(ConsoleSettings.PROP_NULL_STRING))
    {
      nullString = ConsoleSettings.getNullString();
    }
	}

}
