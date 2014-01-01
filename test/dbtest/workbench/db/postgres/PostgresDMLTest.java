/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DmlStatement;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.StatementFactory;

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
	public void testUpdateType()
		throws Exception
	{
		try
		{
			WbConnection conn = PostgresTestUtil.getPostgresConnection();
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
			int rows = dml.execute(conn);
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
			int rows = dml.execute(conn);
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
