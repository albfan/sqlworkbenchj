/*
 * XmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

/**
 * Export writer for the new MS XML Spreadsheet format
 * @author  support@sql-workbench.net
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
