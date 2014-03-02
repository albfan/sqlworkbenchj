/*
 * MySQLColumnEnhancerTest.java
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
package workbench.db.mysql;

import java.sql.SQLException;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLColumnEnhancerTest
	extends WbTestCase
{

	public MySQLColumnEnhancerTest()
	{
		super("MySqlEnumReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		MySQLTestUtil.initTestcase("MySqlEnumReaderTest");

		WbConnection con = MySQLTestUtil.getMySQLConnection();
		Assume.assumeNotNull("No connection available", con);

		String sql = "CREATE TABLE enum_test \n" +
								 "( \n" +
								 "   nr     INT, \n" +
								 "   color  enum('red','green','blue') \n" +
								 ");";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		Assume.assumeNotNull("No connection available", con);
		String sql = "DROP TABLE enum_test;";
		TestUtil.executeScript(con, sql);
		MySQLTestUtil.cleanUpTestCase();
	}

	@Test
	public void testUpdateColumnDefinition()
		throws SQLException
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		assertNotNull("No connection available", con);

		TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("enum_test"));
		assertNotNull(def);

		List<ColumnIdentifier> cols = def.getColumns();
		assertNotNull(cols);
		assertEquals(2, cols.size());
		String type = cols.get(1).getDbmsType();
		assertEquals("enum('red','green','blue')", type);
	}
}
