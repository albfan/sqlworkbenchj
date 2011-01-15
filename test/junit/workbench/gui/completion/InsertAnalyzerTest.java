/*
 * InsertAnalyzerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 *
 * @author thomas
 */
public class InsertAnalyzerTest
	extends WbTestCase
{
	private TestUtil util;
	
	public InsertAnalyzerTest()
	{
		super("InsertAnalyzerTest");
	}

	@Before
  public void setUp()
    throws Exception
  {
		util = new TestUtil("InsertAnalyzerTest");
  }

	@After
  public void tearDown()
    throws Exception
  {
		util.emptyBaseDirectory();
  }

	@Test
  public void testCheckContext()
  {
		WbConnection con = null;
		try
		{
			con = util.getConnection("insert_completion_test");
			prepareDatabase(con);
			String sql = "insert into  ";
			int pos = sql.length() - 1;
			StatementContext context = new StatementContext(con, sql, pos);
			assertTrue(context.isStatementSupported());
			BaseAnalyzer analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof InsertAnalyzer);
			List tables = analyzer.getData();
			assertEquals(3, tables.size());
			Object t1 = tables.get(0);
			assertTrue(t1 instanceof TableIdentifier);
			TableIdentifier tbl = (TableIdentifier)t1;
			assertEquals("one", tbl.getTableName().toLowerCase());
			
			sql = "insert into one (  )";
			pos = sql.length() - 2;
			context = new StatementContext(con, sql , pos);
			assertTrue(context.isStatementSupported());
			analyzer = context.getAnalyzer();
			assertTrue(analyzer instanceof InsertAnalyzer);
			List columns = analyzer.getData();
			assertEquals(3, tables.size());
			
			Object c1 = columns.get(0);
			assertTrue(c1 instanceof ColumnIdentifier);
			ColumnIdentifier col = (ColumnIdentifier)c1;
			assertEquals("firstname", col.getColumnName().toLowerCase());
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
