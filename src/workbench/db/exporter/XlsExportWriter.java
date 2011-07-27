/*
 * XlsExportWriter.java
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

	@Override
	public RowDataConverter createConverter()
	{
		return new XlsRowDataConverter();
	}

	@Override
	public boolean managesOutput()
	{
		return true;
	}

}
