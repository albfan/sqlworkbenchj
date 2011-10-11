/*
 * DefaultTransactionChecker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DefaultTransactionChecker
	implements TransactionChecker
{

	private String query;
	public DefaultTransactionChecker(String sql)
	{
		query = sql;
	}

	@Override
	public boolean hasUncommittedChanges(WbConnection con)
	{
		Savepoint sp = null;
		ResultSet rs = null;
		Statement stmt = null;
		int count = 0;
		try
		{
			if (con.getDbSettings().useSavePointForDML())
			{
				sp = con.setSavepoint();
			}
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(query);
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			con.releaseSavepoint(sp);
		}
		catch (SQLException sql)
		{
			LogMgr.logDebug(getClass().getSimpleName() + ".hasUncommittedChanges()", "Could not retrieve transaction state", sql);
			con.rollback(sp);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return count > 0;
	}

}
