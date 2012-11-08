/*
 * PostgresColumnEnhancerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.util.List;
import workbench.TestUtil;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
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
public class PostgresColumnEnhancerTest
{
	public PostgresColumnEnhancerTest()
	{
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(null);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testUpdateColumnDefinition()
		throws Exception
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null)
		{
			System.out.println("No local postgres connection. Skipping test...");
			return;
		}
		String sql =
			"create table foo (id1 integer[], id2 integer[][], id3 integer[][][]);\n" +
			"commit;\n";
		TestUtil.executeScript(conn, sql);
		TableDefinition tbl = conn.getMetadata().getTableDefinition(new TableIdentifier("foo"));
		List<ColumnIdentifier> cols = tbl.getColumns();
		for (ColumnIdentifier col : cols)
		{
			if (col.getColumnName().equals("id1"))
			{
				assertEquals("integer[]", col.getDbmsType());
			}
			if (col.getColumnName().equals("id2"))
			{
				assertEquals("integer[][]", col.getDbmsType());
			}
			if (col.getColumnName().equals("id3"))
			{
				assertEquals("integer[][][]", col.getDbmsType());
			}
		}
	}

}
