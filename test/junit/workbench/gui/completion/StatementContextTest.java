/*
 * StatementContextTest.java
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
package workbench.gui.completion;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author thomas
 */
public class StatementContextTest
	extends WbTestCase
{
	private WbConnection con;

	public StatementContextTest()
	{
		super("StatementContextTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
		con = getTestUtil().getConnection();
		prepareDatabase(con);
	}

	@After
	public void tearDown()
		throws Exception
	{
		getTestUtil().emptyBaseDirectory();
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testCte()
		throws Exception
	{
		String sql =
			"with cte (col1, col2, col3, col4) as (\n" +
			"  select * from one\n" +
			")" +
			"select \n" +
			"from cte;";

		int pos = sql.indexOf("from") + "from".length() + 1;
		StatementContext context = new StatementContext(con, sql, pos);
		assertTrue(context.isStatementSupported());
		List data = context.getData();
		assertEquals(3, data.size()); // three tables available.
		for (Object o : data)
		{
			assertTrue(o instanceof TableIdentifier);
		}
		pos = sql.indexOf("select \n") + "select".length();
		context = new StatementContext(con, sql, pos);

		data = context.getData();
		assertEquals(5, data.size()); // 4 columns defined for the CTE plus the (All) marker
		for (int i=1; i < data.size(); i++)
		{
			assertEquals("col" + Integer.toString(i), data.get(i).toString());
		}

		sql =
			"with cte_one (col1, col2, col3, col4) as (\n" +
			"  select * from one\n" +
			"), cte_two as (\n" +
			"  select * from cte_one" +
			")" +
			"select *\n" +
			"from ;";

		pos = sql.indexOf("from ;") + "from".length();
		context = new StatementContext(con, sql, pos);
		data = context.getData();

		assertEquals(5, data.size()); // two cte names plus three tables
		for (int i=0; i < data.size(); i++)
		{
			Object o = data.get(i);
			assertTrue(o instanceof TableIdentifier);

			String name = ((TableIdentifier)o).getTableName().toLowerCase();
			if (i==0)
			{
				assertEquals("cte_one", name);
			}
			if (i==1)
			{
				assertEquals("cte_two", name);
			}
		}

	}

  @Test
  public void testCTAS()
  {
		StatementContext context = new StatementContext(con, "create table foo as select  from bar;", 27);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
    analyzer.checkContext();
    assertEquals("bar", analyzer.getTableForColumnList().getTableName());

		context = new StatementContext(con, "create table foo (a int, b varchar(100)) as select  from bar;", 51);
		analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
    analyzer.checkContext();
    assertEquals("bar", analyzer.getTableForColumnList().getTableName());
  }

	@Test
	public void testSubSelect()
	{
		StatementContext context = new StatementContext(con, "select * from one where x in (select  from two)", 36);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		List columns = analyzer.getData();
		assertNotNull(columns);
		assertEquals(3, columns.size());
		Object o = columns.get(1);
		assertTrue(o instanceof ColumnIdentifier);
		ColumnIdentifier t = (ColumnIdentifier) o;
		assertEquals("id2", t.getColumnName().toLowerCase());
	}

	@Test
	public void testCombinedSubSelect()
	{
		String sql = "select * from one o where x in (select id2 from two where o. )";
		int pos = sql.indexOf("o.") + 2;
		StatementContext context = new StatementContext(con, sql, pos);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		TableIdentifier tbl = analyzer.getTableForColumnList();
		assertNotNull(tbl);
		assertEquals("one", tbl.getTableName().toLowerCase());
	}

	@Test
	public void testUpdateSubSelect()
	{
		String sql = "update one set firstname = 'xx' where id1 in (select id2 from two where one. )";
		int pos = sql.indexOf("one.") + 4;
		StatementContext context = new StatementContext(con, sql, pos);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		TableIdentifier tbl = analyzer.getTableForColumnList();
		assertEquals("one", tbl.getTableName().toLowerCase());
	}

	@Test
	public void testDeleteSubSelect()
	{
		String sql = "delete from one where id1 in (select id2 from two where one. )";
		int pos = sql.indexOf("one.") + 4;
		StatementContext context = new StatementContext(con, sql, pos);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		TableIdentifier tbl = analyzer.getTableForColumnList();
		assertEquals("one", tbl.getTableName().toLowerCase());
	}

	@Test
	public void testSelectColumnList()
	{
		StatementContext context = new StatementContext(con, "select  from one", 7);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		List objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(4, objects.size());
		Object o = objects.get(1);
		assertTrue(o instanceof ColumnIdentifier);
		ColumnIdentifier c = (ColumnIdentifier) o;
		assertEquals("firstname", c.getColumnName().toLowerCase());

		context = new StatementContext(con, "select * from one where  ", 24);
		analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(3, objects.size());
		o = objects.get(0);
		assertTrue(o instanceof ColumnIdentifier);
		c = (ColumnIdentifier) o;
		assertEquals("firstname", c.getColumnName().toLowerCase());
	}

	@Test
	public void testSelectTableList()
	{
		StatementContext context = new StatementContext(con, "select * from ", 14);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		List objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(3, objects.size());
		Object o = objects.get(0);
		assertTrue(o instanceof TableIdentifier);
		TableIdentifier t = (TableIdentifier) o;
		assertEquals("one", t.getTableName().toLowerCase());
	}

	@Test
	public void testDeleteTableList()
	{
		StatementContext context = new StatementContext(con, "delete from  where ", 12);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof DeleteAnalyzer);
		List objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(3, objects.size());
		Object o = objects.get(0);
		assertTrue(o instanceof TableIdentifier);
		TableIdentifier t = (TableIdentifier) o;
		assertEquals("one", t.getTableName().toLowerCase());
	}

	@Test
	public void createView()
	{
		StatementContext context = new StatementContext(con, "create view v_test as select * from  ", 36);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		List objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(3, objects.size());
		Object o = objects.get(0);
		assertTrue(o instanceof TableIdentifier);
		TableIdentifier t = (TableIdentifier) o;
		assertEquals("one", t.getTableName().toLowerCase());

		context = new StatementContext(con, "create or replace view v_test as select * from  ", 48);
		analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(3, objects.size());
		o = objects.get(0);
		assertTrue(o instanceof TableIdentifier);
		t = (TableIdentifier) o;
		assertEquals("one", t.getTableName().toLowerCase());

		context = new StatementContext(con, "create or replace view v_test as select  from one", 38);
		analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		assertEquals(BaseAnalyzer.CONTEXT_COLUMN_LIST, analyzer.getContext());
		objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(4, objects.size());
		o = objects.get(1); // the first element will be the "select all" marker
		assertTrue(o instanceof ColumnIdentifier);
		ColumnIdentifier c = (ColumnIdentifier) o;
		assertEquals("firstname", c.getColumnName().toLowerCase());

		context = new StatementContext(con, "create or replace materialized view v_test as select  from one", 53);
		analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		assertEquals(BaseAnalyzer.CONTEXT_COLUMN_LIST, analyzer.getContext());
		objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(4, objects.size());
		o = objects.get(1); // the first element will be the "select all" marker
		assertTrue(o instanceof ColumnIdentifier);
		c = (ColumnIdentifier) o;
		assertEquals("firstname", c.getColumnName().toLowerCase());
	}

	@Test
	public void testDeleteColumnList()
	{
		StatementContext context = new StatementContext(con, "delete from two where ", 22);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof DeleteAnalyzer);
		List objects = analyzer.getData();
		assertNotNull(objects);
		assertEquals(2, objects.size());
		Object o = objects.get(0);
		assertTrue(o instanceof ColumnIdentifier);
		ColumnIdentifier t = (ColumnIdentifier) o;
		assertEquals("id2", t.getColumnName().toLowerCase());
	}

	private void prepareDatabase(WbConnection con)
		throws SQLException
	{
		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			stmt.executeUpdate("create table one (id1 integer, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("create table two (id2 integer, some_data varchar(100))");
			stmt.executeUpdate("create table three (id3 integer, more_data varchar(100))");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}
}
