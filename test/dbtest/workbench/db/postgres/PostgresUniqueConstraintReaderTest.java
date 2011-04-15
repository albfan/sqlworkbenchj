/*
 * PostgresUniqueConstraintReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;import workbench.TestUtil;
import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;


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
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testProcessIndexList()
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;
		TableIdentifier parent = con.getMetadata().findObject(new TableIdentifier("PARENT"));
		List<IndexDefinition> indexList = con.getMetadata().getIndexReader().getTableIndexList(parent);
		for (IndexDefinition idx : indexList)
		{
			if (idx.getName().equals("uk_id"))
			{
				assertTrue(idx.isUniqueConstraint());
				assertEquals("uk_id", idx.getUniqueConstraintName());
			}
		}
	}
}
