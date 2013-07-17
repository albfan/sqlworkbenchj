/*
 * XlsRowDataConverter.java
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
package workbench.db.exporter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.storage.RowData;

import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.helpers.ColumnHelper;

import workbench.util.WbFile;

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
	private boolean optimizeCols = true;
	private boolean append;

	public XlsRowDataConverter()
	{
		super();
	}

	public void setAppend(boolean flag)
	{
		this.append = flag;
	}

	/**
	 * Switch to the new OOXML Format
	 */
	public void setUseXLSX()
	{
		useXLSX = true;
	}

	public void setOptimizeColumns(boolean flag)
	{
		this.optimizeCols = flag;
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

	private void loadExcelFile()
	{
		InputStream in = null;
		try
		{
			WbFile file = new WbFile(getOutputFile());
			useXLSX = file.getExtension().equalsIgnoreCase("xlsx");
			in = new FileInputStream(file);
			if (useXLSX)
			{
				workbook = new XSSFWorkbook(in);
			}
			else
			{
				workbook = new HSSFWorkbook(in);
			}
		}
		catch (IOException io)
		{
			LogMgr.logError("XlsRowDataConverter.loadExcelFile()", "Could not load Excel file", io);
			workbook = null;
		}
	}

	@Override
	public StrBuffer getStart()
	{
		createFormatters();
		firstRow = 0;

		if (append && getOutputFile().exists())
		{
			loadExcelFile();
		}
		else
		{
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
					setCellValueAndStyle(cell, SqlUtil.removeObjectQuotes(this.metaData.getColumnDisplayName(c)), true, false);
					column ++;
				}
			}
		}
		return null;
	}

	@Override
	public StrBuffer getEnd(long totalRows)
	{
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

		if (getEnableAutoFilter() && writeHeader)
		{
			String lastColumn = CellReference.convertNumToColString(metaData.getColumnCount() - 1);

			String rangeName = "A1:" + lastColumn + Long.toString(totalRows + 1);
			CellRangeAddress range = CellRangeAddress.valueOf(rangeName);
			sheet.setAutoFilter(range);
		}

		if (optimizeCols)
		{
			for (int col = 0; col < this.metaData.getColumnCount(); col++)
			{
				sheet.autoSizeColumn(col);
			}

			// POI seems to use a strange unit for specifying column widths.
			int charWidth = Settings.getInstance().getIntProperty("workbench.export.xls.defaultcharwidth", 200);

			for (int col = 0; col < this.metaData.getColumnCount(); col++)
			{
				int width = sheet.getColumnWidth(col);
				int minWidth = metaData.getColumnName(col).length() * charWidth;
				if (getEnableAutoFilter())
				{
					minWidth += charWidth * 2;
				}
				if (width < minWidth)
				{
					LogMgr.logDebug("XlsRowDataConverter.getEnd()", "Calculated width of column " + col + " is: " + width + ". Applying min width: " + minWidth);
					sheet.setColumnWidth(col, minWidth);
					if (sheet instanceof XSSFSheet)
					{
						ColumnHelper helper = ((XSSFSheet)sheet).getColumnHelper();
						helper.setColBestFit(col, false);
						helper.setColHidden(col, false);
					}
				}
			}
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
