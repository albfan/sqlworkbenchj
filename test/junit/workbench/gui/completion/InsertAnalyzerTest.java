/*
 * InsertAnalyzerTest.java
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

		sql = "insert into two  (  ) \n select id1, \n firstname \n from one where id not in (1,2,3);";
		pos = sql.indexOf('(') + 1;
		context = new StatementContext(con, sql , pos);
		assertTrue(context.isStatementSupported());
		analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof InsertAnalyzer);
		columns = analyzer.getData();
		assertEquals(2, columns.size());
  }

	@Test
	public void testAlternateSeparator()
	{
		String sql = "insert into mylib/sometable ( ) values ";
		InsertAnalyzer analyzer = new InsertAnalyzer(null, sql, sql.indexOf('(') + 1);
		analyzer.setCatalogSeparator('/');
		analyzer.checkContext();
		TableIdentifier table = analyzer.getTableForColumnList();
		assertEquals("mylib", table.getCatalog());
		assertEquals("sometable", table.getTableName());
	}

	@Test
	public void testSeparator()
	{
		String sql = "insert into public.sometable (  ) values ";
		InsertAnalyzer analyzer = new InsertAnalyzer(null, sql, sql.indexOf('(') + 1);
		analyzer.checkContext();
		TableIdentifier table = analyzer.getTableForColumnList();
		assertEquals("public", table.getSchema());
		assertEquals("sometable", table.getTableName());
	}

}
