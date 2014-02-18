/*
 * SelectAnalyzerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.util.List;

import workbench.WbTestCase;

import workbench.db.TableIdentifier;

import workbench.util.TableAlias;

import org.junit.Test;

import static org.junit.Assert.*;

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
	public void testJoin()
	{
		String sql =
			"select b. \n" +
			" from public.t1 a join public.t2 as b using (id)";
		int pos = sql.indexOf('.') + 1;
		StatementContext ctx = new StatementContext(null, sql, pos);
		BaseAnalyzer analyzer = ctx.getAnalyzer();
		analyzer.checkContext();
		int context = analyzer.getContext();
		assertEquals(BaseAnalyzer.CONTEXT_COLUMN_LIST, context);
		TableIdentifier tbl = analyzer.getTableForColumnList();
		assertEquals("t2", tbl.getTableName());
	}

	@Test
	public void testCTE()
	{
		String sql =
			"with foobar as (\n" +
			"  select t1.c1, t2.  \n" +
			"  from table1 t1\n" +
			"    join table2 t2 on t1.id = t2.id1\n" +
			")\n" +
			"select *\n" +
			"from foobar;";
		int pos = sql.indexOf("t2.") + 3;
		StatementContext ctx = new StatementContext(null, sql, pos);
		BaseAnalyzer analyzer = ctx.getAnalyzer();
		analyzer.checkContext();
		int context = analyzer.getContext();
		assertEquals(BaseAnalyzer.CONTEXT_COLUMN_LIST, context);
		TableIdentifier tbl = analyzer.getTableForColumnList();
		assertEquals("table2", tbl.getTableName());
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

	@Test
	public void testAlternateSeparator()
	{
		String select = "select * from mylib/sometable";

	}

}
