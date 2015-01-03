/*
 * OracleExplainTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.oracle;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.completion.BaseAnalyzer;
import workbench.gui.completion.OracleExplainAnalyzer;
import workbench.gui.completion.SelectAllMarker;
import workbench.gui.completion.SelectAnalyzer;
import workbench.gui.completion.StatementContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleExplainTest
	extends WbTestCase
{

	public OracleExplainTest()
	{
		super("OracleExplainAnalyzerTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql =
			"create table some_table (id integer, some_data varchar(100));\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testRetrieveTables()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		String sql = "explain plan into " + OracleTestUtil.SCHEMA_NAME + ". for select * from person";

		StatementContext context = new StatementContext(con, sql, sql.indexOf('.') + 1);
		BaseAnalyzer analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof OracleExplainAnalyzer);
		List tables = analyzer.getData();
		assertNotNull(tables);
		assertEquals(1, tables.size());
		TableIdentifier tbl = (TableIdentifier)tables.get(0);
		assertEquals("SOME_TABLE", tbl.getTableName());

		sql = "explain plan for select  from some_table";
		context = new StatementContext(con, sql, sql.indexOf("select") + 7);
		analyzer = context.getAnalyzer();
		assertTrue(analyzer instanceof SelectAnalyzer);
		List columns = analyzer.getData();
		assertNotNull(columns);
		assertEquals(3, columns.size());

		assertTrue(columns.get(0) instanceof SelectAllMarker);
		ColumnIdentifier id = (ColumnIdentifier)columns.get(1);
		assertEquals("ID", id.getColumnName());
		ColumnIdentifier name = (ColumnIdentifier)columns.get(2);
		assertEquals("SOME_DATA", name.getColumnName());

		// The cursor position used by the nested SELECT analyzer for the explain
		// must be the same as the position for a "regular" analyzer
		sql = "select  from some_table";
		context = new StatementContext(con, sql, sql.indexOf("select") + 7);
		BaseAnalyzer select = context.getAnalyzer();
		assertEquals(analyzer.getCursorPosition(), select.getCursorPosition());
	}

}
