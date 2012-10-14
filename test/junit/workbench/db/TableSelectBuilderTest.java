/*
 * TableSelectBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;

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
		builder.setTemplate("select %columnlist%\nfrom %catalog_name%.%schema_name%.%simple_table_name%");
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("person"));

		TableDefinition def = con.getMetadata().getTableDefinition(tbl);

		TableIdentifier t1 = tbl.createCopy();
		t1.setSchema(null);
		t1.setCatalog(null);

		String sql = builder.getSelectForColumns(t1, def.getColumns());
		String expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom PERSON";
//			System.out.println("----\n" + expected + "\n------\n" + sql);
		assertEquals(expected, sql);

		t1 = tbl.createCopy();
		t1.setCatalog(null);

		sql = builder.getSelectForColumns(t1, def.getColumns());
		expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom FOO.PERSON";
//			System.out.println("----\n" + expected + "\n------\n" + sql);
		assertEquals(expected, sql);

		t1 = tbl.createCopy();
		t1.setSchema(null);

		sql = builder.getSelectForColumns(t1, def.getColumns());
		expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom TABLESELECTBUILDERTEST.PERSON";
//			System.out.println("----\n" + expected + "\n------\n" + sql);
		assertEquals(expected, sql);

		sql = builder.getSelectForTable(tbl);
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

		String sql = builder.getSelectForTable(tbl);
		String expected = "select NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\nfrom PERSON with (nolock)";
		assertEquals(expected, sql);

		sql = builder.getSelectForColumns(tbl, new ArrayList<ColumnIdentifier>());
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

		String sql = builder.getSelectForColumns(tbl, new ArrayList<ColumnIdentifier>());
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

		String sql = builder.getSelectForTable(tbl);
		String expected = "SELECT NR,\n" +
			"       FIRSTNAME,\n" +
			"       LASTNAME\n" +
			"FROM PERSON";
		assertEquals(expected, sql);

		dbconfig.setDataTypeExpression("varchar", "upper(" + TableSelectBuilder.COLUMN_PLACEHOLDER + ")");

		sql = builder.getSelectForTable(tbl);
		expected = "SELECT NR,\n" +
			"       upper(FIRSTNAME),\n" +
			"       upper(LASTNAME)\n" +
			"FROM PERSON";
		assertEquals(expected, sql);

		dbconfig.setDataTypeExpression("varchar", null);

		// test invalid expression
		dbconfig.setDataTypeExpression("integer", "abs()");
		sql = builder.getSelectForTable(tbl);
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
}
