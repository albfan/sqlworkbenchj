/*
 * OracleObjectCompiler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to re-compile Oracle objects like stored procedures, packages.
 *
 * @author  Thomas Kellerer
 */
public class OracleObjectCompiler
{
	private WbConnection dbConnection;
	private OracleErrorInformationReader errorReader;

	private static final Set<String> COMPILABLE_TYPES = CollectionUtil.caseInsensitiveSet(
		"VIEW", "PROCEDURE", "MATERIALIZED VIEW", "FUNCTION", "PACKAGE", "TRIGGER"
	);

	public OracleObjectCompiler(WbConnection conn)
		throws SQLException
	{
		dbConnection = conn;
		errorReader = new OracleErrorInformationReader(dbConnection);
	}

	/**
	 * Recompile the given object.
	 *
	 * @param object the object to recompile
	 * @return the error message if the compile was not successful
	 *         null if the compile was ok.
	 */
	public String compileObject(DbObject object)
	{
		String sql = createCompileStatement(object);

		if (Settings.getInstance().getLogAllStatements())
		{
			LogMgr.logInfo("OracleObjectCompiler.compileObject()", "Compiling " + object.getObjectType() + " " + object.getObjectName() + " using: " + sql);
		}

		Statement stmt = null;
		try
		{
			stmt = dbConnection.createStatement();
			this.dbConnection.setBusy(true);
			stmt.executeUpdate(sql);
			String error = errorReader.getErrorInfo(null, object.getObjectName(), object.getObjectType(), false);
			if (StringUtil.isBlank(error))
			{
				return null;
			}
			return error.trim();
		}
		catch (SQLException e)
		{
			return e.getMessage();
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
			this.dbConnection.setBusy(false);
		}
	}

	String createCompileStatement(DbObject object)
	{
		StringBuilder sql = new StringBuilder(50);
		sql.append("ALTER ");

		if (StringUtil.isNonBlank(object.getCatalog()))
		{
			// If it's a package, compile the whole package.
			sql.append("PACKAGE ");
			sql.append(object.getSchema());
			sql.append('.');
			sql.append(object.getCatalog());
		}
		else
		{
			sql.append(object.getObjectType());
			sql.append(' ');
			sql.append(object.getFullyQualifiedName(dbConnection));
		}
		sql.append(" COMPILE");
		return sql.toString();
	}

	public static boolean canCompile(DbObject object)
	{
		if (object == null) return false;
		String type = object.getObjectType();
		return COMPILABLE_TYPES.contains(type);
	}

}
