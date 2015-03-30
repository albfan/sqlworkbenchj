/*
 * WbListTablesTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.StatementRunnerResult;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class WbListTablesTest
	extends WbTestCase
{

	public WbListTablesTest()
	{
		super("WbListTablesTest");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		String sql =
			"create table person (nr integer, name varchar(10));\n" +
			"create table address (id integer, info varchar(100));\n" +
			"create view v_address as select * from address;\n" +
			"create view all_people as select * from person;\n" +
			"commit;";

		WbConnection con = null;
		TestUtil util = new TestUtil("ListTest");

		try
		{
			con = util.getConnection();
			TestUtil.executeScript(con, sql);
			WbListTables list = new WbListTables();
			list.setConnection(con);
			StatementRunnerResult result = list.execute("wblist");
			assertTrue(result.isSuccess());
			List<DataStore> data = result.getDataStores();
			assertNotNull(data);
			assertTrue(data.size() == 1);
			DataStore objectList = data.get(0);
			assertNotNull(objectList);

			// default is to list tables only
			assertEquals(2, objectList.getRowCount());

			result = list.execute("wblist -types=VIEW,TABLE");
			assertTrue(result.isSuccess());
			objectList = result.getDataStores().get(0);
			assertEquals(4, objectList.getRowCount());

			result = list.execute("wblist -types=VIEW,TABLE -objects=P%,A%");
			assertTrue(result.isSuccess());
			objectList = result.getDataStores().get(0);
			assertEquals(3, objectList.getRowCount());
			objectList.sortByColumn(0, true);
			assertEquals("ADDRESS", objectList.getValueAsString(0, 0));
			assertEquals("ALL_PEOPLE", objectList.getValueAsString(1, 0));
			assertEquals("PERSON", objectList.getValueAsString(2, 0));
		}
		finally
		{
			con.disconnect();
		}
	}

	@Test
	public void testSchemaRetrieve()
		throws Exception
	{
		String sql =
			"create table person (nr integer, name varchar(10));\n" +
			"create table address (id integer, info varchar(100));\n" +
			"create view v_address as select * from address;\n" +
			"create view all_people as select * from person;\n" +
			"create schema test_2;\n" +
			"set schema test_2;\n" +
			"create table p2 (nr integer, name varchar(20));\n" +
			"create table a2 (nr integer, name_2 varchar(100));\n" +
			"commit;\n" +
			"set schema public;\n" +
			"commit;";

		WbConnection con = null;
		TestUtil util = new TestUtil("ListTest");

		try
		{
			con = util.getConnection();
			TestUtil.executeScript(con, sql);
			WbListTables list = new WbListTables();
			list.setConnection(con);
			StatementRunnerResult result = list.execute("wblist -schema=test_2");
			assertTrue(result.isSuccess());
			List<DataStore> data = result.getDataStores();
			assertNotNull(data);
			assertTrue(data.size() == 1);
			DataStore objectList = data.get(0);
			assertNotNull(objectList);

			// default is to list tables only
			assertEquals(2, objectList.getRowCount());


			String tname = objectList.getValueAsString(0, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			assertEquals("A2", tname);

			tname = objectList.getValueAsString(1, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			assertEquals("P2", tname);
		}
		finally
		{
			con.disconnect();
		}
	}

}
