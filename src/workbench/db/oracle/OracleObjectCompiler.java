/*
 * OracleObjectCompiler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.oracle;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbObject;
import workbench.db.WbConnection;

import workbench.sql.ErrorDescriptor;

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
		"VIEW", "PROCEDURE", "MATERIALIZED VIEW", "FUNCTION", "PACKAGE", "TRIGGER", "TYPE"
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
      String type = object.getObjectType();
      String name = object.getObjectName();
      if ("PACKAGE".equals(type))
      {
        // an "alter package .. compile"  will report errors for the package body, not the package
        type = "PACKAGE BODY";
      }

      if ("PACKAGE BODY".equals(type))
      {
        // the errors will be reported for the package name, not the procedure name
        name = object.getCatalog();
      }
      ErrorDescriptor error = errorReader.getErrorInfo(null, null, name, type, false);
			if (error == null)
			{
				return null;
			}
			return error.getErrorMessage();
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
