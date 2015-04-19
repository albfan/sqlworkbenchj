/*
 * OdsReader.java
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
package workbench.db.importer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.util.CollectionUtil;
import workbench.util.MessageBuffer;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;

import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Column;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;


/**
 *
 * @author Thomas Kellerer
 */
public class OdsReader
	implements SpreadsheetReader
{
	private String nullIndicator;
	private File inputFile;
	private SpreadsheetDocument dataFile;
	private Table worksheet;
	private int worksheetIndex;
	private String worksheetName;
	private List<String> headerColumns;
	private final Set<String> tsFormats = CollectionUtil.treeSet("HH", "mm", "ss", "SSS", "KK", "kk");
	private MessageBuffer messages = new MessageBuffer();
	private final ValueConverter converter = new ValueConverter();
  private boolean emptyStringIsNull;

	public OdsReader(File odsFile, int sheetIndex, String name)
	{
		inputFile = odsFile;
		if (sheetIndex > -1 && StringUtil.isBlank(name))
		{
			worksheetIndex = sheetIndex;
		}
		else if (StringUtil.isNonBlank(name))
		{
			worksheetIndex = -1;
			worksheetName = name;
		}
		else
		{
			worksheetIndex = 0;
		}
	}

  @Override
  public void setReturnDatesAsString(boolean flag)
  {
    // we always use Strings anyway
  }

	@Override
	public MessageBuffer getMessages()
	{
		return messages;
	}

	@Override
	public synchronized List<String> getHeaderColumns()
	{
		if (headerColumns == null)
		{
			headerColumns = new ArrayList<>();
			if (Settings.getInstance().getBoolProperty("workbench.ods.use.get_cell_count", true))
			{
				readHeaderColsDefault();
			}
			else
			{
				readHeaderColsAlternate();
			}
		}
		return headerColumns;
	}

  @Override
  public void setEmptyStringIsNull(boolean flag)
  {
    emptyStringIsNull = flag;
  }

	private void readHeaderColsDefault()
	{
		Row rowData = worksheet.getRowByIndex(0);

		int colCount = 0;

		try
		{
			if (rowData != null) colCount = rowData.getCellCount();
		}
		catch (Exception ex)
		{
			colCount = -1;
		}

		if (colCount <= 0)
		{
			LogMgr.logError("OdsReader.readHeaderColsDefault()", "Cannot retrieve column names because no columns are available in the first row of the sheet: " + worksheet.getTableName(), null);
			String msg = ResourceMgr.getFormattedString("ErrExportNoCols", worksheet.getTableName());
			messages.append(msg);
			messages.appendNewLine();
			return;
		}

		for (int i=0; i < colCount; i++)
		{
			Cell cell = rowData.getCellByIndex(i);
			String title = cell.getDisplayText();

			if (title != null)
			{
				headerColumns.add(title);
			}
			else
			{
				headerColumns.add("Col" + Integer.toString(i));
			}
		}
	}

	private void readHeaderColsAlternate()
	{
		List<Column> columnList = worksheet.getColumnList();
		if (CollectionUtil.isEmpty(columnList))
		{
			LogMgr.logError("OdsReader.readHeaderColsAlternate()", "Cannot retrieve column names because no columns are available in the first row of the sheet: " + worksheet.getTableName(), null);
			String msg = ResourceMgr.getFormattedString("ErrExportNoCols", worksheet.getTableName());
			messages.append(msg);
			messages.appendNewLine();
			return;
		}

		for (Column col : columnList)
		{
			Cell cell = col.getCellByIndex(0);
			if (cell != null && cell.getColumnSpannedNumber() == 1)
			{
				String title = cell.getDisplayText();
				if (StringUtil.isNonBlank(title))
				{
					headerColumns.add(title);
				}
			}
		}
	}

	@Override
	public void setActiveWorksheet(int index)
	{
		worksheetIndex = index;
		worksheetName = null;
		headerColumns = null;
		initCurrentWorksheet();
	}

	@Override
	public void setActiveWorksheet(String name)
	{
		worksheetIndex = -1;
		worksheetName = name;
		headerColumns = null;
		initCurrentWorksheet();
	}

	private void initCurrentWorksheet()
	{
		if (dataFile == null) return;
		if (worksheetIndex > -1)
		{
			worksheet = dataFile.getSheetByIndex(worksheetIndex);
		}
		else if (worksheetName != null)
		{
			worksheet = dataFile.getSheetByName(worksheetName);
		}
		else
		{
			worksheet = dataFile.getSheetByIndex(0);
		}
	}

	private boolean isTimestampFormat(String format)
	{
		for (String key : tsFormats)
		{
			if (format.contains(key)) return true;
		}
		return false;
	}

	@Override
	public List<Object> getRowValues(int row)
	{
		Row rowData = worksheet.getRowByIndex(row);
		int colCount = 0;
		if (headerColumns != null)
		{
			colCount = headerColumns.size();
		}
		else if (Settings.getInstance().getBoolProperty("workbench.ods.use.get_cell_count", true))
		{
			colCount = rowData.getCellCount();
		}
		else
		{
			List<Column> columnList = worksheet.getColumnList();
			colCount = columnList.size();
		}

		List<Object> result = new ArrayList<>(colCount);
		int nullCount = 0;

		for (int col=0; col < colCount; col++)
		{
			Cell cell = rowData.getCellByIndex(col);
			String type = cell.getValueType();
			Object value = null;
			if ("boolean".equals(type))
			{
				value = cell.getBooleanValue();
			}
			else if ("time".equals(type))
			{
				String text = cell.getDisplayText();
				try
				{
					value = converter.parseTime(text);
				}
				catch (Exception ex)
				{
					LogMgr.logError("OdsReader.getRowValues()", "Could not parse time value: " + text, ex);
					Calendar cal = cell.getTimeValue();
					if (cal != null)
					{
						value = new java.sql.Time(cal.getTime().getTime());
					}
				}
			}
			else if ("date".equals(type))
			{
				String fmt = cell.getFormatString();
				String text = cell.getDisplayText();
				try
				{
					SimpleDateFormat formatter = new SimpleDateFormat(fmt);
					java.util.Date udt = formatter.parse(text);
					if (isTimestampFormat(fmt))
					{
						value = new java.sql.Timestamp(udt.getTime());
					}
					else
					{
						value = new java.sql.Date(udt.getTime());
					}
				}
				catch (Exception ex)
				{
					LogMgr.logError("OdsReader.getRowValues()", "Could not parse date format: " + fmt, ex);
					Calendar cal = cell.getDateValue();
					if (cal != null)
					{
						value = cal.getTime();
					}
				}
			}
			else if ("float".equals(type))
			{
				value = cell.getDoubleValue();
			}
			else if ("currency".equals(type))
			{
				value = cell.getCurrencyValue();
			}
			else
			{
				String sValue = cell.getStringValue();
        if (isNullString(sValue))
        {
          value = null;
        }
        else
        {
          value = sValue;
        }
			}

      if (value == null)
			{
				nullCount ++;
			}
			result.add(value);
		}
		if (nullCount == result.size())
		{
			result.clear();
		}
		return result;
	}

  private boolean isNullString(String value)
  {
    if (value == null) return true;
    if (emptyStringIsNull && StringUtil.isEmptyString(value)) return true;
    return StringUtil.equalString(value, nullIndicator);
  }

	@Override
	public void setNullString(String nullString)
	{
		nullIndicator = nullString;
	}

	@Override
	public int getRowCount()
	{
		return worksheet.getRowCount();
	}

	@Override
	public void done()
	{
		if (dataFile != null)
		{
			dataFile.close();
		}
		dataFile = null;
		worksheet = null;
	}

	@Override
	public void load()
		throws IOException
	{
		try
		{
			dataFile = SpreadsheetDocument.loadDocument(inputFile);
			initCurrentWorksheet();
			LogMgr.logDebug("OdsReader.load()", "Document loaded. Rows: " + getRowCount());
		}
		catch (Exception ex)
		{
			throw new IOException("Could not load file " + inputFile.getAbsolutePath(), ex);
		}
	}

	@Override
	public List<String> getSheets()
	{
		List<String> result = new ArrayList<>();
		if (dataFile == null)
		{
			try
			{
				load();
			}
			catch (IOException io)
			{
				LogMgr.logError("OdsReader.getSheets()", io.getMessage(), io);
				return result;
			}
		}

		int sheetCount = dataFile.getSheetCount();
		for (int i=0; i < sheetCount; i ++)
		{
			String name = dataFile.getSheetByIndex(i).getTableName();
			result.add(name);
		}
		return result;
	}

}
