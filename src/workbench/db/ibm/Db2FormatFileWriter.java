/*
 * Db2FormatFileWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.ibm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;

import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.FormatFileWriter;
import workbench.db.exporter.RowDataConverter;

import workbench.storage.ResultInfo;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.FileUtil;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2FormatFileWriter
	implements FormatFileWriter
{
	private Map<String, String> codePageMap;

	public Db2FormatFileWriter()
	{
		codePageMap = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
		codePageMap.put("UTF-8", "1208");
		codePageMap.put("UTF-16", "1200");
		codePageMap.put("UTF-16BE", "1200");
		codePageMap.put("UTF-16LE", "1200");
		codePageMap.put("ISO-8859-1", "819");
		codePageMap.put("ISO-8859-15", "923");
		codePageMap.put("ISO-8859-6", "1089");
		codePageMap.put("ISO-8859-5", "915");
		codePageMap.put("US-ASCII", "437");
	}


	@Override
	public void writeFormatFile(DataExporter exporter, RowDataConverter converter)
	{
		ResultInfo resultInfo = converter.getResultInfo();
		WbFile baseFile = new WbFile(exporter.getFullOutputFilename());
		String dir = baseFile.getParent();
		String baseName = baseFile.getFileName();
		WbFile ctl = new WbFile(dir, baseName + ".clp");
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new FileWriter(ctl));
			out.println("-- use db2 -tf " + ctl.getName() + " to run this file");
			out.println("IMPORT FROM " + baseFile.getName() + " OF DEL");

			int count = resultInfo.getColumnCount();
			boolean containsLobs = false;
			boolean clobAsFile = exporter.getWriteClobAsFile();
			for (int i = 0; i < count; i++)
			{
				int type = resultInfo.getColumnType(i);
				String dbmsType = resultInfo.getDbmsTypeName(i);
				if (SqlUtil.isBlobType(type) || (clobAsFile && SqlUtil.isClobType(type, dbmsType, getDbSettings(exporter))))
				{
					containsLobs = true;
					break;
				}
			}
			if (containsLobs)
			{
				out.println("  LOBS FROM . ");
			}

			out.println("  MODIFIED BY coldel" + convertDelimiter(exporter.getTextDelimiter()));
			String quote = exporter.getTextQuoteChar();
			if (quote != null)
			{
				out.println("              chardel" + convertDelimiter(quote));
			}
			String decimal = exporter.getDecimalSymbol();
			out.println("              decpt=" + (decimal == null ? "." : decimal));
			String codepage = convertEncodingToCodePage(exporter.getEncoding());
			if (codepage != null)
			{
				out.println("              codepage=" + codepage);
			}
			String format = exporter.getTimestampFormat();
			if (format != null)
			{
				out.println("              timestampformat=\"" + convertJavaDateFormatToDB2(format) + "\"");
			}
			format = exporter.getDateFormat();
			if (format != null)
			{
				out.println("              dateformat=\"" + convertJavaDateFormatToDB2(format) + "\"");
			}

			out.print("  METHOD P(");
			for (int i = 0; i < count; i++)
			{
				if (i > 0) out.print(", ");
				out.print(NumberStringCache.getNumberString(i+1));
			}
			out.println(")");
			out.print("  INSERT INTO ");
			String table = exporter.getTableNameToUse();
			out.println(table);
			out.println(";");
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

	private DbSettings getDbSettings(DataExporter exporter)
	{
		if (exporter == null) return null;
		WbConnection con = exporter.getConnection();
		if (con == null) return null;
		return con.getDbSettings();
	}

	private String convertDelimiter(String delim)
	{
		if (delim == null) return "";
		if (delim.equals("\\t")) return "X09";
		if (delim.equals("\"")) return "\"\"";
		if (delim.equals("'")) return "''";
		char d = delim.charAt(0);

		if (d < 32)
		{
			String hex = Integer.toHexString(d);
			if (hex.length() == 1) return "X0" + hex;
			return "X" + hex;
		}
		return delim;
	}

	private String convertJavaDateFormatToDB2(String format)
	{
		if (format == null) return "";
		String result = format.replace("HH", "HH");
		result = result.replace("hh", "TTHH");
		result = result.replace("mm", "MM");
		result = result.replace("yy", "YY");
		result = result.replace("dd", "DD");
		result = result.replace("ss", "SS");
		return result;
	}


	private String convertEncodingToCodePage(String encoding)
	{
		if (StringUtil.isBlank(encoding)) return null;
		String codepage = this.codePageMap.get(encoding);
		if (codepage != null) return codepage;
		if (codepage.startsWith("IBM"))
		{
			codepage = encoding.replaceAll("^IBM-?", "");
		}
		if (codepage.toLowerCase().startsWith("windows"))
		{
			codepage = encoding.replaceAll("(?i)^windows-?", "");
		}
		return codepage;
	}
}
