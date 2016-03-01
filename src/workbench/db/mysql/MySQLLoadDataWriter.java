/*
 * MySQLLoadDataWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.mysql;

import java.io.FileWriter;
import java.io.PrintWriter;

import workbench.log.LogMgr;

import workbench.db.exporter.DataExporter;
import workbench.db.exporter.FormatFileWriter;
import workbench.db.exporter.RowDataConverter;

import workbench.util.CharacterRange;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * Creates a "LOAD DATA INFILE" statement to import a flat file created by the DataExporter.
 *
 * This LOAD DATA statement can be used to import the flat file into a MySQL database
 *
 * @author Thomas Kellerer
 */
public class MySQLLoadDataWriter
	implements FormatFileWriter
{

  private boolean useFullFilepath;

  @Override
  public void setUseFullFilepath(boolean flag)
  {
    useFullFilepath = flag;
  }

	@Override
	public void writeFormatFile(DataExporter exporter, RowDataConverter converter)
	{
		WbFile baseFile = new WbFile(exporter.getFullOutputFilename());
		String dir = baseFile.getParent();

		String tableName = exporter.getTableNameToUse();
		WbFile ctl = new WbFile(dir, "load_" + tableName + ".sql");
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new FileWriter(ctl));
			out.print("load data infile '");
			out.print(useFullFilepath ? baseFile.getFullPath() : baseFile.getName());
			out.print("'\n");
			out.print("  into table ");
			out.println(tableName);

			String encoding = exporter.getEncoding();
			if (encoding != null)
			{
				out.print("  character set " + encoding + "\n");
			}
			String delim = StringUtil.escapeText(exporter.getTextDelimiter(), CharacterRange.RANGE_CONTROL);
			out.print("  columns\n");
			out.print("    terminated by '" + delim + "'\n");
			String quote = exporter.getTextQuoteChar();

			if (quote != null)
			{
				out.print("    ");
				if (!exporter.getQuoteAlways())
				{
					out.print("optionally ");
				}
				out.print("enclosed by '" + quote + "'\n");
			}

			if (exporter.getExportHeaders())
			{
				out.print("  ignore 1 lines\n");
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("PostgresCopyStatementWriter.writeFormatFile()", "Could not write format file", e);
		}
		finally
		{
			FileUtil.closeQuietely(out);
		}	}

}
