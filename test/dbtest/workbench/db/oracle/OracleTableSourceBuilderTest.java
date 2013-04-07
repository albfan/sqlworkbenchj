/*
 * OracleTableSourceBuilderTest.java
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
package workbench.db.oracle;


import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.ScriptParser;

import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTableSourceBuilderTest
	extends WbTestCase
{

	public OracleTableSourceBuilderTest()
	{
		super("OracleTableSourceBuilderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		String sql =
			"CREATE TABLE index_test (test_id integer not null, tenant_id integer);\n" +
			"ALTER TABLE index_test \n" +
			"   ADD CONSTRAINT pk_indexes PRIMARY KEY (test_id)  \n" +
			"   USING INDEX (CREATE INDEX idx_pk_index_test ON index_test (test_id, tenant_id) REVERSE);";

		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		TestUtil.executeScript(con, sql, false);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetSource()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		TableIdentifier table = con.getMetadata().findTable(new TableIdentifier("INDEX_TEST"));
		assertNotNull(table);
		String sql = table.getSource(con).toString();

//		System.out.println(sql);
		//assertTrue(sql.indexOf("USING INDEX (") > 0);
		ScriptParser p = new ScriptParser(sql);
		assertEquals(2, p.getSize());
		String indexSql = p.getCommand(1);
		indexSql = indexSql.replaceAll("\\s+", " ");
//		System.out.println(indexSql);
		String expected = "ALTER TABLE INDEX_TEST ADD CONSTRAINT PK_INDEXES PRIMARY KEY (TEST_ID) USING INDEX ( CREATE INDEX IDX_PK_INDEX_TEST ON INDEX_TEST (TEST_ID ASC, TENANT_ID ASC) TABLESPACE USERS REVERSE )";
		assertEquals(expected, indexSql);

	}
}
