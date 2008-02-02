/*
 * XmlRowDataConverter.java
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

import java.text.SimpleDateFormat;
import java.util.Date;
import workbench.storage.RowData;
import workbench.util.StrBuffer;

/**
 * Convert row data to our own XML format.
 * 
 * @author  support@sql-workbench.net
 */
public class XlsXMLRowDataConverter
	extends RowDataConverter
{
	private SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	private int[] maxColSizes;
	
	public XlsXMLRowDataConverter()
	{
		super();
	}

	public StrBuffer getStart()
	{
		StrBuffer out = new StrBuffer(5000);
		out.append("<?xml version=\"1.0\"?>\n");
		out.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
		out.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");
		out.append("<DocumentProperties xmlns=\"urn:schemas-microsoft-com:office:office\">\n");
		out.append("<LastAuthor>SQL Workbench/J</LastAuthor>\n");
		out.append("<Created>");
		out.append(tsFormat.format(new Date()));
		out.append("</Created>\n");
		out.append("</DocumentProperties>\n");

		out.append("<Styles>\n");
		for (int i = 0; i < metaData.getColumnCount(); i++)
		{
			int size = metaData.getColumnSize(i) * 10;
		}
		out.append("</Styles>\n");

		int colCount = metaData.getColumnCount();

		out.append("<Worksheet ss:Name=\"" + getPageTitle("Export") + "\">\n");
		out.append("<Table ss:ExpandedColumnCount=\"" + colCount + "\" x:FullColumns=\"1\" x:FullRows=\"1\">\n");

		for (int i = 0; i < colCount; i++)
		{
			if (!this.includeColumnInExport(i)) continue;
			out.append("<Column ss:AutoFitWidth=\"1\" ss:Width=\"" + (metaData.getColumnSize(i) * 10) + "\"/>\n");
		}
		maxColSizes = new int[colCount];
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
			xml.append("<Cell>\n");
			
			xml.append("<Data ss:Type=\"");
			xml.append(getDataType(row.getValue(i)));
			xml.append("\">");
			String value = getValueAsFormattedString(row, i);
			if (value != null)
			{
				xml.append(value);
				if (value.length() > maxColSizes[i])
				{
					maxColSizes[i] = value.length();
				}
			}
			xml.append("</Data>\n");
			xml.append("</Cell>\n");
		}
		xml.append("</Row>\n\n");
		
		return xml;
	}

	private String getDataType(Object data)
	{
		if (data instanceof Number)
		{
			return "Number";
		}
		else if (data instanceof java.util.Date)
		{
			return "Date";
		}
		return "String";
	}
}
