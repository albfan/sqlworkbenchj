/*
 * XlsExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

/**
 * Export data into an Excel 2007 (XLSX, Office Open) spreadsheet using Apache's POI
 * 
 * @author Alessandro Palumbo
 */
public class XlsxExportWriter
	extends ExportWriter
{
	
	public XlsxExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter()
	{
		XlsRowDataConverter xls = new XlsRowDataConverter();
		xls.setUseXLSX();
		return xls;
	}

	public boolean managesOutput()
	{
		return true;
	}
	
}
