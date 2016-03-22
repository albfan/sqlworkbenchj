/*
 * ExcelDataFormat.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
  protected CellStyle multilineCellStyle = null;
  protected short gridDateFormat;
  protected short gridDecimalFormat;
  protected short gridIntegerFormat;
  protected short gridTsFormat;

  ExcelDataFormat(String decFormat, String dtFormat, String intFormat, String tsFormat)
  {
    this.decimalFormat = decFormat;
    this.dateFormat = dtFormat;
    this.integerFormat = intFormat;
    this.timestampFormat = tsFormat;
  }

  protected void setupWithWorkbook(Workbook wb)
  {
    CreationHelper helper = wb.getCreationHelper();
    DataFormat dataFormat = helper.createDataFormat();
    setUpHeader(wb);
    setUpText(wb);
    setUpDate(wb, dataFormat);
    setUpDecimal(wb, dataFormat);
    setUpInteger(wb, dataFormat);
    setUpTs(wb, dataFormat);
    setUpMultiline(wb);
  }

  protected void setUpMultiline(Workbook wb)
  {
    multilineCellStyle = wb.createCellStyle();
    multilineCellStyle.setAlignment(CellStyle.ALIGN_LEFT);
    multilineCellStyle.setWrapText(true);
  }

  protected void setUpText(Workbook wb)
  {
    textCellStyle = wb.createCellStyle();
    textCellStyle.setAlignment(CellStyle.ALIGN_LEFT);
    textCellStyle.setWrapText(false);
  }

  protected void setUpDate(Workbook wb, DataFormat dataFormat)
  {
    dateCellStyle = wb.createCellStyle();
    dateCellStyle.setAlignment(CellStyle.ALIGN_LEFT);
    gridDateFormat = getFormat(dataFormat, dateFormat);
    dateCellStyle.setDataFormat(gridDateFormat);
  }

  protected void setUpDecimal(Workbook wb, DataFormat dataFormat)
  {
    decimalCellStyle = wb.createCellStyle();
    decimalCellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
    gridDecimalFormat = getFormat(dataFormat, decimalFormat);
    decimalCellStyle.setDataFormat(gridDecimalFormat);
  }

  protected void setUpInteger(Workbook wb, DataFormat dataFormat)
  {
    integerCellStyle = wb.createCellStyle();
    integerCellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
    gridIntegerFormat = getFormat(dataFormat, integerFormat);
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

  protected void setUpTs(Workbook wb, DataFormat dataFormat)
  {
    tsCellStyle = wb.createCellStyle();
    tsCellStyle.setAlignment(CellStyle.ALIGN_LEFT);
    gridTsFormat = getFormat(dataFormat, timestampFormat);
    tsCellStyle.setDataFormat(gridTsFormat);
  }

  private short getFormat(DataFormat dataFormat, String formatString)
  {
    return dataFormat.getFormat(formatString);
  }
}
