/*
 * XlsExportWriter.java
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

import java.awt.Point;

/**
 * Export data into an Excel spreadsheet using Apache's POI
 *
 * @author Alessandro Palumbo
 */
public class XlsExportWriter
	extends ExportWriter
{

	public XlsExportWriter(DataExporter exp)
	{
		super(exp);
		canAppendStart = true;
	}

	@Override
	public RowDataConverter createConverter()
	{
		return new XlsRowDataConverter();
	}

	@Override
	public void configureConverter()
	{
		super.configureConverter();
		converter.setNullString(exporter.getNullString());
		XlsRowDataConverter xls = (XlsRowDataConverter) converter;
		xls.setOptimizeColumns(exporter.getOptimizeSpreadsheetColumns());
		xls.setAppend(exporter.getAppendToFile());
		xls.setTargetSheetIndex(exporter.getTargetSheetIndex());
		xls.setTargetSheetName(exporter.getTargetSheetName());
    Point offset = exporter.getSpreadSheetOffset();
    if (offset != null)
    {
      xls.setStartOffset(offset.y, offset.x);
    }
	}

	@Override
	public boolean managesOutput()
	{
		return true;
	}
}
