/*
 * OracleControlFileWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.CharacterRange;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * @author support@sql-workbench.net
 */
public class OracleControlFileWriter
	implements FormatFileWriter
{
	
	public void writeFormatFile(DataExporter exporter, RowDataConverter converter)
	{
		ResultInfo resultInfo = converter.getResultInfo();
		WbFile baseFile = new WbFile(exporter.getFullOutputFilename());
		String dir = baseFile.getParent();
		String baseName = baseFile.getFileName();
		File ctl = new File(dir, baseName + ".ctl");
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new FileWriter(ctl));
			if (exporter.getExportHeaders())
			{
				out.println("-- Skip the header row of the input file");
				out.println("OPTIONS (skip=1)");
			}

			out.println("-- The specified characterset might not be correct, please check the Oracle documentation");
			out.print("LOAD DATA CHARACTERSET '");
			out.println(convertJavaCharsetToOracle(exporter.getEncoding()) + "'");
			File f = new File(exporter.getFullOutputFilename());
			out.println("INFILE '" + f.getName() + "'");
			out.println("-- to replace the data in the table use TRUNCATE instead of APPEND");
			out.println("APPEND");
			out.print("INTO TABLE ");
			out.println(resultInfo.getUpdateTable().getTableName());
			out.print("FIELDS TERMINATED BY '");
			out.print(StringUtil.escapeUnicode(exporter.getTextDelimiter(), CharacterRange.RANGE_CONTROL));
			out.println("' TRAILING NULLCOLS");
			out.println("(");
			int count = resultInfo.getColumnCount();
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

			String format = exporter.getTimestampFormat();
			if (format == null)
			{
				format = Settings.getInstance().getDefaultTimestampFormat();
			//if (format == null) format = exporter.getDateFormat();
			}
			String oraFormat = convertJavaDateFormatToOracle(format);
			List<String> blobColumns = new LinkedList<String>();
			boolean clobAsFile = exporter.getWriteClobAsFile();
			for (int i = 0; i < count; i++)
			{
				String col = resultInfo.getColumnName(i);
				int type = resultInfo.getColumnType(i);
				out.print("  ");
				if (SqlUtil.isBlobType(type) || (clobAsFile && SqlUtil.isClobType(type, exporter.getConnection().getDbSettings())))
				{
					blobColumns.add(col);
					out.print("lob_file_" + col.toLowerCase() + " FILLER");
				}
				else
				{
					out.print(col);
				}

				if (SqlUtil.isDateType(type))
				{
					for (int k = col.length(); k < max; k++)
					{
						out.print(" ");
					}
					out.print("DATE");
					if (format != null)
					{
						out.print(" \"");
						out.print(oraFormat);
						out.print("\"");
					}
				}
				if (i < count - 1 || blobColumns.size() > 0)
				{
					out.print(",");
				}
				out.println();
			}

			if (blobColumns.size() > 0)
			{
				Iterator<String> itr = blobColumns.iterator();
				while (itr.hasNext())
				{
					String col = itr.next();
					out.print("  ");
					out.print(col);
					out.print(" LOBFILE(lob_file_" + col.toLowerCase() + ") TERMINATED BY EOF");
					if (itr.hasNext())
					{
						out.print(",");
					}
					out.println();
				}
			}
			out.print(")");
		}
		catch (IOException io)
		{
			LogMgr.logError("OracleControlFileWriter.writeFormatFile()", "Error opening outputfile", io);
		}
		finally
		{
			FileUtil.closeQuitely(out);
		}
	}

	private String convertJavaCharsetToOracle(String encoding)
	{
		if (encoding == null) return Settings.getInstance().getDefaultFileEncoding();
		if (encoding.equalsIgnoreCase("UTF8")) return "UTF-8";
		if (encoding.equalsIgnoreCase("ISO-8859-1")) return "WE8ISO8859P1";
		if (encoding.equalsIgnoreCase("ISO-8859-15")) return "WE8ISO8859P15";
		if (encoding.equalsIgnoreCase("ISO-8859-10")) return "NE8ISO8859P10";
		if (encoding.equalsIgnoreCase("ISO-8859-5")) return "CL8ISO8859P5";
		if (encoding.equalsIgnoreCase("ISO-8859-5")) return "CL8ISO8859P5";
		if (encoding.equalsIgnoreCase("windows-1250")) return "EE8MSWIN1250";
		if (encoding.equalsIgnoreCase("windows-1253")) return "EL8MSWIN1253";
		if (encoding.equalsIgnoreCase("ISO-2022-JP")) return "ISO2022-JP";
		if (encoding.equalsIgnoreCase("ISO-2022-CN")) return "ISO2022-CN";
		if (encoding.equalsIgnoreCase("ISO-2022-KR")) return "ISO2022-KR";
		if (encoding.equalsIgnoreCase("UTF-16BE")) return "AL16UTF16";
		return encoding.toUpperCase();
	}

	private String convertJavaDateFormatToOracle(String format)
	{
		String result = format.replace("HH", "HH24");
		result = result.replace("hh", "HH12");
		result = result.replace("mm", "MI");
		result = result.replace("yy", "YY");
		result = result.replace("dd", "DD");
		result = result.replace("ss", "SS");
		return result;
	}
}
