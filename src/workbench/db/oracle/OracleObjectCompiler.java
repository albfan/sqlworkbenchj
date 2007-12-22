/*
 * OracleObjectCompiler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.SQLException;
import java.sql.Statement;

import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class OracleObjectCompiler
{
	private WbConnection dbConnection;
	private Statement stmt;
	private String lastError = null;
	
	public OracleObjectCompiler(WbConnection conn)
		throws SQLException
	{
		this.dbConnection = conn;
		this.stmt = this.dbConnection.createStatement();
	}

	public void close()
	{
		if (this.stmt != null)
		{
			SqlUtil.closeStatement(stmt);
		}
	}
	
	public String getLastError()
	{
		return this.lastError;
	}
	
	public String compileObject(DbObject object)
	{
		String sql = "ALTER " + object.getObjectType() + " " + object.getObjectExpression(dbConnection) + " COMPILE";
		this.lastError = null;
		try
		{
			this.dbConnection.setBusy(true);
			this.stmt.executeUpdate(sql);
			return null;
		}
		catch (SQLException e)
		{
			return e.getMessage();
		}
		finally
		{
			this.dbConnection.setBusy(false);
		}
	}
	
	public static boolean canCompile(DbObject object)
	{
		if (object == null) return false;
		String type = object.getObjectType();
		return (type.equalsIgnoreCase("VIEW") || 
			type.equalsIgnoreCase("PROCEDURE") || 
			type.equalsIgnoreCase("MATERIALIZED VIEW") || 
			type.equalsIgnoreCase("FUNCTION") || 
			type.equalsIgnoreCase("PACKAGE") ||
			type.equalsIgnoreCase("TRIGGER"));
	}
	
}
