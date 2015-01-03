/*
 * OracleTestUtil.java
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.AppArguments;
import workbench.TestUtil;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.sql.BatchRunner;
import workbench.sql.DelimiterDefinition;

import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTestUtil
{
	public static final String SCHEMA_NAME = "WBJUNIT";
	public static final String SCHEMA2_NAME = "WBJUNIT2";

	/**
	 * Return a connection to a locally running Oracle database.
	 *
	 * The user WBJUNIT should have the password WBJUNIT and the following privileges:
	 * <ul>
	 * <li>create synonym</li>
	 * <li>create materialized view</li>
	 * <li>create sequence</li>
	 * <li>create type</li>
	 * <li>create view</li>
	 * </ul>
	 * @return null if Oracle is not available
	 */
	public static WbConnection getOracleConnection()
	{
		return getOracleConnection(SCHEMA_NAME);
	}

	public static WbConnection getOracleConnection2()
	{
		return getOracleConnection(SCHEMA2_NAME);
	}

	public static WbConnection getOracleConnection(String username)
	{
		return getOracleConnection(username, "wbjunit", "localhost", "oradb");
	}

	public static WbConnection getOracleConnection(String username, String password, String servername, String sid)
	{
		final String id = "Oracle_" + username;
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(id);
			if (con != null) return con;

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:oracle:thin:@" + servername + ":1521:" + sid + "' -username=" + username + " -password=" + password + " -driver=oracle.jdbc.OracleDriver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
			prof.setName(id);
			prof.addConnectionProperty("oracle.jdbc.remarksReporting", "true");
			ConnectionMgr.getInstance().addProfile(prof);
			con = ConnectionMgr.getInstance().getConnection(prof, id);
			return con;
		}
		catch (Throwable th)
		{
			return null;
		}
	}

	public static void initTestCase()
		throws Exception
	{
		initTestCase(SCHEMA_NAME);
	}

	public static void initTestCase(String username)
		throws Exception
	{
		TestUtil util = new TestUtil("OracleJUnit");
		util.prepareEnvironment();

		WbConnection con = getOracleConnection(username);
		if (con == null)
		{
			return;
		}
		dropAllObjects(con);
	}

	public static void cleanUpTestCase()
	{
		cleanUpTestCase(SCHEMA_NAME);
	}

	public static void cleanUpTestCase(String username)
	{
		WbConnection con = getOracleConnection(username);
		dropAllObjects(con);
		ConnectionMgr.getInstance().disconnectAll();
	}

	public static void dropAllObjects(WbConnection con)
	{
		if (con == null) return;

		Statement stmt = null;
		Statement drop = null;
		ResultSet rs = null;
		String sql = "SELECT object_type, object_name FROM user_objects";
		try
		{
			stmt = con.createStatement();
			drop = con.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String type = rs.getString(1);
				String name = rs.getString(2);
				String dropSql = getDropStatement(name, type);
				try
				{
					drop.execute(dropSql);
				}
				catch (SQLException e)
				{
					// ignore
				}
			}
			stmt.executeUpdate("purge recyclebin");
			con.getObjectCache().clear();
		}
		catch (Exception e)
		{
			con.rollbackSilently();
			e.printStackTrace();
		}
		finally
		{
			SqlUtil.closeResult(rs);
			SqlUtil.closeStatement(stmt);
			SqlUtil.closeStatement(drop);
		}
	}

	private static String getDropStatement(String name, String type)
	{
		if (type.equals("TABLE"))
		{
			return "DROP TABLE " + name + " CASCADE CONSTRAINTS";
		}
		else
		{
			return "DROP " + type + " " + name;
		}
	}
}
