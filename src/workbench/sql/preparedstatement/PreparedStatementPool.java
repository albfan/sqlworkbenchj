/*
 * PreparedStatementPool.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
 * A Pool to store parameters for prepared statements
 * 
 * @author  support@sql-workbench.net
 */
public class PreparedStatementPool
{
	// A map to store the statements and their parameter definitions
	// The key to the map is the SQL Statement, the value is an Object
	// of type StatementParameter
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
		return this.statements.get(sql);
	}
	
	public synchronized boolean addPreparedStatement(String sql)
		throws SQLException
	{
		if (sql == null) return false;
		if (sql.indexOf('?') == -1) return false;
	
		String clean = SqlUtil.makeCleanSql(sql, false, false);
		if (clean.indexOf('?') == -1) return false;
		
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
		
		StatementParameters parm = this.getParameters(sql);
		if (parm == null) throw new IllegalArgumentException("SQL Statement has not been registered with pool");
		PreparedStatement pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
		parm.applyParameter(pstmt);
		return pstmt;
	}
	
	public synchronized boolean isRegistered(String sql)
	{
		return this.statements.containsKey(sql);
	}
}
