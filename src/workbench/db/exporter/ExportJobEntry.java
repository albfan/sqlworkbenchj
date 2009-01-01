/*
 * ExportJobEntry.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.ResultInfo;
import workbench.util.SqlUtil;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class ExportJobEntry
{
	private WbFile outputFile;
	private String query;
	private TableIdentifier baseTable;
	private ResultInfo resultInfo;
	
	public ExportJobEntry(File file, String sql)
	{
		outputFile = new WbFile(file);
		query = sql;
		List<String> tables = SqlUtil.getTables(query);
		if (tables.size() == 1)
		{
			this.baseTable = new TableIdentifier(tables.get(0)); 
		}
	}

	public ExportJobEntry(File file, TableIdentifier table, WbConnection con)
		throws SQLException
	{
		resultInfo = new ResultInfo(table, con);
		outputFile = new WbFile(file);
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT ");
		ColumnIdentifier[] cols = resultInfo.getColumns();
		if (cols.length == 0) throw new SQLException("Table '" + table.getTableExpression() + "' not found!");
		for (int i = 0; i < cols.length; i++)
		{
			if (i > 0) sql.append(',');
			sql.append(cols[i].getColumnName());
		}
		sql.append(" FROM ");
		baseTable = table;
		sql.append(table.getTableExpression(con));
		resultInfo.setUpdateTable(baseTable);
		this.query = sql.toString();
	}
	
	public WbFile getOutputFile()
	{
		return outputFile;
	}

	public ResultInfo getResultInfo()
	{
		return resultInfo;
	}
	
	public TableIdentifier getTable()
	{
		if (resultInfo != null)
		{
			return resultInfo.getUpdateTable();
		}
		return baseTable;
	}
	
	public String getQuerySql()
	{
		return query;
	}

}
