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
 *
 * @author  support@sql-workbench.net
 */
public class OdsExportWriter
	extends ExportWriter
{
	public OdsExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter()
	{
		return new OdsRowDataConverter();
	}
	
	public boolean managesOutput()
	{
		return true;
	}

}
