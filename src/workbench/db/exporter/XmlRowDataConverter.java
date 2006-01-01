/*
 * XmlRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Types;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportTable;
import workbench.db.report.TagWriter;
import workbench.log.LogMgr;
import workbench.storage.NullValue;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class XmlRowDataConverter
	extends RowDataConverter
{
	public static final String LONG_ROW_TAG  = "row-data";
	public static final String LONG_COLUMN_TAG = "column-data";
	public static final String SHORT_ROW_TAG = "rd";
	public static final String SHORT_COLUMN_TAG = "cd";
	public static final String COLUMN_DEF_TAG = ReportColumn.TAG_COLUMN_DEFINITION;
	public static final String JAVA_CLASS_TAG = ReportColumn.TAG_COLUMN_JAVA_CLASS;
	public static final String JAVA_TYPE_TAG = ReportColumn.TAG_COLUMN_JAVA_TYPE;
	public static final String DBMS_TYPE_TAG = ReportColumn.TAG_COLUMN_DBMS_TYPE;
	public static final String DATA_FORMAT_TAG = "data-format";
	public static final String TABLE_NAME_TAG = ReportTable.TAG_TABLE_NAME;
	public static final String TABLE_DEF_TAG = ReportTable.TAG_TABLE_DEF;
	public static final String COLUMN_COUNT_TAG = "column-count";
	public static final String COLUMN_NAME_TAG = ReportColumn.TAG_COLUMN_NAME;
	public static final String ATTR_LONGVALUE = "longValue";
	public static final String ATTR_NULL = "null";
	public static final String ATTR_DATA_FILE = "dataFile";
	public static final String KEY_FORMAT_LONG = "long";
	public static final String KEY_FORMAT_SHORT = "short";
	public static final String TAG_TAG_FORMAT = "wb-tag-format";

	private boolean useCData = false;
	private boolean verboseFormat = true;
	private String lineEnding = "\n";
	private String coltag = LONG_COLUMN_TAG;
	private String rowtag = LONG_ROW_TAG;
	private String numAttrib = "row-num";
	private String startColTag = "  <" + coltag + " index=\"";
	private String closeColTag = "</" + coltag + ">";
	private String closeRowTag = "</" + rowtag + ">";
	private String tableToUse = null;
	private String baseFilename;
	private StrBuffer dbInfo;
	
	public XmlRowDataConverter()
	{
		super();
	}

	public void setTableNameToUse(String name)
	{
		this.tableToUse = name;
	}

	public void setOriginalConnection(WbConnection con)
	{
		super.setOriginalConnection(con);
		StrBuffer indent = new StrBuffer("    ");
		this.dbInfo = con.getDatabaseInfoAsXml(indent);
	}
	
	public void setUseVerboseFormat(boolean flag)
	{
		this.verboseFormat = flag;
		if (flag)
		{
			coltag = LONG_COLUMN_TAG;
			rowtag = LONG_ROW_TAG;
			startColTag = "  <" + coltag + " index=\"";
		}
		else
		{
			coltag = SHORT_COLUMN_TAG;
			rowtag = SHORT_ROW_TAG;
			startColTag = "<" + coltag;
		}
		closeColTag = "</" + coltag + ">";
		closeRowTag = "</" + rowtag + ">";
	}
	public StrBuffer convertData()
	{
		return null;
	}

	public StrBuffer getStart()
	{
		StrBuffer xml = new StrBuffer(250);
		String enc = this.getEncoding();
		xml.append("<?xml version=\"1.0\"");
		if (enc != null) xml.append(" encoding=\"" + enc + "\"");
		xml.append("?>");
		xml.append(this.lineEnding);
		xml.append("<wb-export>");
		xml.append(this.lineEnding);
		xml.append(this.getMetaDataAsXml("  "));
		xml.append(this.lineEnding);
		if (this.verboseFormat) xml.append("  ");
		xml.append("<data>");
		xml.append(this.lineEnding);
		return xml;
	}

	public void setBaseFilename(String name) 
	{ 
		File f = new File(name);
		String fname = f.getName();
		int pos = fname.lastIndexOf('.');
		if (pos == -1) pos = fname.length() - 1;
		this.baseFilename = fname.substring(0, pos) + "_";
	}
	
	public StrBuffer getEnd(long totalRows)
	{
		StrBuffer xml = new StrBuffer(100);
		if (this.verboseFormat) xml.append("  ");
		xml.append("</data>");
		xml.append(this.lineEnding);
		xml.append("</wb-export>");
		xml.append(this.lineEnding);
		return xml;
	}

	public void setUseCDATA(boolean flag)
	{
		this.useCData = flag;
	}

	public boolean getUseCDATA()
	{
		return this.useCData;
	}

	public String getFormatName()
	{
		return "XML";
	}

	public void setLineEnding(String ending)
	{
		if (ending != null) this.lineEnding = ending;
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		TagWriter tagWriter = new TagWriter();
		StrBuffer indent = new StrBuffer("    ");
		int colCount = this.metaData.getColumnCount();
		StrBuffer xml = new StrBuffer(colCount * 100);
		StringBuffer dataFile = null;

		if (this.verboseFormat)
		{
			tagWriter.appendOpenTag(xml, indent, rowtag, numAttrib, Long.toString(rowIndex + 1));
		}
		else
		{
			tagWriter.appendOpenTag(xml, null, rowtag);
		}

		if (verboseFormat) xml.append(this.lineEnding);
		for (int c=0; c < colCount; c ++)
		{
			if (!this.includeColumnInExport(c)) continue;
			Object data = row.getValue(c);
			int type = this.metaData.getColumnType(c);
			boolean isNull = (data == null || data instanceof NullValue);
			boolean writeCloseTag = true;
			if (this.verboseFormat) xml.append(indent);
			xml.append(startColTag);
			if (this.verboseFormat)
			{
				xml.append(c);
				xml.append('"');
			}
			if (isNull)
			{
				xml.append(" null=\"true\"");
			}

			if (SqlUtil.isDateType(type))
			{
				try
				{
					java.util.Date d = (java.util.Date)data;
					xml.append(" longValue=\"");
					xml.append(Long.toString(d.getTime()));
					xml.append('"');
				}
				catch (Exception e)
				{
				}
			}
			else if (SqlUtil.isBlobType(type))
			{
				dataFile = new StringBuffer(100);
				dataFile.append(baseFilename);
				dataFile.append('r');
				dataFile.append(rowIndex + 1);
				dataFile.append("_c");
				dataFile.append(c);
				dataFile.append(".dat");
				xml.append(' ');
				xml.append(ATTR_DATA_FILE);
				xml.append("=\"");
				xml.append(dataFile);
				xml.append("\"/");
				writeCloseTag = false;
			}
			xml.append('>');

			if (!isNull)
			{
				// String data needs to be escaped!
				if (SqlUtil.isCharacterType(type))
				{
					if (this.useCData)
					{
						xml.append(TagWriter.CDATA_START);
						xml.append(this.getValueAsFormattedString(row, c));
						xml.append(TagWriter.CDATA_END);
					}
					else
					{
						xml.append(StringUtil.escapeXML(this.getValueAsFormattedString(row, c)));
					}
				}
				else if (SqlUtil.isBlobType(type))
				{
					writeBlobFile(dataFile, data);
				}
				else
				{
					xml.append(this.getValueAsFormattedString(row, c));
				}
			}
			if (writeCloseTag) xml.append(closeColTag);
			if (this.verboseFormat) xml.append(this.lineEnding);
		}
		if (this.verboseFormat) xml.append(indent);
		xml.append(closeRowTag);
		xml.append(this.lineEnding);
		return xml;
	}

	private void writeBlobFile(StringBuffer file, Object data)
	{
		OutputStream out = null;
		try
		{
			File f = new File(this.baseDir, file.toString());
			out = new BufferedOutputStream(new FileOutputStream(f), 64*1024);
			if (data instanceof byte[])
			{
				out.write((byte[])data);
			}
			else if (data instanceof Blob)
			{
				Blob bl = (Blob)data;
				InputStream in = bl.getBinaryStream();
				int buffsize = 32*1024;
				byte[] buffer = new byte[buffsize];
				int read = in.read(buffer);
				while (read > -1)
				{
					out.write(buffer, 0, read);
					read = in.read(buffer);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("XmlRowDataConverter.writeBlobFile()", "Error writing blob to external file", e);
		}
		finally
		{
			try { out.close(); } catch (Throwable e) {}
		}
	}
	
	private StrBuffer getMetaDataAsXml(String anIndent)
	{
		TagWriter tagWriter = new TagWriter();
		StrBuffer indent = new StrBuffer(anIndent);
		StrBuffer indent2 = new StrBuffer(anIndent);
		indent2.append("  ");

		int colCount = this.metaData.getColumnCount();
		StrBuffer result = new StrBuffer(colCount * 50);
		tagWriter.appendOpenTag(result, indent, "meta-data");
		result.append(this.lineEnding);

		if (this.generatingSql != null)
		{
			result.append(this.lineEnding);
			tagWriter.appendOpenTag(result, indent2, "generating-sql");
			result.append(this.lineEnding);
			result.append(indent2);
			result.append("<![CDATA[");
			result.append(this.lineEnding);
			result.append(indent2);
			result.append(this.generatingSql);
			result.append(this.lineEnding);
			result.append(indent2);
			result.append("]]>");
			result.append(this.lineEnding);
			tagWriter.appendCloseTag(result, indent2, "generating-sql");
			result.append(this.lineEnding);
		}

		if (this.dbInfo != null)
		{
			result.append(this.dbInfo);
		}
		else if (this.originalConnection != null)
		{
			result.append(this.originalConnection.getDatabaseInfoAsXml(indent2));
		}

		//result.append(this.lineEnding);
		result.append(indent2);
		result.append('<');result.append(TAG_TAG_FORMAT);result.append('>');
		result.append(this.verboseFormat ? KEY_FORMAT_LONG : KEY_FORMAT_SHORT);
		result.append("</");result.append(TAG_TAG_FORMAT);result.append('>');
		result.append(this.lineEnding);
		result.append(indent);
		result.append("</meta-data>");
		result.append(this.lineEnding);
		result.append(this.lineEnding);

		result.append(indent);
		result.append('<');
		result.append(TABLE_DEF_TAG);
		result.append('>');
		result.append(this.lineEnding);

		result.append(indent);
		result.append("  <!-- The following information was retrieved from the JDBC driver's ResultSetMetaData -->");
		result.append(this.lineEnding);

		result.append(indent);
		result.append("  <!-- column-name is retrieved from ResultSetMetaData.getColumnName() -->");
		result.append(this.lineEnding);

		result.append(indent);
		result.append("  <!-- java-class is retrieved from ResultSetMetaData.getColumnClassName() -->");
		result.append(this.lineEnding);

		result.append(indent);
		result.append("  <!-- java-sql-type-name is the constant's name from java.sql.Types -->");
		result.append(this.lineEnding);

		result.append(indent);
		result.append("  <!-- java-sql-type is the constant's numeric value from java.sql.Types as returned from ResultSetMetaData.getColumnType() -->");
		result.append(this.lineEnding);

		result.append(indent);
		result.append("  <!-- dbms-data-type is retrieved from ResultSetMetaData.getColumnTypeName() -->");
		result.append(this.lineEnding);

		result.append(this.lineEnding);
		result.append(indent);
		result.append("  <!-- For date and timestamp types, the internal long value obtained from java.util.Date.getTime()");
		result.append(this.lineEnding);
		result.append(indent);
		result.append("       is written as an attribute to the <column-data> tag. That value can be used");
		result.append(this.lineEnding);
		result.append(indent);
		result.append("       to create a java.util.Date() object directly, without the need to parse the actual tag content.");
		result.append(this.lineEnding);
		result.append(indent);
		result.append("       If Java is not used to parse this file, the date/time format used to write the data");
		result.append(this.lineEnding);
		result.append(indent);
		result.append("       is provided in the <data-format> tag of the column definition");
		result.append(this.lineEnding);
		result.append(indent);
		result.append("  -->");
		result.append(this.lineEnding);
		result.append(this.lineEnding);

		result.append(indent);
		result.append("  <");
		result.append(TABLE_NAME_TAG);
		result.append('>');
		if (this.tableToUse != null)
		{
			result.append(tableToUse);
		}
		else
		{
			TableIdentifier table = this.metaData.getUpdateTable();
			if (table != null)
			{
				result.append(table.getTableName());
			}
		}
		result.append("</");
		result.append(TABLE_NAME_TAG);
		result.append(">");
		result.append(this.lineEnding);

		result.append(indent);
		result.append("  <column-count>");
		result.append(colCount);
		result.append("</column-count>");
		result.append(this.lineEnding);
		result.append(this.lineEnding);

		for (int i=0; i < colCount; i++)
		{
			if (!this.includeColumnInExport(i)) continue;
			result.append(indent);
			result.append("  <");
			result.append(COLUMN_DEF_TAG);
			result.append(" index=\"");
			result.append(i);
			result.append("\">");
			result.append(this.lineEnding);

			result.append(indent);
			appendTag(result, "    ", COLUMN_NAME_TAG, this.metaData.getColumnName(i));

			result.append(indent);
			appendTag(result, "    ", JAVA_CLASS_TAG, this.metaData.getColumnClassName(i));

			result.append(indent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_JAVA_TYPE_NAME, SqlUtil.getTypeName(this.metaData.getColumnType(i)));

			result.append(indent);
			appendTag(result, "    ", JAVA_TYPE_TAG, String.valueOf(this.metaData.getColumnType(i)));

			result.append(indent);
			appendTag(result, "    ", DBMS_TYPE_TAG, this.metaData.getDbmsTypeName(i));

			int type = this.metaData.getColumnType(i);
			if (SqlUtil.isDateType(type) )
			{
				if (type == Types.TIMESTAMP)
				{
					result.append(indent);
					result.append("    <data-format>");
					result.append(defaultTimestampFormatter.toPattern());
					result.append("</data-format>");
					result.append(this.lineEnding);
				}
				else
				{
					result.append(indent);
					result.append("    <data-format>");
					result.append(defaultDateFormatter.toPattern());
					result.append("</data-format>");
					result.append(this.lineEnding);
				}
			}
			result.append(indent);
			result.append("  </");
			result.append(COLUMN_DEF_TAG);
			result.append(">");
			result.append(this.lineEnding);
		}
		result.append(indent);
		result.append("</");
		result.append(TABLE_DEF_TAG);
		result.append(">");
		result.append(this.lineEnding);

		return result;
	}

	private void appendOpenTag(StrBuffer target, String indent, String tag)
	{
		target.append(indent);
		target.append('<');
		target.append(tag);
		target.append('>');
	}

	private void appendCloseTag(StrBuffer target, String tag)
	{
		target.append("</");
		target.append(tag);
		target.append('>');
	}

	private void appendTag(StrBuffer target, String indent, String tag, String value)
	{
		appendOpenTag(target, indent, tag);
		target.append(value);
		appendCloseTag(target, tag);
		target.append(this.lineEnding);
	}

}
