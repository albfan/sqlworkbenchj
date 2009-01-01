/*
 * SqlServerFormatFileWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import workbench.log.LogMgr;
import workbench.storage.ResultInfo;
import workbench.util.CharacterRange;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * Write control files for SQL Server's <tt>bcp</tt> command.
 * 
 * @author support@sql-workbench.net
 */
public class SqlServerFormatFileWriter
	implements FormatFileWriter
{
	public void writeFormatFile(DataExporter exporter, RowDataConverter converter)
	{
		ResultInfo resultInfo = converter.getResultInfo();
		WbFile baseFile = new WbFile(exporter.getFullOutputFilename());
		String dir = baseFile.getParent();
		String baseName = baseFile.getFileName();
		WbFile ctl = new WbFile(dir, baseName + ".fmt");
		PrintWriter out = null;
		try
		{
			int count = resultInfo.getColumnCount();
			out = new PrintWriter(new BufferedWriter(new FileWriter(ctl)));
			out.println("7.0"); // Write bcp version string
			out.println(Integer.toString(count));

			int max = 0;
			// calculate max. column name length for proper formatting
			for (int i = 0; i < count; i++)
			{
				int l = resultInfo.getColumnName(i).length();
				if (l > max)
				{
					max = l;
				}
			}
			max++;

			String delim = StringUtil.escapeUnicode(exporter.getTextDelimiter(), CharacterRange.RANGE_CONTROL);
			String nl = StringUtil.escapeUnicode(exporter.getLineEnding(), CharacterRange.RANGE_CONTROL);

			for (int i = 0; i < count; i++)
			{
				String name = resultInfo.getColumnName(i);
				String col = StringUtil.padRight(name, max);
				if (name.indexOf(' ') > -1)
				{
					col = "\"" + col + "\"";
				}
				String pos = StringUtil.formatNumber(i + 1, 4, true);
				String term = null;
				if (i < count - 1)
				{
					term = delim;
				}
				else
				{
					term = nl;
				}
				out.println(pos + " SQLCHAR 0  0 \"" + term + "\"   " + pos + " " + col);
			}
		}
		catch (IOException io)
		{
			LogMgr.logError("SqlServerFormatFileWriter.writeFormatFile()", "Error opening outputfile", io);
		}
		finally
		{
			FileUtil.closeQuitely(out);
		}
	}
}
