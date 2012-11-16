/*
 * ExportJobEntry.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;
import workbench.storage.ResultInfo;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class ExportJobEntry
{
	private WbFile outputFile;
	private String query;
	private TableIdentifier baseTable;
	private ResultInfo resultInfo;

	public ExportJobEntry(File file, String sql, String where, WbConnection conn)
	{
		outputFile = new WbFile(file);
		query = sql;
		List<String> tables = SqlUtil.getTables(query, false, conn);
		if (tables.size() == 1)
		{
			this.baseTable = new TableIdentifier(tables.get(0), conn);
		}
		appendWhere(where);
	}

	public ExportJobEntry(File file, TableIdentifier table, String where, WbConnection con)
		throws SQLException
	{
		resultInfo = new ResultInfo(table, con);
		outputFile = new WbFile(file);
		baseTable = resultInfo.getUpdateTable();
		TableSelectBuilder builder = new TableSelectBuilder(con, "export");
		query = builder.getSelectForColumns(table, Arrays.asList(resultInfo.getColumns()));
		resultInfo.setUpdateTable(baseTable);
		appendWhere(where);
	}

	private void appendWhere(String where)
	{
		if (StringUtil.isNonBlank(where))
		{
			if (!where.trim().toLowerCase().startsWith("where"))
			{
				query += " WHERE";
			}
			query += " ";
			query += SqlUtil.trimSemicolon(where);
		}
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
