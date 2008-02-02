/*
 * HtmlRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import workbench.log.LogMgr;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author Alessandro Palumbo
 */
public class XlsRowDataConverter
	extends RowDataConverter
{
	private HSSFWorkbook wb = null;
	private HSSFSheet sheet = null;
	private ExcelDataFormat excelFormat = null;
	private int[] maxLengths = null;

	public XlsRowDataConverter()
	{
		super();
	}

	// This should not be called in the constructor as 
	// at that point in time the formatters are not initialized
	public void createFormatters()
	{
		String dateFormat = this.defaultDateFormatter != null ? this.defaultDateFormatter.toLocalizedPattern() : StringUtil.ISO_DATE_FORMAT;
		String tsFormat = this.defaultTimestampFormatter != null ? this.defaultTimestampFormatter.toLocalizedPattern() : StringUtil.ISO_TIMESTAMP_FORMAT;
		String numFormat = this.defaultNumberFormatter != null ? this.defaultNumberFormatter.toLocalizedPattern() : "0.00";
		excelFormat = new ExcelDataFormat(numFormat, dateFormat, "0", tsFormat);
	}

	public StrBuffer getStart()
	{
		createFormatters();
		
		wb = new HSSFWorkbook();
		
		excelFormat.setupWithWorkbook(wb);
		sheet = wb.createSheet(getPageTitle("SQLExport"));

		// table header with column names
		HSSFRow headRow = sheet.createRow(0);
		maxLengths = new int[this.metaData.getColumnCount()];
		for (int c = 0; c < this.metaData.getColumnCount(); c++)
		{
			HSSFCell cell = headRow.createCell((short)c);
			maxLengths[c] = setCellValueAndStyle(wb, cell, this.metaData.getColumnName(c), true, excelFormat);
		}

		return null;
	}
	
	public StrBuffer getEnd(long totalRows)
	{
		if (maxLengths != null)
		{
			for (short i = 0; i < maxLengths.length; i++)
			{
				sheet.setColumnWidth(i, (short)(maxLengths[i] * 256));
			}
		}

		FileOutputStream fileOut = null;
		try
		{
			// Scrive il file
			fileOut = new FileOutputStream(getOutputFile());
			wb.write(fileOut);
			fileOut.close();
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			if (fileOut != null)
			{
				try
				{
					fileOut.close();
				}
				catch (IOException e)
				{
					LogMgr.logError("XlsRowDataConverter.getEnd()", "Error closing file!", e);
				}
			}
		}

		return null;
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		StrBuffer ret = new StrBuffer();
		int count = this.metaData.getColumnCount();
		HSSFRow myRow = sheet.createRow(sheet.getLastRowNum() + 1);
		for (int c = 0; c < count; c++)
		{
			HSSFCell cell = myRow.createCell((short)c);

			Object value = row.getValue(c);

			int valLen = setCellValueAndStyle(wb, cell, value, false,	excelFormat);
			if (valLen > maxLengths[c])
			{
				maxLengths[c] = valLen;
			}
		}

		return ret;
	}

	private static int setCellValueAndStyle(HSSFWorkbook wb, HSSFCell cell,
		Object value, boolean isHead, ExcelDataFormat excelFormat)
	{
		HSSFCellStyle cellStyle = null;

		int length = 0;
		if (value instanceof BigDecimal && value != null)
		{
			cellStyle = excelFormat.decimalCellStyle;
			cell.setCellValue(((BigDecimal)value).doubleValue());
			length = value.toString().length() + 3;
		}
		else if (value instanceof Double && value != null)
		{
			cellStyle = excelFormat.decimalCellStyle;
			cell.setCellValue(((Double)value).doubleValue());
			length = value.toString().length() + 3;
		}
		else if (value instanceof Number && value != null)
		{
			cellStyle = excelFormat.integerCellStyle;
			cell.setCellValue(((Number)value).doubleValue());
			length = value.toString().length() + 3;
		}
		else if (value instanceof java.sql.Timestamp)
		{
			cellStyle = excelFormat.tsCellStyle;
			cell.setCellValue((java.util.Date)value);
			length = excelFormat.tsFormat.length() + 3;
		}
		else if (value instanceof java.util.Date)
		{
			cellStyle = excelFormat.dateCellStyle;
			cell.setCellValue((java.util.Date)value);
			length = excelFormat.dateFormat.length() + 3;
		}
		else
		{
			cell.setCellValue(value != null ? value.toString() : "");
			length = cell.getStringCellValue().length();
			length = length + 3;
			if (length > 50)
			{
				length = 50;
			}
			cellStyle = excelFormat.textCellStyle;
		}

		if (isHead)
		{
			cellStyle = excelFormat.headerCellStyle;
		}

		cell.setCellStyle(cellStyle);

		return length;
	}
}
