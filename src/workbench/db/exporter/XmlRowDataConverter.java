/*
 * XmlRowDataConverter.java
 *
 * Created on August 26, 2004, 10:54 PM
 */

package workbench.db.exporter;

import java.sql.Types;
import java.text.SimpleDateFormat;
import workbench.db.TableIdentifier;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportTable;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.storage.*;

/**
 *
 * @author  workbench@kellerer.org
 */
public class XmlRowDataConverter
	extends RowDataConverter
{
	
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");

	public XmlRowDataConverter(ResultInfo info)
	{
		super(info);
	}
	
	public StrBuffer convertData()
	{
		return null;
	}

	public StrBuffer getEnd()
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

	public StrBuffer convertRowData(RowData row, int rowIndex)
	{
		boolean indent = true;
		String anIndent = "    ";
		int colCount = this.metaData.getColumnCount();
		StrBuffer xml = new StrBuffer(colCount * 100);
		if (indent) xml.append(anIndent);
		xml.append("<row-data ");
		xml.append("row-num=\"");
		xml.append(rowIndex + 1);
		xml.append("\">");
		xml.append(StringUtil.LINE_TERMINATOR);
		for (int c=0; c < colCount; c ++)
		{
			String value = null;//this.getValueAsFormattedString(aRow, c);
			Object data = row.getValue(c);
			if (data != null)
				value = data.toString();

			if (indent) xml.append(anIndent);
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
		if (indent) xml.append(anIndent);
		xml.append("</row-data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
	}

	public StrBuffer getStart()
	{
		StrBuffer xml = new StrBuffer(250);
		xml.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("<wb-export>");
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append(this.getMetaDataAsXml("  "));
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("  <data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
	}

	private StrBuffer getMetaDataAsXml(String anIndent)
	{
		boolean indent = (anIndent != null && anIndent.length() > 0);
		int colCount = this.metaData.getColumnCount();
		StrBuffer result = new StrBuffer(colCount * 50);
		if (indent) result.append(anIndent);
		result.append("<meta-data>");
		result.append(StringUtil.LINE_TERMINATOR);

		if (this.generatingSql != null)
		{
			if (indent) result.append(anIndent);
			result.append("  <generating-sql>");
			result.append(StringUtil.LINE_TERMINATOR);
			if (indent) result.append(anIndent);
			result.append("  <![CDATA[");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(this.generatingSql);
			result.append(StringUtil.LINE_TERMINATOR);
			if (indent) result.append(anIndent);
			result.append("  ]]>");
			result.append(StringUtil.LINE_TERMINATOR);
			if (indent) result.append(anIndent);
			result.append("  </generating-sql>");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(StringUtil.LINE_TERMINATOR);
		}

		if (this.originalConnection != null)
		{
			result.append(this.originalConnection.getDatabaseInfoAsXml(anIndent));
			result.append(StringUtil.LINE_TERMINATOR);
		}

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		if (indent) result.append(anIndent);
		result.append("  <created>");
		result.append(df.format(new java.util.Date(System.currentTimeMillis())));
		result.append("</created>" + StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("</meta-data>");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append('<');
		result.append(ReportTable.TAG_TABLE_DEF);
		result.append('>');
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- The following information was retrieved from the JDBC driver's ResultSetMetaData -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- name is retrieved from ResultSetMetaData.getColumnName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- java-class is retrieved from ResultSetMetaData.getColumnClassName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- java-sql-type-name is the constant's name from java.sql.Types -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- java-sql-type is the constant's numeric value from java.sql.Types as returned from ResultSetMetaData.getColumnType() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- dbms-data-type is retrieved from ResultSetMetaData.getColumnTypeName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(StringUtil.LINE_TERMINATOR);
		if (indent) result.append(anIndent);
		result.append("  <!-- For date and timestamp types, the internal long value obtained from java.util.Date.getTime()");
		result.append(StringUtil.LINE_TERMINATOR);
		if (indent) result.append(anIndent);
		result.append("       is written as an attribute to the <column-data> tag. That value can be used");
		result.append(StringUtil.LINE_TERMINATOR);
		if (indent) result.append(anIndent);
		result.append("       to create a java.util.Date() object directly, without the need to parse the actual tag content");
		result.append(StringUtil.LINE_TERMINATOR);
		if (indent) result.append(anIndent);
		result.append("  -->");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		boolean hasTable = false;
		if (indent) result.append(anIndent);
		result.append("  <");
		result.append(ReportTable.TAG_TABLE_NAME);
		result.append('>');
		TableIdentifier table = this.metaData.getUpdateTable();
		if (table != null) result.append(table.getTableExpression());
		result.append("</");
		result.append(ReportTable.TAG_TABLE_NAME);
		result.append(">");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <column-count>");
		result.append(colCount);
		result.append("</column-count>");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		for (int i=0; i < colCount; i++)
		{
			if (indent) result.append(anIndent);
			result.append("  <");
			result.append(ReportColumn.TAG_COLUMN_DEFINITION);
			result.append(" index=\"");
			result.append(i);
			result.append("\">");
			result.append(StringUtil.LINE_TERMINATOR);

			if (indent) result.append(anIndent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_NAME, this.metaData.getColumnName(i));

			if (indent) result.append(anIndent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_JAVA_CLASS, this.metaData.getColumnClassName(i));

			if (indent) result.append(anIndent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_JAVA_TYPE_NAME, SqlUtil.getTypeName(this.metaData.getColumnType(i)));

			if (indent) result.append(anIndent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_JAVA_TYPE, String.valueOf(this.metaData.getColumnType(i)));

			if (indent) result.append(anIndent);
			appendTag(result, "    ", ReportColumn.TAG_COLUMN_DBMS_TYPE, this.metaData.getDbmsTypeName(i));

			int type = this.metaData.getColumnType(i);
			if (SqlUtil.isDateType(type) )
			{
				if (type == Types.TIMESTAMP)
				{
					if (indent) result.append(anIndent);
					result.append("    <data-format>");
					result.append(this.timestampFormatter.toPattern());
					result.append("</data-format>");
					result.append(StringUtil.LINE_TERMINATOR);
				}
				else
				{
					if (indent) result.append(anIndent);
					result.append("    <data-format>");
					result.append(this.dateFormatter.toPattern());
					result.append("</data-format>");
					result.append(StringUtil.LINE_TERMINATOR);
				}
			}
			if (indent) result.append(anIndent);
			result.append("  </");
			result.append(ReportColumn.TAG_COLUMN_DEFINITION);
			result.append(">");
			result.append(StringUtil.LINE_TERMINATOR);
		}
		if (indent) result.append(anIndent);
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