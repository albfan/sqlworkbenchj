/*
 * OdsRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.math.BigInteger;

import workbench.log.LogMgr;

import workbench.storage.RowData;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbNumberFormatter;

import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.style.Font;
import org.odftoolkit.simple.style.StyleTypeDefinitions;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Table;

/**
 * Convert row data to OpenDocument Spreadsheet format (OpenOffice).
 *
 * @author  Thomas Kellerer
 */
public class NativeOdsRowDataConverter
	extends RowDataConverter
{
	private boolean append;
	private SpreadsheetDocument dataFile;
	private Table worksheet;
	private int firstDataRow;

	public void setAppend(boolean flag)
	{
		this.append = flag;
	}

	@Override
	public StringBuilder getStart()
	{
		if (append && getOutputFile().exists())
		{
			loadOdsFile();
			worksheet = dataFile.appendSheet(getPageTitle("SQLExport"));
		}
		else
		{
			try
			{
				if (isTemplate())
				{
					dataFile = SpreadsheetDocument.newSpreadsheetTemplateDocument();
				}
				else
				{
					dataFile = SpreadsheetDocument.newSpreadsheetDocument();
				}
				int sheets = dataFile.getSheetCount();
				if (sheets == 0)
				{
					worksheet = dataFile.appendSheet(getPageTitle("SQLExport"));
				}
				else
				{
					worksheet = dataFile.getSheetByIndex(0);
					worksheet.setTableName(getPageTitle("SQLExport"));
				}
			}
			catch (Exception ex)
			{
				throw new IllegalStateException("Could not create ODS document");
			}
		}

		if (includeColumnComments)
		{
			worksheet.appendRow();
			firstDataRow = 1;

			int column = 0;

			for (int c = 0; c < this.metaData.getColumnCount(); c++)
			{
				if (includeColumnInExport(c))
				{
					Cell cell = worksheet.getCellByPosition(column, 0);
					cell.setStringValue(StringUtil.trimQuotes(this.metaData.getColumn(c).getComment()));
					column ++;
				}
			}
		}

		if (writeHeader)
		{
			worksheet.appendRow();
			firstDataRow ++;

			int column = 0;
			for (int c = 0; c < this.metaData.getColumnCount(); c++)
			{
				if (includeColumnInExport(c))
				{
					Cell cell = worksheet.getCellByPosition(column, firstDataRow-1);
					cell.setStringValue(SqlUtil.removeObjectQuotes(this.metaData.getColumnDisplayName(c)));
					Font font = cell.getFont();
					font.setSize(font.getSize());
					font.setFontStyle(StyleTypeDefinitions.FontStyle.BOLD);
					cell.setFont(font);
					column ++;
				}
			}
		}

		return null;
	}

	private void loadOdsFile()
	{
		try
		{
			dataFile = SpreadsheetDocument.loadDocument(getOutputFile());
		}
		catch (Exception ex)
		{
			LogMgr.logError("NativeOdsRowDataConverter.loadOdsFile()", "Could not load ODS file", ex);
			dataFile = null;
		}
	}

	@Override
	public StringBuilder getEnd(long totalRows)
	{
		try
		{
			dataFile.save(getOutputFile());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return null;
	}

	protected String getTimestampFormat()
	{
		if (defaultTimestampFormatter != null)
		{
			return defaultTimestampFormatter.toPattern();
		}
		return StringUtil.ISO_TIMESTAMP_FORMAT;
	}

	protected String getDateFormat()
	{
		if (defaultDateFormatter != null)
		{
			return defaultDateFormatter.toPattern();
		}
		return StringUtil.ISO_DATE_FORMAT;
	}

	protected String getTimeFormat()
	{
		if (defaultTimeFormatter != null)
		{
			return defaultTimeFormatter.toPattern();
		}
		return "HH:mm:ss.SSS";
	}

	private String getFormatForDateTime(Object value)
	{
		if (value instanceof java.sql.Timestamp)
		{
			return getTimestampFormat();
		}
		if (value instanceof java.sql.Date)
		{
			return getDateFormat();
		}
		if (value instanceof java.sql.Time)
		{
			return getTimeFormat();
		}
		return StringUtil.ISO_TIMESTAMP_FORMAT;
	}

	private String getNumberFormat()
	{
		if (defaultNumberFormatter != null)
		{
			return defaultNumberFormatter.toFormatterPattern();
		}
		WbNumberFormatter fmt = new WbNumberFormatter(-1, '.');
		return fmt.toFormatterPattern();
	}

	@Override
	public StringBuilder convertRowData(RowData row, long rowIndex)
	{
		int colCount = row.getColumnCount();

		int column = 0;
		int rowNum = (int)rowIndex + firstDataRow;

		for (int i = 0; i < colCount; i++)
		{
			if (!this.includeColumnInExport(i)) continue;

			Object value = row.getValue(i);
			String formatted = getValueAsFormattedString(row, i);

			Cell cell = worksheet.getCellByPosition(column, rowNum);

			if (value == null && StringUtil.isNonEmpty(getNullDisplay()))
			{
				cell.setStringValue(getNullDisplay());
			}
			else
			{
				if (value instanceof java.util.Date)
				{
					cell.setDisplayText(formatted);
					String fmt = getFormatForDateTime(value);
					cell.setFormatString(fmt);
					cell.setStringValue(formatted);
					cell.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.LEFT);
				}
				else if (value instanceof Integer || value instanceof Long || value instanceof BigInteger)
				{
					cell.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.RIGHT);
					cell.setDisplayText(formatted);
				}
				else if (value instanceof Double || value instanceof Float)
				{
					cell.setDoubleValue(((Number)value).doubleValue());
					cell.setFormatString(getNumberFormat());
					cell.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.RIGHT);
					cell.setDisplayText(formatted);
				}
				else
				{
					cell.setDisplayText(formatted);
					boolean multiline = isMultiline(i);
					if (multiline)
					{
						cell.setTextWrapped(true);
					}
					cell.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.LEFT);
				}
			}
			column++;
		}
		return null;
	}

	public boolean isTemplate()
	{
		return hasOutputFileExtension("ots");
	}

}
