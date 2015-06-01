/*
 * DB2SynonymReaderTest.java
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
package workbench.db.ibm;


import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DB2SynonymReaderTest
	extends WbTestCase
{

	public DB2SynonymReaderTest()
	{
		super("DB2SynonymReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String schema = Db2TestUtil.getSchemaName();

		TestUtil.executeScript(con,
			"CREATE TABLE " + schema + ".person (id integer, firstname varchar(50), lastname varchar(50));\n" +
			"CREATE ALIAS " + schema + ".s_person FOR wbjunit.person;\n" +
			"commit;");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		dropObjects(con);
	}

	private static void dropObjects(WbConnection con)
	{
		if (con == null) return;
		try
		{
			TestUtil.executeScript(con,
				"DROP ALIAS wbjunit.s_person;\n" +
				"DROP TABLE wbjunit.person;\n" +
				"commit;");
		}
		catch (SQLException e)
		{
			// ignore
		}
	}

	@Test
	public void testGetSynonymList()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		assertNotNull("No connection available", con);

		SynonymReader reader = con.getMetadata().getSynonymReader();
		assertNotNull(reader);
		Collection<String> types = con.getMetadata().getObjectTypes();
		assertTrue(types.contains("SYNONYM"));

		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "ALIAS"});
		assertNotNull(objects);
		assertEquals(1, objects.size());
		TableIdentifier syn = objects.get(0);
		assertEquals("ALIAS", syn.getObjectType());

		TableIdentifier table = con.getMetadata().getSynonymTable(syn);
		assertNotNull(table);
		assertEquals("PERSON", table.getTableName());

		String sql = reader.getSynonymSource(con, null, syn.getSchema(), syn.getTableName());

		String schema = Db2TestUtil.getSchemaName();
		String expected = "CREATE OR REPLACE ALIAS " + schema + ".S_PERSON\n   FOR " + schema + ".PERSON;";
		assertEquals(expected, sql.trim());
	}

}
