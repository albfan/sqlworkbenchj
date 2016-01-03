/*
 * XmlDataFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import workbench.interfaces.ImportFileParser;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.BlobMode;
import workbench.db.exporter.XmlRowDataConverter;

import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author  Thomas Kellerer
 */
public class XmlDataFileParser
	extends AbstractImportFileParser
	implements ImportFileParser
{
	private String tableNameFromFile;

	private int currentRowNumber = 1;
	private int colCount;
	private int realColCount;

	private ColumnIdentifier[] columns;

	private Object[] currentRow;

	private boolean[] warningAdded;
	private boolean verboseFormat = true;
	private boolean formatKnown = false;
	private String missingColumn;
	private boolean ignoreCurrentRow;

	private int currentColIndex = 0;
	private int realColIndex = 0;
	private long columnLongValue = 0;
	private String columnDataFile = null;
	private boolean isNull = false;
	private StringBuilder chars;
	private String rowTag = XmlRowDataConverter.LONG_ROW_TAG;
	private String columnTag = XmlRowDataConverter.LONG_COLUMN_TAG;

	private SAXParser saxParser;

	private DefaultHandler handler = new SaxHandler();

  public XmlDataFileParser()
  {
		super();
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    try
    {
      saxParser = factory.newSAXParser();
    }
    catch (Exception e)
    {
      // should not happen!
      LogMgr.logError("XmlDataFileParser.<init>", "Error creating XML parser", e);
    }
  }

	public XmlDataFileParser(File file)
	{
		this();
		this.inputFile = file;
	}

	@Override
	public String getColumns()
	{
		return StringUtil.listToString(this.importColumns, ',', false);
	}

	@Override
	public void addColumnFilter(String colname, String regex)
	{
		// not yet supported
	}

	@Override
	public String getLastRecord()
	{
		return null;
	}

	@Override
	public Map<Integer, Object> getInputColumnValues(Collection<Integer> inputFileIndexes)
	{
		return null;
	}

	@Override
	protected TablenameResolver getTableNameResolver()
	{
		return new XmlTableNameResolver(getEncoding());
	}

	@Override
	public boolean hasWarnings()
	{
		if (super.hasWarnings()) return true;
		if (this.warningAdded == null) return false;
		for (boolean b : warningAdded)
		{
			if (b) return true;
		}
		return false;
	}

	public void setColumns(String columnList)
		throws SQLException
	{
		if (StringUtil.isNonBlank(columnList))
		{
			WbStringTokenizer tok = new WbStringTokenizer(columnList, ",");
			importColumns = ImportFileColumn.createList();
			while (tok.hasMoreTokens())
			{
				String col = tok.nextToken();
				if (col == null) continue;
				col = col.trim();
				if (col.length() == 0) continue;
				ColumnIdentifier ci = new ColumnIdentifier(col);
        importColumns.add(new ImportFileColumn(ci));
			}
		}
		else
		{
			importColumns = null;
		}
		checkImportColumns();
	}

	/**
	 * Define the columns to be imported
	 */
	@Override
	public void setColumns(List<ColumnIdentifier> cols)
		throws SQLException
	{
		if (cols != null && cols.size() > 0)
		{
			importColumns = ImportFileColumn.createList();
			Iterator<ColumnIdentifier> itr = cols.iterator();
			while (itr.hasNext())
			{
				ColumnIdentifier id = itr.next();
				if (!id.getColumnName().equals(RowDataProducer.SKIP_INDICATOR))
				{
          importColumns.add(new ImportFileColumn(id));
				}
			}
		}
		else
		{
			this.importColumns = null;
		}
		checkImportColumns();
	}

	/**
	 * Check if all columns defined for the import (through the table definition
	 * as part of the XML file, or passed by the user on the command line) are
	 * actually available in the target table.
	 * For this all columns of the target table are retrieved from the database,
	 * and each column that has been defined through setColumns() is checked
	 * whether it exists there. Columns that are not found are dropped from
	 * the list of import columns
	 * If continueOnError == true, a warning is added to the messages. Otherwise
	 * an Exception is thrown.
	 */
	public void checkTargetColumns(TableIdentifier tbl)
		throws SQLException
	{
		if (this.connection == null) return;
		if (this.columns == null) return;
		if (tbl == null) return;

		if (!this.connection.getMetadata().tableExists(tbl))
		{
			if (this.receiver.getCreateTarget())
			{
				LogMgr.logDebug("XmlDataFileParser.checkTargetColumns()", "Table " + tbl.getTableName() + " not found, but receiver will create it. Skipping column check...");
				return;
			}
			else
			{
				String msg = ResourceMgr.getFormattedString("ErrTargetTableNotFound", tbl.getTableName());
				this.messages.append(msg);
				this.messages.appendNewLine();
				throw new SQLException("Table '" + tbl.getTableName() + "' not found!");
			}
		}
		List<ColumnIdentifier> tableCols = this.connection.getMetadata().getTableColumns(tbl);
		List<ImportFileColumn> validCols = ImportFileColumn.createList();

		for (int colIndex=0; colIndex < this.columns.length; colIndex++)
		{
			int i = tableCols.indexOf(this.columns[colIndex]);

			if (i != -1)
			{
				// Use the column definition retrieved from the database
				// to make sure we are using the correct data types when converting the input (String) values
				// this is also important to get quoting of column names
				// with special characters correctly (as this is handled by DbMetadata already
				// but the columns retrieved from the XML file are not quoted correctly)
				ColumnIdentifier tc = tableCols.get(i);
				this.columns[colIndex] = tc;
        validCols.add(new ImportFileColumn(tc));
			}
			else
			{
				String errorColumn = (this.columns[colIndex] != null ? this.columns[colIndex].getColumnName() : "n/a");
				String msg = ResourceMgr.getFormattedString("ErrImportColumnNotFound", errorColumn, tbl.getTableExpression());
				this.messages.append(msg);
				this.messages.appendNewLine();
				if (this.abortOnError)
				{
					this.hasErrors = true;
					throw new SQLException("Column " + errorColumn + " not found in target table");
				}
				else
				{
					this.hasWarnings = true;
					LogMgr.logWarning("XmlDataFileParser.checkTargetColumns()", msg);
				}
			}
		}

		// Make sure we are using the columns collected during the check
		if (validCols.size() != columns.length)
		{
			this.importColumns = validCols;
			this.realColCount = this.importColumns.size();
		}
	}

	private void checkImportColumns()
		throws SQLException
	{
		if (importColumns == null)
		{
			this.realColCount = this.colCount;
			return;
		}

		this.missingColumn = null;

		try
		{
			if (this.columns == null) this.readXmlTableDefinition();
		}
		catch (Throwable e)
		{
			LogMgr.logError("XmlDataFileParser.checkImportColumns()", "Error reading table definition from XML file", e);
			this.hasErrors = true;
			throw new SQLException("Could not read table definition from XML file");
		}

		Iterator<ImportFileColumn> cols = importColumns.iterator();
		while (cols.hasNext())
		{
			ColumnIdentifier c = cols.next().getColumn();
			if (!this.containsColumn(c))
			{
				if (ignoreMissingColumns || !abortOnError)
				{
					String msg = ResourceMgr.getFormattedString("ErrImportColumnIgnored", c.getColumnName(), this.tableName);
					LogMgr.logWarning("XmlDataFileParser.checkImportColumns()", "Ignoring table column " + c.getColumnName() + " because it is not present in the input file");
					this.hasWarnings = true;
					if (!ignoreMissingColumns) this.hasErrors = true;
					this.messages.append(msg);
					cols.remove();
				}
				else
				{
					this.missingColumn = c.getColumnName();
					this.hasErrors = true;
					throw new SQLException("The column " + c.getColumnName() + " from the table " + this.tableName + " is not present in input file!");
				}
			}
		}
		this.realColCount = this.importColumns.size();
	}

	/**
	 *	Returns the first column from the import columns
	 *  that is not found in the import file
	 *	@see #setColumns(String)
	 *  @see #setColumns(List)
	 */
	public String getMissingColumn()
	{
		return this.missingColumn;
	}

	private boolean containsColumn(ColumnIdentifier col)
	{
		if (this.columns == null) return false;
		for (ColumnIdentifier column : this.columns)
		{
			if (column.equals(col))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public List<ColumnIdentifier> getColumnsFromFile()
	{
		try
		{
			if (this.columns == null) this.readXmlTableDefinition();
		}
		catch (IOException | SAXException e)
		{
			return Collections.emptyList();
		}
		ArrayList<ColumnIdentifier> result = new ArrayList<>(this.columns.length);
		result.addAll(Arrays.asList(this.columns));
		return result;
	}

	private void detectBlobEncoding()
	{
		try
		{
			fileHandler.setMainFile(this.inputFile, getEncoding());
			XmlTableDefinitionParser tableDef = new XmlTableDefinitionParser(this.fileHandler);
			String mode = tableDef.getBlobEncoding();
			if (StringUtil.isNonBlank(mode))
			{
				BlobMode bmode = BlobMode.getMode(mode);
				blobDecoder.setBlobMode(bmode);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("XmlDataFileParser", "Could not detect XML tag format. Assuming 'verbose'", e);
			this.setUseVerboseFormat(true);
		}
	}

	@Override
	public String getEncoding()
	{
		return (StringUtil.isEmptyString(this.encoding) ? "UTF-8" : this.encoding);
	}

	private void detectTagFormat()
	{
		try
		{
			fileHandler.setMainFile(this.inputFile, getEncoding());
			XmlTableDefinitionParser tableDef = new XmlTableDefinitionParser(this.fileHandler);
			detectTagFormat(tableDef);
		}
		catch (Exception e)
		{
			LogMgr.logError("XmlDataFileParser", "Could not detect XML tag format. Assuming 'verbose'", e);
			this.setUseVerboseFormat(true);
		}
	}

	private void detectTagFormat(XmlTableDefinitionParser tableDef)
	{
		String format = tableDef.getTagFormat();
		if (format != null)
		{
			if (XmlRowDataConverter.KEY_FORMAT_LONG.equals(format))
			{
				this.setUseVerboseFormat(true);
			}
			else if (XmlRowDataConverter.KEY_FORMAT_SHORT.equals(format))
			{
				this.setUseVerboseFormat(false);
			}
		}
	}

	private void readXmlTableDefinition()
		throws IOException, SAXException
	{
		fileHandler.setMainFile(this.inputFile, getEncoding());

		XmlTableDefinitionParser tableDef = new XmlTableDefinitionParser(this.fileHandler);
		this.columns = tableDef.getColumns();
		this.colCount = this.columns.length;
		this.tableNameFromFile = tableDef.getTableName();
		this.warningAdded = new boolean[this.colCount];
		detectTagFormat(tableDef);
	}

	@Override
	protected void processOneFile()
		throws Exception
	{
		// readTableDefinition relies on the fileHandler, so this
		// has to be called after initializing the fileHandler
		if (this.columns == null)
		{
			this.readXmlTableDefinition();
		}

		if (!this.formatKnown)
		{
			detectTagFormat();
		}
		detectBlobEncoding();

		if (this.importColumns == null)
		{
			this.realColCount = this.colCount;
		}
		else
		{
			this.realColCount = this.importColumns.size();
		}

		// Re-initialize the reader in case we are reading from a ZIP archive
		// because readTableDefinition() can change the file handler
		this.fileHandler.setMainFile(this.inputFile, getEncoding());

		blobDecoder.setBaseDir(inputFile.getParentFile());

		if (!sharedMessages) this.messages = new MessageBuffer();
		this.sendTableDefinition();
		Reader in = null;

		try
		{
			in = this.fileHandler.getMainFileReader();
			InputSource source = new InputSource(in);
			saxParser.parse(source, handler);
			filesProcessed.add(inputFile);
			this.receiver.tableImportFinished();
		}
		catch (ParsingInterruptedException e)
		{
			if (this.regularStop)
			{
				this.receiver.tableImportFinished();
			}
			else
			{
				this.hasErrors = true;
			}
		}
		catch (ParsingConverterException pce)
		{
			// already logged and added to the messages
			this.receiver.tableImportError();
			this.hasErrors = true;
			throw pce;
		}
		catch (Exception e)
		{
		  String msg = "Error during parsing of data row: " + (this.currentRowNumber) +
				  ", column: " + this.currentColIndex +
				  ", current data: " + (this.chars == null ? "<n/a>" : "[" + this.chars.toString() + "]" ) +
				  ", message: " + ExceptionUtil.getDisplay(e);
			LogMgr.logWarning("XmlDataFileParser.processOneFile()", msg);
			this.hasErrors = true;
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.receiver.tableImportError();
			throw e;
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
	}

	@Override
	protected void resetForFile()
	{
		super.resetForFile();
		tableNameFromFile = null;
		ignoreCurrentRow = false;
		currentColIndex = 0;
		realColIndex = 0;
		columnLongValue = 0;
		isNull = false;
		chars = null;
		columns = null;
		importColumns = null;
	}

	private void clearRowData()
	{
		for (int i=0; i < this.realColCount; i++)
		{
			this.currentRow[i] = null;
		}
		this.currentColIndex = 0;
		this.realColIndex = 0;
	}

	/**
	 *	Creates the approriate column data object and puts it
	 *	into rowData[currentColIndex]
	 *  {@link workbench.util.ValueConverter} is not used because
	 *  for most of the datatypes we have some special processing here
	 *  Date and time can be initialized through the long value in the XML file
	 *  Numeric types contain the actual class to be used
	 */
	private void buildColumnData()
		throws ParsingConverterException
	{
		if (importColumns != null && getColumnIndex(this.columns[currentColIndex].getColumnName()) < 0) return;
		this.currentRow[this.realColIndex] = null;

		if (!this.receiver.shouldProcessNextRow()) return;

		// the isNull flag will be set by the startElement method
		// as that is an attribute of the tag
		if (this.isNull)
		{
			this.realColIndex ++;
			return;
		}

		int type = this.columns[this.realColIndex].getDataType();

		String value = this.chars.toString();
    if (trimValues && !SqlUtil.isBlobType(type))
    {
      value = value.trim();
    }

		if (this.valueModifier != null)
		{
			value = this.valueModifier.modifyValue(this.columns[this.realColIndex], value);
		}

		try
		{
			if (SqlUtil.isCharacterType(type))
			{
				// if clobs are exported as external files, than we'll have a filename in the
				// attribute (just like with BLOBS)
				if (this.columnDataFile == null)
				{
					this.currentRow[this.realColIndex] = value;
				}
				else
				{
					String fileDir = this.inputFile.getParent();
					this.currentRow[this.realColIndex] = new File(fileDir, columnDataFile);
				}
			}
			else if (SqlUtil.isBlobType(type))
			{
				if (columnDataFile != null)
				{
					this.currentRow[this.realColIndex] = blobDecoder.decodeBlob(columnDataFile);
				}
				else
				{
					this.currentRow[this.realColIndex] = blobDecoder.decodeBlob(value);
				}
			}
			else if (SqlUtil.isDateType(type))
			{
				// For Date types we don't need the ValueConverter as already we
				// have a suitable long value that doesn't need parsing
				java.sql.Date d = new java.sql.Date(this.columnLongValue);
				if (type == Types.TIMESTAMP)
				{
					this.currentRow[this.realColIndex] = new java.sql.Timestamp(d.getTime());
				}
				else
				{
					this.currentRow[this.realColIndex] = d;
				}
			}
			else
			{
				// for all other types we can use the ValueConverter
				this.currentRow[this.realColIndex] = converter.convertValue(value, type);
			}
		}
		catch (Exception e)
		{
			String msg = ResourceMgr.getString("ErrConvertError");
			msg = StringUtil.replace(msg, "%type%", SqlUtil.getTypeName(this.columns[realColIndex].getDataType()));
			msg = StringUtil.replace(msg, "%column%", this.columns[realColIndex].getColumnName());
			msg = StringUtil.replace(msg, "%error%", e.getMessage());
			msg = StringUtil.replace(msg, "%value%", value);
			msg = StringUtil.replace(msg, "%row%", Integer.toString(this.currentRowNumber));

			this.messages.append(msg);
			this.messages.appendNewLine();

			if (this.abortOnError)
			{
				LogMgr.logError("XmlDataFileParser.buildColumnData()", msg, e);
				this.hasErrors = true;
				throw new ParsingConverterException();
			}
			else
			{
				this.messages.append(ResourceMgr.getString("ErrConvertWarning"));
				this.hasWarnings = true;
				LogMgr.logWarning("XmlDataFileParser.buildColumnData()", msg, null);
			}
		}

		this.realColIndex ++;
	}

	private TableIdentifier getImportTable()
	{
		String tname = (this.tableName == null ? tableNameFromFile : tableName);

		// this is possible when importing into a DataStore
		if (StringUtil.isEmptyString(tname)) return null;

		TableIdentifier id = new TableIdentifier(tname);

		// this is possible when importing into a DataStore
		if (this.connection == null) return id;

		TableIdentifier tbl = this.connection.getMetadata().findTable(id);
		if (tbl == null)
		{
			return id;
		}
		return tbl;
	}

	private void sendTableDefinition()
		throws SQLException
	{
		try
		{
			TableIdentifier tbl = getImportTable();

			checkTargetColumns(tbl);
			if (this.importColumns == null)
			{
				this.receiver.setTargetTable(tbl, Arrays.asList(this.columns));
			}
			else
			{
				List<ColumnIdentifier> cols = new ArrayList<>(this.realColCount);
				for (int i=0; i < this.colCount; i++)
				{
					if (getColumnIndex(this.columns[i].getColumnName()) > -1)
					{
						cols.add(this.columns[i]);
					}
				}
				this.receiver.setTargetTable(tbl, cols);
			}
			this.currentRow = new Object[this.realColCount];
		}
		catch (SQLException e)
		{
			this.currentRow = null;
			this.hasErrors = true;
			throw e;
		}
	}

  private boolean includeCurrentRow()
  {
    for (int colIndex = 0; colIndex < currentRow.length; colIndex ++)
    {
      Object value = currentRow[colIndex];
      if (value != null)
      {
        String svalue = value.toString();
        if (isColumnFiltered(colIndex, svalue))
        {
          return false;
        }
      }
    }
    return true;
  }

	private void sendRowData()
		throws SAXException, Exception
	{
		if (this.receiver != null)
		{
			try
			{
				if (includeCurrentRow())
        {
          this.receiver.processRow(this.currentRow);
        }
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.sendRowData()", "Error when sending row data to receiver", e);
				if (this.abortOnError)
				{
					this.hasErrors = true;
					throw e;
				}
				this.hasWarnings = true;
				if (this.errorHandler != null)
				{
					int choice = errorHandler.getActionOnError(this.currentRowNumber + 1, null, null, ExceptionUtil.getDisplay(e, false));
					if (choice == JobErrorHandler.JOB_ABORT) throw e;
					if (choice == JobErrorHandler.JOB_IGNORE_ALL)
					{
						this.abortOnError = false;
					}
				}

			}
		}
		if (this.cancelImport) throw new ParsingInterruptedException();
	}

	private void setUseVerboseFormat(boolean flag)
	{
		this.formatKnown = true;
		this.verboseFormat = flag;
		if (this.verboseFormat)
		{
			rowTag = XmlRowDataConverter.LONG_ROW_TAG;
			columnTag = XmlRowDataConverter.LONG_COLUMN_TAG;
		}
		else
		{
			rowTag = XmlRowDataConverter.SHORT_ROW_TAG;
			columnTag = XmlRowDataConverter.SHORT_COLUMN_TAG;
		}
	}

	private class SaxHandler
		extends DefaultHandler
	{
		private SaxHandler()
		{
			super();
		}

		@Override
		public void startDocument()
			throws SAXException
		{
			Thread.yield();
			if (cancelImport) throw new ParsingInterruptedException();
		}

		@Override
		public void endDocument()
			throws SAXException
		{
			Thread.yield();
			if (cancelImport)
			{
				throw new ParsingInterruptedException();
			}
		}

		@Override
		public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
			throws SAXException
		{
			Thread.yield();
			if (cancelImport)
			{
				throw new ParsingInterruptedException();
			}

			if (qName.equals(rowTag))
			{
				// row definition ended, start a new row
				clearRowData();
				chars = null;
			}
			else if (qName.equals(columnTag))
			{
				chars = new StringBuilder();
				String attrValue = attrs.getValue(XmlRowDataConverter.ATTR_LONGVALUE);
				if (attrValue != null)
				{
					try
					{
						columnLongValue = Long.parseLong(attrValue);
					}
					catch (NumberFormatException e)
					{
						LogMgr.logError("XmlDataFileParser.startElement()", "Error converting longvalue", e);
					}
				}
				attrValue = attrs.getValue(XmlRowDataConverter.ATTR_NULL);
				isNull = "true".equals(attrValue);
				columnDataFile = attrs.getValue(XmlRowDataConverter.ATTR_DATA_FILE);
			}
			else
			{
				chars = null;
			}
		}

		@Override
		public void endElement(String namespaceURI, String sName, String qName)
			throws SAXException
		{
			if (cancelImport)
			{
				throw new ParsingInterruptedException();
			}
			if (qName.equals(rowTag))
			{
				if (!receiver.shouldProcessNextRow())
				{
					receiver.nextRowSkipped();
				}
				else
				{
					if (!ignoreCurrentRow)
					{
						try
						{
							sendRowData();
						}
						catch (Exception e)
						{
							// don't need to log the error as sendRowData() has already done that.
							if (abortOnError)
							{
								throw new ParsingInterruptedException();
							}
						}
					}
				}
				ignoreCurrentRow = false;
				currentRowNumber++;
			}
			else if (qName.equals(columnTag))
			{
				buildColumnData();
				currentColIndex++;
			}
			chars = null;
		}

		@Override
		public void characters(char[] buf, int offset, int len)
			throws SAXException
		{
			Thread.yield();
			if (cancelImport)
			{
				throw new ParsingInterruptedException();
			}
			if (chars != null)
			{
				chars.append(buf, offset, len);
			}
		}

		/** Only implemented to have even more possibilities for cancelling the import */
		@Override
		public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException
		{
			Thread.yield();
			if (cancelImport)
			{
				throw new ParsingInterruptedException();
			}
		}

		@Override
		public void processingInstruction(String target, String data)
			throws SAXException
		{
			Thread.yield();
			if (cancelImport)
			{
				throw new ParsingInterruptedException();
			}
		}

		@Override
		public void error(SAXParseException e)
			throws SAXParseException
		{
			String msg = "XML Parse error in line=" + e.getLineNumber() + ",data-row=" + (currentRowNumber);
			LogMgr.logError("XmlDataFileParser.error()", msg, e);
			ignoreCurrentRow = true;
		}

		@Override
		public void fatalError(SAXParseException e)
			throws SAXParseException
		{
			String msg = "Fatal XML parse error in line=" + e.getLineNumber() + ",data-row=" + (currentRowNumber) + "\nRest of file will be ignored!";
			LogMgr.logError("XmlDataFileParser.fatalError()", msg, e);
			ignoreCurrentRow = true;
		}

		// dump warnings too
		@Override
		public void warning(SAXParseException err)
			throws SAXParseException
		{
			messages.append(ExceptionUtil.getDisplay(err));
			messages.appendNewLine();
			if (cancelImport)
			{
				throw err;
			}
		}

	}

}
