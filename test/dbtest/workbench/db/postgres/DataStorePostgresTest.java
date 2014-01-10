/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.storage.DataStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DataStorePostgresTest
	extends WbTestCase
{

	public DataStorePostgresTest()
	{
		super("PgDataStoreTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase("ds_test_pg");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;
		PostgresTestUtil.dropAllObjects(con);
	}

	@Test
	public void testRetrieveGenerated()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		Statement stmt = con.createStatement();
		TestUtil.executeScript(con,
			"create table gen_test(id serial primary key, some_data varchar(100)); \n" +
			"insert into gen_test (some_data) values ('foobar'); \n" +
			"commit;");

		String sql = "select id, some_data from gen_test";

		ResultSet rs = stmt.executeQuery(sql);
		DataStore ds = new DataStore(rs, con);
		rs.close();
		stmt.close();
		ds.setGeneratingSql(sql);
		ds.checkUpdateTable(con);

		ds.setValue(0, 1, "bar");
		int row = ds.addRow();
		ds.setValue(row, 1, "foo");
		ds.updateDb(con, null);
		int id = ds.getValueAsInt(row, 0, Integer.MIN_VALUE);
		assertEquals(id, 2);

		id = ds.getValueAsInt(0, 0, Integer.MIN_VALUE);
		assertEquals(id, 1);

		ds.deleteRow(0);
		ds.updateDb(con, null);
		id = ds.getValueAsInt(0, 0, Integer.MIN_VALUE);
		assertEquals(id, 2);
	}

}
