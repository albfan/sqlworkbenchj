/*
 * SqlExportWriter.java
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

import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SqlExportWriter
	extends ExportWriter
{
	
	/** Creates a new instance of SqlExportWriter
	 * @param exp The exporter to convert the rows for
	 */
	public SqlExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter()
	{
		return new SqlRowDataConverter(exporter.getConnection());
	}

	public void configureConverter()
	{
		super.configureConverter();
		SqlRowDataConverter conv = (SqlRowDataConverter)this.converter;
		conv.setIncludeTableOwner(Settings.getInstance().getIncludeOwnerInSqlExport());
		conv.setCommitEvery(exporter.getCommitEvery());
		conv.setChrFunction(exporter.getChrFunction());
		conv.setConcatString(exporter.getConcatString());
		conv.setConcatFunction(exporter.getConcatFunction());
		conv.setSqlLiteralType(exporter.getDateLiteralType());
    
		BlobMode type = exporter.getBlobMode();
		if (type == BlobMode.DbmsLiteral)
		{
			conv.setBlobTypeDbmsLiteral();
		}
		else if (type == BlobMode.AnsiLiteral)
		{
			conv.setBlobTypeAnsiLiteral();
		}
		else if (type == BlobMode.SaveToFile)
		{
			conv.setBlobTypeFile();
		}
		else
		{
			conv.setBlobTypeNone();
		}

		if (exporter.getWriteClobAsFile())
		{
			String encoding = exporter.getEncoding();
			if (encoding == null) encoding = Settings.getInstance().getDefaultFileEncoding();
			conv.setClobAsFile(encoding);
		}
		
		// the key columns need to be set before the createInsert flag!
		conv.setKeyColumnsToUse(exporter.getKeyColumnsToUse());
		try
		{
			conv.setType(exporter.getSqlType());
		}
		catch (IllegalArgumentException e)
		{
			LogMgr.logError("SqlExportWriter.createConverter()", "Illegal SQL type requested. Reverting to INSERT", null);
			conv.setCreateInsert();
		}
		
		String table = exporter.getTableName();
		if (table != null)
		{
			conv.setAlternateUpdateTable(new TableIdentifier(table));
		}
		conv.setCreateTable(exporter.isIncludeCreateTable());
	
	}

}
