/*
 * ExcelReader.java
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import workbench.util.CollectionUtil;
import workbench.util.DurationFormatter;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import workbench.log.LogMgr;

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
	private WbFile inputFile;
	private Workbook dataFile;
	private Sheet dataSheet;
	private List<String> headerColumns;
	private String nullString;
	private List<CellRangeAddress> mergedRegions;
	private final Set<String> tsFormats = CollectionUtil.treeSet("HH", "mm", "ss", "SSS", "KK", "kk");

	private boolean useXLSX;

	public ExcelReader(File excelFile)
	{
		this(excelFile, 0);
	}

	public ExcelReader(File excelFile, int sheetNumber)
	{
		inputFile = new WbFile(excelFile);
		sheetIndex = sheetNumber > -1 ? sheetNumber : -1;
		useXLSX = inputFile.getExtension().equalsIgnoreCase("xlsx");
	}

	@Override
	public List<String> getSheets()
	{
		List<String> names = new ArrayList<String>();

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
		if (dataSheet != null)
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

		dataSheet = null;
		if (sheetIndex > -1)
		{
			dataSheet = dataFile.getSheetAt(sheetIndex);
			if (dataSheet == null)
			{
				throw new IndexOutOfBoundsException("Sheet with index " + sheetIndex + " does not exist");
			}
		}
		else
		{
			int index = dataFile.getActiveSheetIndex();
			dataSheet = dataFile.getSheetAt(index);
		}
		int numMergedRegions = dataSheet.getNumMergedRegions();
		mergedRegions = new ArrayList<CellRangeAddress>(numMergedRegions);
		for (int i=0; i < numMergedRegions; i++)
		{
			mergedRegions.add(dataSheet.getMergedRegion(i));
		}

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

	@Override
	public List<String> getHeaderColumns()
	{
		if (headerColumns == null)
		{
			List<Object> values = getRowValues(0);
			headerColumns = new ArrayList<String>(values.size());
			for (int i=0; i < values.size(); i++)
			{
				if (values.get(i) != null)
				{
					headerColumns.add(values.get(i).toString());
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
	public void setActiveWorksheet(int index)
	{
		if (index > -1)
		{
			sheetIndex = index;
			if (dataFile != null)
			{
				dataSheet = dataFile.getSheetAt(index);
				headerColumns = null;
				if (dataSheet == null)
				{
					throw new IndexOutOfBoundsException("Sheet with index " + sheetIndex + " does not exist");
				}
			}
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
		ArrayList<Object> values = new ArrayList<Object>();
		Iterator<Cell> cells = row.cellIterator();
		int nullCount = 0;

		while (cells.hasNext())
		{
			Cell cell = cells.next();
			int type = cell.getCellType();
			Object value = null;

			// treat rows with merged cells as "empty"
			if (isMerged(cell)) return Collections.emptyList();

			if (type == Cell.CELL_TYPE_FORMULA)
			{
				type = cell.getCachedFormulaResultType();
			}

			switch (type)
			{
				case Cell.CELL_TYPE_BLANK:
				case Cell.CELL_TYPE_ERROR:
					value = null;
					break;
				case Cell.CELL_TYPE_NUMERIC:
					boolean isDate = HSSFDateUtil.isCellDateFormatted(cell);
					String fmt = cell.getCellStyle().getDataFormatString();
					double dv = cell.getNumericCellValue();
					if (isDate)
					{
						java.util.Date dval = getJavaDate(dv);
						if (dval != null)
						{
							if (isTimestampFormat(fmt))
							{
								value = new java.sql.Timestamp(dval.getTime());
							}
							else
							{
								value = new java.sql.Date(dval.getTime());
							}
						}
					}
					else
					{
						value = Double.valueOf(dv);
					}
					break;
				default:
					String svalue = cell.getStringCellValue();
					if (svalue != null && StringUtil.equalString(svalue, nullString))
					{
						value = null;
					}
					else
					{
						value = svalue;
					}
			}
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
		dataFile = null;
	}

}
