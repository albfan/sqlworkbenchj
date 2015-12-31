/*
 * PostgresEnumReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.Collection;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.EnumIdentifier;
import workbench.db.WbConnection;

import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresEnumReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "enumtest";

	public PostgresEnumReaderTest()
	{
		super("PostgresEnumReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null)
		{
			return;
		}
		TestUtil.executeScript(con,
			"CREATE TYPE stimmung_enum AS ENUM ('sad','ok','happy');\n" +
			"COMMENT ON TYPE stimmung_enum IS 'my enum';\n" +
			"COMMIT;\n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testSupportedTypes()
	{
		PostgresEnumReader reader = new PostgresEnumReader();
		List<String> expResult = CollectionUtil.arrayList("ENUM");
		List<String> result = reader.supportedTypes();
		assertEquals(expResult, result);
		assertTrue(reader.handlesType("ENUM"));
		assertTrue(reader.handlesType("enum"));
		assertFalse(reader.handlesType("enumeration"));
	}

	@Test
	public void testEnumRetrieval()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

		PostgresEnumReader reader = new PostgresEnumReader();
		Collection<EnumIdentifier> enums = reader.getDefinedEnums(con, TEST_ID, null);
		assertEquals(1, enums.size());
		EnumIdentifier enumId = enums.iterator().next();
		assertEquals("stimmung_enum", enumId.getObjectName());
		assertEquals("my enum", enumId.getComment());

		String sql = enumId.getSource(con).toString();
		ScriptParser parser = new ScriptParser(sql, ParserType.Postgres);
		assertEquals(2, parser.getSize());
		String create = parser.getCommand(0);
		assertEquals(create, "CREATE TYPE stimmung_enum AS ENUM ('sad','ok','happy')");
		String comment = parser.getCommand(1);
		assertEquals(comment, "COMMENT ON TYPE stimmung_enum IS 'my enum'");

		enums = reader.getDefinedEnums(con, TEST_ID, "stimmung_enum");
		assertEquals(1, enums.size());

	}

}
