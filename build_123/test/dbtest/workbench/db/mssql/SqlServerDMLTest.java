/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.mssql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerDMLTest
	extends WbTestCase
{

	public SqlServerDMLTest()
	{
		super("SqlServerDMLTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerDropTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull(conn);
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull(conn);
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@Test
	public void testUpdateDatastore()
		throws SQLException
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		assertNotNull("No connection available", con);

		TestUtil.executeScript(con,
			"create table gen_test(pk_col integer not null primary key identity, some_data varchar(100)); \n" +
			"insert into gen_test (some_data) values ('foobar'); \n" +
			"commit;");

		String sql = "select pk_col, some_data from gen_test";

		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		DataStore ds = new DataStore(rs, con);

		SqlUtil.closeAll(rs, stmt);

		ds.setGeneratingSql(sql);
		ds.checkUpdateTable(con);

		// change the existing row
		ds.setValue(0, 1, "bar");

		// add a new row to retrieve the identity value
		int row = ds.addRow();
		ds.setValue(row, 1, "foo");
		ds.updateDb(con, null);
		int id = ds.getValueAsInt(row, 0, Integer.MIN_VALUE);
		assertEquals(2, id);

		// make sure the modified row was not changed
		id = ds.getValueAsInt(0, 0, Integer.MIN_VALUE);
		assertEquals(1, id);

		// make sure no existing row is changed when deleting something
		ds.deleteRow(0);
		ds.updateDb(con, null);
		id = ds.getValueAsInt(0, 0, Integer.MIN_VALUE);
		assertEquals(2, id);
	}

}
