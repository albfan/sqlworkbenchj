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

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import workbench.log.LogMgr;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.ZipOutputFactory;

/**
 * Convert row data to our own XML format.
 * 
 * @author  support@sql-workbench.net
 */
public class OdsRowDataConverter
	extends RowDataConverter
{
	private Writer content;
	private SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private int[] maxColSizes;
	
	public OdsRowDataConverter()
	{
		super();
	}

	public StrBuffer getStart()
	{
		Writer out = null;
		try
		{
			if (this.factory != null)
			{
				this.factory.done();
			}
			this.factory = new ZipOutputFactory(getOutputFile());
			out = factory.createWriter("META-INF/manifest.xml", "UTF-8");

			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			out.write("<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\">\n");
			out.write(" <manifest:file-entry manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet\" manifest:full-path=\"/\"/>\n");
			out.write(" <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"content.xml\"/>\n");
			out.write("</manifest:manifest>\n");
			out.close();

			out = factory.createWriter("mimetype", "UTF-8");
			out.write("application/vnd.oasis.opendocument.spreadsheet");
			out.close();
			this.content = factory.createWriter("content.xml", "UTF-8");

			content.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n");
			content.write("<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" xmlns:number=\"urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0\" xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\" xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\" xmlns:chart=\"urn:oasis:names:tc:opendocument:xmlns:chart:1.0\" xmlns:dr3d=\"urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0\" xmlns:math=\"http://www.w3.org/1998/Math/MathML\" xmlns:form=\"urn:oasis:names:tc:opendocument:xmlns:form:1.0\" xmlns:script=\"urn:oasis:names:tc:opendocument:xmlns:script:1.0\" xmlns:ooo=\"http://openoffice.org/2004/office\" xmlns:ooow=\"http://openoffice.org/2004/writer\" xmlns:oooc=\"http://openoffice.org/2004/calc\" xmlns:dom=\"http://www.w3.org/2001/xml-events\" xmlns:xforms=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" office:version=\"1.0\"> \n");
			int colCount = this.metaData.getColumnCount();
			writeStyles(colCount);
			
			content.write("<office:body>\n");
			content.write("<office:spreadsheet> \n");
			content.write("<table:table table:name=\"" + getPageTitle("Export") + "\"  table:style-name=\"ta1\">\n\n");

			for (int i=0; i < colCount; i++)
			{
				content.write("<table:table-column table:style-name=\"co" + (i+1) + "\" table:default-cell-style-name=\"Default\"/>\n");
			}
			maxColSizes = new int[colCount];

			// Write header row
			content.write("<table:table-header-rows>\n");
			content.write("  <table:table-row table:style-name=\"ro1\">\n");

			for (int i = 0; i < colCount; i++)
			{
				if (!this.includeColumnInExport(i))	continue;
				
				String colname = this.metaData.getColumnName(i);
				maxColSizes[i] = colname.length();
				
				content.write("  <table:table-cell table:style-name=\"ce1\" office:value-type=\"string\">\n");
				content.write("    <text:p>");
				content.write(colname);
				content.write("</text:p>\n");
				content.write("  </table:table-cell>\n");
			}
			content.write("  </table:table-row>\n");
			content.write("</table:table-header-rows>\n\n");
		}
		catch (IOException ex)
		{
			LogMgr.logError("OdsRowDataConverter.getStart()", "Error creating archive!", ex);
		}

		return null;
	}

	private void writeStyles(int cols)
		throws IOException
	{
		content.write("<office:automatic-styles> \n");
		for (int i=0; i < cols; i++)
		{
			int size = metaData.getColumnSize(i) * 10;
			
			content.write("<style:style style:name=\"co" + (i+1) + "\" style:family=\"table-column\"> \n");
			content.write("  <style:table-column-properties style:use-optimal-column-width=\"true\" fo:break-before=\"auto\"/> \n");
			content.write("</style:style> \n");
		}
		String styles = 
			"<style:style style:name=\"ro1\" style:family=\"table-row\"> \n" +
			"  <style:table-row-properties fo:break-before=\"auto\" style:use-optimal-row-height=\"true\"/> \n" +
			"</style:style> \n" +
			"<style:style style:name=\"ta1\" style:family=\"table\" style:master-page-name=\"Default\"> \n" +
			"  <style:table-properties table:display=\"true\" style:writing-mode=\"lr-tb\"/> \n" +
			"</style:style> \n" +
			"<style:style style:name=\"ce1\" style:family=\"table-cell\" style:parent-style-name=\"Default\"> \n" +
			"  <style:text-properties fo:font-weight=\"bold\" style:font-weight-asian=\"bold\" style:font-weight-complex=\"bold\"/> \n" +
			"</style:style> \n" +
			"</office:automatic-styles>";
		content.write(styles);
	}

	public StrBuffer getEnd(long totalRows)
	{
		try
		{
			content.write("</table:table>\n");
			content.write("</office:spreadsheet> \n");
			content.write("</office:body>\n");
			content.write("</office:document-content>\n");
			content.close();
			factory.done();
			factory = null;
		}
		catch (Exception e)
		{
		// ignore
		}
		return null;
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		int colCount = row.getColumnCount();
		StringBuilder xml = new StringBuilder(colCount * 50);
		xml.append("<table:table-row>\n");
		for (int i = 0; i < colCount; i++)
		{
			if (!this.includeColumnInExport(i))
			{
				continue;
			}
			xml.append("<table:table-cell ");
			xml.append(getCellAttribs(row.getValue(i)));
			xml.append(">\n");
			xml.append("<text:p>");
			String value = getValueAsFormattedString(row, i);
			if (value != null)
			{
				xml.append(value);
				if (value.length() > maxColSizes[i])
				{
					maxColSizes[i] = value.length();
				}
			}
			xml.append("</text:p>\n");
			xml.append("</table:table-cell>\n");
		}
		xml.append("</table:table-row>\n\n");
		try
		{
			content.write(xml.toString());
		//content.flush();
		}
		catch (IOException e)
		{
			LogMgr.logError("OdsRowDataConverter.convertRowData()", "Error writing row " + rowIndex, e);
		}
		return null;
	}

	private StringBuilder getCellAttribs(Object data)
	{

		StringBuilder attr = new StringBuilder("office:value-type=");
		if (data instanceof Number)
		{
			attr.append("\"float\" ");
			attr.append(" office:value=\"" + data.toString() + "\"");
			attr.append(" table:style-name=\"Number\"");
		}
		else if (data instanceof java.util.Date)
		{
			attr.append("\"date\" ");
			Date d = (Date)data;
			attr.append(" office:date-value=\"");
			attr.append(tsFormat.format(d));
			attr.append("\"");
			attr.append(" table:style-name=\"DateAndTime\"");
		}
		else
		{
			attr.append("\"string\"");
		}
		return attr;
	}
}
