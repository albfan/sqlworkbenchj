/*
 * OracleTableSourceBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;


import org.junit.*;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.ScriptParser;

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

		//System.out.println(sql);
		//assertTrue(sql.indexOf("USING INDEX (") > 0);
		ScriptParser p = new ScriptParser(sql);
		assertEquals(2, p.getSize());
		String indexSql = p.getCommand(1);
		indexSql = indexSql.replaceAll("\\s+", " ");
		System.out.println(indexSql);
		String expected = "ALTER TABLE INDEX_TEST ADD CONSTRAINT PK_INDEXES PRIMARY KEY (TEST_ID) USING INDEX ( CREATE INDEX IDX_PK_INDEX_TEST ON INDEX_TEST (TEST_ID ASC, TENANT_ID ASC) REVERSE )";
		assertEquals(expected, indexSql);

	}
}
