/*
 * SpreadsheetFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.importer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.TabularDataParser;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 *
 * @author  Thomas Kellerer
 */
public class SpreadsheetFileParser
	extends AbstractImportFileParser
	implements TabularDataParser
{
	private File baseDir;

	private boolean withHeader = true;
	private boolean emptyStringIsNull;
	private boolean illegalDateIsNull;
	private boolean checkDependencies;
	private boolean ignoreOwner;

	private String nullString;
	private int currentRow;
	private int sheetIndex;
	private String sheetName;
	private SpreadsheetReader reader;
	protected List<Object> dataRowValues;

	public SpreadsheetFileParser()
	{
		converter.setCheckBuiltInFormats(false);
		converter.setDefaultTimestampFormat(StringUtil.ISO_TIMESTAMP_FORMAT);
		converter.setDefaultDateFormat(StringUtil.ISO_DATE_FORMAT);
	}

	public SpreadsheetFileParser(File aFile)
	{
		this();
		this.inputFile = aFile;
	}

	public void setIgnoreOwner(boolean flag)
	{
		this.ignoreOwner = flag;
	}

	public void setCheckDependencies(boolean flag)
	{
		this.checkDependencies = flag;
	}

	public void setNullString(String value)
	{
		nullString = value;
	}

	public void setIllegalDateIsNull(boolean flag)
	{
		this.illegalDateIsNull = flag;
	}

	public boolean getContainsHeader()
	{
		return withHeader;
	}

	public void setSheetIndex(int index)
	{
		this.sheetIndex = index;
	}

	public void setSheetName(String name)
	{
		this.sheetName = name;
	}

	@Override
	public void setContainsHeader(boolean aFlag)
	{
		this.withHeader = aFlag;
	}

	@Override
	public void setColumns(List<ColumnIdentifier> columnList)
		throws SQLException
	{
		setColumns(columnList, null);
	}

	@Override
	public void setInputFile(File file)
	{
		super.setInputFile(file);
		if (reader != null)
		{
			reader.done();
			reader = null;
		}
	}

	/**
	 * Define the columns in the input file.
	 * If a column name equals RowDataProducer.SKIP_INDICATOR
	 * then the column will not be imported.
	 * @param fileColumns the list of columns present in the input file
	 * @param columnsToImport the list of columns to import, if null all columns are imported
	 * @throws SQLException if the columns could not be verified
	 *         in the DB or the target table does not exist
	 */
	@Override
	public void setColumns(List<ColumnIdentifier> fileColumns, List<ColumnIdentifier> columnsToImport)
		throws SQLException
	{
		TableDefinition target = getTargetTable();
		List<ColumnIdentifier> tableCols = null;

		// When using the TextFileParser to import into a DataStore
		// no target table is defined, so this is an expected situation
		if (target != null)
		{
			tableCols = target.getColumns();
		}

		importColumns = ImportFileColumn.createList();

		int colCount = 0;
		if (columnsToImport == null)
		{
			columnsToImport = Collections.emptyList();
		}

		try
		{
			for (ColumnIdentifier sourceCol : fileColumns)
			{
				boolean ignoreColumn = sourceCol.getColumnName().equalsIgnoreCase(RowDataProducer.SKIP_INDICATOR);
				if (!ignoreColumn && !columnsToImport.isEmpty())
				{
					ignoreColumn = !columnsToImport.contains(sourceCol);
				}

				if (ignoreColumn)
				{
					ImportFileColumn col = new ImportFileColumn(sourceCol);
					col.setTargetIndex(-1);
					importColumns.add(col);
					continue;
				}
				int index = (tableCols == null ? -1 : tableCols.indexOf(sourceCol));
				if (index < 0 && tableCols != null)
				{
					if (this.abortOnError)
					{
						String msg = ResourceMgr.getFormattedString("ErrImportColumnNotFound", sourceCol.getColumnName(), this.tableName);
						this.messages.append(msg);
						this.messages.appendNewLine();
						this.hasErrors = true;
						throw new SQLException(msg);
					}
					else
					{
						String msg = ResourceMgr.getFormattedString("ErrImportColumnIgnored", sourceCol.getColumnName(), this.tableName);
						LogMgr.logWarning("SpreadsheetFileParser.setColumns()", msg);
						this.hasWarnings = true;
						this.messages.append(msg);
						this.messages.appendNewLine();
					}
				}
				else
				{
					ColumnIdentifier col = (tableCols != null ? tableCols.get(index) : sourceCol);
					ImportFileColumn importCol = new ImportFileColumn(col);

					importCol.setTargetIndex(colCount);
					importColumns.add(importCol);
					colCount ++;
				}
			}
		}
		catch (SQLException e)
		{
			this.hasErrors = true;
			throw e;
		}
		catch (Exception e)
		{
			LogMgr.logError("SpreadsheetFileParser.setColumns()", "Error when setting column definition", e);
			this.importColumns = null;
		}

		if (colCount == 0)
		{
			String msg = ResourceMgr.getString("ErrImportNoColumns");
			msg = StringUtil.replace(msg, "%table%", this.tableName);
			this.hasErrors = true;
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.importColumns = null;
			throw new SQLException("No column matched in import file");
		}
	}

	/**
	 * Return the column value from the input file for each column
	 * passed in to the function.
	 * @param inputFileIndexes the index of each column in the input file
	 * @return for each column index the value in the inputfile
	 */
	@Override
	public Map<Integer, Object> getInputColumnValues(Collection<Integer> inputFileIndexes)
	{
		if (dataRowValues == null) return null;
		if (inputFileIndexes == null) return null;

		Map<Integer, Object> result = new HashMap<Integer, Object>(inputFileIndexes.size());
		for (Integer index : inputFileIndexes)
		{
			if (index > 0 && index <= dataRowValues.size())
			{
				result.put(index, dataRowValues.get(index - 1));
			}
		}
		return result;
	}

	@Override
	public String getLastRecord()
	{
		if (currentRow < 0) return null;
		StringBuilder result = new StringBuilder(100);
		List<Object> values = reader.getRowValues(currentRow);
		boolean first = true;
		SimpleDateFormat dtFormatter = new SimpleDateFormat(StringUtil.ISO_DATE_FORMAT);
		SimpleDateFormat tsFormatter = new SimpleDateFormat(StringUtil.ISO_TIMESTAMP_FORMAT);
		for (Object value : values)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				result.append(", ");
			}
			String svalue = null;
			if (value instanceof java.sql.Date)
			{
				svalue = dtFormatter.format((java.sql.Date)value);
			}
			else if (value instanceof java.sql.Timestamp)
			{
				svalue = tsFormatter.format((java.sql.Timestamp)value);
			}
			else if (value != null)
			{
				svalue = value.toString();
			}
			else
			{
				svalue = "";
			}
			result.append(svalue);
		}
		return result.toString();
	}

	@Override
	public String getSourceFilename()
	{
		if (this.inputFile == null) return null;
		String fname = inputFile.getAbsolutePath();
		String sheet = sheetName;
		if (sheet == null)
		{
			sheet = Integer.toString(sheetIndex);
		}
		fname += ":" + sheet;
		return fname;
	}

	private void createReader()
		throws IOException
	{
		if (reader == null)
		{
			reader = SpreadsheetReader.Factory.createReader(inputFile, sheetIndex, sheetName);
			if (sheetIndex < 0 && StringUtil.isNonBlank(sheetName))
			{
				reader.setActiveWorksheet(sheetName);
			}
			reader.load();
		}
	}

	private List<Integer> getSheets()
	{
		List<String> allSheets = reader.getSheets();
		List<Integer> result = new  ArrayList<Integer>(allSheets.size());
		if (this.checkDependencies)
		{
			TableDependencySorter sorter = new TableDependencySorter(this.connection);
			List<TableIdentifier> tables = new ArrayList<TableIdentifier>(allSheets.size());
			for (String sheet : allSheets)
			{
				TableIdentifier ts = new TableIdentifier(sheet);
				if (this.cancelImport) break;
				if (ignoreOwner)
				{
					ts.setSchema(null);
					ts.setCatalog(null);
				}
				TableIdentifier tbl = connection.getMetadata().findObject(ts);
				if (tbl != null)
				{
					tables.add(tbl);
				}
				else
				{
					LogMgr.logWarning("SpreadsheetFileParser.getSheets()", "Could not find table " + sheet);
				}
			}
			List<TableIdentifier> sorted = sorter.sortForInsert(tables);
			LogMgr.logDebug("SpreadsheetFileParser.getSheets()", "Using insert sequence: " + sorted);
			for (TableIdentifier table : sorted)
			{
				int index = findSheet(table, allSheets);
				result.add(Integer.valueOf(index));
			}
		}
		else
		{
			for (int i=0; i < allSheets.size(); i++)
			{
				result.add(Integer.valueOf(i));
			}
		}
		return result;
	}

	private int findSheet(TableIdentifier table, List<String> sheets)
	{
		for (int i=0; i < sheets.size(); i++)
		{
			TableIdentifier sheet = new TableIdentifier(sheets.get(i));
			if (ignoreOwner)
			{
				sheet.setSchema(null);
				sheet.setCatalog(null);
			}
			if (sheet.compareNames(table))
			{
				return i;
			}
		}
		return -1;
	}


	@Override
	protected void processOneFile()
		throws Exception
	{
		if (this.inputFile.isAbsolute())
		{
			this.baseDir = this.inputFile.getParentFile();
		}

		if (baseDir == null)
		{
			this.baseDir = new File(".");
		}

		if (reader != null)
		{
			reader.done();
			reader = null;
		}

		createReader();

		reader.setNullString(nullString);

		try
		{
			if (sheetIndex != -1)
			{
				processOneSheet();
			}
			else
			{
				this.receiver.beginMultiTable();
				List<Integer> sheets = getSheets();
				List<String> allSheets = reader.getSheets();
				for (int i=0; i < sheets.size(); i++)
				{
					sheetIndex = sheets.get(i);
					sheetName = allSheets.get(sheetIndex);
					importColumns = null;
					tableName = allSheets.get(sheetIndex);
					targetTable = null;
					reader.setActiveWorksheet(sheetIndex);
					processOneSheet();
				}
			}
		}
		finally
		{
			done();
		}
	}

	protected void processOneSheet()
		throws Exception
	{
		if (this.withHeader && importColumns == null)
		{
			setupFileColumns(null); // null means: import all columns
		}

		// If no header is available in the file and no columns have been
		// specified by the user then we assume all columns from the table are present in the input file
		if (!this.withHeader && importColumns == null)
		{
			this.setColumns(getTargetTable().getColumns(), null);
		}

		if (CollectionUtil.isEmpty(importColumns))
		{
			throw new Exception("Cannot import file without a column definition");
		}

		List<ColumnIdentifier> columnsToImport = getColumnsToImport();
		try
		{
			// The target table might be null if an import is done into a DataStore
			this.receiver.setTargetTable((targetTable != null ? targetTable.getTable() : null), columnsToImport);
		}
		catch (Exception e)
		{
			LogMgr.logError("SpreadsheetFileParser.processOneFile()", "Error setting target table", e);
			throw e;
		}

		Object[] rowData = new Object[columnsToImport.size()];

		int startRow = (withHeader ? 1 : 0);
		int importRow = 0;
		boolean includeRow = true;
		int sourceCount = importColumns.size();

		converter.setIllegalDateIsNull(illegalDateIsNull);

		long rowCount = reader.getRowCount();
		for (currentRow = startRow; currentRow < rowCount; currentRow++)
		{
			Arrays.fill(rowData, null);
			if (cancelImport) break;

			boolean processRow = receiver.shouldProcessNextRow();
			if (!processRow) receiver.nextRowSkipped();

			if (!processRow)
			{
				continue;
			}

			importRow ++;
			dataRowValues = reader.getRowValues(currentRow);

			// Silently ignore empty rows
			if (dataRowValues.isEmpty()) continue;

			if (dataRowValues.size() < rowData.length)
			{
				String msg = ResourceMgr.getFormattedString("ErrImpIgnoreShortRow", currentRow, dataRowValues.size(), rowData.length);
				messages.append(msg);
				messages.appendNewLine();
				continue;
			}

			int targetIndex = -1;

			for (int sourceIndex=0; sourceIndex < sourceCount; sourceIndex++)
			{
				ImportFileColumn fileCol = importColumns.get(sourceIndex);
				if (fileCol == null) continue;

				targetIndex = fileCol.getTargetIndex();
				if (targetIndex == -1) continue;

				if (sourceIndex >= dataRowValues.size())
				{
					// Log this warning only once
					if (importRow == 1)
					{
						LogMgr.logWarning("SpreadsheetFileParser.processOneFile()", "Ignoring column with index=" + (sourceIndex + 1) + " because the import file has fewer columns");
					}
					continue;
				}
				Object value = dataRowValues.get(sourceIndex);
				String svalue = (value != null ? value.toString() : null);

				ColumnIdentifier col = fileCol.getColumn();
				int colType = col.getDataType();
				try
				{
					if (fileCol.getColumnFilter() != null)
					{
						if (value == null)
						{
							includeRow = false;
							break;
						}
						Matcher m = fileCol.getColumnFilter().matcher(svalue);
						if (!m.matches())
						{
							includeRow = false;
							break;
						}
					}

					if (valueModifier != null)
					{
						value = valueModifier.modifyValue(col, svalue);
					}

					if (SqlUtil.isCharacterType(colType))
					{
						if (this.emptyStringIsNull && StringUtil.isEmptyString(svalue))
						{
							value = null;
						}
						rowData[targetIndex] = value;
					}
					else
					{
						rowData[targetIndex] = converter.convertValue(value, colType);
					}
				}
				catch (Exception e)
				{
					if (targetIndex != -1) rowData[targetIndex] = null;
					String msg = ResourceMgr.getString("ErrConvertError");
					msg = msg.replace("%row%", Integer.toString(importRow));
					msg = msg.replace("%column%", (fileCol == null ? "n/a" : fileCol.getColumn().getColumnName()));
					msg = msg.replace("%value%", (svalue == null ? "(NULL)" : svalue));
					msg = msg.replace("%msg%", e.getClass().getName() + ": " + ExceptionUtil.getDisplay(e, false));
					msg = msg.replace("%type%", SqlUtil.getTypeName(colType));
					msg = msg.replace("%error%", e.getMessage());
					this.messages.append(msg);
					this.messages.appendNewLine();
					if (this.abortOnError)
					{
						this.hasErrors = true;
						this.cancelImport = true;
						throw e;
					}
					this.hasWarnings = true;
					LogMgr.logWarning("SpreadsheetFileParser.processOneFile()", msg, e);
					if (this.errorHandler != null)
					{
						int choice = errorHandler.getActionOnError(importRow, fileCol.getColumn().getColumnName(), (svalue == null ? "(NULL)" : svalue), ExceptionUtil.getDisplay(e, false));
						if (choice == JobErrorHandler.JOB_ABORT) throw e;
						if (choice == JobErrorHandler.JOB_IGNORE_ALL)
						{
							this.abortOnError = false;
						}
					}
					this.receiver.recordRejected(getLastRecord(), importRow, e);
					includeRow = false;
				}
			}

			if (this.cancelImport) break;

			try
			{
				if (includeRow) receiver.processRow(rowData);
			}
			catch (Exception e)
			{
				if (cancelImport)
				{
					LogMgr.logDebug("SpreadsheetFileParser.processOneFile()", "Error sending line " + importRow, e);
				}
				else
				{
					hasErrors = true;
					cancelImport = true;
					// processRow() will only throw an exception if abortOnError is true
					// so we can always re-throw the exception here.
					LogMgr.logError("SpreadsheetFileParser.processOneFile()", "Error sending line " + importRow, e);
					throw e;
				}
			}

			// read next line from Excel file
		}

		filesProcessed.add(inputFile);
		if (!cancelImport)
		{
			receiver.tableImportFinished();
		}
	}

	@Override
	public void done()
	{
		if (reader != null)
		{
			reader.done();
		}
	}

	/**
	 * Return the column names found in the input file.
	 *
	 * The ColumnIdentifier instances will only have a name but no data type assigned because this
	 * information is not available in an Excel File
	 *
	 * @return the columns defined in the input file
	 */
	@Override
	public List<ColumnIdentifier> getColumnsFromFile()
	{
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
		try
		{
			createReader();
			List<String> columns = reader.getHeaderColumns();
			for (String col : columns)
			{
				cols.add(new ColumnIdentifier(col));
			}
		}
		catch (Exception e)
		{
			this.hasErrors = true;
			LogMgr.logError("SpreadsheetFileParser.getColumnsFromFile()", "Error when reading columns", e);
		}
		return cols;
	}

	@Override
	public void checkTargetTable()
		throws SQLException
	{
		TableDefinition def = getTargetTable();

		if (def == null || def.getColumns().isEmpty())
		{
			TableIdentifier tbl = createTargetTableId();
			String msg = ResourceMgr.getFormattedString("ErrTableNotFound", tbl.getTableExpression());
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.importColumns = null;
			this.hasErrors = true;
			throw new SQLException("Table " + tbl.getTableExpression() + " not found!");
		}
	}

	@Override
	public void setupFileColumns(List<ColumnIdentifier> importColumns)
		throws SQLException, IOException
	{
		List<ColumnIdentifier> cols = null;

		if (this.withHeader)
		{
			cols = getColumnsFromFile();
		}
		else
		{
			TableDefinition def = getTargetTable();
			cols = def.getColumns();
		}
		setColumns(cols, importColumns);
	}

	public int getColumnCount()
	{
		return importColumns.size();
	}

	List<ImportFileColumn> getImportColumns()
	{
		return importColumns;
	}

	/**
	 * Setter for property emptyStringIsNull.
	 * @param flag New value of property emptyStringIsNull.
	 */
	public void setEmptyStringIsNull(boolean flag)
	{
		this.emptyStringIsNull = flag;
	}

	@Override
	public void addColumnFilter(String colname, String regex)
	{
	}
}
