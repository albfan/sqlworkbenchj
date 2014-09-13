/*
 * PostgresTriggerReaderTest.java
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
package workbench.db.postgres;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTriggerReaderTest
	extends WbTestCase
{
	private static final String TEST_SCHEMA = "trgreadertest";

	public PostgresTriggerReaderTest()
	{
		super("PostgresTriggerReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_SCHEMA);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con, "create table some_table (id integer, some_data varchar(100));\n" +
			"commit;\n");

		String sql =
			"CREATE OR REPLACE FUNCTION my_trigger_func()  \n" +
			"RETURNS trigger AS  \n" +
			"$body$ \n" +
			"BEGIN \n" +
			"    if new.comment IS NULL then \n" +
			"        new.comment = 'n/a'; \n" +
			"    end if; \n" +
			"    RETURN NEW; \n" +
			"END; \n" +
			"$body$  \n" +
			"LANGUAGE plpgsql; \n" +
			"" +
			"\n" +
			"CREATE TRIGGER some_trg BEFORE UPDATE ON some_table \n" +
			"    FOR EACH ROW EXECUTE PROCEDURE my_trigger_func();\n";

		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetDependentSource()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TriggerReader reader = TriggerReaderFactory.createReader(con);
		assertTrue(reader instanceof PostgresTriggerReader);
		List<TriggerDefinition> triggers = reader.getTriggerList(null, TEST_SCHEMA, "some_table");
		assertEquals(1, triggers.size());

		TriggerDefinition trg = triggers.get(0);
		assertEquals("some_trg", trg.getObjectName());

		String sql = trg.getSource(con, false).toString();
		assertTrue(sql.startsWith("CREATE TRIGGER some_trg"));

		sql = reader.getDependentSource(null, TEST_SCHEMA, trg.getObjectName(), trg.getRelatedTable()).toString();
		assertNotNull(sql);
		System.out.println("***\n" + sql);
		assertTrue(sql.contains("CREATE OR REPLACE FUNCTION trgreadertest.my_trigger_func()"));
	}
}
