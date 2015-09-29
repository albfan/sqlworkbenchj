/*
 * XmlRowDataConverter.java
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

import java.io.File;
import java.sql.SQLException;
import java.sql.Types;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionInfoBuilder;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportTable;
import workbench.db.report.TagWriter;

import workbench.storage.RowData;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Convert row data to our own XML format.
 *
 * @author  Thomas Kellerer
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
	public static final String TAG_BLOB_ENCODING = "wb-blob-encoding";

	private boolean useCData;
	private boolean verboseFormat = true;
	private String lineEnding = "\n";
	private String coltag = LONG_COLUMN_TAG;
	private String rowtag = LONG_ROW_TAG;
	private String numAttrib = "row-num";
	private String startColTag = "  <" + coltag + " index=\"";
	private String closeColTag = "</" + coltag + ">";
	private String closeRowTag = "</" + rowtag + ">";
	private String tableToUse;
	private StringBuilder dbInfo;
	private boolean writeClobFiles;
	private boolean addColName;
	private String xmlVersion = Settings.getInstance().getDefaultXmlVersion();
	private boolean useDiffFormat;
	private boolean writeBlobFiles = true;

	public XmlRowDataConverter()
	{
		super();
		this.addColName = Settings.getInstance().getBoolProperty("workbench.export.xml.verbose.includecolname", false);
	}

	public void setUseDiffFormat(boolean flag)
	{
		useDiffFormat = flag;
		if (flag)
		{
			coltag = "col";
			rowtag = "row";
			startColTag = "<col";
			closeColTag = "</col>";
			closeRowTag = "</row>";
		}
		else
		{
			// re-initialize the tags
			this.setUseVerboseFormat(this.verboseFormat);
		}
	}

	public void setTableNameToUse(String name)
	{
		this.tableToUse = name;
	}

	public void setWriteBlobToFile(boolean flag)
	{
		writeBlobFiles = flag;
	}

	public void setWriteClobToFile(boolean flag)
	{
		this.writeClobFiles = flag;
	}

	@Override
	public void setOriginalConnection(WbConnection con)
	{
		super.setOriginalConnection(con);
		// This should be done before running the actual export
		// in order to avoid concurrent statement execution during export.

		// getDatabaseInfoAsXml() indirectly runs some statements because
		// it retrieves user and schema information from the database
		// This method is called during initialization of the DataExporter
		// and before the actual export is started.
		if (con != null)
		{
			StringBuilder indent = new StringBuilder("    ");
			ConnectionInfoBuilder builder = new ConnectionInfoBuilder();
			this.dbInfo = builder.getDatabaseInfoAsXml(con, indent);
		}
	}

	public void setXMLVersion(String version)
	{
		if ("1.1".equals(version) || "1.0".equals(version))
		{
			this.xmlVersion = version;
		}
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

	@Override
	public StringBuilder getStart()
	{
		StringBuilder xml = new StringBuilder(250);
		String enc = this.getEncoding();
		xml.append("<?xml version=\"" + xmlVersion + "\"");
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

	@Override
	public StringBuilder getEnd(long totalRows)
	{
		StringBuilder xml = new StringBuilder(100);
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

	public void setLineEnding(String ending)
	{
		if (ending != null) this.lineEnding = ending;
	}

	@Override
	public StringBuilder convertRowData(RowData row, long rowIndex)
	{
		TagWriter tagWriter = new TagWriter();
		StringBuilder indent = new StringBuilder("    ");
		int colCount = this.metaData.getColumnCount();
		StringBuilder xml = new StringBuilder(colCount * 100);

		// No row tag for diff
		if (!useDiffFormat)
		{
			if (this.verboseFormat)
			{
				tagWriter.appendOpenTag(xml, indent, rowtag, numAttrib, Long.toString(rowIndex + 1));
			}
			else
			{
				tagWriter.appendOpenTag(xml, null, rowtag);
			}
			if (verboseFormat) xml.append(this.lineEnding);
		}

		for (int c=0; c < colCount; c ++)
		{
			if (!this.includeColumnInExport(c)) continue;

			Object data = row.getValue(c);
			int type = this.metaData.getColumnType(c);
			String dbmsType = metaData.getDbmsTypeName(c);
			boolean isNull = (data == null);
			boolean writeCloseTag = true;

			boolean externalFile = false;

			if (!useDiffFormat && this.verboseFormat) xml.append(indent);
			xml.append(startColTag);
			if (this.verboseFormat)
			{
				xml.append(c);
				xml.append('"');
			}

			if (addColName || useDiffFormat)
			{
				xml.append(" name=\"" + metaData.getColumnName(c) + "\"");
			}

			if (useDiffFormat)
			{
        if (metaData.getColumn(c).isPkColumn())
        {
          xml.append(" pk=\"true\"");
        }
        if (row.isColumnModified(c))
        {
          xml.append(" modified=\"true\"");
        }
			}

			if (isNull)
			{
				xml.append(" null=\"true\"/");
				writeCloseTag = false;
			}
			else
			{
				if (SqlUtil.isDateType(type))
				{
					java.util.Date d = (java.util.Date)data;
					xml.append(" longValue=\"");
					xml.append(Long.toString(d.getTime()));
					xml.append('"');
				}
				else if (writeClobFiles && SqlUtil.isClobType(type, dbmsType, originalConnection.getDbSettings()))
				{
					externalFile = true;
					try
					{
						File clobFile = createBlobFile(row, c, rowIndex);
						String dataFile = getBlobFileValue(clobFile);
						writeClobFile((String)data, clobFile, this.encoding);
						xml.append(' ');
						xml.append(ATTR_DATA_FILE);
						xml.append("=\"");
						xml.append(dataFile);
						xml.append("\"/");
						writeCloseTag = false;
					}
					catch (Exception e)
					{
						throw new RuntimeException("Error writing CLOB file", e);
					}
				}
				else if (SqlUtil.isBlobType(type))
				{
					if (writeBlobFiles)
					{
						externalFile = true;
						try
						{
							File blobFile = createBlobFile(row, c, rowIndex);
							String dataFile = getBlobFileValue(blobFile);
							writeBlobFile(data, blobFile);
							xml.append(' ');
							xml.append(ATTR_DATA_FILE);
							xml.append("=\"");
							xml.append(dataFile);
							xml.append("\"/");
							writeCloseTag = false;
						}
						catch (Exception e)
						{
							throw new RuntimeException("Error writing BLOB file", e);
						}
					}
					else if (blobFormatter != null)
					{
						externalFile = false;
					}
				}
			}
			xml.append('>');

			// Only write the tag content if the value is not null (we have already closed the tag)
			// or if the value is not a blob value (which has already been written!)
			if (!isNull && !externalFile)
			{
				if (this.useCData && SqlUtil.isCharacterType(type))
				{
					// CDATA should only be used for character data types
					xml.append(TagWriter.CDATA_START);
					xml.append(this.getValueAsFormattedString(row, c));
					xml.append(TagWriter.CDATA_END);
				}
				else
				{
					writeEscapedXML(xml, this.getValueAsFormattedString(row, c), true);
				}
			}
			if (writeCloseTag) xml.append(closeColTag);
			if (this.verboseFormat && !useDiffFormat) xml.append(this.lineEnding);
		}

		if (!useDiffFormat)
		{
			if (this.verboseFormat) xml.append(indent);
			xml.append(closeRowTag);
			xml.append(this.lineEnding);
		}
		return xml;
	}

	private StringBuilder getMetaDataAsXml(String anIndent)
	{
		TagWriter tagWriter = new TagWriter();
		StringBuilder indent = new StringBuilder(anIndent);
		StringBuilder indent2 = new StringBuilder(anIndent);
		indent2.append("  ");

		int colCount = this.metaData.getColumnCount();
		StringBuilder result = new StringBuilder(colCount * 50);
		tagWriter.appendOpenTag(result, indent, "meta-data");
		result.append(this.lineEnding);

		if (this.generatingSql != null)
		{
			result.append(this.lineEnding);
			tagWriter.appendOpenTag(result, indent2, "generating-sql");
			result.append(this.lineEnding);
			result.append(indent2);
			result.append(TagWriter.CDATA_START);
			result.append(this.lineEnding);
			result.append(indent2);
			result.append(this.generatingSql);
			result.append(this.lineEnding);
			result.append(indent2);
			result.append(TagWriter.CDATA_END);
			result.append(this.lineEnding);
			tagWriter.appendCloseTag(result, indent2, "generating-sql");
			result.append(this.lineEnding);
		}

		if (this.dbInfo != null)
		{
			result.append(this.dbInfo);
		}

		//result.append(this.lineEnding);
		result.append(indent2);
		result.append("<" + TAG_TAG_FORMAT + ">");
		result.append(this.verboseFormat ? KEY_FORMAT_LONG : KEY_FORMAT_SHORT);
		result.append("</" + TAG_TAG_FORMAT + ">");
		result.append(this.lineEnding);
		if (blobFormatter != null)
		{
			result.append(indent2);
			result.append("<" + TAG_BLOB_ENCODING + ">");
			result.append(blobFormatter.getType().toString());
			result.append("</" + TAG_BLOB_ENCODING + ">");
			result.append(this.lineEnding);
		}

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

		TableDefinition tableDef = null;
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
				try
				{
					tableDef = originalConnection.getMetadata().getTableDefinition(table);
				}
				catch (SQLException sql)
				{
					LogMgr.logError("XmlRowDataConverter.getMetaDataAsXml()", "Error retrieving table definition", sql);
				}
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

			String colname = this.metaData.getColumnName(i);

			result.append(indent);
			appendTag(result, "    ", COLUMN_NAME_TAG, SqlUtil.removeObjectQuotes(colname));

			String label = this.metaData.getColumnDisplayName(i);
			if (!label.equals(colname))
			{
				result.append(indent);
				appendTag(result, "    ", "column-label", SqlUtil.removeObjectQuotes(label));
			}

			String comment = this.metaData.getColumn(i).getComment();
			if (StringUtil.isNonBlank(comment))
			{
				result.append(indent);
				appendTag(result, "    ", ReportColumn.TAG_COLUMN_COMMENT, comment);
			}
			result.append(indent);
			appendTag(result, "    ", JAVA_CLASS_TAG, getReadableClassName(this.metaData.getColumnClassName(i)));

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
				}
				else
				{
					result.append(indent);
					result.append("    <data-format>");
					result.append(defaultDateFormatter.toPattern());
				}
				result.append("</data-format>");
				result.append(this.lineEnding);
			}

			if (tableDef != null)
			{
				ColumnIdentifier realCol = tableDef.findColumn(metaData.getColumnName(i));
				if (realCol != null)
				{
					result.append(indent);
					appendTag(result, "    ", ReportColumn.TAG_COLUMN_PK, Boolean.toString(realCol.isPkColumn()));
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

	private String getReadableClassName(String cls)
	{
		if (cls.charAt(0) != '[') return cls;

		String displayName = cls;
		if (cls.charAt(0) == '[')
		{
			if (cls.charAt(1) == 'B') displayName = "byte[]";
			else if (cls.charAt(1) == 'C') displayName = "char[]";
			else if (cls.charAt(1) == 'I') displayName = "int[]";
			else if (cls.charAt(1) == 'J') displayName = "long[]";
			else if (cls.charAt(1) == 'L')
			{
				// a "class" starting with [L is a "real" Object not
				// a native data type, so we'll extract the real class
				// name, and make that array of that class
				displayName = cls.substring(2, cls.length() - 1) + "[]";
			}
		}
		return displayName;
	}

	private void appendOpenTag(StringBuilder target, String indent, String tag)
	{
		target.append(indent);
		target.append('<');
		target.append(tag);
		target.append('>');
	}

	private void appendCloseTag(StringBuilder target, String tag)
	{
		target.append("</");
		target.append(tag);
		target.append('>');
	}

	private void appendTag(StringBuilder target, String indent, String tag, String value)
	{
		appendOpenTag(target, indent, tag);
		if (TagWriter.needsCData(value))
		{
			target.append(TagWriter.CDATA_START);
		}
		target.append(value);
		if (TagWriter.needsCData(value))
		{
			target.append(TagWriter.CDATA_END);
		}
		appendCloseTag(target, tag);
		target.append(this.lineEnding);
	}

}
