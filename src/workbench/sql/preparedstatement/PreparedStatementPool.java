/*
 * PreparedStatementPool.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.preparedstatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 * A Pool to store parameters for prepared statements.
 *
 * @author  Thomas Kellerer
 */
public class PreparedStatementPool
{
	private Map<String, StatementParameters> statements = new HashMap<String, StatementParameters>();
	private WbConnection dbConnection;

	public PreparedStatementPool(WbConnection conn)
	{
		setConnection(conn);
	}

	private void setConnection(WbConnection conn)
	{
		this.done();
		this.dbConnection = conn;
	}

	public void done()
	{
		this.dbConnection = null;
		if (this.statements != null) this.statements.clear();
	}

	public synchronized StatementParameters getParameters(String sql)
	{
		return this.statements.get(getSqlToUse(sql));
	}

	public synchronized boolean addPreparedStatement(String sql)
		throws SQLException
	{
		if (sql == null) return false;

		// this might give a false positive if the ? is part of a comment
		if (sql.indexOf('?') == -1) return false;

		// remove all comments, because the ? could also be part of a comment
		String clean = SqlUtil.makeCleanSql(sql, false, false);
		if (clean.indexOf('?') == -1) return false;

		sql = getSqlToUse(sql);

		if (this.statements.containsKey(sql))
		{
			return true;
		}
		StatementParameters p = new StatementParameters(sql, this.dbConnection);
		if (!p.hasParameter())
		{
			return false;
		}
		this.statements.put(sql, p);
		return true;
	}

	public PreparedStatement prepareStatement(String sql)
		throws SQLException
	{
		if (this.dbConnection == null) throw new NullPointerException("No SQL Connection!");

		sql = getSqlToUse(sql);
		StatementParameters parm = this.getParameters(sql);
		if (parm == null) throw new IllegalArgumentException("SQL Statement has not been registered with pool");

		PreparedStatement pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
		parm.applyParameter(pstmt);
		return pstmt;
	}

	private String getSqlToUse(String sql)
	{
		if (dbConnection.getDbSettings().useCleanSQLForPreparedStatements())
		{
			// this is mainly for Microsoft's buggy JDBC driver which can't parse statements with embedded newlines
			return SqlUtil.makeCleanSql(sql, false, false);
		}
		return sql;
	}
	public synchronized boolean isRegistered(String sql)
	{
		return this.statements.containsKey(getSqlToUse(sql));
	}
}
