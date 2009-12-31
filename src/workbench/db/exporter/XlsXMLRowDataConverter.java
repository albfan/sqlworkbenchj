/*
 * XlsXMLRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.text.SimpleDateFormat;
import java.util.Date;
import workbench.storage.RowData;
import workbench.util.EncodingUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Convert row data to Microsoft's XLSX Spreadsheet format.
 *
 * @author  Thomas Kellerer
 */
public class XlsXMLRowDataConverter
	extends RowDataConverter
{
	private SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	public StrBuffer getStart()
	{
		StrBuffer out = new StrBuffer(5000);
		out.append("<?xml version=\"1.0\" encoding=\"" + EncodingUtil.cleanupEncoding(getEncoding()) + "\"?>\n");
		out.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
		out.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");
		out.append("<DocumentProperties xmlns=\"urn:schemas-microsoft-com:office:office\">\n");
		out.append("<LastAuthor>SQL Workbench/J</LastAuthor>\n");
		out.append("<Created>");
		out.append(tsFormat.format(new Date()));
		out.append("</Created>\n");
		out.append("</DocumentProperties>\n");

		out.append("<Styles>\n");
		if (writeHeader)
		{
			out.append("  <Style ss:ID=\"wbHeader\"><Font ss:Bold=\"1\"/></Style>\n");
		}
		out.append("  <Style ss:ID=\"wbTS\"><NumberFormat ss:Format=\""  + getDateFormat() + "\"/></Style>\n");
		out.append("  <Style ss:ID=\"wbML\"><Alignment ss:Vertical=\"Top\" ss:WrapText=\"1\"/></Style>\n");
		out.append("</Styles>\n");

		int colCount = metaData.getColumnCount();

		out.append("<Worksheet ss:Name=\"" + getPageTitle("Export") + "\">\n");
		out.append("<Table ss:ExpandedColumnCount=\"" + colCount + "\" x:FullColumns=\"1\" x:FullRows=\"1\">\n");

		for (int i = 0; i < colCount; i++)
		{
			if (!this.includeColumnInExport(i)) continue;
			out.append("<Column ss:AutoFitWidth=\"1\"/>\n");
		}

		if (writeHeader)
		{
			out.append("<Row>\n");
			for (int i = 0; i < colCount; i++)
			{
				if (!this.includeColumnInExport(i)) continue;
				out.append("  <Cell ss:StyleID=\"wbHeader\"><Data ss:Type=\"String\">");
				out.append(metaData.getColumnName(i));
				out.append("</Data></Cell>\n");
			}
			out.append("</Row>");
		}

		out.append('\n');
		return out;
	}

	public StrBuffer getEnd(long totalRows)
	{
		StrBuffer out = new StrBuffer(250);
		out.append("</Table>\n");
		out.append("</Worksheet>\n");
		out.append("</Workbook>\n");
		return out;
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		int colCount = row.getColumnCount();
		StrBuffer xml = new StrBuffer(colCount * 50);
		xml.append("<Row>\n");
		for (int i = 0; i < colCount; i++)
		{
			if (!this.includeColumnInExport(i))
			{
				continue;
			}
			boolean isDate = (row.getValue(i) instanceof Date);
			String value = null;

			if (isDate)
			{
				Date d = (Date)row.getValue(i);
				value = tsFormat.format(d);
			}
			else
			{
				value = getValueAsFormattedString(row, i);
			}
			boolean isMultiline = (value == null ? false : value.indexOf('\n') > 0);

			if (isDate)
			{
				xml.append("  <Cell ss:StyleID=\"wbTS\">");
			}
			else if (isMultiline)
			{
				xml.append("  <Cell ss:StyleID=\"wbML\">");
			}
			else
			{
				xml.append("  <Cell>");
			}
			xml.append("<Data ss:Type=\"");
			xml.append(getDataType(row.getValue(i)));
			xml.append("\">");

			writeEscapedXML(xml, value, false);

			xml.append("</Data></Cell>\n");
		}
		xml.append("</Row>\n\n");

		return xml;
	}

	private String getDateFormat()
	{
		String javaFormat = this.defaultTimestampFormatter != null ? this.defaultTimestampFormatter.toPattern() : "yyyy\\-mm\\-dd\\ hh:mm:ss";
		String excelFormat = StringUtil.replace(javaFormat, "-", "\\-");
		excelFormat = StringUtil.replace(excelFormat, " ", "\\ ");
		excelFormat = StringUtil.replace(excelFormat, "/", "\\/");
		return excelFormat.toLowerCase();
	}

	private String getDataType(Object data)
	{
		if (data instanceof Number)
		{
			return "Number";
		}
		else if (data instanceof java.util.Date)
		{
			return "DateTime";
		}
		return "String";
	}
}
