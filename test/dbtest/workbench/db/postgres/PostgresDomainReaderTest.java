/*
 * PostgresDomainReaderTest.java
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

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.GenericObjectDropper;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresDomainReaderTest
	extends WbTestCase
{

	private static final String TEST_SCHEMA = "domaintest";

	public PostgresDomainReaderTest()
	{
		super("PostgresDomainReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_SCHEMA);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null)
		{
			return;
		}
		TestUtil.executeScript(con,
			"CREATE SCHEMA other; \n" +
			"CREATE DOMAIN " + TEST_SCHEMA + ".salary AS numeric(12,2) NOT NULL CHECK (value > 0);\n" +
			"CREATE DOMAIN " + TEST_SCHEMA + ".zz_int AS integer NOT NULL;\n" +
			"CREATE DOMAIN other.positive_int AS integer CHECK (value > 0);\n" +
			"COMMIT; \n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testDomainRetrieval()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);
    
		Collection<String> types = con.getMetadata().getObjectTypes();
		assertTrue(types.contains("DOMAIN"));
		List<TableIdentifier> objects = con.getMetadata().getObjectList(TEST_SCHEMA, new String[] { "DOMAIN" });
		assertEquals(2, objects.size());
		DbObject salary = objects.get(0);
		DbObject zz_int = objects.get(1);

		objects = con.getMetadata().getObjectList("other", new String[] { "DOMAIN" });
		assertEquals(1, objects.size());

		objects = con.getMetadata().getObjectList("%", new String[] { "DOMAIN" });
		assertEquals(objects.toString(), 3, objects.size());

		PostgresDomainReader reader = new PostgresDomainReader();

		List<DomainIdentifier> domains = reader.getDomainList(con, "%", "%");
		assertEquals(3, domains.size());

		assertEquals("DOMAIN", salary.getObjectType());
		assertTrue(domains.get(0) instanceof DomainIdentifier);

		String sql = salary.getSource(con).toString().trim();
		String expected = "CREATE DOMAIN " + TEST_SCHEMA.toLowerCase() + ".salary AS numeric(12,2)\n" +
                      "   CONSTRAINT NOT NULL CHECK (VALUE > 0::numeric);";
		assertEquals(expected, sql);


		sql = zz_int.getSource(con).toString().trim();
		expected = "CREATE DOMAIN " + TEST_SCHEMA.toLowerCase() + ".zz_int AS integer\n" +
                      "   CONSTRAINT NOT NULL;";
		assertEquals(expected, sql);


		GenericObjectDropper dropper = new GenericObjectDropper();
		dropper.setCascade(true);
		dropper.setObjects(domains);
		dropper.setConnection(con);

		String drop = dropper.getScript().toString().trim();
//    System.out.println(drop);
		assertTrue(drop.contains("DROP DOMAIN IF EXISTS " + TEST_SCHEMA + ".zz_int"));
		assertTrue(drop.contains("DROP DOMAIN IF EXISTS " + TEST_SCHEMA + ".salary"));
		assertTrue(drop.contains("DROP DOMAIN IF EXISTS other.positive_int"));

		DataStore details = reader.getObjectDetails(con, salary);
		assertNotNull(details);
		assertEquals(1, details.getRowCount());
		assertEquals("salary", details.getValueAsString(0, 0));
		assertEquals("numeric(12,2)", details.getValueAsString(0, 1));


	}


}
