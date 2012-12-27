/*
 * SqlServerSynonymReaderTest.java
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
package workbench.db.mssql;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerSynonymReaderTest
	extends WbTestCase
{

	public SqlServerSynonymReaderTest()
	{
		super("SqlServerSynonymReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerProcedureReaderTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SQLServerTestUtil.dropAllObjects(conn);

		String sql =
			"create table person (id integer);\n " +
			"create synonym s_person for person;\n " +
			"commit;\n";
		TestUtil.executeScript(conn, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SQLServerTestUtil.dropAllObjects(conn);
	}


	@Test
	public void testReader()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SynonymReader reader = conn.getMetadata().getSynonymReader();
		assertNotNull(reader);
		List<TableIdentifier> syns = reader.getSynonymList(conn, null, "dbo", null);
		assertNotNull(syns);
		assertEquals(1, syns.size());
		assertEquals("s_person", syns.get(0).getObjectName());

		String source = syns.get(0).getSource(conn).toString().trim();
		String expected =
				"CREATE SYNONYM dbo.s_person\n" +
				"   FOR dbo.person;";
//		System.out.println("***********\n" + expected + "\n--------------\n" + source + "\n++++++++++++++++++++");
		assertEquals(expected, source);
		TableIdentifier table = conn.getMetadata().getSynonymTable(syns.get(0));
		assertNotNull(table);
		assertEquals("person", table.getTableName());

		syns = reader.getSynonymList(conn, null, "dbo", "s_person");
		assertEquals(1, syns.size());
	}
}
