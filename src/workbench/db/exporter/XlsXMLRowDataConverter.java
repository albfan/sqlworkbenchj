/*
 * XlsXMLRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.exporter;

import java.text.SimpleDateFormat;
import java.util.Date;

import workbench.db.report.TagWriter;

import workbench.storage.RowData;

import workbench.util.EncodingUtil;
import workbench.util.SqlUtil;
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

	@Override
	public StringBuilder getStart()
	{
		StringBuilder out = new StringBuilder(5000);
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
		out.append("  <Style ss:ID=\"wbNW\"><Alignment ss:Vertical=\"Top\" ss:WrapText=\"0\"/></Style>\n");
		out.append("</Styles>\n");

		int colCount = metaData.getColumnCount();

		out.append("<Worksheet ss:Name=\"" + escapeXML(getPageTitle("Export"), false) + "\">\n");
		out.append("<Table ss:ExpandedColumnCount=\"" + getRealColumnCount() + "\" x:FullColumns=\"1\" x:FullRows=\"1\">\n");

		for (int i = 0; i < colCount; i++)
		{
			if (!this.includeColumnInExport(i)) continue;
			out.append("<Column ss:AutoFitWidth=\"1\"/>\n");
		}

		if (includeColumnComments)
		{
			out.append("<Row>\n");
			for (int i = 0; i < colCount; i++)
			{
				if (!this.includeColumnInExport(i)) continue;
				out.append("  <Cell ss:StyleID=\"wbHeader\"><Data ss:Type=\"String\">");
				out.append(TagWriter.CDATA_START);
				out.append(metaData.getColumn(i).getComment());
				out.append(TagWriter.CDATA_END);
				out.append("</Data></Cell>\n");
			}
			out.append("</Row>");
		}

		if (writeHeader)
		{
			out.append("<Row>\n");
			for (int i = 0; i < colCount; i++)
			{
				if (!this.includeColumnInExport(i)) continue;
				out.append("  <Cell ss:StyleID=\"wbHeader\"><Data ss:Type=\"String\">");
				out.append(SqlUtil.removeObjectQuotes(metaData.getColumnDisplayName(i)));
				out.append("</Data></Cell>\n");
			}
			out.append("</Row>");
		}

		out.append('\n');
		return out;
	}

	@Override
	public StringBuilder getEnd(long totalRows)
	{
		StringBuilder out = new StringBuilder(250);
		out.append("</Table>\n");

		if (getEnableFixedHeader() && writeHeader)
		{
			out.append("<WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">\n");
			out.append("  <Selected/>\n<FreezePanes/>\n<FrozenNoSplit/>\n");
			out.append("  <SplitHorizontal>1</SplitHorizontal>\n<TopRowBottomPane>1</TopRowBottomPane>\n<ActivePane>2</ActivePane>\n");
			out.append("</WorksheetOptions>\n");
		}

		if (getEnableAutoFilter() && writeHeader)
		{
			out.append("<AutoFilter x:Range=\"R1C1:R1C" + getRealColumnCount() + "\" xmlns=\"urn:schemas-microsoft-com:office:excel\"></AutoFilter>\n");
		}
		out.append("</Worksheet>\n");

		if (getAppendInfoSheet())
		{
			out.append("<Worksheet ss:Name=\"SQL\">\n");
			out.append("  <Table ss:ExpandedColumnCount=\"1\">\n");
			out.append("    <Row>\n");
			out.append("      <Cell ss:StyleID=\"wbNW\"><Data ss:Type=\"String\">");
			out.append(TagWriter.CDATA_START);
			out.append(generatingSql);
			out.append(TagWriter.CDATA_END);
			out.append("      </Data></Cell>");
			out.append("    </Row>\n");
			out.append("  </Table>\n");
			out.append("</Worksheet>\n");
		}
		out.append("</Workbook>\n");
		return out;
	}

	@Override
	public StringBuilder convertRowData(RowData row, long rowIndex)
	{
		int colCount = row.getColumnCount();
		StringBuilder xml = new StringBuilder(colCount * 50);
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
				if (value == null) value = getNullDisplay();
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
