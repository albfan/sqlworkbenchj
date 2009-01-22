/*
 * DummyInsert.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;
import java.sql.SQLException;
import workbench.resource.Settings;
import workbench.storage.DmlStatement;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;
import workbench.storage.StatementFactory;
import workbench.util.SqlUtil;
/**
 * @author support@sql-workbench.net
 */
public class DummyInsert 
	implements DbObject
{
	private TableIdentifier table;
	
	public DummyInsert(TableIdentifier tbl)
	{
		this.table = tbl;
	}

	public String getComment()
	{
		return null;
	}

	public void setComment(String c)
	{
	}
	
	public String getCatalog()
	{
		return null;
	}

	public String getObjectExpression(WbConnection conn)
	{
		return null;
	}

	public String getObjectName()
	{
		return null;
	}

	public String getObjectName(WbConnection conn)
	{
		return null;
	}

	public String getObjectNameForDrop(WbConnection con)
	{
		return null;
	}

	public String getObjectType()
	{
		return "INSERT";
	}

	public String getSchema()
	{
		return null;
	}

	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
    boolean makePrepared = Settings.getInstance().getBoolProperty("workbench.sql.generate.defaultinsert.prepared", false);
		ResultInfo info = new ResultInfo(table, con);
		info.setUpdateTable(table);
		StatementFactory factory = new StatementFactory(info, con);
		
		SqlLiteralFormatter f = new SqlLiteralFormatter(con);
		
		RowData dummyData = new RowData(info.getColumnCount());

		// This is a "trick" to fool the StatementFactory which will
		// check the type of the Data. In case it does not "know" the 
		// class, it calls toString() to get the value of the column
		// this way we get a question mark for each value
		StringBuilder marker = new StringBuilder("?");
		
		for (int i=0; i < info.getColumnCount(); i++)
		{
			if (makePrepared)
			{
				dummyData.setValue(i, marker);
			}
			else
			{
				int type = info.getColumnType(i);
				StringBuilder dummy = new StringBuilder();
				if (SqlUtil.isCharacterType(type)) dummy.append('\'');
				dummy.append(info.getColumnName(i));
				dummy.append("_value");
				if (SqlUtil.isCharacterType(type)) dummy.append('\'');
				dummyData.setValue(i, dummy);
			}
		}
		String le = Settings.getInstance().getInternalEditorLineEnding();
		DmlStatement stmt = factory.createInsertStatement(dummyData, true, le);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		String sql = stmt.getExecutableStatement(f) + ";" + nl;
		return sql;
	}
	
}
