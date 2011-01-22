/*
 * XlsXMLExportWriter.java
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
 * Export writer for the new MS XML Spreadsheet format
 *
 * @author  Thomas Kellerer
 */
public class XlsXMLExportWriter
	extends ExportWriter
{
	public XlsXMLExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter()
	{
		return new XlsXMLRowDataConverter();
	}

}
