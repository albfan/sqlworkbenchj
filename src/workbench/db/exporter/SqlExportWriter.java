/*
 * SqlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.exporter;

import workbench.storage.ResultInfo;

/**
 *
 * @author  info@sql-workbench.net
 */
public class SqlExportWriter
	extends ExportWriter
{

	/** Creates a new instance of SqlExportWriter */
	public SqlExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter(ResultInfo info)
	{
		SqlRowDataConverter converter = new SqlRowDataConverter(info);
		converter.setOriginalConnection(exporter.getConnection());
		converter.setCommitEvery(exporter.getCommitEvery());
		converter.setChrFunction(exporter.getChrFunction());
		converter.setConcatString(exporter.getConcatString());
		converter.setConcatFunction(exporter.getConcatFunction());
		// the key columns need to be set before the createInsert flag!
		converter.setKeyColumnsToUse(exporter.getKeyColumnsToUse());
		converter.setCreateInsert(exporter.getCreateSqlInsert());
		converter.setSql(exporter.getSql());
		converter.setAlternateUpdateTable(exporter.getTableName());
		converter.setCreateTable(exporter.isIncludeCreateTable());
		return converter;
	}

}
