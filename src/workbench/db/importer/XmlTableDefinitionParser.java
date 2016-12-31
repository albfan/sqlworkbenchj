/*
 * XmlTableDefinitionParser.java
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
package workbench.db.importer;

import java.io.IOException;
import java.io.Reader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import workbench.db.ColumnIdentifier;
import workbench.db.exporter.XmlRowDataConverter;
import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 * A parser to read the table definition from a Workbench XML file.
 * @author  Thomas Kellerer
 */
public class XmlTableDefinitionParser
  extends DefaultHandler
{
  private int currentColIndex;
  private ColumnIdentifier[] columnList;
  private String tableName;
  private ImportFileHandler fileHandler;
  private StringBuilder chars;
  private String tagFormat;
  private String blobEncoding;

  public XmlTableDefinitionParser(ImportFileHandler handler)
    throws IOException, SAXException
  {
    super();
    this.fileHandler = handler;
    this.parseTableStructure();
  }

  public ColumnIdentifier[] getColumns()
  {
    return this.columnList;
  }

  public String getBlobEncoding()
  {
    return blobEncoding;
  }

  public String getTagFormat()
  {
    return this.tagFormat;
  }

  public String getTableName()
  {
    return this.tableName;
  }

  private void parseTableStructure()
    throws IOException, SAXException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    Reader in = null;
    try
    {
      SAXParser saxParser = factory.newSAXParser();
      in = this.fileHandler.getMainFileReader();
      InputSource source = new InputSource(in);
      saxParser.parse(source, this);
    }
    catch (ParserConfigurationException | ParsingEndedException ce)
    {
      // should not happen
    }
    catch (IOException e)
    {
      throw e;
    }
    catch (SAXException e)
    {
      LogMgr.logError("XmlDataTableParser.parseTableStructure()", "Error reading table structure", e);
      this.columnList = null;
      this.tableName = null;
      throw e;
    }
    finally
    {
      try { fileHandler.done(); } catch (Throwable th) {}
    }
  }

  @Override
  public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
    throws SAXException
  {
    this.chars = new StringBuilder();
    if (qName.equals(XmlRowDataConverter.COLUMN_DEF_TAG))
    {
      this.columnList[this.currentColIndex] = new ColumnIdentifier();
    }
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName)
    throws SAXException
  {
    if (qName.equals(XmlRowDataConverter.TAG_TAG_FORMAT))
    {
      this.tagFormat = this.chars.toString();
    }
    else if (qName.equals(XmlRowDataConverter.TAG_BLOB_ENCODING))
    {
      this.blobEncoding = chars.toString();
    }
    else if (qName.equals(XmlRowDataConverter.COLUMN_COUNT_TAG))
    {
      try
      {
        int count = StringUtil.getIntValue(this.chars.toString(), -1);
        this.columnList = new ColumnIdentifier[count];
        this.currentColIndex = 0;
      }
      catch (Exception e)
      {
        LogMgr.logError("XmlTableDefinitionParser.endElement", "Incorrec value for " + XmlRowDataConverter.COLUMN_COUNT_TAG + ": " + this.chars, e);
        throw new SAXException("Invalid column count", e);
      }
    }
    else if (qName.equals(XmlRowDataConverter.COLUMN_DEF_TAG))
    {
      currentColIndex ++;
    }
    else if (qName.equals(XmlRowDataConverter.TABLE_NAME_TAG))
    {
      this.tableName = this.chars.toString();
    }
    else if (qName.equals(XmlRowDataConverter.COLUMN_NAME_TAG))
    {
      this.columnList[this.currentColIndex].setColumnName(this.chars.toString());
    }
    else if (qName.equals(XmlRowDataConverter.JAVA_TYPE_TAG))
    {
      try
      {
        int type = Integer.parseInt(this.chars.toString());
        this.columnList[currentColIndex].setDataType(type);
      }
      catch (Exception e)
      {
        LogMgr.logError("XmlTableDefinitionParser.endElement()", "Could not read columnn type!", e);
        throw new SAXException("Could not read columnn type", e);
      }
    }
    else if (qName.equals("dbms-data-type"))
    {
      try
      {
        this.columnList[currentColIndex].setDbmsType(this.chars.toString());
      }
      catch (Exception e)
      {
        LogMgr.logError("XmlDataFileParser.endElement()", "Could not read dbms columnn type!", e);
        throw new SAXException("Could not read dbms columnn type", e);
      }
    }
    else if (qName.equals(XmlRowDataConverter.JAVA_CLASS_TAG))
    {
      try
      {
        this.columnList[currentColIndex].setColumnClassName(this.chars.toString());
      }
      catch (Exception e)
      {
        LogMgr.logError("XmlTableDefinitionParser.endElement()", "Could not read columnn class name!", e);
        throw new SAXException("Could not read columnn name", e);
      }
    }
    else if (qName.equals(XmlRowDataConverter.TABLE_DEF_TAG))
    {
      throw new ParsingEndedException();
    }
  }

  @Override
  public void characters(char[] buf, int offset, int len)
    throws SAXException
  {
    if (chars != null)
    {
      this.chars.append(buf, offset, len);
    }
  }

}
