/*
 * ExcelReader.java
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.util.CollectionUtil;
import workbench.util.DurationFormatter;
import workbench.util.FileUtil;
import workbench.util.MessageBuffer;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Thomas Kellerer
 */
public class ExcelReader
	implements SpreadsheetReader
{
	/**
	 * Amount of milliseconds in a day.
	 */
	private static final long ONE_DAY = (24L * DurationFormatter.ONE_HOUR);

	private int sheetIndex = -1;
	private String sheetName;

	private final WbFile inputFile;
	private Workbook dataFile;
	private Sheet dataSheet;
	private List<String> headerColumns;
	private String nullString;
	private List<CellRangeAddress> mergedRegions;
	private final Set<String> tsFormats = CollectionUtil.treeSet("HH", "mm", "ss", "SSS", "KK", "kk");

	private final boolean useXLSX;
	private MessageBuffer messages = new MessageBuffer();
  private boolean emptyStringIsNull;
  private boolean useStringDates;
  private DataFormatter dataFormatter = new DataFormatter(true);

	public ExcelReader(File excelFile, int sheetNumber, String name)
	{
		inputFile = new WbFile(excelFile);
		sheetIndex = sheetNumber > -1 ? sheetNumber : -1;
		if (sheetIndex < 0 && StringUtil.isNonBlank(name))
		{
			sheetName = name.trim();
		}
		else
		{
			sheetName = null;
		}
		useXLSX = inputFile.getExtension().equalsIgnoreCase("xlsx");
	}

  @Override
  public void setReturnDatesAsString(boolean flag)
  {
    useStringDates = flag;
  }

  @Override
  public void setEmptyStringIsNull(boolean flag)
  {
    emptyStringIsNull = flag;
  }

	@Override
	public MessageBuffer getMessages()
	{
		return messages;
	}

	@Override
	public List<String> getSheets()
	{
		List<String> names = new ArrayList<>();

		if (dataFile == null)
		{
			try
			{
				load();
			}
			catch (Exception io)
			{
				LogMgr.logError("ExcelReader.getSheets()", "Could not load Excel file: " + inputFile.getFullPath(), io);
				return names;
			}
		}

		int count = dataFile.getNumberOfSheets();
		for (int i=0; i < count; i++)
		{
			names.add(dataFile.getSheetName(i));
		}
		return names;
	}


	@Override
	public void load()
		throws IOException
	{
		if (dataFile != null)
		{
			// do not load the file twice.
			return;
		}

		InputStream in = null;
		try
		{
			in = new FileInputStream(inputFile);
			if (useXLSX)
			{
				dataFile = new XSSFWorkbook(in);
			}
			else
			{
				dataFile = new HSSFWorkbook(in);
			}
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}

		initActiveSheet();

		try
		{
			if (useXLSX)
			{
				XSSFFormulaEvaluator.evaluateAllFormulaCells((XSSFWorkbook)dataFile);
			}
			else
			{
				HSSFFormulaEvaluator.evaluateAllFormulaCells((HSSFWorkbook)dataFile);
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("ExcelReader.load()", "Could not refresh formulas!", ex);
		}
	}

	private void initActiveSheet()
	{
		if (dataFile == null) return;

		if (sheetIndex > -1)
		{
			dataSheet = dataFile.getSheetAt(sheetIndex);
			if (dataSheet == null)
			{
				throw new IndexOutOfBoundsException("Sheet with index " + sheetIndex + " does not exist in file: " + inputFile.getFullPath());
			}
		}
		else if (sheetName != null)
		{
			dataSheet = dataFile.getSheet(sheetName);
			if (dataSheet == null)
			{
				throw new IllegalArgumentException("Sheet with name " + sheetName + " does not exist in file: " + inputFile.getFullPath());
			}
		}
		else
		{
			int index = dataFile.getActiveSheetIndex();
			dataSheet = dataFile.getSheetAt(index);
		}
		headerColumns = null;
		int numMergedRegions = dataSheet.getNumMergedRegions();
		mergedRegions = new ArrayList<>(numMergedRegions);
		for (int i = 0; i < numMergedRegions; i++)
		{
			mergedRegions.add(dataSheet.getMergedRegion(i));
		}
	}

	@Override
	public List<String> getHeaderColumns()
	{
		if (headerColumns == null)
		{
			headerColumns = new ArrayList<>();
			Row row = dataSheet.getRow(0);

			int colCount = row != null ? row.getLastCellNum() : 0;

			if (row == null || colCount == 0)
			{
				LogMgr.logError("ExcelReader.getHeaderColumns()", "Cannot retrieve column names because no data is available in the first row of the sheet: " + dataSheet.getSheetName(), null);
				String msg = ResourceMgr.getFormattedString("ErrExportNoCols", dataSheet.getSheetName());
				messages.append(msg);
				messages.appendNewLine();
				return headerColumns;
			}

			for (int i=0; i < colCount; i++)
			{
				Cell cell = row.getCell(i);
				Object value = getCellValue(cell);

				if (value != null)
				{
					headerColumns.add(value.toString());
				}
				else
				{
					headerColumns.add("Col" + Integer.toString(i));
				}
			}
		}
		return headerColumns;
	}

	@Override
	public void setActiveWorksheet(String name)
	{
		if (StringUtil.isNonBlank(name) && !StringUtil.equalStringIgnoreCase(name, sheetName))
		{
			this.sheetName = name;
			this.sheetIndex = -1;
			initActiveSheet();
		}
	}

	@Override
	public void setActiveWorksheet(int index)
	{
		if (index > -1 && index != sheetIndex)
		{
			sheetIndex = index;
			sheetName = null;
			initActiveSheet();
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

	/**
	 * This is a copy of the POI function DateUtil.getJavaDate().
	 *
	 * The POI function does not consider hours, minutes and seconds, which means
	 * that columns with date <b>and</b> time are not retrieved correctly from an Excel file.
	 *
	 * @param date the "Excel" date
	 * @return a properly initialized Java Date
	 */
	private Date getJavaDate(double date)
	{
		int wholeDays = (int) Math.floor(date);
    int millisecondsInDay = (int)((date - wholeDays) * ONE_DAY + 0.5);
		Calendar calendar = new GregorianCalendar(); // using default time-zone

		int startYear = 1900;
		int dayAdjust = -1; // Excel thinks 2/29/1900 is a valid date, which it isn't
		if (wholeDays < 61)
		{
			// Date is prior to 3/1/1900, so adjust because Excel thinks 2/29/1900 exists
			// If Excel date == 2/29/1900, will become 3/1/1900 in Java representation
			dayAdjust = 0;
		}

		int hours = (int)(millisecondsInDay / DurationFormatter.ONE_HOUR);
		millisecondsInDay -= (hours * DurationFormatter.ONE_HOUR);
		int minutes = (int)(millisecondsInDay / DurationFormatter.ONE_MINUTE);
		millisecondsInDay -= (minutes * DurationFormatter.ONE_MINUTE);
		int seconds = (int)Math.floor(millisecondsInDay / DurationFormatter.ONE_SECOND);
		millisecondsInDay -= (seconds * DurationFormatter.ONE_SECOND);
		calendar.set(startYear, 0, wholeDays + dayAdjust, hours, minutes, seconds);
		calendar.set(GregorianCalendar.MILLISECOND, millisecondsInDay);
		return calendar.getTime();
	}

	private boolean isMerged(Cell cell)
	{
		if (cell == null) return false;
		for (CellRangeAddress range : mergedRegions)
		{
			if (range.isInRange(cell.getRowIndex(), cell.getColumnIndex())) return true;
		}
		return false;
	}

	@Override
	public List<Object> getRowValues(int rowIndex)
	{
		Row row = dataSheet.getRow(rowIndex);
		ArrayList<Object> values = new ArrayList<>();

		if (row == null) return values;

		int nullCount = 0;
		int colCount = row.getLastCellNum();

		for (int col=0; col < colCount; col++)
		{
			Cell cell = row.getCell(col);

			// treat rows with merged cells as "empty"
			if (isMerged(cell))
			{
				LogMgr.logDebug("ExcelReader.getRowValues()", dataSheet.getSheetName() + ": column:" + cell.getColumnIndex() + ", row:" + cell.getRowIndex() + " is merged. Ignoring row!");
				return Collections.emptyList();
			}

			Object value = getCellValue(cell);

			if (value == null)
			{
				nullCount ++;
			}
			values.add(value);
		}

		if (nullCount == values.size())
		{
			// return an empty list if all columns are null
			values.clear();
		}

		return values;
	}

	private Object getCellValue(Cell cell)
	{
		if (cell == null) return null;
		int type = cell.getCellType();
		if (type == Cell.CELL_TYPE_FORMULA)
		{
			type = cell.getCachedFormulaResultType();
		}

		Object value = null;

		switch (type)
		{
			case Cell.CELL_TYPE_BLANK:
			case Cell.CELL_TYPE_ERROR:
				value = null;
				break;
			case Cell.CELL_TYPE_NUMERIC:
				boolean isDate = HSSFDateUtil.isCellDateFormatted(cell);
				if (isDate)
				{
          if (useStringDates)
          {
            value = dataFormatter.formatCellValue(cell);
          }
          else
          {
            value = getDateValue(cell);
          }
				}
				else
				{
          double dv = cell.getNumericCellValue();
          value = Double.valueOf(dv);
				}
				break;
			default:
				String svalue = cell.getStringCellValue();
        if (isNullString(svalue))
				{
					value = null;
				}
				else
				{
					value = svalue;
				}
		}
		return value;
	}

  private java.util.Date getDateValue(Cell cell)
  {
    HSSFDataFormatter formatter = new HSSFDataFormatter();
    String strValue = formatter.formatCellValue(cell);
    System.out.println("strValue: " + strValue);

    java.util.Date dtValue = null;
    try
    {
      dtValue = cell.getDateCellValue();
    }
    catch (Exception ex)
    {
      // ignore
    }
    String fmt = cell.getCellStyle().getDataFormatString();
    double dv = cell.getNumericCellValue();
    if (dtValue == null)
    {
      dtValue = getJavaDate(dv);
    }

    if (dtValue != null)
    {
      if (isTimestampFormat(fmt))
      {
        return new java.sql.Timestamp(dtValue.getTime());
      }
      else
      {
        return new java.sql.Date(dtValue.getTime());
      }
    }
    return null;
  }

  private boolean isNullString(String value)
  {
    if (value == null) return true;
    if (emptyStringIsNull && StringUtil.isEmptyString(value)) return true;
    return StringUtil.equalString(value, nullString);
  }

	@Override
	public void setNullString(String nullString)
	{
		this.nullString = nullString;
	}

	@Override
	public int getRowCount()
	{
		return dataSheet.getLastRowNum() + 1;
	}

	@Override
	public void done()
	{
		dataSheet = null;
		dataFile = null;
	}

}
