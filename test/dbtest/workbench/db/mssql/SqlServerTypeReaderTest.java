/*
 * SqlServerTypeReaderTest.java
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
package workbench.db.mssql;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

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
public class SqlServerTypeReaderTest
	extends WbTestCase
{

	public SqlServerTypeReaderTest()
	{
		super("SqlServerSynonymReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerProcedureReaderTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", conn);
		SQLServerTestUtil.dropAllObjects(conn);

		String sql =
			"CREATE TYPE address_type \n" +
			"AS \n" +
			"TABLE \n" +
			"( \n" +
			"   streetname  varchar(50), \n" +
			"   city        varchar(50)     DEFAULT ('Munich'), \n" +
			"   some_value  numeric(12,4) \n" +
			")\n" +
			"commit;\n";
		TestUtil.executeScript(conn, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", conn);
		SQLServerTestUtil.dropAllObjects(conn);
	}


	@Test
	public void testReader()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		assertNotNull("No connection available", conn);
		List<TableIdentifier> types = conn.getMetadata().getObjectList(null, new String[] { "TYPE" });
		assertNotNull(types);
		assertEquals(1, types.size());
		assertEquals("address_type", types.get(0).getObjectName());

		String source = types.get(0).getSource(conn).toString().trim();
		String expected =
			"CREATE TYPE dbo.address_type\n" +
			"AS\n" +
			"TABLE\n" +
			"(\n" +
			"   streetname  varchar(50),\n" +
			"   city        varchar(50)     DEFAULT ('Munich'),\n" +
			"   some_value  numeric(12,4)\n" +
			");";
//		System.out.println("----------------\n" + source + "\n++++++++++++\n" + expected);
		assertEquals(expected, source);
	}
}
