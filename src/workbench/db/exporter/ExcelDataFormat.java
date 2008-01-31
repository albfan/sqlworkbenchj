/*
 * ExcelDataFormat.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * @author Alessandro Palumbo
 */
class ExcelDataFormat
{
	protected String decimalFormat;
	protected String dateFormat;
	protected String tsFormat;
	protected String integerFormat;
	protected HSSFCellStyle headerCellStyle = null;
	protected HSSFCellStyle dateCellStyle = null;
	protected HSSFCellStyle tsCellStyle = null;
	protected HSSFCellStyle decimalCellStyle = null;
	protected HSSFCellStyle integerCellStyle = null;
	protected HSSFCellStyle textCellStyle = null;
	protected HSSFDataFormat dataFormat = null;
	protected short gridDateFormat;
	protected short gridDecimalFormat;
	protected short gridIntegerFormat;
	protected short gridTsFormat;

	public ExcelDataFormat(String decimalFormat, String dateFormat,
		String integerFormat, String tsFormat)
	{
		this.decimalFormat = decimalFormat;
		this.dateFormat = dateFormat;
		this.integerFormat = integerFormat;
		this.tsFormat = tsFormat;
	}

	protected void setupWithWorkbook(HSSFWorkbook wb)
	{
		dataFormat = wb.createDataFormat();
		setUpHeader(wb);
		setUpText(wb);
		setUpDate(wb);
		setUpDecimal(wb);
		setUpInteger(wb);
		setUpTs(wb);
	}

	protected void setUpText(HSSFWorkbook wb)
	{
		textCellStyle = wb.createCellStyle();
		textCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
	}

	protected void setUpDate(HSSFWorkbook wb)
	{
		dateCellStyle = wb.createCellStyle();
		dateCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		gridDateFormat = safeGetFormat(dataFormat, dateFormat);
		dateCellStyle.setDataFormat(gridDateFormat);
	}

	protected void setUpDecimal(HSSFWorkbook wb)
	{
		decimalCellStyle = wb.createCellStyle();
		decimalCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		gridDecimalFormat = safeGetFormat(dataFormat, decimalFormat);
		decimalCellStyle.setDataFormat(gridDecimalFormat);
	}

	protected void setUpInteger(HSSFWorkbook wb)
	{
		integerCellStyle = wb.createCellStyle();
		integerCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		gridIntegerFormat = safeGetFormat(dataFormat, integerFormat);
		integerCellStyle.setDataFormat(gridIntegerFormat);
	}

	protected void setUpHeader(HSSFWorkbook wb)
	{
		headerCellStyle = wb.createCellStyle();
		HSSFFont font = wb.createFont();
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		headerCellStyle.setFont(font);
		headerCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
	}

	protected void setUpTs(HSSFWorkbook wb)
	{
		tsCellStyle = wb.createCellStyle();
		tsCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		gridTsFormat = safeGetFormat(dataFormat, tsFormat);
		tsCellStyle.setDataFormat(gridTsFormat);
	}

	protected static short safeGetFormat(HSSFDataFormat dataFormat, String formatString)
	{
		short format = HSSFDataFormat.getBuiltinFormat(formatString);
		if (format < 0)
		{
			// It's not a builtin format, need to create
			format = dataFormat.getFormat(formatString);
		}
		return format;
	}
}
