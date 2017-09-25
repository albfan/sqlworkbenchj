/*
 * OdsRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.exporter;

import java.io.IOException;
import java.io.Writer;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;

import workbench.log.LogMgr;

import workbench.db.report.TagWriter;

import workbench.storage.RowData;

import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ZipOutputFactory;

/**
 * Convert row data to OpenDocument Spreadsheet format (OpenOffice).
 *
 * @author  Thomas Kellerer
 */
public class OdsRowDataConverter
  extends RowDataConverter
{
  private Writer content;

  // The formatters used for the interal date/time formats in the cell values
  private SimpleDateFormat tFormat = new SimpleDateFormat("HH:mm:ss");
  private SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd");
  private SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  private final String dateStyle = "ce2";
  private final String tsStyle = "ce3";
  private final String timeStyle = "ce4";

  @Override
  public StringBuilder getStart()
  {
    Writer out;
    try
    {
      if (this.factory != null)
      {
        this.factory.done();
      }
      this.factory = new ZipOutputFactory(getOutputFile());
      out = factory.createWriter("META-INF/manifest.xml", "UTF-8");

      boolean template = isTemplate();
      out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      out.write("<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\">\n");
      if (template)
      {
        out.write(" <manifest:file-entry manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet-template\" manifest:full-path=\"/\"/>\n");
      }
      else
      {
        out.write(" <manifest:file-entry manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet\" manifest:full-path=\"/\"/>\n");
      }
      out.write(" <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"content.xml\"/>\n");
      out.write(" <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"meta.xml\"/>\n");
      out.write("</manifest:manifest>\n");
      out.close();

      writeMeta();
      if (getEnableFixedHeader() && (writeHeader || includeColumnComments))
      {
        writeSettings();
      }

      out = factory.createWriter("mimetype", "UTF-8");
      if (template)
      {
        out.write("application/vnd.oasis.opendocument.spreadsheet-template");
      }
      else
      {
        out.write("application/vnd.oasis.opendocument.spreadsheet");
      }
      out.close();
      this.content = factory.createWriter("content.xml", "UTF-8");

      content.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n");
      content.write("<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" xmlns:number=\"urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0\" xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\" xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\" xmlns:chart=\"urn:oasis:names:tc:opendocument:xmlns:chart:1.0\" xmlns:dr3d=\"urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0\" xmlns:math=\"http://www.w3.org/1998/Math/MathML\" xmlns:form=\"urn:oasis:names:tc:opendocument:xmlns:form:1.0\" xmlns:script=\"urn:oasis:names:tc:opendocument:xmlns:script:1.0\" xmlns:ooo=\"http://openoffice.org/2004/office\" xmlns:ooow=\"http://openoffice.org/2004/writer\" xmlns:oooc=\"http://openoffice.org/2004/calc\" xmlns:dom=\"http://www.w3.org/2001/xml-events\" xmlns:xforms=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" office:version=\"1.0\"> \n");

      writeInlineStyles();

      content.write("<office:body>\n");
      content.write("<office:spreadsheet> \n");
      content.write("<table:table table:name=\"" + getPageTitle("Export") + "\"  table:style-name=\"ta1\">\n\n");

      int colCount = this.metaData.getColumnCount();
      for (int i=0; i < colCount; i++)
      {
        if (includeColumnInExport(i))
        {
          int type = getResultInfo().getColumnType(i);
          String defaultStyle = "Default";
          if (type == Types.DATE)
          {
            defaultStyle = dateStyle;
          }
          else if (type == Types.TIMESTAMP)
          {
            defaultStyle = tsStyle;
          }
          else if (type == Types.TIME)
          {
            defaultStyle = timeStyle;
          }
          content.write("<table:table-column table:style-name=\"co" + (i+1) + "\" table:default-cell-style-name=\"" + defaultStyle + "\"/>\n");
        }
      }

      if (includeColumnComments)
      {
        content.write("<table:table-header-rows>\n");
        content.write("  <table:table-row table:style-name=\"ro1\">\n");

        for (int i = 0; i < colCount; i++)
        {
          if (!this.includeColumnInExport(i)) continue;

          String comment = this.metaData.getColumn(i).getComment();

          content.write("  <table:table-cell table:style-name=\"ce1\" office:value-type=\"string\">\n");
          content.write("    <text:p>");
          content.write(TagWriter.CDATA_START);
          content.write(comment);
          content.write(TagWriter.CDATA_END);
          content.write("</text:p>\n");
          content.write("  </table:table-cell>\n");
        }
        content.write("  </table:table-row>\n");
        content.write("</table:table-header-rows>\n\n");
      }

      if (writeHeader)
      {
        content.write("<table:table-header-rows>\n");
        content.write("  <table:table-row table:style-name=\"ro1\">\n");

        for (int i = 0; i < colCount; i++)
        {
          if (!this.includeColumnInExport(i)) continue;

          String colname = SqlUtil.removeObjectQuotes(this.metaData.getColumnDisplayName(i));

          content.write("  <table:table-cell table:style-name=\"ce1\" office:value-type=\"string\">\n");
          content.write("    <text:p>");
          content.write(TagWriter.CDATA_START);
          content.write(colname);
          content.write(TagWriter.CDATA_END);
          content.write("</text:p>\n");
          content.write("  </table:table-cell>\n");
        }
        content.write("  </table:table-row>\n");
        content.write("</table:table-header-rows>\n\n");
      }
    }
    catch (IOException ex)
    {
      LogMgr.logError("OdsRowDataConverter.getStart()", "Error creating archive!", ex);
    }

    return null;
  }

  /**
   * Write a settings.xml that will create a vertical split that makes the table header fixed
   */
  private void writeSettings()
  {
    Writer out = null;
    try
    {
      String splitStart = (includeColumnComments ? "2" : "1");
      out = factory.createWriter("settings.xml", "UTF-8");
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      out.write("<office:document-settings xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" \n");
      out.write("    xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                "    xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\" " +
                "    xmlns:ooo=\"http://openoffice.org/2004/office\" office:version=\"1.2\">");
      out.write("  <office:settings>\n");
      out.write("    <config:config-item-set config:name=\"ooo:view-settings\">\n");
      out.write("      <config:config-item-map-indexed config:name=\"Views\">\n");
      out.write("        <config:config-item-map-entry>\n");
      out.write("          <config:config-item config:name=\"ViewId\" config:type=\"string\">View1</config:config-item>\n");
      out.write("          <config:config-item-map-named config:name=\"Tables\">\n");
      out.write("            <config:config-item-map-entry config:name=\"" + getPageTitle("Export") + "\">\n");
      out.write("              <config:config-item config:name=\"CursorPositionX\" config:type=\"int\">0</config:config-item>\n");
      out.write("              <config:config-item config:name=\"CursorPositionY\" config:type=\"int\">" + splitStart + "</config:config-item>\n");
      out.write("              <config:config-item config:name=\"VerticalSplitMode\" config:type=\"short\">2</config:config-item>\n");
      out.write("              <config:config-item config:name=\"VerticalSplitPosition\" config:type=\"int\">" + splitStart  + "</config:config-item>\n");
      out.write("              <config:config-item config:name=\"ActiveSplitRange\" config:type=\"short\">2</config:config-item>\n");
      out.write("              <config:config-item config:name=\"PositionBottom\" config:type=\"int\">" + splitStart + "</config:config-item>\n");
      out.write("            </config:config-item-map-entry>\n");
      out.write("          </config:config-item-map-named>\n");
      out.write("        </config:config-item-map-entry>\n");
      out.write("      </config:config-item-map-indexed>\n");
      out.write("    </config:config-item-set>\n");
      out.write("  </office:settings>\n");
      out.write("</office:document-settings>\n");
    }
    catch (Exception e)
    {
      LogMgr.logError("OdsRowDataConverter.writeSettings()", "Error writing settings", e);
    }
    finally
    {
      FileUtil.closeQuietely(out);
    }
  }

  private void writeMeta()
  {
    Writer out = null;
    try
    {
      out = factory.createWriter("meta.xml", "UTF-8");
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      out.write("<office:document-meta xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" xmlns:ooo=\"http://openoffice.org/2004/office\" office:version=\"1.0\">\n");
      out.write("<office:meta>\n");
      out.write("<meta:generator>SQL Workbench/J</meta:generator>\n");
      out.write("<dc:title>SQL Workbench/J Export</dc:title>\n");
      String s;
      if (this.generatingSql != null)
      {
        Matcher m = StringUtil.PATTERN_CRLF.matcher(generatingSql);
        s = m.replaceAll(" ");
      }
      else
      {
        s = "SELECT * FROM " + metaData.getUpdateTable().getTableExpression(originalConnection);
      }
      out.write("<dc:description>");
      out.write(TagWriter.CDATA_START);
      out.write(s);
      out.write(TagWriter.CDATA_END);
      out.write("</dc:description>");
      out.write("<meta:initial-creator>SQL Workbench/J</meta:initial-creator>\n");
      out.write("<meta:creation-date>");
      out.write(tsFormat.format(new Date()));
      out.write("</meta:creation-date>\n");
      out.write("</office:meta>\n");
      out.write("</office:document-meta>\n");
    }
    catch (Exception e)
    {
      LogMgr.logError("OdsRowDataConverter.writeMeta()", "Error writing meta data", e);
    }
    finally
    {
      FileUtil.closeQuietely(out);
    }
  }

  private void writeInlineStyles()
    throws IOException
  {
    content.write("<office:automatic-styles> \n");
    for (int i=0; i < metaData.getColumnCount(); i++)
    {
      //int size = metaData.getColumnSize(i) * 2;
      //style:column-width=\"" + (size)+  "pt\"
      content.write("  <style:style style:name=\"co" + (i+1) + "\" style:family=\"table-column\"> \n");
      content.write("    <style:table-column-properties style:use-optimal-column-width=\"true\"/> \n");
      content.write("  </style:style> \n");
    }

    String tsStyleDef = timestampIncluded() ? buildDateStyle(new OdsDateStyleBuilder(this.defaultTimestampFormatter.toPattern()), tsStyle, "N50") : "";
    String dateStyleDef = dateIncluded() ? buildDateStyle(new OdsDateStyleBuilder(this.defaultDateFormatter.toPattern()), dateStyle, "N60") : "";
    String timeStyleDef = timeIncluded() ? buildDateStyle(new OdsDateStyleBuilder(this.defaultTimeFormatter.toPattern()), dateStyle, "N80") : "";

    String styles =
      "  <style:style style:name=\"ro1\" style:family=\"table-row\"> \n" +
      "    <style:table-row-properties fo:break-before=\"auto\" style:use-optimal-row-height=\"true\"/> \n" +
      "  </style:style> \n" +
      "  <style:style style:name=\"ta1\" style:family=\"table\" style:master-page-name=\"Default\"> \n" +
      "    <style:table-properties table:display=\"true\" style:writing-mode=\"lr-tb\"/> \n" +
      "  </style:style> \n" +
      "  <style:style style:name=\"ce1\" style:family=\"table-cell\" style:parent-style-name=\"Default\"> \n" +
      "    <style:text-properties fo:font-weight=\"bold\" style:font-weight-asian=\"bold\" style:font-weight-complex=\"bold\"/> \n" +
      "  </style:style> \n" +
      dateStyleDef +
      tsStyleDef +
      timeStyleDef +
      "</office:automatic-styles>\n";
    content.write(styles);
  }

  private boolean timestampIncluded()
  {
    return typeStyleNeeded(Types.TIMESTAMP);
  }

  private boolean dateIncluded()
  {
    return typeStyleNeeded(Types.DATE);
  }

  private boolean timeIncluded()
  {
    return typeStyleNeeded(Types.TIME);
  }

  private boolean typeStyleNeeded(int typeToCheck)
  {
    int colCount = this.metaData.getColumnCount();
    for (int i = 0; i < colCount; i++)
    {
      if (includeColumnInExport(i))
      {
        int type = getResultInfo().getColumnType(i);
        if (type == typeToCheck) return true;
      }
    }
    return false;
  }

  private String buildDateStyle(OdsDateStyleBuilder builder, String name, String baseName)
  {
    String style = "  <number:date-style style:name=\"" + baseName + "\">\n" + builder.getXML("    ") + "  </number:date-style>\n";
    style += "  <style:style style:name=\"" + name + "\" style:family=\"table-cell\" style:parent-style-name=\"Default\" style:data-style-name=\"" + baseName + "\"/>\n";
    return style;
  }

  @Override
  public StringBuilder getEnd(long totalRows)
  {
    try
    {
      content.write("</table:table>\n");
      if (getAppendInfoSheet())
      {
        content.write("<table:table table:name=\"SQL\" table:style-name=\"ta1\">\n");
        content.append("<table:table-row>\n");
        content.append("<table:table-cell office:value-type=\"string\">");
        String[] lines = generatingSql.split(StringUtil.REGEX_CRLF);
        StringBuilder buff = new StringBuilder(generatingSql.length() + 50);
        for (String line : lines)
        {
          buff.append("<text:p>");
          writeEscapedXML(buff, line, true);
          buff.append("</text:p>\n");
        }
        content.append(buff.toString());
        content.append("</table:table-cell>");
        content.append("</table:table-row>\n");
        content.write("</table:table>");
      }

      if (getEnableAutoFilter() && writeHeader)
      {
        String colName = columnToName(getRealColumnCount());
        String title = "&apos;" + escapeXML(getPageTitle("Export"), false) + "&apos;";
        String start = "A1";
        if (includeColumnComments)
        {
          start = "A2";
        }
        content.append("<table:database-ranges>\n");
        content.append("<table:database-range table:target-range-address=\"" + title + "." + start + ":" + title + "." + colName + Long.toString(totalRows)+ "\" table:display-filter-buttons=\"true\" />\n");
        content.append("</table:database-ranges>\n");
      }

      content.write("</office:spreadsheet> \n");
      content.write("</office:body>\n");
      content.write("</office:document-content>\n");
      content.close();
      factory.done();
      factory = null;
    }
    catch (Exception e)
    {
      LogMgr.logError("OdsRowDataConverter.getEnd()", "Error writing end of worksheet", e);
    }
    return null;
  }

  protected String columnToName(int col)
  {
    StringBuilder result = new StringBuilder(3);
    int div = col;

    while (div > 0)
    {
      int remain = (div - 1) % 26;
      char c = (char)('A' + (char)remain);
      result.insert(0, c);
      div = (div - remain) / 26;
    }
    return result.toString();
  }

  @Override
  public StringBuilder convertRowData(RowData row, long rowIndex)
  {
    int colCount = row.getColumnCount();
    StringBuilder xml = new StringBuilder(colCount * 50);
    xml.append("<table:table-row>\n");
    for (int i = 0; i < colCount; i++)
    {
      if (!this.includeColumnInExport(i)) continue;
      Object o = row.getValue(i);
      String value = getValueAsFormattedString(row, i);
      if (value == null && getNullDisplay() != null)
      {
        value = getNullDisplay();
      }
      if (o == null && StringUtil.isEmptyString(value))
      {
        xml.append("<table:table-cell />");
        continue;
      }
      xml.append("<table:table-cell ");
      xml.append(getCellAttribs(o, i));
      xml.append(">\n");

      if (SqlUtil.isCharacterType(metaData.getColumnType(i)))
      {
        String[] lines = value.split(StringUtil.REGEX_CRLF);
        for (String line : lines)
        {
          xml.append("<text:p>");
          writeEscapedXML(xml, line, true);
          xml.append("</text:p>\n");
        }
      }
      else
      {
        xml.append("<text:p>");
        xml.append(value);
        xml.append("</text:p>\n");
      }
      xml.append("</table:table-cell>\n");
    }
    xml.append("</table:table-row>\n\n");
    try
    {
      content.append(xml);
    }
    catch (IOException e)
    {
      LogMgr.logError("OdsRowDataConverter.convertRowData()", "Error writing row " + rowIndex, e);
    }
    return null;
  }

  private StringBuilder getCellAttribs(Object data, int column)
  {
    StringBuilder attr = new StringBuilder("office:value-type=");
    int type = metaData.getColumnType(column);

    if (SqlUtil.isNumberType(type))
    {
      attr.append("\"float\" ");
      attr.append(" office:value=\"");
      attr.append(data.toString());
      attr.append("\"");
    }
    else if (type == Types.DATE)
    {
      attr.append("\"date\" table:style-name=\"" + dateStyle + "\" office:date-value=\"");
      if (data instanceof Date)
      {
        Date d = (Date)data;
        attr.append(dtFormat.format(d));
      }
      attr.append("\"");
    }
    else if (type == Types.TIMESTAMP)
    {
      attr.append("\"date\" table:style-name=\"" + tsStyle + "\" office:date-value=\"");
      if (data instanceof Date)
      {
        Date d = (Date)data;
        attr.append(tsFormat.format(d));
      }
      attr.append("\"");
    }
    else if (type == Types.TIME)
    {
      attr.append("\"date\" office:time-value=\"");
      if (data instanceof Date)
      {
        Date d = (Date)data;
        attr.append(tFormat.format(d));
      }
      attr.append("\"");
    }
    else
    {
      attr.append("\"string\"");
    }

    return attr;
  }

  public boolean isTemplate()
  {
    return hasOutputFileExtension("ots");
  }

}
