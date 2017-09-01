/*
 * TableCreatorTest.java
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
package workbench.db;

import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
	public void testCreateTemplateTable()
		throws Exception
	{
		try
		{
			WbConnection con = util.getConnection();

			List<TableIdentifier> tables = con.getMetadata().getTableList("%", "PUBLIC");
			assertEquals(0, tables.size());

			TableIdentifier tbl = new TableIdentifier("mytemp");
			List<ColumnIdentifier> cols = new ArrayList<>();

			ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
			id.setIsPkColumn(true);
			id.setIsNullable(false);
			cols.add(id);

			ColumnIdentifier name = new ColumnIdentifier("FIRST_NAME", Types.VARCHAR);
			name.setColumnSize(50);
			name.setIsPkColumn(false);
			name.setIsNullable(true);
			name.setDefaultValue("'Arthur'");
			cols.add(name);

			ColumnIdentifier lastname = new ColumnIdentifier("LAST_NAME", Types.VARCHAR);
			lastname.setColumnSize(50);
			lastname.setIsPkColumn(false);
			lastname.setIsNullable(true);
			lastname.setDefaultValue(" 'Dent'");
			cols.add(lastname);

			String template =
				"CREATE LOCAL TEMPORARY TABLE " + MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER +
				"\n(\n" + MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER + "\n)";

			con.getDbSettings().setCreateTableTemplate("table_creator_default", template);

			TableCreator creator = new TableCreator(con, "table_creator_default", tbl, cols);
			creator.setStoreSQL(true);
			creator.createTable();
			List<String> sql = creator.getGeneratedSQL();
			assertEquals(2, sql.size());

			tables = con.getMetadata().getTableList("%", "PUBLIC");
			assertEquals(1, tables.size());
			TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("mytemp"));
			assertNotNull(def);
			assertEquals(3, def.getColumnCount());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testCreateInlinePK()
		throws Exception
	{
		try
		{
			WbConnection con = util.getConnection();

			String template =
				"CREATE TABLE " + MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER +
			"\n(\n" +
			MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER + " " +
				MetaDataSqlManager.PK_INLINE_DEFINITION +
			"\n)";

			con.getDbSettings().setCreateTableTemplate("creator_inline_test", template);

			List<TableIdentifier> tables = con.getMetadata().getTableList("%", "PUBLIC");
			assertEquals(0, tables.size());

			TableIdentifier tbl = new TableIdentifier("INLINE_PK");
			List<ColumnIdentifier> cols = new ArrayList<>();

			ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
			id.setIsPkColumn(true);
			id.setIsNullable(false);
			cols.add(id);

			ColumnIdentifier id2 = new ColumnIdentifier("ID2", Types.INTEGER);
			id2.setIsPkColumn(true);
			id2.setIsNullable(false);
			id2.setDefaultValue("42");
			cols.add(id2);

			ColumnIdentifier name = new ColumnIdentifier("FIRST_NAME", Types.VARCHAR);
			name.setColumnSize(50);
			name.setIsPkColumn(false);
			name.setIsNullable(true);
			name.setDefaultValue("'Arthur'");
			cols.add(name);

			ColumnIdentifier lastname = new ColumnIdentifier("LAST_NAME", Types.VARCHAR);
			lastname.setColumnSize(50);
			lastname.setIsPkColumn(false);
			lastname.setIsNullable(true);
			lastname.setDefaultValue(" 'Dent'");
			cols.add(lastname);

			TableCreator creator = new TableCreator(con, "creator_inline_test", tbl, cols);
			creator.setStoreSQL(true);
			creator.createTable();
			List<String> sql = creator.getGeneratedSQL();
			assertEquals(1, sql.size());

			tables = con.getMetadata().getTableList("%", "PUBLIC");
			assertEquals(1, tables.size());
			TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("INLINE_PK"));
			assertNotNull(def);
			assertEquals(4, def.getColumnCount());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testTemplateWithoutPK()
		throws Exception
	{
		try
		{
			WbConnection con = util.getConnection();

			String template =
				"CREATE TABLE " + MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER +
			"\n(\n" +
			MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER + " " +
				MetaDataSqlManager.PK_INLINE_DEFINITION +
			"\n)";

			con.getDbSettings().setCreateTableTemplate("creator_inline_test", template);

			List<ColumnIdentifier> cols = new ArrayList<>();
			// Now a table without PK, but with the PK template
			ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
			id.setIsPkColumn(false);
			id.setIsNullable(false);
			cols.add(id);

			ColumnIdentifier name = new ColumnIdentifier("FIRST_NAME", Types.VARCHAR);
			name.setColumnSize(50);
			name.setIsPkColumn(false);
			name.setIsNullable(true);
			name.setDefaultValue("'Arthur'");
			cols.add(name);

			ColumnIdentifier lastname = new ColumnIdentifier("LAST_NAME", Types.VARCHAR);
			lastname.setColumnSize(50);
			lastname.setIsPkColumn(false);
			lastname.setIsNullable(true);
			lastname.setDefaultValue(" 'Dent'");
			cols.add(lastname);

			TableIdentifier tbl = new TableIdentifier("INLINE_NOPK");
			TableCreator creator = new TableCreator(con, "creator_inline_test", tbl, cols);
			creator.setStoreSQL(true);
			creator.createTable();
			List<String> sqls = creator.getGeneratedSQL();
			assertEquals(1, sqls.size());

			TableDefinition def = con.getMetadata().getTableDefinition(tbl);
			assertNotNull(def);
			assertEquals(3, def.getColumnCount());
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
			List<ColumnIdentifier> cols = new ArrayList<>();

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

	@Test
	public void testRemoveDefaults()
		throws Exception
	{
		try {
			WbConnection con = util.getConnection();
			Statement stmt = con.createStatement();

			// Include column names that are keywords or contain special characters to
			// make sure TableCreator is properly handling those names
			stmt.executeUpdate("CREATE TABLE create_test (id integer not null default 42)");
			TableIdentifier oldTable = new TableIdentifier("create_test");
			TableIdentifier newTable = new TableIdentifier("new_table");

			List<ColumnIdentifier> clist = con.getMetadata().getTableColumns(oldTable);
			assertEquals("42", clist.get(0).getDefaultValue());

			TableCreator creator = new TableCreator(con, null, newTable, clist);
			creator.setRemoveDefaults(true);
			creator.createTable();

			List<ColumnIdentifier> newCols = con.getMetadata().getTableColumns(newTable);
			assertEquals(1, newCols.size());
			assertNull(newCols.get(0).getDefaultValue());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
