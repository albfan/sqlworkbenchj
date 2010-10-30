/*
 * TableCreatorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

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
public class TableCreatorTest
	extends WbTestCase
{
	private TestUtil util;

	public TableCreatorTest()
	{
		super("TableCreatorTest");
		util = getTestUtil();
	}

	@Before
	public void setUp()
		throws Exception
	{
		util.emptyBaseDirectory();
	}

	@Test
	public void testCreateTable()
		throws Exception
	{
		try
		{
			WbConnection con = util.getConnection();
			Statement stmt = con.createStatement();

			// Include column names that are keywords or contain special characters to
			// make sure TableCreator is properly handling those names
			stmt.executeUpdate("CREATE TABLE create_test (zzz integer, bbb integer, aaa integer, \"PRIMARY\" decimal(10,2), \"W\u00E4hrung\" varchar(3) not null)");
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
			creator.createTable();

			clist = con.getMetadata().getTableColumns(newTable);
			assertEquals(5, clist.size());

			assertEquals("ZZZ", clist.get(0).getColumnName());
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
	public void testCreateTempTable()
		throws Exception
	{
		try
		{
			WbConnection con = util.getConnection();

			List<TableIdentifier> tables = con.getMetadata().getTableList("%", "PUBLIC");
			assertEquals(0, tables.size());

			TableIdentifier tbl = new TableIdentifier("mytemp");
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();

			ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
			id.setIsPkColumn(true);
			id.setIsNullable(false);
			cols.add(id);

			ColumnIdentifier name = new ColumnIdentifier("SOME_NAME", Types.VARCHAR);
			name.setColumnSize(50);
			name.setIsPkColumn(false);
			name.setIsNullable(true);
			cols.add(name);

			// For H2 a a localtemp definition is part of default.properties
			List<CreateTableTypeDefinition> types = DbSettings.getCreateTableTypes(con.getMetadata().getDbId());
			assertEquals(1, types.size());

			TableCreator creator = new TableCreator(con, types.get(0).getType(), tbl, cols);
			creator.createTable();

			tables = con.getMetadata().getTableList("%", "PUBLIC");
			assertEquals(1, tables.size());
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
			WbConnection con = util.getConnection();
			
			TestUtil.executeScript(con, 
				"create schema other;\n" +
				"commit;\n");

			List<TableIdentifier> tables = con.getMetadata().getTableList("%", "OTHER");
			assertEquals(0, tables.size());

			TableIdentifier tbl = new TableIdentifier("other.foo");
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();

			ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
			id.setIsPkColumn(true);
			id.setIsNullable(false);
			cols.add(id);

			ColumnIdentifier name = new ColumnIdentifier("SOME_NAME", Types.VARCHAR);
			name.setColumnSize(50);
			name.setIsPkColumn(false);
			name.setIsNullable(true);
			cols.add(name);
			TableCreator creator = new TableCreator(con, null, tbl, cols);
			creator.createTable();

			tables = con.getMetadata().getTableList("%", "OTHER");
			assertEquals(1, tables.size());

			tables = con.getMetadata().getTableList("%", "PUBLIC");
			assertEquals(0, tables.size());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
