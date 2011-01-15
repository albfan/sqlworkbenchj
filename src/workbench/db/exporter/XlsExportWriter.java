/*
 * XlsExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

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
	}

	public RowDataConverter createConverter()
	{
		return new XlsRowDataConverter();
	}

	public boolean managesOutput()
	{
		return true;
	}
	
}
