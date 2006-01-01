/*
 * ExportJobEntry.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.sql.SQLException;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.ResultInfo;

/**
 *
 * @author support@sql-workbench.net
 */
public class ExportJobEntry
{
	// <editor-fold defaultstate="collapsed" desc=" Variables ">
	private ResultInfo resultInfo;
	private String outputFileName;
	private String query;
	// </editor-fold>
	
	public ExportJobEntry(String outputfile, TableIdentifier table, WbConnection con)
		throws SQLException
	{
		resultInfo = new ResultInfo(table, con);
		outputFileName = outputfile;
		StringBuffer sql = new StringBuffer(100);
		sql.append("SELECT ");
		ColumnIdentifier[] cols = resultInfo.getColumns();
		for (int i = 0; i < cols.length; i++)
		{
			if (i > 0) sql.append(',');
			sql.append(cols[i].getColumnName());
		}
		sql.append(" FROM ");
		resultInfo.setUpdateTable(table);
		sql.append(table.getTableExpression(con));
		this.query = sql.toString();
	}
	
	public String getOutputFile()
	{
		return outputFileName;
	}
	
	public String getTableName()
	{
		if (resultInfo != null)
		{
			return resultInfo.getUpdateTable().getTableName();
		}
		else
		{
			return null;
		}
	}
	
	public String getQuerySql()
	{
		return query;
	}
	
	public ResultInfo getResultInfo()
	{
		return resultInfo;
	}
}
