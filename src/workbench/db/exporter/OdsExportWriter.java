/*
 * OdsExportWriter.java
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
 * An export writer to create an OpenDocument spreadsheet format.
 *
 * @author  Thomas Kellerer
 * @see OdsRowDataConverter
 */
public class OdsExportWriter
	extends ExportWriter
{
	public OdsExportWriter(DataExporter exp)
	{
		super(exp);
	}

	@Override
	public RowDataConverter createConverter()
	{
		return new OdsRowDataConverter();
	}

	@Override
	public boolean managesOutput()
	{
		return true;
	}

}
