/*
 * PostgresCopyStatementWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.postgres;

import java.io.FileWriter;
import java.io.PrintWriter;

import workbench.log.LogMgr;

import workbench.db.exporter.DataExporter;
import workbench.db.exporter.FormatFileWriter;
import workbench.db.exporter.RowDataConverter;

import workbench.storage.ResultInfo;

import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 *
 * Creates a COPY statement to import a flat file created by the DataExporter.
 *
 * This COPY statement can be used to import the flat file into a PostgreSQL database
 *
 * @author Thomas Kellerer
 */
public class PostgresCopyStatementWriter
	implements FormatFileWriter
{

	@Override
	public void writeFormatFile(DataExporter exporter, RowDataConverter converter)
	{
		ResultInfo resultInfo = converter.getResultInfo();

		WbFile baseFile = new WbFile(exporter.getFullOutputFilename());
		String dir = baseFile.getParent();
		String baseName = baseFile.getFileName();
		WbFile ctl = new WbFile(dir, "copy_" + baseName + ".sql");
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new FileWriter(ctl));
			out.print("copy ");
			String table = exporter.getTableNameToUse();
			out.print(table);
			out.print(" (");
			for (int i=0; i < resultInfo.getColumnCount(); i++)
			{
				if (i > 0) out.print(", ");
				out.print(resultInfo.getColumnName(i));
			}
			out.print(")");


			out.print("\n     from ");
			out.print("'" + baseFile.getFullPath() + "'");

			String delim = exporter.getTextDelimiter();
			out.print("\n     with (format csv");

			out.print(", delimiter '" + delim + "'");  // a tab should be written as a tab!

			if (exporter.getExportHeaders())
			{
				out.print(", header true");
			}
			String quote = exporter.getTextQuoteChar();
			if (quote != null)
			{
				out.print(", quote '" + quote + "'");
			}
			String encoding = exporter.getEncoding();
			if (encoding != null)
			{
				out.print(", encoding '" + encoding + "'");   // only available from 9.1 onwards but I'm writing it anyways
			}
			out.println(");");
		}
		catch (Exception e)
		{
			LogMgr.logError("PostgresCopyStatementWriter.writeFormatFile()", "Could not write format file", e);
		}
		finally
		{
			FileUtil.closeQuietely(out);
		}
	}

}
