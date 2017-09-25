/*
 * FirebirdDomainReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.firebird;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdDomainReaderTest
	extends WbTestCase
{
	public FirebirdDomainReaderTest()
	{
		super("FirebirdDomainReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		FirebirdTestUtil.initTestCase();
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;
		String sql =
			"CREATE DOMAIN AAA_SALARY AS DECIMAL(12,2) NOT NULL;\n" +
			"CREATE DOMAIN BBB_POSITIVE_INTEGER AS BIGINT DEFAULT 42 CHECK (value > 0);\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		FirebirdTestUtil.cleanUpTestCase();
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void testGetDomainList()
		throws Exception
	{
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		assertNotNull("No connection available", con);

		List<TableIdentifier> domains = con.getMetadata().getObjectList(null, new String[] { "DOMAIN" });
		assertNotNull(domains);
		assertEquals(2, domains.size());

		DbObject dbo = con.getMetadata().getObjectDefinition(domains.get(0));
		assertNotNull(dbo);
		assertEquals("DOMAIN", dbo.getObjectType());
		assertTrue(dbo instanceof DomainIdentifier);
		DomainIdentifier salary = (DomainIdentifier)dbo;
		assertEquals("AAA_SALARY", salary.getObjectName());
		String sql = salary.getSource(con).toString().trim();
		String expected = "CREATE DOMAIN AAA_SALARY AS BIGINT\n  NOT NULL;";
		assertEquals(expected, sql);

		DomainIdentifier positive = (DomainIdentifier)con.getMetadata().getObjectDefinition(domains.get(1));
		assertEquals("CHECK (value > 0)", positive.getCheckConstraint());
		sql = positive.getSource(con).toString().trim();
		expected = "CREATE DOMAIN BBB_POSITIVE_INTEGER AS BIGINT\n  DEFAULT 42\n  CHECK (value > 0);";
		assertEquals(expected, sql);

		TableIdentifier dom = new TableIdentifier("AAA_SALARY");
		dom.setType("DOMAIN");
		DataStore details = con.getMetadata().getObjectDetails(dom);
		assertNotNull(details);
		assertEquals(1, details.getRowCount());
		assertEquals("AAA_SALARY", details.getValueAsString(0, 0));
		assertEquals("BIGINT", details.getValueAsString(0, 1));
	}

}
