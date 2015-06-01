/*
 * OracleSynonymReaderTest.java
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
package workbench.db.oracle;

import java.util.Collection;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.SynonymDDLHandler;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import workbench.db.DropType;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleSynonymReaderTest
	extends WbTestCase
{

	public OracleSynonymReaderTest()
	{
		super("OracleSynonymReaderTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"CREATE TABLE person (id integer, firstname varchar(50), lastname varchar(50));\n" +
			"CREATE SYNONYM s_person FOR person;");
	}

	@After
	public void cleanup()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetSynonymList()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		SynonymReader reader = con.getMetadata().getSynonymReader();
		assertNotNull(reader);
		Collection<String> types = con.getMetadata().getObjectTypes();
		assertTrue(types.contains("SYNONYM"));
		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "SYNONYM"});
		assertNotNull(objects);
		assertEquals(1, objects.size());
		TableIdentifier syn = objects.get(0);
		assertEquals("SYNONYM", syn.getObjectType());
		TableIdentifier table = con.getMetadata().getSynonymTable(syn);
		assertNotNull(table);
		assertEquals("PERSON", table.getTableName());
		String sql = reader.getSynonymSource(con, null, syn.getSchema(), syn.getTableName());
//		System.out.println(sql);
		String expected = "CREATE OR REPLACE SYNONYM S_PERSON\n   FOR WBJUNIT.PERSON;";
		assertEquals(expected, sql.trim());

		TestUtil.executeScript(con, "drop table person purge;");
		objects = con.getMetadata().getObjectList(null, new String[] { "SYNONYM"});

		assertNotNull(objects);
		assertEquals(1, objects.size());
		syn = objects.get(0);
		assertEquals("SYNONYM", syn.getObjectType());

		sql = reader.getSynonymSource(con, null, syn.getSchema(), syn.getTableName());
//		System.out.println(sql);
		assertEquals(expected, sql.trim());
	}

	@Test
	public void testHandler()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		SynonymDDLHandler handler = new SynonymDDLHandler();
		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] {"SYNONYM"});
		assertNotNull(objects);
		assertEquals(1, objects.size());
		TableIdentifier syn = objects.get(0);

		String sql = handler.getSynonymSource(con, syn, true, DropType.none);
//		System.out.println(sql);

		assertTrue(sql.contains("CREATE OR REPLACE SYNONYM S_PERSON"));
		assertTrue(sql.contains("CREATE TABLE PERSON"));

		sql = handler.getSynonymSource(con, syn, false, DropType.none);
//		System.out.println(sql);
		assertTrue(sql.contains("CREATE OR REPLACE SYNONYM S_PERSON"));
		assertFalse(sql.contains("CREATE TABLE PERSON"));
	}

}
