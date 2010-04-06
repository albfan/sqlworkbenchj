/*
 * SelectAnalyzerTest.java
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

import workbench.WbTestCase;
import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectAnalyzerTest
	extends WbTestCase
{

	public SelectAnalyzerTest(String testName)
	{
		super(testName);
	}

	protected void setUp() throws Exception
	{
	}

	protected void tearDown() throws Exception
	{
	}

	public void testAnalyzer()
	{
		String sql = "SELECT a.att1\n      ,a.\nFROM   adam   a";
		SelectAnalyzer analyzer = new SelectAnalyzer(null, sql, 23);
		String quali = analyzer.getQualifierLeftOfCursor();
		assertEquals("Wrong qualifier detected", "a", quali);

		sql = "SELECT a.att1 FROM t1 a JOIN t2 b ON a.id = b.id";
		int pos = sql.indexOf("a.id") + 2;

		analyzer = new SelectAnalyzer(null, sql, pos);
		analyzer.checkContext();
		TableIdentifier tbl = analyzer.getTableForColumnList();
		assertNotNull(tbl);
		assertEquals("t1", tbl.getTableName());

		sql = "SELECT a.att1 FROM t1 a JOIN t2 b ON (a.id = b.id)";
		pos = sql.indexOf("a.id") + 2;
		analyzer = new SelectAnalyzer(null, sql, pos);
		analyzer.checkContext();
		tbl = analyzer.getTableForColumnList();
		assertNotNull(tbl);
		assertEquals("t1", tbl.getTableName());

		sql = "SELECT a.att1 FROM t1 a JOIN t2 b ON (a.id = b.id)";
		pos = sql.indexOf("FROM") + "FROM".length() + 1;
		analyzer = new SelectAnalyzer(null, sql, pos);
		analyzer.checkContext();
		tbl = analyzer.getTableForColumnList();
		assertNull(tbl);
		assertEquals(BaseAnalyzer.CONTEXT_FROM_LIST, analyzer.context);

		pos = sql.indexOf("JOIN") + "JOIN".length() + 1;
		analyzer = new SelectAnalyzer(null, sql, pos);
		analyzer.checkContext();
		tbl = analyzer.getTableForColumnList();
		assertNull(tbl);
		assertEquals(BaseAnalyzer.CONTEXT_TABLE_LIST, analyzer.context);

		sql = "SELECT * \n" +
				 "  FROM person p \n" +
				 "   JOIN address a on a.person_id = p.id \n" +
				 "   JOIN \n" +
				 "   JOIN author at on at.author_id = p.id";

		pos = sql.indexOf("JOIN \n") + 5;
		System.out.println("to test: " + sql.substring(pos));
		analyzer = new SelectAnalyzer(null, sql, pos);
		analyzer.checkContext();
		assertEquals(BaseAnalyzer.CONTEXT_TABLE_LIST, analyzer.context);

	}

}
