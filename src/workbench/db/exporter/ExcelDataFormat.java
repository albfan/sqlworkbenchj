/*
 * ExcelDataFormat.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * @author Alessandro Palumbo
 */
class ExcelDataFormat
{
	protected String decimalFormat;
	protected String dateFormat;
	protected String timestampFormat;
	protected String integerFormat;
	protected CellStyle headerCellStyle = null;
	protected CellStyle dateCellStyle = null;
	protected CellStyle tsCellStyle = null;
	protected CellStyle decimalCellStyle = null;
	protected CellStyle integerCellStyle = null;
	protected CellStyle textCellStyle = null;
	protected DataFormat dataFormat = null;
	protected short gridDateFormat;
	protected short gridDecimalFormat;
	protected short gridIntegerFormat;
	protected short gridTsFormat;

	public ExcelDataFormat(String decFormat, String dtFormat,
		String intFormat, String tsFormat)
	{
		this.decimalFormat = decFormat;
		this.dateFormat = dtFormat;
		this.integerFormat = intFormat;
		this.timestampFormat = tsFormat;
	}

	protected void setupWithWorkbook(Workbook wb)
	{
		CreationHelper helper = wb.getCreationHelper();
		dataFormat = helper.createDataFormat();
		setUpHeader(wb);
		setUpText(wb);
		setUpDate(wb);
		setUpDecimal(wb);
		setUpInteger(wb);
		setUpTs(wb);
	}

	protected void setUpText(Workbook wb)
	{
		textCellStyle = wb.createCellStyle();
		textCellStyle.setAlignment(CellStyle.ALIGN_LEFT);
		textCellStyle.setWrapText(true);
	}

	protected void setUpDate(Workbook wb)
	{
		dateCellStyle = wb.createCellStyle();
		dateCellStyle.setAlignment(CellStyle.ALIGN_LEFT);
		gridDateFormat = safeGetFormat(dataFormat, dateFormat);
		dateCellStyle.setDataFormat(gridDateFormat);
	}

	protected void setUpDecimal(Workbook wb)
	{
		decimalCellStyle = wb.createCellStyle();
		decimalCellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		gridDecimalFormat = safeGetFormat(dataFormat, decimalFormat);
		decimalCellStyle.setDataFormat(gridDecimalFormat);
	}

	protected void setUpInteger(Workbook wb)
	{
		integerCellStyle = wb.createCellStyle();
		integerCellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		gridIntegerFormat = safeGetFormat(dataFormat, integerFormat);
		integerCellStyle.setDataFormat(gridIntegerFormat);
	}

	protected void setUpHeader(Workbook wb)
	{
		headerCellStyle = wb.createCellStyle();
		Font font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headerCellStyle.setFont(font);
		headerCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
	}

	protected void setUpTs(Workbook wb)
	{
		tsCellStyle = wb.createCellStyle();
		tsCellStyle.setAlignment(CellStyle.ALIGN_LEFT);
		gridTsFormat = safeGetFormat(dataFormat, timestampFormat);
		tsCellStyle.setDataFormat(gridTsFormat);
	}

	protected static short safeGetFormat(DataFormat dataFormat, String formatString)
	{
		return dataFormat.getFormat(formatString);
	}
}
