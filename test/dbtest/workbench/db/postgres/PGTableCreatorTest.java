/*
 * TableCreatorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import org.junit.AfterClass;
import workbench.db.*;
import java.util.ArrayList;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author Thomas Kellerer
 */
public class PGTableCreatorTest
	extends WbTestCase
{
	private static final String TEST_ID = "pgtablecreator";

	public PGTableCreatorTest()
	{
		super("TableCreatorTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testCreateTable()
		throws Exception
	{
		try
		{
			WbConnection con = PostgresTestUtil.getPostgresConnection();
			if (con == null) return;

			Statement stmt = con.createStatement();

			// Include column names that are keywords or contain special characters to
			// make sure TableCreator is properly handling those names
			stmt.executeUpdate("CREATE TABLE create_test (zzz integer, bbb integer, aaa integer, \"PRIMARY\" numeric(10,2), \"W\u00E4hrung\" varchar(3) not null)");
			TableIdentifier oldTable = new TableIdentifier("create_test");
			TableIdentifier newTable = new TableIdentifier("new_table");

			List<ColumnIdentifier> clist = con.getMetadata().getTableColumns(oldTable);

			// Make sure the table is created with the same column
			// ordering as the original table.
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();

			for (ColumnIdentifier col : clist)
			{
				cols.add(col);
			}
			ColumnIdentifier c1 = cols.get(0);
			ColumnIdentifier c2 = cols.get(2);
			cols.set(0, c2);
			cols.set(2, c1);

			// Make sure the columns are created with the default case of the target connection
			c1.setColumnName(c1.getColumnName().toLowerCase());
			c2.setColumnName(c2.getColumnName().toLowerCase());

			TableCreator creator = new TableCreator(con, null, newTable, cols);
			creator.useDbmsDataType(true);
			creator.createTable();

			clist = con.getMetadata().getTableColumns(newTable);
			assertEquals(5, clist.size());

			assertEquals("zzz", clist.get(0).getColumnName());
			assertEquals("\"PRIMARY\"", clist.get(3).getColumnName());
			assertEquals("\"W\u00E4hrung\"", clist.get(4).getColumnName());
			assertFalse(clist.get(4).isNullable());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void createInOtherSchema()
		throws Exception
	{
		try
		{
			PostgresTestUtil.cleanUpTestCase(TEST_ID);

			WbConnection con = PostgresTestUtil.getPostgresConnection();
			if (con == null) return;

			TestUtil.executeScript(con,
				"create schema other;\n" +
				"commit;\n");

			List<TableIdentifier> tables = con.getMetadata().getTableList("%", "other");
			assertEquals(0, tables.size());

			TableIdentifier tbl = new TableIdentifier("other.foo");
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();

			ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
			id.setDbmsType("integer");
			id.setIsPkColumn(true);
			id.setIsNullable(false);
			cols.add(id);

			ColumnIdentifier name = new ColumnIdentifier("some_name", Types.VARCHAR);
			name.setColumnSize(50);
			name.setDbmsType("varchar(50)");
			name.setIsPkColumn(false);
			name.setIsNullable(true);
			cols.add(name);
			TableCreator creator = new TableCreator(con, null, tbl, cols);
			creator.useDbmsDataType(true);
			creator.createTable();

			tables = con.getMetadata().getTableList("%", "other");
			assertEquals(1, tables.size());

			tables = con.getMetadata().getTableList("%", "public");
			assertEquals(0, tables.size());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
