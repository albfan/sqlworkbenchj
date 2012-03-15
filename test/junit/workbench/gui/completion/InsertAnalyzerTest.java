/*
 * InsertAnalyzerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author thomas
 */
public class InsertAnalyzerTest
	extends WbTestCase
{

	public InsertAnalyzerTest()
	{
		super("InsertAnalyzerTest");
	}

	@Before
  public void setUp()
    throws Exception
  {
  }

	@After
  public void tearDown()
    throws Exception
  {
		ConnectionMgr.getInstance().disconnectAll();
  }

	@Test
  public void testCheckContext()
		throws Exception
  {
		WbConnection con = getTestUtil().getConnection("insert_completion_test");
		TestUtil.executeScript(con,
			"create table one (id1 integer, firstname varchar(100), lastname varchar(100));\n" +
			"create table two (id2 integer, some_data varchar(100));\n" +
			"create table three (id3 integer, more_data varchar(100));\n" +
			"commit;\n"
		);

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

	@Test
	public void testAlternateSeparator()
	{
		String sql = "insert into mylib/sometable ( ) values ";
		InsertAnalyzer analyzer = new InsertAnalyzer(null, sql, sql.indexOf("(") + 1);
		analyzer.setCatalogSeparator('/');
		analyzer.checkContext();
		TableIdentifier table = analyzer.getTableForColumnList();
		assertEquals("mylib", table.getSchema());
		assertEquals("sometable", table.getTableName());
	}

	@Test
	public void testSeparator()
	{
		String sql = "insert into public.sometable (  ) values ";
		InsertAnalyzer analyzer = new InsertAnalyzer(null, sql, sql.indexOf("(") + 1);
		analyzer.checkContext();
		TableIdentifier table = analyzer.getTableForColumnList();
		assertEquals("public", table.getSchema());
		assertEquals("sometable", table.getTableName());
	}

}
