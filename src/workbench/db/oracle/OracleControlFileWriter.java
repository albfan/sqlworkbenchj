/*
 * OracleControlFileWriter.java
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
package workbench.db.oracle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.exporter.DataExporter;
import workbench.db.exporter.FormatFileWriter;
import workbench.db.exporter.RowDataConverter;

import workbench.storage.ResultInfo;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.WbFile;

/**
 * Creates control files for Oracle's SQL*Loader.
 *
 * The format of the control file matches the exported flat file.
 * The exported file(s) can be loaded directly with SQL*Loader and
 * the control files created by this class.
 *
 * @author Thomas Kellerer
 */
public class OracleControlFileWriter
	implements FormatFileWriter
{

	private final Map<String, String> encodingMap = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

	public OracleControlFileWriter()
	{
		encodingMap.put("UTF8","AL32UTF8");
		encodingMap.put("UTF-8","AL32UTF8");
		encodingMap.put("ISO-8859-1","WE8ISO8859P1");
		encodingMap.put("ISO-8859-15","WE8ISO8859P15");
		encodingMap.put("ISO-8859-10","NE8ISO8859P10");
		encodingMap.put("ISO-8859-5","CL8ISO8859P5");
		encodingMap.put("ISO-8859-5","CL8ISO8859P5");
		encodingMap.put("windows-1250","EE8MSWIN1250");
		encodingMap.put("windows-1253","EL8MSWIN1253");
		encodingMap.put("ISO-2022-JP","ISO2022-JP");
		encodingMap.put("ISO-2022-CN","ISO2022-CN");
		encodingMap.put("ISO-2022-KR","ISO2022-KR");
		encodingMap.put("UTF-16BE","AL16UTF16");
		List<String> custom = Settings.getInstance().getListProperty("workbench.db.oracle.encodingmap", false, null);
		for (String map : custom)
		{
			String[] elements = map.split(";");
			if (elements.length == 2)
			{
				encodingMap.put(elements[0], elements[1]);
			}
		}
	}

	@Override
	public void writeFormatFile(DataExporter exporter, RowDataConverter converter)
	{
		ResultInfo resultInfo = converter.getResultInfo();
		WbFile baseFile = exporter.getOutputFile();
		String dir = baseFile.getParent();
		String baseName = baseFile.getFileName();
		File ctl = new File(dir, baseName + ".ctl");
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new FileWriter(ctl));
			out.println("-- Please review these options to fit your needs");
			if (exporter.getExportHeaders())
			{
				out.println("OPTIONS (skip=1, direct=true, rows=10000, silent=(feedback) )");
			}
			else
			{
				out.println("OPTIONS (direct=true, rows=10000, silent=(feedback) )");
			}
			out.println("-- The specified characterset might not be correct, please check the Oracle documentation");
			out.print("LOAD DATA CHARACTERSET '");
			out.println(convertJavaCharsetToOracle(exporter.getEncoding()) + "'");
			out.println("INFILE '" + baseFile.getName() + "'");
			out.println("-- to replace the data in the table use TRUNCATE instead of APPEND");
			out.println("APPEND");
			out.print("INTO TABLE ");
			String table = exporter.getTableNameToUse();
			out.println(table);
			out.print("FIELDS TERMINATED BY '");
			out.print(convertDelimiter(exporter.getTextDelimiter()));
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
			}
			String oraFormat = convertJavaDateFormatToOracle(format);
			List<String> blobColumns = new LinkedList<>();
			boolean clobAsFile = exporter.getWriteClobAsFile();
			for (int i = 0; i < count; i++)
			{
				String col = resultInfo.getColumnName(i);
				int type = resultInfo.getColumnType(i);
				String dbmsType = resultInfo.getDbmsTypeName(i);

				out.print("  ");
				if (SqlUtil.isBlobType(type) || (clobAsFile && SqlUtil.isClobType(type, dbmsType, exporter.getConnection().getDbSettings())))
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
				else if (SqlUtil.isNumberType(type) && i == count -1)
				{
					for (int k = col.length(); k < max; k++)
					{
						out.print(" ");
					}
					out.print("TERMINATED BY WHITESPACE");
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
			FileUtil.closeQuietely(out);
		}
	}

	private String convertDelimiter(String delim)
	{
		delim = delim.replace("\t", "\\t");
		delim = delim.replace("\r", "\\r");
		delim = delim.replace("\n", "\\n");
		return delim;
	}

	private String convertJavaCharsetToOracle(String encoding)
	{
		if (encoding == null) return Settings.getInstance().getDefaultFileEncoding();
		encoding = EncodingUtil.cleanupEncoding(encoding);

		String oraEncoding = encodingMap.get(encoding);
		if (oraEncoding != null)
		{
			return oraEncoding;
		}
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
