/*
 * XlsRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import workbench.log.LogMgr;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Export data into an Excel spreadsheet using Apache's POI
 *
 * @author Alessandro Palumbo
 */
public class XlsRowDataConverter
	extends RowDataConverter
{
	private HSSFWorkbook wb = null;
	private HSSFSheet sheet = null;
	private ExcelDataFormat excelFormat = null;

	public XlsRowDataConverter()
	{
		super();
	}
	
	// This should not be called in the constructor as
	// at that point in time the formatters are not initialized
	public void createFormatters()
	{
		String dateFormat = this.defaultDateFormatter != null ? this.defaultDateFormatter.toPattern() : StringUtil.ISO_DATE_FORMAT;
		String tsFormat = this.defaultTimestampFormatter != null ? this.defaultTimestampFormatter.toPattern() : StringUtil.ISO_TIMESTAMP_FORMAT;
		String numFormat = this.defaultNumberFormatter != null ? this.defaultNumberFormatter.toPattern() : "0.00";
		excelFormat = new ExcelDataFormat(numFormat, dateFormat, "0", tsFormat);
	}

	public StrBuffer getStart()
	{
		createFormatters();

		wb = new HSSFWorkbook();
		//wb = (HSSFWorkbook)DynamicPoi.createWorkbook();

		excelFormat.setupWithWorkbook(wb);
		sheet = wb.createSheet(getPageTitle("SQLExport"));

		if (writeHeader)
		{
			// table header with column names
			HSSFRow headRow = sheet.createRow(0);
			for (int c = 0; c < this.metaData.getColumnCount(); c++)
			{
				HSSFCell cell = headRow.createCell((short)c);
				setCellValueAndStyle(cell, this.metaData.getColumnName(c), true);
			}
		}
		return null;
	}

	public StrBuffer getEnd(long totalRows)
	{
		for (short i = 0; i <this.metaData.getColumnCount(); i++)
		{
			sheet.autoSizeColumn(i);
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
		int rowNum = (int)rowIndex;
		if (writeHeader) rowNum ++;
		HSSFRow myRow = sheet.createRow(rowNum);
		for (int c = 0; c < count; c++)
		{
			HSSFCell cell = myRow.createCell((short)c);

			Object value = row.getValue(c);

			setCellValueAndStyle(cell, value, false);
		}
		return ret;
	}

	private void setCellValueAndStyle(HSSFCell cell, Object value, boolean isHead)
	{
		HSSFCellStyle cellStyle = null;

		if (value instanceof BigDecimal)
		{
			cellStyle = excelFormat.decimalCellStyle;
			cell.setCellValue(((BigDecimal)value).doubleValue());
		}
		else if (value instanceof Double)
		{
			cellStyle = excelFormat.decimalCellStyle;
			cell.setCellValue(((Double)value).doubleValue());
		}
		else if (value instanceof Number)
		{
			cellStyle = excelFormat.integerCellStyle;
			cell.setCellValue(((Number)value).doubleValue());
		}
		else if (value instanceof java.sql.Timestamp)
		{
			cellStyle = excelFormat.tsCellStyle;
			cell.setCellValue((java.util.Date)value);
		}
		else if (value instanceof java.util.Date)
		{
			cellStyle = excelFormat.dateCellStyle;
			cell.setCellValue((java.util.Date)value);
		}
		else
		{
			HSSFRichTextString s = new HSSFRichTextString(value != null ? value.toString() : "");
			cell.setCellValue(s);
			cellStyle = excelFormat.textCellStyle;
		}

		if (isHead)
		{
			cellStyle = excelFormat.headerCellStyle;
		}

		cell.setCellStyle(cellStyle);
	}
}
