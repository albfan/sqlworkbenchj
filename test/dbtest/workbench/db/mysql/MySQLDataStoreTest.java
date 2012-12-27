/*
 * MySQLDataStoreTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLDataStoreTest
	extends WbTestCase
{

	public MySQLDataStoreTest()
	{
		super("MySQLDataStoreTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		MySQLTestUtil.initTestcase("MySQLDataStoreTest");

		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;

		String sql =
			"CREATE TABLE data ( id integer primary key, info varchar(100));\n"  +
			"insert into data (id, info) values (1, 'gargleblaster');\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;
		String sql = "DROP TABLE data;";
		TestUtil.executeScript(con, sql);
		MySQLTestUtil.cleanUpTestCase();
	}

	@Test
	public void testUpdateColumnDefinition()
		throws SQLException
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null)
		{
			return;
		}

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT * from data";
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(rs, con, true);
			ds.setGeneratingSql(sql);
			boolean canUpdate = ds.checkUpdateTable();
			assertTrue(canUpdate);
			TableIdentifier table = ds.getUpdateTable();
			assertEquals("data", table.getTableName());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}
}
