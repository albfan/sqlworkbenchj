/*
 * SelectAnalyzerTest.java
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

import java.util.List;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.util.TableAlias;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectAnalyzerTest
	extends WbTestCase
{

	public SelectAnalyzerTest()
	{
		super("SelectAnalyzerTest");
	}

	@Test
	public void testSpaces()
	{
		String sql = "SELECT x. FROM \"Dumb Named Schema\".\"Problematically Named Table\" x";
		SelectAnalyzer analyzer = new SelectAnalyzer(null, sql, sql.indexOf(" FROM"));
		List<TableAlias> tables = analyzer.getTables();
		assertEquals(1, tables.size());
		TableAlias alias = tables.get(0);
		TableIdentifier tbl = alias.getTable();
		assertEquals("\"Dumb Named Schema\".\"Problematically Named Table\"", tbl.getTableExpression());
		assertEquals("Dumb Named Schema", tbl.getSchema());
		assertEquals("Problematically Named Table", tbl.getTableName());
		assertEquals("x", alias.getAlias());
		analyzer.checkContext();
		tbl = analyzer.getTableForColumnList();
		assertEquals("\"Dumb Named Schema\".\"Problematically Named Table\"", tbl.getTableExpression());
	}

	@Test
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
		analyzer = new SelectAnalyzer(null, sql, pos);
		analyzer.checkContext();
		assertEquals(BaseAnalyzer.CONTEXT_TABLE_LIST, analyzer.context);

		sql = "SELECT * \n" +
				 "  FROM person p \n" +
				 "   JOIN \n" +
				 "  WHERE p.id = 42";

		pos = sql.indexOf("JOIN \n") + 5;
		analyzer = new SelectAnalyzer(null, sql, pos);
		analyzer.checkContext();
		assertEquals(BaseAnalyzer.CONTEXT_TABLE_LIST, analyzer.context);

	}

}
