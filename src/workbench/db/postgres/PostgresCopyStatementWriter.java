/*
 * PostgresCopyStatementWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.io.FileWriter;
import java.io.PrintWriter;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.FormatFileWriter;
import workbench.db.exporter.RowDataConverter;
import workbench.log.LogMgr;
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
			out.print("\\copy ");
			out.print(resultInfo.getUpdateTable().getTableName());
			out.print(" (");
			for (int i=0; i < resultInfo.getColumnCount(); i++)
			{
				if (i > 0) out.print(", ");
				out.print(resultInfo.getColumnName(i));
			}
			out.print(")");


			out.print(" from ");
			out.print("'" + baseFile.getFullPath() + "'");

			String delim = exporter.getTextDelimiter();
			out.print(" delimiter as '" + delim + "'");  // a tab should be written as a tab!

			if (exporter.getExportHeaders())
			{
				out.print(" csv header");
			}
			String quote = exporter.getTextQuoteChar();
			if (quote != null)
			{
				out.print(" quote as '" + quote + "'");
			}
			String encoding = exporter.getEncoding();
			if (quote != null)
			{
				out.print(" encoding '" + encoding + "'");   // only available from 9.1 onwards but I'm writing it anyways
			}
			out.println();
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
