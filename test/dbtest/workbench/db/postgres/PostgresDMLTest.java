/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.StatementFactory;

import workbench.util.SqlUtil;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresDMLTest
	extends WbTestCase
{

	public PostgresDMLTest()
	{
		super("PostgresDML");
	}

	@After
	public void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testRetrieveGenerated()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

		Statement stmt = con.createStatement();
		TestUtil.executeScript(con,
			"create table gen_test(id_1 serial primary key, id_2 serial, some_data varchar(100)); \n" +
			"insert into gen_test (some_data) values ('foobar'); \n" +
			"select setval('gen_test_id_2_seq', 100, false); \n" +
			"commit;");

		String sql = "select id_1, id_2, some_data from gen_test";

		ResultSet rs = stmt.executeQuery(sql);
		DataStore ds = new DataStore(rs, con);
		SqlUtil.closeAll(rs, stmt);
		ds.setGeneratingSql(sql);
		ds.checkUpdateTable(con);

		ds.setValue(0, 2, "bar");

		int row = ds.addRow();
		ds.setValue(row, 2, "foo");
		ds.updateDb(con, null);
		int id = ds.getValueAsInt(row, 0, Integer.MIN_VALUE);
		assertEquals(2, id);

		id = ds.getValueAsInt(row, 1, Integer.MIN_VALUE);
		assertEquals(100, id);

		id = ds.getValueAsInt(0, 0, Integer.MIN_VALUE);
		assertEquals(1, id);

		ds.deleteRow(0);
		ds.updateDb(con, null);
		id = ds.getValueAsInt(0, 0, Integer.MIN_VALUE);
		assertEquals(2, id);
	}

	@Test
	public void testUpdateType()
		throws Exception
	{
		try
		{
			WbConnection conn = PostgresTestUtil.getPostgresConnection();
      assertNotNull(conn);

			TestUtil.executeScript(conn,
				"create type rating_range as (min_value integer, max_value integer);\n" +
				"create table ratings (id integer not null primary key, rating rating_range);\n" +
				"commit;"
			);

			Statement query = conn.createStatement();
			ResultSet rs = query.executeQuery("select id, rating from ratings");
			ResultInfo info = new ResultInfo(rs.getMetaData(), conn);
			info.setUpdateTable(new TableIdentifier("ratings"));
			rs.close();

			StatementFactory factory = new StatementFactory(info, conn);
			RowData row = new RowData(info);
			row.setValue(0, Integer.valueOf(42));
			row.setValue(1, "(1,2)");
			DmlStatement dml = factory.createInsertStatement(row, true, "\n");
			int rows = dml.execute(conn, true);
			conn.commit();
			assertEquals(1, rows);
			int count = ((Number)TestUtil.getSingleQueryValue(conn, "select count(*) from ratings where id = 42")).intValue();
			assertEquals(1, count);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testUpdateArray()
		throws Exception
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
    assertNotNull(conn);

		try
		{
			TestUtil.executeScript(conn,
				"create table array_test (id integer not null primary key, tags varchar[]);\n" +
				"commit;"
			);
			Statement query = conn.createStatement();
			ResultSet rs = query.executeQuery("select id, tags from array_test");
			ResultInfo info = new ResultInfo(rs.getMetaData(), conn);
			info.setUpdateTable(new TableIdentifier("array_test"));
			rs.close();

			StatementFactory factory = new StatementFactory(info, conn);
			RowData row = new RowData(info);
			row.setValue(0, Integer.valueOf(42));
			row.setValue(1, "1,2");
			DmlStatement dml = factory.createInsertStatement(row, true, "\n");
			int rows = dml.execute(conn, true);
			conn.commit();
			assertEquals(1, rows);
			int count = ((Number) TestUtil.getSingleQueryValue(conn, "select count(*) from array_test where id = 42")).intValue();
			assertEquals(1, count);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}

	}
}
