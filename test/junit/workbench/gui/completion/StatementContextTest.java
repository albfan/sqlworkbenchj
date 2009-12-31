/*
 * StatementContextTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author thomas
 */
public class StatementContextTest extends TestCase
{
	private TestUtil util;
	private WbConnection con;
	
	public StatementContextTest(String testName)
	{
		super(testName);
	}

  protected void setUp()
    throws Exception
  {
    super.setUp();
		util = new TestUtil("InsertAnalyzerTest");
		con = util.getConnection("completion_test");
		prepareDatabase(con);
  }

  protected void tearDown()
    throws Exception
  {
		util.emptyBaseDirectory();
		ConnectionMgr.getInstance().disconnectAll();
    super.tearDown();
  }

	public void testSubSelect()
	{
		try
		{
			StatementContext context = new StatementContext(con, "select * from one where x in (select  from two)", 36);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			List columns = analyzer.getData();
			assertNotNull(columns);
			assertEquals(3, columns.size());
			Object o = columns.get(1);
			assertTrue(o instanceof ColumnIdentifier);
			ColumnIdentifier t = (ColumnIdentifier)o;
			assertEquals("id2", t.getColumnName().toLowerCase());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testCombinedSubSelect()
	{
		try
		{
			String sql = "select * from one o where x in (select id2 from two where o. )";
			int pos = sql.indexOf("o.") + 2;
			StatementContext context = new StatementContext(con, sql, pos);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			TableIdentifier tbl = analyzer.getTableForColumnList();
			assertEquals("one", tbl.getTableName().toLowerCase());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testUpdateSubSelect()
	{
		try
		{
			String sql = "update one set firstname = 'xx' where id1 in (select id2 from two where one. )";
			int pos = sql.indexOf("one.") + 4;
			StatementContext context = new StatementContext(con, sql, pos);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			TableIdentifier tbl = analyzer.getTableForColumnList();
			assertEquals("one", tbl.getTableName().toLowerCase());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testDeleteSubSelect()
	{
		try
		{
			String sql = "delete from one where id1 in (select id2 from two where one. )";
			int pos = sql.indexOf("one.") + 4;
			StatementContext context = new StatementContext(con, sql, pos);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			TableIdentifier tbl = analyzer.getTableForColumnList();
			assertEquals("one", tbl.getTableName().toLowerCase());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
  public void testSelectColumnList()
  {
		try
		{
			StatementContext context = new StatementContext(con, "select  from one", 7);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			List objects = analyzer.getData();
			assertNotNull(objects);
			assertEquals(4, objects.size());
			Object o = objects.get(1);
			assertTrue(o instanceof ColumnIdentifier);
			ColumnIdentifier c = (ColumnIdentifier)o;
			assertEquals("firstname", c.getColumnName().toLowerCase());
			
			context = new StatementContext(con, "select * from one where  ", 24);
			analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			objects = analyzer.getData();
			assertNotNull(objects);
			assertEquals(3, objects.size());
			o = objects.get(0);
			assertTrue(o instanceof ColumnIdentifier);
			c = (ColumnIdentifier)o;
			assertEquals("firstname", c.getColumnName().toLowerCase());
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
  }

  public void testSelectTableList()
  {
		
		try
		{
			StatementContext context = new StatementContext(con, "select * from ", 14);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			List objects = analyzer.getData();
			assertNotNull(objects);
			assertEquals(3, objects.size());
			Object o = objects.get(0);
			assertTrue(o instanceof TableIdentifier);
			TableIdentifier t = (TableIdentifier)o;
		assertEquals("one", t.getTableName().toLowerCase());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
  }
	
  public void testDeleteTableList()
  {
		try
		{
			StatementContext context = new StatementContext(con, "delete from  where ", 12);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof DeleteAnalyzer);
			List objects = analyzer.getData();
			assertNotNull(objects);
			assertEquals(3, objects.size());
			Object o = objects.get(0);
			assertTrue(o instanceof TableIdentifier);
			TableIdentifier t = (TableIdentifier)o;
			assertEquals("one", t.getTableName().toLowerCase());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
  }

  public void createView()
  {
		try
		{
			StatementContext context = new StatementContext(con, "create view v_test as select * from  ", 36);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			List objects = analyzer.getData();
			assertNotNull(objects);
			assertEquals(3, objects.size());
			Object o = objects.get(0);
			assertTrue(o instanceof TableIdentifier);
			TableIdentifier t = (TableIdentifier)o;
			assertEquals("one", t.getTableName().toLowerCase());
			
			context = new StatementContext(con, "reate or replace view v_test as select * from  ", 48);
			analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof SelectAnalyzer);
			objects = analyzer.getData();
			assertNotNull(objects);
			assertEquals(3, objects.size());
			o = objects.get(0);
			assertTrue(o instanceof TableIdentifier);
			t = (TableIdentifier)o;
			assertEquals("one", t.getTableName().toLowerCase());			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
  }	
  public void testDeleteColumnList()
  {
		try
		{
			StatementContext context = new StatementContext(con, "delete from two where ", 22);
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof DeleteAnalyzer);
			List objects = analyzer.getData();
			assertNotNull(objects);
			assertEquals(2, objects.size());
			Object o = objects.get(0);
			assertTrue(o instanceof ColumnIdentifier);
			ColumnIdentifier t = (ColumnIdentifier)o;
			assertEquals("id2", t.getColumnName().toLowerCase());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
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
