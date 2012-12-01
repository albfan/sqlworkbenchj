/*
 * XlsRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;

import org.apache.poi.POIXMLProperties;

/**
 * Export data into an Excel spreadsheet using Apache's POI
 *
 * @author Alessandro Palumbo
 * @author Thomas Kellerer
 */
public class XlsRowDataConverter
	extends RowDataConverter
{
	private Workbook workbook = null;
	private Sheet sheet = null;
	private ExcelDataFormat excelFormat = null;
	private boolean useXLSX;
	private int firstRow = 0;

	public XlsRowDataConverter()
	{
		super();
	}

	/**
	 * Switch to the new OOXML Format
	 */
	public void setUseXLSX()
	{
		useXLSX = true;
	}

	// This should not be called in the constructor as
	// at that point in time the formatters are not initialized
	private void createFormatters()
	{
		String dateFormat = this.defaultDateFormatter != null ? this.defaultDateFormatter.toPattern() : StringUtil.ISO_DATE_FORMAT;
		String tsFormat = this.defaultTimestampFormatter != null ? this.defaultTimestampFormatter.toPattern() : StringUtil.ISO_TIMESTAMP_FORMAT;
		String numFormat = this.defaultNumberFormatter != null ? this.defaultNumberFormatter.toFormatterPattern() : "0.00";
		excelFormat = new ExcelDataFormat(numFormat, dateFormat, "0", tsFormat);
	}

	@Override
	public StrBuffer getStart()
	{
		createFormatters();

		if (useXLSX)
		{
			workbook = new XSSFWorkbook();
			if (isTemplate())
			{
				makeTemplate();
			}
		}
		else
		{
			workbook = new HSSFWorkbook();
		}

		excelFormat.setupWithWorkbook(workbook);
		sheet = workbook.createSheet(getPageTitle("SQLExport"));

		if (includeColumnComments)
		{
			Row commentRow = sheet.createRow(0);
			firstRow = 1;
			int column = 0;
			for (int c = 0; c < this.metaData.getColumnCount(); c++)
			{
				if (includeColumnInExport(c))
				{
					Cell cell = commentRow.createCell(column);
					setCellValueAndStyle(cell, StringUtil.trimQuotes(this.metaData.getColumn(c).getComment()), true, false);
					column ++;
				}
			}
		}

		if (writeHeader)
		{
			// table header with column names
			Row headRow = sheet.createRow(firstRow);
			firstRow ++;
			int column = 0;
			for (int c = 0; c < this.metaData.getColumnCount(); c++)
			{
				if (includeColumnInExport(c))
				{
					Cell cell = headRow.createCell(column);
					setCellValueAndStyle(cell, StringUtil.trimQuotes(this.metaData.getColumnDisplayName(c)), true, false);
					column ++;
				}
			}
		}
		return null;
	}

	@Override
	public StrBuffer getEnd(long totalRows)
	{
		for (short i = 0; i < this.metaData.getColumnCount(); i++)
		{
			sheet.autoSizeColumn(i);
		}

		if (getAppendInfoSheet())
		{
			Sheet info = workbook.createSheet("SQL");
			Row infoRow = info.createRow(0);
			Cell cell = infoRow.createCell(0);

			CellStyle style = workbook.createCellStyle();
			style.setAlignment(CellStyle.ALIGN_LEFT);
			style.setWrapText(false);

			RichTextString s = workbook.getCreationHelper().createRichTextString(generatingSql);
			cell.setCellValue(s);
			cell.setCellStyle(style);
		}

		if (getEnableFixedHeader() && writeHeader)
		{
			sheet.createFreezePane(0, firstRow);
		}

		FileOutputStream fileOut = null;
		try
		{
			fileOut = new FileOutputStream(getOutputFile());
			workbook.write(fileOut);
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
			FileUtil.closeQuietely(fileOut);
		}

		return null;
	}

	@Override
	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		StrBuffer ret = new StrBuffer();
		int count = this.metaData.getColumnCount();
		int rowNum = (int)rowIndex + firstRow;
		Row myRow = sheet.createRow(rowNum);
		int column = 0;
		for (int c = 0; c < count; c++)
		{
			if (includeColumnInExport(c))
			{
				Cell cell = myRow.createCell(column);

				Object value = row.getValue(c);
				boolean multiline = SqlUtil.isMultiLineColumn(metaData.getColumn(c));
				setCellValueAndStyle(cell, value, false, multiline);
				column ++;
			}
		}
		return ret;
	}

	private void setCellValueAndStyle(Cell cell, Object value, boolean isHead, boolean multiline)
	{
		CellStyle cellStyle = null;

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
			RichTextString s = workbook.getCreationHelper().createRichTextString(value != null ? value.toString() : getNullDisplay());
			cell.setCellValue(s);
			if (multiline)
			{
				cellStyle = excelFormat.multilineCellStyle;
			}
			else
			{
				cellStyle = excelFormat.textCellStyle;
			}
		}

		if (isHead)
		{
			cellStyle = excelFormat.headerCellStyle;
		}

		cell.setCellStyle(cellStyle);
	}

	public boolean isTemplate()
	{
		return hasOutputFileExtension("xltx");
	}

	private void makeTemplate()
	{
		if (!useXLSX) return;
		POIXMLProperties props = ((XSSFWorkbook)workbook).getProperties();
		POIXMLProperties.ExtendedProperties ext =  props.getExtendedProperties();
    ext.getUnderlyingProperties().setTemplate("XSSF");
	}
}
