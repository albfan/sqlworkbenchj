/*
 * TableSelectBuilderTest.java
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

import java.util.ArrayList;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableSelectBuilderTest
	extends WbTestCase
{

	public TableSelectBuilderTest()
	{
		super("TableSelectBuilderTest");
	}

	@Test
	public void testTemplateWithSchema()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		TestUtil.executeScript(con,
			"create schema foo; \n" +
			"create table foo.person (nr integer, firstname varchar(20), lastname varchar(20))");

		TableSelectBuilder builder = new TableSelectBuilder(con);
		builder.setTemplate("select %columnlist%\nfrom %catalog_name%.%schema_name%.%table_name%");
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("person"));

		TableDefinition def = con.getMetadata().getTableDefinition(tbl);

		TableIdentifier t1 = tbl.createCopy();
		t1.setSchema(null);
		t1.setCatalog(null);

		String sql = builder.getSelectForColumns(t1, def.getColumns(), -1);
		String expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom PERSON";
			System.out.println("----\n" + expected + "\n------\n" + sql);
		assertEquals(expected, sql);

		t1 = tbl.createCopy();
		t1.setCatalog(null);

		sql = builder.getSelectForColumns(t1, def.getColumns(), -1);
		expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom FOO.PERSON";
//			System.out.println("----\n" + expected + "\n------\n" + sql);
		assertEquals(expected, sql);

		t1 = tbl.createCopy();
		t1.setSchema(null);

		sql = builder.getSelectForColumns(t1, def.getColumns(), -1);
		expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom TABLESELECTBUILDERTEST.PERSON";
//			System.out.println("----\n" + expected + "\n------\n" + sql);
		assertEquals(expected, sql);

		sql = builder.getSelectForTable(tbl, -1);
		expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom TABLESELECTBUILDERTEST.FOO.PERSON";
		assertEquals(expected, sql);
	}

	@Test
	public void testTemplate()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		TestUtil.executeScript(con, "create table person (nr integer, firstname varchar(20), lastname varchar(20))");

		TableSelectBuilder builder = new TableSelectBuilder(con, "junittabledata");
		builder.setTemplate("select %columnlist%\nfrom %table_name% with (nolock)");
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("person"));

		String sql = builder.getSelectForTable(tbl, -1);
		String expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom PERSON with (nolock)";
		assertEquals(expected, sql);

		sql = builder.getSelectForColumns(tbl, new ArrayList<ColumnIdentifier>(), -1);
		expected = "select *\nfrom PERSON with (nolock)";
		assertEquals(expected, sql);
	}

	@Test
	public void testNoColumns()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		TestUtil.executeScript(con, "create table person (nr integer, firstname varchar(20), lastname varchar(20))");

		TableSelectBuilder builder = new TableSelectBuilder(con);
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("person"));

		String sql = builder.getSelectForColumns(tbl, new ArrayList<ColumnIdentifier>(), -1);
		String expected = "SELECT *\nFROM PERSON";
		assertEquals(expected, sql);
	}

	@Test
	public void testBuildSelect()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		TestUtil.executeScript(con, "create table person (nr integer, firstname varchar(20), lastname varchar(20))");

		DbSettings dbconfig = con.getDbSettings();
		TableSelectBuilder builder = new TableSelectBuilder(con);
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("person"));

		String sql = builder.getSelectForTable(tbl, -1);
		String expected = "SELECT NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\n" +
			"FROM PERSON";
		assertEquals(expected, sql);

		dbconfig.setDataTypeExpression("varchar", "upper(" + TableSelectBuilder.COLUMN_PLACEHOLDER + ")");

		sql = builder.getSelectForTable(tbl, -1);
		expected = "SELECT NR,\n" +
			"       upper(FIRSTNAME),\n" +
			"       upper(LASTNAME)\n" +
			"FROM PERSON";
		assertEquals(expected, sql);

		dbconfig.setDataTypeExpression("varchar", null);

		// test invalid expression
		dbconfig.setDataTypeExpression("integer", "abs()");
		sql = builder.getSelectForTable(tbl, -1);
		expected = "SELECT NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\n" +
			"FROM PERSON";
		assertEquals(expected, sql);

		sql = builder.getSelectForCount(tbl);
		expected = "SELECT count(*)\n" +
			"FROM PERSON";
		assertEquals(expected, sql);
	}

	@Test
	public void testLimit()
		throws Exception
	{
		TableSelectBuilder builder = new TableSelectBuilder(null, "junittabledata");

		builder.setTemplate("select %columnlist% from %table_name% " + TableSelectBuilder.LIMIT_EXPRESSION_PLACEHOLDER);
		builder.setLimitClause("LIMIT " + TableSelectBuilder.MAX_ROWS_PLACEHOLDER);
		TableIdentifier tbl = new TableIdentifier("person");

		List<ColumnIdentifier> cols = new ArrayList<>();
		String sql = builder.getSelectForColumns(tbl, cols, -1);
		assertEquals("select * from person", sql.trim());

		sql = builder.getSelectForColumns(tbl, cols, 5);
		assertEquals("select * from person LIMIT 5", sql.trim());

		builder.setTemplate("select " + TableSelectBuilder.LIMIT_EXPRESSION_PLACEHOLDER + " %columnlist% from %table_name% ");
		builder.setLimitClause("TOP (" + TableSelectBuilder.MAX_ROWS_PLACEHOLDER + ")");

		sql = builder.getSelectForColumns(tbl, cols, -1);
		assertEquals("select  * from person", sql.trim());

		sql = builder.getSelectForColumns(tbl, cols, 5);
		assertEquals("select TOP (5) * from person", sql.trim());

		builder.setLimitClause(null);
		sql = builder.getSelectForColumns(tbl, cols, 5);
		assertEquals("select  * from person", sql.trim());

		builder.setTemplate("select %columnlist% from %table_name% %order_by% " + TableSelectBuilder.LIMIT_EXPRESSION_PLACEHOLDER);
		builder.setLimitClause("LIMIT " + TableSelectBuilder.MAX_ROWS_PLACEHOLDER);
		sql = builder.getSelectForColumns(tbl, cols, 5);
		assertEquals("select * from person  LIMIT 5", sql.trim());

		sql = builder.getSelectForColumns(tbl, cols, "id", 5);
		assertEquals("select * from person  \nORDER BY id LIMIT 5", sql.trim());

		builder.setTemplate("select %columnlist% from %table_name%");
		builder.setLimitClause("LIMIT " + TableSelectBuilder.MAX_ROWS_PLACEHOLDER);

		sql = builder.getSelectForColumns(tbl, cols, "id", 5);
		assertEquals("select * from person \nORDER BY id", sql.trim());

		sql = builder.getSelectForColumns(tbl, cols, null, 5);
		assertEquals("select * from person", sql.trim());
	}

}
