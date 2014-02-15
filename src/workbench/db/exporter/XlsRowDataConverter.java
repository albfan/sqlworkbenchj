/*
 * XlsRowDataConverter.java
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.storage.RowData;

import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

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

/**
 * Export data into an Excel spreadsheet using Apache's POI
 *
 * @author Alessandro Palumbo
 * @author Thomas Kellerer
 */
public class XlsRowDataConverter
	extends RowDataConverter
{
	public static final String INFO_SHEETNAME = "SQL";

	private Workbook workbook = null;
	private Sheet sheet = null;
	private ExcelDataFormat excelFormat = null;
	private boolean useXLSX;
	private int firstRow = 0;
	private boolean optimizeCols = true;
	private boolean append;
	private int targetSheetIndex = -1;
	private String targetSheetName;

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

	public void setTargetSheetName(String name)
	{
		this.targetSheetName = name;
	}

	public void setTargetSheetIndex(int index)
	{
		this.targetSheetIndex = index;
	}

	public void setOptimizeColumns(boolean flag)
	{
		this.optimizeCols = flag;
	}

	private boolean applyFormatting()
	{
		return this.targetSheetIndex < 0;
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
		finally
		{
			FileUtil.closeQuietely(in);
		}
	}

	@Override
	public StringBuilder getStart()
	{
		firstRow = 0;

		boolean loadFile = append || this.targetSheetIndex > 0 || this.targetSheetName != null;

		if (loadFile && getOutputFile().exists())
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

		createFormatters();
		excelFormat.setupWithWorkbook(workbook);

		String suppliedTitle = getPageTitle(null);

		if (this.targetSheetIndex > 0 && this.targetSheetIndex <= workbook.getNumberOfSheets())
		{
			// The user supplies a one based sheet index
			sheet = workbook.getSheetAt(targetSheetIndex - 1);
			if (suppliedTitle != null)
			{
				workbook.setSheetName(targetSheetIndex - 1, suppliedTitle);
			}
		}
		else if (this.targetSheetName != null)
		{
			sheet = workbook.getSheet(targetSheetName);
			if (sheet == null)
			{
				LogMgr.logWarning("XlsRowDataConverter.getStart()", "Sheet '" + targetSheetName + "' not found!");
				targetSheetIndex = -1;
				targetSheetName = null;
			}
			else
			{
				targetSheetIndex = workbook.getSheetIndex(sheet);
			}
			if (sheet != null && suppliedTitle != null)
			{
				workbook.setSheetName(targetSheetIndex, suppliedTitle);
			}
		}
		else
		{
			this.targetSheetIndex = -1;
		}

		if (sheet == null)
		{
			String sheetTitle = getPageTitle("SQLExport");
			sheet = workbook.createSheet(sheetTitle);
		}

		if (includeColumnComments)
		{
			Row commentRow = createSheetRow(0);
			firstRow = 1;
			int column = 0;
			for (int c = 0; c < this.metaData.getColumnCount(); c++)
			{
				if (includeColumnInExport(c))
				{
					Cell cell = createSheetCell(commentRow, column);
					setCellValueAndStyle(cell, StringUtil.trimQuotes(this.metaData.getColumn(c).getComment()), true, false, c);
					column ++;
				}
			}
		}

		if (writeHeader)
		{
			// table header with column names
			Row headRow = createSheetRow(firstRow);
			firstRow ++;
			int column = 0;
			for (int c = 0; c < this.metaData.getColumnCount(); c++)
			{
				if (includeColumnInExport(c))
				{
					Cell cell = createSheetCell(headRow, column);
					setCellValueAndStyle(cell, SqlUtil.removeObjectQuotes(this.metaData.getColumnDisplayName(c)), true, false, c);
					column ++;
				}
			}
		}
		return null;
	}

	private Cell createSheetCell(Row row, int cellIndex)
	{
		if (applyFormatting())
		{
			return row.createCell(cellIndex);
		}
		Cell cell = row.getCell(cellIndex);
		if (cell == null)
		{
			cell = row.createCell(cellIndex);
		}
		return cell;
	}

	private Row createSheetRow(int rowIndex)
	{
		if (this.applyFormatting())
		{
			return sheet.createRow(rowIndex);
		}
		Row row = sheet.getRow(rowIndex);
		if (row == null)
		{
			row = sheet.createRow(rowIndex);
		}
		return row;
	}

	@Override
	public StringBuilder getEnd(long totalRows)
	{
		if (getAppendInfoSheet())
		{
			writeInfoSheet();
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
			workbook = null;
			sheet = null;
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

	private void writeInfoSheet()
	{
		Sheet info = workbook.getSheet(INFO_SHEETNAME);

		if (info == null)
		{
			info = workbook.createSheet(INFO_SHEETNAME);
			Row headRow = info.createRow(0);
			Cell cell = headRow.createCell(0);
			setCellValueAndStyle(cell, ResourceMgr.getString("TxtSheet"), true, false, 0);
			cell = headRow.createCell(1);
			setCellValueAndStyle(cell, "SQL", true, false, 1);
		}
		else
		{
			// move the info sheet to the end
			int count = workbook.getNumberOfSheets();
			workbook.setSheetOrder(info.getSheetName(), count - 1);
		}

		int rowNum = info.getLastRowNum() + 1;

		Row infoRow = info.createRow(rowNum);

		Cell name = infoRow.createCell(0);
		CellStyle nameStyle = workbook.createCellStyle();
		nameStyle.setAlignment(CellStyle.ALIGN_LEFT);
		nameStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
		nameStyle.setWrapText(false);
		name.setCellValue(sheet.getSheetName());
		name.setCellStyle(nameStyle);
		info.autoSizeColumn(0);

		Cell sqlCell = infoRow.createCell(1);
		CellStyle sqlStyle = workbook.createCellStyle();
		sqlStyle.setAlignment(CellStyle.ALIGN_LEFT);
		sqlStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
		sqlStyle.setWrapText(false);

		RichTextString s = workbook.getCreationHelper().createRichTextString(generatingSql);
		sqlCell.setCellValue(s);
		sqlCell.setCellStyle(sqlStyle);
	}

	@Override
	public StringBuilder convertRowData(RowData row, long rowIndex)
	{
		StringBuilder ret = new StringBuilder();
		int count = this.metaData.getColumnCount();
		int rowNum = (int)rowIndex + firstRow;
		Row myRow = createSheetRow(rowNum);
		int column = 0;
		for (int c = 0; c < count; c++)
		{
			if (includeColumnInExport(c))
			{
				Cell cell = createSheetCell(myRow, column);

				Object value = row.getValue(c);
				boolean multiline = SqlUtil.isMultiLineColumn(metaData.getColumn(c));
				setCellValueAndStyle(cell, value, false, multiline, c);
				column ++;
			}
		}
		return ret;
	}

	private boolean isIntegerColumn(int column)
	{
		try
		{
			int type = metaData.getColumnType(column);
			String name = metaData.getDbmsTypeName(column);
			return (SqlUtil.isIntegerType(type) || (name.startsWith("NUMBER") && name.indexOf(',') == -1));
		}
		catch (Exception e)
		{
			LogMgr.logWarning("XlsRowDataConverter.isIntegerColumn()", "Could not check data type for column " + column, e);
			return false;
		}
	}

	private void setCellValueAndStyle(Cell cell, Object value, boolean isHead, boolean multiline, int column)
	{
		CellStyle cellStyle = null;

		if (value instanceof BigDecimal)
		{
			BigDecimal bd = (BigDecimal)value;

			// this is a workaround for exports using Oracle and NUMBER columns
			// which are essentially integer values. But it shouldn't hurt for other DBMS
			// either in case the driver returns a BigDecimal for "real" integer column
			if (bd.scale() == 0 && isIntegerColumn(column))
			{
				cellStyle = excelFormat.integerCellStyle;
			}
			else
			{
				cellStyle = excelFormat.decimalCellStyle;
			}
			cell.setCellValue(bd.doubleValue());
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

		// do not mess with the formatting if we are writing to an existing sheet
		if (applyFormatting())
		{
			try
			{
				CellStyle style = workbook.createCellStyle();
				style.cloneStyleFrom(cellStyle);
				cell.setCellStyle(style);
			}
			catch (IllegalArgumentException iae)
			{
				LogMgr.logWarning("XlsRowDataConverter.setCellValueAndStyle()", "Could not set style for column: " + metaData.getColumnName(column) + ", row: " + cell.getRowIndex() + ", column: " + cell.getColumnIndex());
			}
		}
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
