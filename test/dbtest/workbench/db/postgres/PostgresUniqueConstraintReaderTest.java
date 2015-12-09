/*
 * PostgresUniqueConstraintReaderTest.java
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
package workbench.db.postgres;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.report.SchemaReporter;

import workbench.util.CollectionUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class PostgresUniqueConstraintReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "uc_reader";

	public PostgresUniqueConstraintReaderTest()
	{
		super("PostgresUniqueConstraintReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null) return;
		String sql =
		"CREATE TABLE parent \n" +
		"( \n" +
		"   id          integer    NOT NULL PRIMARY KEY, \n" +
		"   unique_id1  integer, \n" +
		"   unique_id2  integer \n" +
		"); \n" +
		"ALTER TABLE parent \n" +
		"   ADD CONSTRAINT uk_id UNIQUE (unique_id1, unique_id2); \n" +
		" \n" +
		" \n" +
		"COMMIT;";
		TestUtil.executeScript(conn, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testProcessIndexList()
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

		TableIdentifier parent = con.getMetadata().findObject(new TableIdentifier("parent"));
		List<IndexDefinition> indexList = con.getMetadata().getIndexReader().getTableIndexList(parent, true);

		boolean foundConstraint = false;
		for (IndexDefinition idx : indexList)
		{
			if (idx.getName().equals("uk_id"))
			{
				assertTrue(idx.isUniqueConstraint());
				assertEquals("uk_id", idx.getUniqueConstraintName());
				foundConstraint = true;
			}
		}
		assertTrue(foundConstraint);
	}

  @Test
  public void testSchemaReport()
    throws Exception
  {
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);
    SchemaReporter reporter = new SchemaReporter(con);
    TableIdentifier parent = con.getMetadata().findObject(new TableIdentifier("parent"));
    reporter.setObjectList(CollectionUtil.arrayList(parent));
    String xml = reporter.getXml();
//    System.out.println(xml);

    String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
    assertEquals("Incorrect table count", "1", count);
    count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='parent']/index-def)");
    assertEquals("2", count);
    String constraint = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='parent']/index-def[name='uk_id']/constraint-name/text()");
    assertEquals("uk_id", constraint);
  }
}
