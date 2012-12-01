/*
 * ExcelReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Thomas Kellerer
 */
public class ExcelReader
	implements SpreadsheetReader
{
	private int sheetIndex = -1;

	private WbFile inputFile;
	private Workbook dataFile;
	private Sheet dataSheet;
	private List<String> headerColumns;
	private String nullString;

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
		if (dataFile != null)
		{
			int count = dataFile.getNumberOfSheets();
			List<String> names = new ArrayList<String>(count);
			for (int i=0; i < count; i++)
			{
				names.add(dataFile.getSheetName(i));
			}
		}
		return Collections.emptyList();
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

	@Override
	public List<Object> getRowValues(int rowIndex)
	{
		Row row = dataSheet.getRow(rowIndex);
		ArrayList<Object> values = new ArrayList<Object>();
		Iterator<Cell> cells = row.cellIterator();
		while (cells.hasNext())
		{
			Cell cell = cells.next();
			int type = cell.getCellType();
			Object value = null;

			switch (type)
			{
				case Cell.CELL_TYPE_BLANK:
				case Cell.CELL_TYPE_ERROR:
					value = null;
					break;
				case Cell.CELL_TYPE_NUMERIC:
					boolean isDate = HSSFDateUtil.isCellDateFormatted(cell);
					double dv = cell.getNumericCellValue();
					if (isDate)
					{
						value = HSSFDateUtil.getJavaDate(dv);
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
			values.add(value);
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
