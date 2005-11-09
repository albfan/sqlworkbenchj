/*
 * TextExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TextExportWriter
	extends ExportWriter
{
	private ResultInfo resultInfo;
	
	/** Creates a new instance of TextExportWriter */
	public TextExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter(ResultInfo info)
	{
		TextRowDataConverter converter = new TextRowDataConverter(info);
		converter.setDelimiter(exporter.getTextDelimiter());
		converter.setWriteHeader(exporter.getExportHeaders());
		converter.setQuoteCharacter(exporter.getTextQuoteChar());
		converter.setCleanNonPrintable(exporter.getCleanupCarriageReturns());
		converter.setQuoteAlways(exporter.getQuoteAlways());
		converter.setEscapeRange(exporter.getEscapeRange());
		converter.setEncodingUsed(exporter.getEncoding());
		converter.setLineEnding(exporter.getLineEnding());
		this.resultInfo = info;
		return converter;
	}

	protected void writeStart()
		throws IOException
	{
		writeOracleControlFile();
		super.writeStart();
	}
	
	private void writeOracleControlFile()
	{
		if (!exporter.getWriteOracleControlFile()) return;
		File baseFile = new File(exporter.getFullOutputFilename());
		String dir = baseFile.getParent();
		String baseName = baseFile.getName();
		baseName = baseName.substring(0,baseName.lastIndexOf('.'));
		File ctl = new File(dir, baseName + ".ctl");
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new BufferedWriter(new FileWriter(ctl)));
			if (exporter.getExportHeaders())
			{
				out.println("OPTIONS (skip=1)");
			}
			out.print("LOAD DATA CHARACTERSET ");
			out.println(exporter.getEncoding());
			out.println("TRUNCATE");
			out.print("INTO TABLE ");
			out.println(resultInfo.getUpdateTable().getTableName());
			out.print("FIELDS TERMINATED BY '");
			out.print(StringUtil.escapeUnicode(exporter.getTextDelimiter(), CharacterRange.RANGE_CONTROL));
			out.println("'");
			out.println("(");
			int count = resultInfo.getColumnCount();
			int max = 0;
			// calculate max. column name length for proper formatting
			for (int i=0; i < count; i++)
			{
				int l = resultInfo.getColumnName(i).length();
				if (l > max) max = l;
			}
			max ++;
			
			String format = exporter.getTimestampFormat();
			if (format == null) format = Settings.getInstance().getDefaultDateTimeFormat();
			//if (format == null) format = exporter.getDateFormat();
			
			String oraFormat = convertJavaDateFormatToOracle(format);
			
			for (int i=0; i < count; i++)
			{
				String col = resultInfo.getColumnName(i);
				out.print("  ");
				out.print(col);
				if (SqlUtil.isDateType(resultInfo.getColumnType(i)))
				{
					for (int k=col.length(); k < max; k++) out.print(" ");
					out.print("DATE");
					if (format != null)
					{
						out.print(" \"");
						out.print(oraFormat);
						out.print("\"");
					}
				}
				if (i < count - 1) out.print(",");
				out.println();
			}
			out.print(")");
		}
		catch (IOException io)
		{
			LogMgr.logError("TextExportWriter.writeOracleControlFile()", "Error opening outputfile", io);
		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
		}
	}

	private String convertJavaDateFormatToOracle(String format)
	{
		String result = format.replaceAll("HH", "HH24");
		result = result.replaceAll("hh", "HH12");
		result = result.replaceAll("mm", "MI");
		result = result.replaceAll("yy", "YY");
		result = result.replaceAll("dd", "DD");
		result = result.replaceAll("ss", "SS");
		return result;
	}
}
