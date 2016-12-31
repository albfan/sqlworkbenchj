/*
 * SQLServerTestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.mssql;

import java.sql.SQLException;

import workbench.AppArguments;
import workbench.TestUtil;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.sql.BatchRunner;

import workbench.util.ArgumentParser;

/**
 *
 * @author Thomas Kellerer
 */
public class SQLServerTestUtil
{
	public static final String TEST_USER = "wbjunit";
	public static final String TEST_PWD = "wbjunit";
	public static final String PROFILE_NAME = "WBJUnitMSSQL";
	public static final String DB_NAME = "wb_junit";

	/**
	 * Return a connection to a locally running SQL Server database on port 1433
	 */
	public static WbConnection getSQLServerConnection()
	{
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(PROFILE_NAME);
			if (con != null) return con;

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:sqlserver://localhost:1433;databaseName=" + DB_NAME + "' -username=" + TEST_USER + " -password=" + TEST_PWD + " -driver=com.microsoft.sqlserver.jdbc.SQLServerDriver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setName(PROFILE_NAME);
			ConnectionMgr.getInstance().addProfile(prof);
			con = ConnectionMgr.getInstance().getConnection(prof, PROFILE_NAME);
			return con;
		}
		catch (Throwable th)
		{
			th.printStackTrace();
			return null;
		}
	}


	public static void initTestcase(String name)
		throws Exception
	{
		TestUtil util = new TestUtil(name);
		util.prepareEnvironment();
	}

	public static void dropAllObjects(WbConnection con)
	{
		String sql =
				"declare @n char(1) \n" +
				"set @n = char(10) \n" +
				" \n" +
				"declare @stmt nvarchar(max) \n" +
				" \n" +
				"-- procedures \n" +
				"select @stmt = isnull( @stmt + @n, '' ) + \n" +
				"    'drop procedure [' + name + ']' \n" +
				"from sys.procedures \n" +
				" \n" +
			  " -- synonyms \n" +
        "select @stmt = isnull( @stmt + @n, '' ) + 'drop synonym [' + name + ']' \n" +
        "from sys.synonyms \n"+
			  "-- check constraints \n" +
				"select @stmt = isnull( @stmt + @n, '' ) + \n" +
				"    'alter table [' + object_name( parent_object_id ) + '] drop constraint [' + name + ']' \n" +
				"from sys.check_constraints \n" +
				" \n" +
				"-- functions \n" +
				"select @stmt = isnull( @stmt + @n, '' ) + \n" +
				"    'drop function [' + name + ']' \n" +
				"from sys.objects \n" +
				"where type in ( 'FN', 'IF', 'TF' ) \n" +
				" \n" +
				"-- views \n" +
				"select @stmt = isnull( @stmt + @n, '' ) + \n" +
				"    'drop view [' + name + ']' \n" +
				"from sys.views \n" +
				" \n" +
				"-- foreign keys \n" +
				"select @stmt = isnull( @stmt + @n, '' ) + \n" +
				"    'alter table [' + object_name( parent_object_id ) + '] drop constraint [' + name + ']' \n" +
				"from sys.foreign_keys \n" +
				" \n" +
				"-- tables \n" +
				"select @stmt = isnull( @stmt + @n, '' ) + \n" +
				"    'drop table [' + name + ']' \n" +
				"from sys.tables \n" +
				" \n" +
				"-- user defined types \n" +
				"select @stmt = isnull( @stmt + @n, '' ) + \n" +
				"    'drop type [' + name + ']' \n" +
				"from sys.types \n" +
				"where is_user_defined = 1 \n" +
				" \n" +
				"select @stmt = isnull( @stmt + @n, '' ) + \n" +
				"    'drop rule [' + name + ']' \n" +
				"from sys.objects \n" +
				"where type in ( 'R' ) \n" +
			  " \n" +
				"exec sp_executesql @stmt";
		try
		{
			TestUtil.executeScript(con, sql);
			con.commit();
			con.getObjectCache().clear();
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
		}
	}
}
