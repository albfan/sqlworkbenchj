/*
 * SpreadsheetFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.interfaces.TabularDataParser;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
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

	private String nullString;
	private int currentRow;
	private int sheetIndex;
	private SpreadsheetReader content;

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
		File old = inputFile;
		super.setInputFile(file);
		if (old != null && file == null || old == null && inputFile != null || !inputFile.equals(old))
		{
			if (content != null)
			{
				content.done();
				content = null;
			}
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
		if (currentRowValues == null) return null;
		if (inputFileIndexes == null) return null;

		Map<Integer, Object> result = new HashMap<Integer, Object>(inputFileIndexes.size());
		for (Integer index : inputFileIndexes)
		{
			if (index > 0 && index <= currentRowValues.size())
			{
				result.put(index, currentRowValues.get(index - 1));
			}
		}
		return result;
	}

	@Override
	public String getLastRecord()
	{
		if (currentRow < 0) return null;
		StringBuilder result = new StringBuilder(100);
		List<String> values = content.getRowValues(currentRow);
		boolean first = true;
		for (String value : values)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				result.append(", ");
			}
			result.append(value);
		}
		return result.toString();
	}

	private void createReader()
		throws IOException
	{
		if (content == null)
		{
			content = new ExcelReader(inputFile, sheetIndex);
			content.load();
		}
	}

	@Override
	protected void processOneFile()
		throws Exception
	{
		if (this.inputFile.isAbsolute())
		{
			this.baseDir = this.inputFile.getParentFile();
		}
		if (baseDir == null) this.baseDir = new File(".");

		if (content != null)
		{
			content.done();
			content = null;
		}
		createReader();
		content.setNullString(nullString);

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

		long rowCount = content.getRowCount();
		try
		{
			for (currentRow = startRow; currentRow < rowCount; currentRow++)
			{
				if (cancelImport) break;

				boolean processRow = receiver.shouldProcessNextRow();
				if (!processRow) receiver.nextRowSkipped();

				if (!processRow)
				{
					continue;
				}

				importRow ++;
				currentRowValues = content.getRowValues(currentRow);

				int targetIndex = -1;

				for (int sourceIndex=0; sourceIndex < sourceCount; sourceIndex++)
				{
					ImportFileColumn fileCol = importColumns.get(sourceIndex);
					if (fileCol == null) continue;

					targetIndex = fileCol.getTargetIndex();
					if (targetIndex == -1) continue;

					if (sourceIndex >= currentRowValues.size())
					{
						// Log this warning only once
						if (importRow == 1)
						{
							LogMgr.logWarning("SpreadsheetFileParser.processOneFile()", "Ignoring column with index=" + (sourceIndex + 1) + " because the import file has fewer columns");
						}
						continue;
					}
					String value = currentRowValues.get(sourceIndex);

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
							Matcher m = fileCol.getColumnFilter().matcher(value);
							if (!m.matches())
							{
								includeRow = false;
								break;
							}
						}

						if (valueModifier != null)
						{
							value = valueModifier.modifyValue(col, value);
						}

						if (SqlUtil.isCharacterType(colType))
						{
								if (this.emptyStringIsNull && StringUtil.isEmptyString(value))
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
						msg = msg.replace("%col%", (fileCol == null ? "n/a" : fileCol.getColumn().getColumnName()));
						msg = msg.replace("%value%", (value == null ? "(NULL)" : value));
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
							int choice = errorHandler.getActionOnError(importRow, fileCol.getColumn().getColumnName(), (value == null ? "(NULL)" : value), ExceptionUtil.getDisplay(e, false));
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
		finally
		{
			content.done();
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
			List<String> columns = content.getHeaderColumns();
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
