/*
 * XmlRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.sql.Types;

import workbench.db.TableIdentifier;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportTable;
import workbench.db.report.TagWriter;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author  info@sql-workbench.net
 */
public class XmlRowDataConverter
	extends RowDataConverter
{
	public XmlRowDataConverter(ResultInfo info)
	{
		super(info);
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
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("<wb-export>");
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append(this.getMetaDataAsXml("  "));
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("  <data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
	}

	
	public StrBuffer getEnd(long totalRows)
	{
		StrBuffer xml = new StrBuffer(100);
		xml.append("  </data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("</wb-export>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
	}

	public String getFormatName()
	{
		return "XML";
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		TagWriter tagWriter = new TagWriter();
		StrBuffer indent = new StrBuffer("    ");
		int colCount = this.metaData.getColumnCount();
		StrBuffer xml = new StrBuffer(colCount * 100);
		tagWriter.appendOpenTag(xml, indent, "row-data", "row-num", Long.toString(rowIndex + 1));
		xml.append(StringUtil.LINE_TERMINATOR);
		for (int c=0; c < colCount; c ++)
		{
			String value = this.getValueAsFormattedString(row, c);
			Object data = row.getValue(c);

			xml.append(indent);
			xml.append("  <column-data index=\"");
			xml.append(c);
			xml.append('"');
			if (value == null)
			{
				xml.append(" null=\"true\"");
			}
			else if (value.length() == 0)
			{
				xml.append(" null=\"false\"");
			}

			if (SqlUtil.isDateType(this.metaData.getColumnType(c)))
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
			xml.append('>');

			if (value != null)
			{
				// String data needs to be escaped!
				if (data instanceof String)
				{
					xml.append(StringUtil.escapeXML((String)data));
				}
				else
				{
					xml.append(value);
				}
			}
			xml.append("</column-data>");
			xml.append(StringUtil.LINE_TERMINATOR);
		}
		xml.append(indent);
		xml.append("</row-data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
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

		if (this.generatingSql != null)
		{
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(StringUtil.LINE_TERMINATOR);
			tagWriter.appendOpenTag(result, indent2, "generating-sql");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(indent2);
			result.append("<![CDATA[");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(indent2);
			result.append(this.generatingSql);
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(indent2);
			result.append("]]>");
			result.append(StringUtil.LINE_TERMINATOR);
			tagWriter.appendCloseTag(result, indent2, "generating-sql");
			result.append(StringUtil.LINE_TERMINATOR);
		}

		if (this.originalConnection != null)
		{
			result.append(this.originalConnection.getDatabaseInfoAsXml(indent2));
		}

		result.append(StringUtil.LINE_TERMINATOR);
		result.append(indent);
		result.append("</meta-data>");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(indent);
		result.append('<');
		result.append(ReportTable.TAG_TABLE_DEF);
		result.append('>');
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(indent);
		result.append("  <!-- The following information was retrieved from the JDBC driver's ResultSetMetaData -->");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(indent);
		result.append("  <!-- column-name is retrieved from ResultSetMetaData.getColumnName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(indent);
		result.append("  <!-- java-class is retrieved from ResultSetMetaData.getColumnClassName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(indent);
		result.append("  <!-- java-sql-type-name is the constant's name from java.sql.Types -->");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(indent);
		result.append("  <!-- java-sql-type is the constant's numeric value from java.sql.Types as returned from ResultSetMetaData.getColumnType() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(indent);
		result.append("  <!-- dbms-data-type is retrieved from ResultSetMetaData.getColumnTypeName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(StringUtil.LINE_TERMINATOR);
		result.append(indent);
		result.append("  <!-- For date and timestamp types, the internal long value obtained from java.util.Date.getTime()");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(indent);
		result.append("       is written as an attribute to the <column-data> tag. That value can be used");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(indent);
		result.append("       to create a java.util.Date() object directly, without the need to parse the actual tag content.");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(indent);
		result.append("       If Java is not used to parse this file, the date/time format used to write the data");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(indent);
		result.append("       is provided in the <data-format> tag of the column definition");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(indent);
		result.append("  -->");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		boolean hasTable = false;
		result.append(indent);
		result.append("  <");
		result.append(ReportTable.TAG_TABLE_NAME);
		result.append('>');
		TableIdentifier table = this.metaData.getUpdateTable();
		if (table != null) result.append(table.getTableExpression());
		result.append("</");
		result.append(ReportTable.TAG_TABLE_NAME);
		result.append(">");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(indent);
		result.append("  <column-count>");
		result.append(colCount);
		result.append("</column-count>");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		for (int i=0; i < colCount; i++)
		{
			result.append(indent);
			result.append("  <");
			result.append(ReportColumn.TAG_COLUMN_DEFINITION);
			result.append(" index=\"");
			result.append(i);
			result.append("\">");
			result.append(StringUtil.LINE_TERMINATOR);

			result.append(indent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_NAME, this.metaData.getColumnName(i));

			result.append(indent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_JAVA_CLASS, this.metaData.getColumnClassName(i));

			result.append(indent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_JAVA_TYPE_NAME, SqlUtil.getTypeName(this.metaData.getColumnType(i)));

			result.append(indent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_JAVA_TYPE, String.valueOf(this.metaData.getColumnType(i)));

			result.append(indent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_DBMS_TYPE, this.metaData.getDbmsTypeName(i));

			int type = this.metaData.getColumnType(i);
			if (SqlUtil.isDateType(type) )
			{
				if (type == Types.TIMESTAMP)
				{
					result.append(indent);
					result.append("    <data-format>");
					result.append(defaultTimestampFormatter.toPattern());
					result.append("</data-format>");
					result.append(StringUtil.LINE_TERMINATOR);
				}
				else
				{
					result.append(indent);
					result.append("    <data-format>");
					result.append(defaultDateFormatter.toPattern());
					result.append("</data-format>");
					result.append(StringUtil.LINE_TERMINATOR);
				}
			}
			result.append(indent);
			result.append("  </");
			result.append(ReportColumn.TAG_COLUMN_DEFINITION);
			result.append(">");
			result.append(StringUtil.LINE_TERMINATOR);
		}
		result.append(indent);
		result.append("</");
		result.append(ReportTable.TAG_TABLE_DEF);
		result.append(">");
		result.append(StringUtil.LINE_TERMINATOR);

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
		target.append(StringUtil.LINE_TERMINATOR);
	}
}
