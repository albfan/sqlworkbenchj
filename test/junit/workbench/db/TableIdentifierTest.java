/*
 * TableIdentifierTest.java
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
package workbench.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.util.StringUtil;

import org.junit.Test;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableIdentifierTest
	extends WbTestCase
{
	public TableIdentifierTest()
	{
		super("TableIdentifierTest");
	}


	@Test
	public void testTwoElement()
	{
		TableIdentifier tbl = new TableIdentifier("cat.table", '.', '.', true, false);
		assertNull(tbl.getSchema());
		assertEquals("cat", tbl.getCatalog());
		assertEquals("table", tbl.getTableName());

		tbl = new TableIdentifier("schema.table", '.', '.', false, true);
		assertNull(tbl.getCatalog());
		assertEquals("schema", tbl.getSchema());
		assertEquals("table", tbl.getTableName());

		tbl = new TableIdentifier("schema.table", '.', '.', true, true);
		assertNull(tbl.getCatalog());
		assertEquals("schema", tbl.getSchema());
		assertEquals("table", tbl.getTableName());
	}

	@Test
	public void testGetParts()
	{
		String id = "foobar";
		assertNull(TableIdentifier.getCatalogPart(id, '.'));
		assertNull(TableIdentifier.getCatalogPart(id, ':'));
		assertEquals("foo", TableIdentifier.getCatalogPart("foo.bar", '.'));
		assertEquals("bar", TableIdentifier.getNamePart("foo.bar", '.'));

		assertEquals("foo", TableIdentifier.getCatalogPart("foo:bar", ':'));
		assertEquals("bar", TableIdentifier.getNamePart("foo/bar", '/'));

		assertEquals("bar.tbl", TableIdentifier.getNamePart("foo/bar.tbl", '/'));

		TableIdentifier tbl = new TableIdentifier("RICH/\"FOO.BAR\"", '/', '/');
		assertEquals("RICH", tbl.getSchema());
		assertEquals("FOO.BAR", tbl.getTableName());
		assertNull(tbl.getCatalog());
	}

	@Test
	public void fullyQualifiedNewTable()
	{
		TableIdentifier tbl = new TableIdentifier("some_schema.new_table");
		tbl.setNewTable(true);
		String fullname = tbl.getTableExpression();
		assertEquals("some_schema", tbl.getSchema());
		assertEquals("some_schema.new_table", fullname);
	}

	@Test
	public void testAlternateSeparator()
	{
		TableIdentifier tbl = new TableIdentifier("somelib/sometable", '/', '/');
		assertEquals("somelib", tbl.getSchema());
		assertEquals("sometable", tbl.getTableName());

		tbl = new TableIdentifier("somelib/sometable", '.', '.');
		assertNull(tbl.getSchema());
		assertEquals("somelib/sometable", tbl.getTableName());

		tbl = new TableIdentifier("somelib:someschema.tablename", ':', '.');
		assertEquals("somelib", tbl.getCatalog());
		assertEquals("someschema", tbl.getSchema());
		assertEquals("tablename", tbl.getTableName());
	}

	@Test
	public void testRemoveCollection()
	{
		List<TableIdentifier> tables = new ArrayList<>();
		tables.add(new TableIdentifier("SCHEMA", "TABLE_1"));
		tables.add(new TableIdentifier("SCHEMA", "TABLE_2"));
		tables.add(new TableIdentifier("SCHEMA", "TABLE_3"));

		List<TableIdentifier> toRemove = new ArrayList<>();
		toRemove.add(new TableIdentifier("SCHEMA", "TABLE_2"));
		tables.removeAll(toRemove);

		assertEquals(2, tables.size());
	}

	@Test
	public void testSQLServerNaming()
	{
		String name = "[linked_server].[some_catalog].dbo.some_table";
		TableIdentifier tbl = new TableIdentifier(name);

		assertEquals("some_table", tbl.getTableName());
		assertEquals("dbo", tbl.getSchema());
		assertEquals("some_catalog", tbl.getCatalog());
		assertEquals("[linked_server]", tbl.getServerPart());

		TableIdentifier copy = tbl.createCopy();
		assertEquals("some_table", copy.getTableName());
		assertEquals("dbo", copy.getSchema());
		assertEquals("some_catalog", copy.getCatalog());
		assertEquals("[linked_server]", copy.getServerPart());

		assertEquals(tbl, copy);
	}

	@Test
	public void testDropName()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			WbConnection con = util.getConnection("dropExpressionTest");

			TestUtil.executeScript(con, "create schema s1;\n " +
				"set schema public;\n " +
				"create table table1 (id integer);\n" +
				"create table table2 (id integer);\n" +
				"commit;\n " +
				"set schema s1;" +
				"create table table2 (id integer);\n " +
				"create table table3 (id integer);\n " +
				"set schema s1;\n " +
				"commit; \n");

			TableIdentifier t1 = con.getMetadata().findTable(new TableIdentifier("PUBLIC.TABLE1"));
			TestUtil.executeScript(con, "set schema s1;");
			assertEquals("PUBLIC.TABLE1", t1.getObjectNameForDrop(con));
//			assertNull(t1);
//			System.out.println("table: " + t1.getObjectNameForDrop(con));
			TestUtil.executeScript(con, "set schema public;");
			assertEquals("TABLE1", t1.getObjectNameForDrop(con));
//			System.out.println("table: " + t1.getObjectNameForDrop(con));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testQuotedSeparator()
	{
		TableIdentifier tbl = new TableIdentifier("\"foo.bar\"");
		assertEquals("foo.bar", tbl.getTableName());
		assertEquals(null, tbl.getSchema());

		tbl = new TableIdentifier("schema.\"foo.bar\"");
		assertEquals("foo.bar", tbl.getTableName());
		assertEquals("schema", tbl.getSchema());
		assertEquals("schema.\"foo.bar\"", tbl.getTableExpression());
	}

	@Test
	public void testQuoteSpecialChars()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			WbConnection con = util.getConnection("tidTest");
			String t = "\"123\".TABLENAME";
			TableIdentifier tbl = new TableIdentifier(t);
			tbl.setPreserveQuotes(true);
			String exp = tbl.getTableExpression(con);
			assertEquals("Wrong expression", t, exp);

			t = "table:bla";
			tbl = new TableIdentifier(t);
			tbl.setPreserveQuotes(true);
			exp = tbl.getTableExpression(con);
			assertEquals("Wrong expression", "\"" + t.toUpperCase() + "\"", exp);

			t = "table\\bla";
			tbl = new TableIdentifier(t);
			tbl.setPreserveQuotes(true);
			exp = tbl.getTableExpression(con);
			assertEquals("Wrong expression", "\"" + t.toUpperCase() + "\"", exp);

			t = "table bla";
			tbl = new TableIdentifier(t);
			tbl.setPreserveQuotes(true);
			exp = tbl.getTableExpression(con);
			assertEquals("Wrong expression", "\"" + t.toUpperCase() + "\"", exp);

			t = "\"TABLE.BLA\"";
			tbl = new TableIdentifier(t);
			//tbl.setPreserveQuotes(true);
			exp = tbl.getTableExpression();
			assertEquals("Wrong expression", t.toUpperCase(), exp);
			assertNull("Schema present", tbl.getSchema());

			t = "table;1";
			tbl = new TableIdentifier(t);
			//tbl.setPreserveQuotes(true);
			exp = tbl.getTableExpression(con);
			assertEquals("Wrong expression", "\"" + t.toUpperCase() + "\"", exp);
			assertNull("Schema present", tbl.getSchema());

			t = "table,1";
			tbl = new TableIdentifier(t);
			//tbl.setPreserveQuotes(true);
			exp = tbl.getTableExpression(con);
			assertEquals("Wrong expression", "\"" + t.toUpperCase() + "\"", exp);
			assertNull("Schema present", tbl.getSchema());

			t = "123.TABLENAME";
			tbl = new TableIdentifier(t);
			exp = tbl.getTableExpression(con);
			assertEquals("Wrong expression", "\"123\".TABLENAME", exp);

			t = "dbo.company";
			tbl = new TableIdentifier(t);
			exp = tbl.getTableExpression(con);
//			System.out.println("****" + exp);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testIdentifier()
	{
		String sql = "BDB_IE.dbo.tblBDBMMPGroup";
		TableIdentifier tbl = new TableIdentifier(sql);
		assertEquals("BDB_IE", tbl.getCatalog());
		assertEquals("dbo", tbl.getSchema());
		assertEquals("BDB_IE.dbo.tblBDBMMPGroup", tbl.getTableExpression());

		sql = "\"APP\".\"BLOB_TEST\"";
		tbl = new TableIdentifier(sql);
		assertEquals("APP", tbl.getSchema());
		assertEquals("BLOB_TEST", tbl.getTableName());

		tbl = new TableIdentifier(sql);
		tbl.setPreserveQuotes(true);
		assertEquals("\"APP\"", tbl.getSchema());
		assertEquals("\"BLOB_TEST\"", tbl.getTableName());

		sql = "\"Some.Table\"";
		tbl = new TableIdentifier(sql);
		tbl.setPreserveQuotes(true);
		assertEquals("\"Some.Table\"", tbl.getTableName());

		sql = "\"123\".mytable";
		tbl = new TableIdentifier(sql);
		tbl.setPreserveQuotes(true);
		assertEquals("mytable", tbl.getTableName());
		assertEquals("\"123\"", tbl.getSchema());

		sql = "information_schema.s";
		tbl = new TableIdentifier(sql);
		assertEquals("s", tbl.getTableName());
		assertEquals("information_schema", tbl.getSchema());

		sql = "information_schema.";
		tbl = new TableIdentifier(sql);
		assertTrue(StringUtil.isBlank(tbl.getTableName()));
		assertEquals("information_schema", tbl.getSchema());
	}

	@Test
	public void testCopy()
	{
		String sql = "\"catalog\".\"schema\".\"table\"";
		TableIdentifier tbl = new TableIdentifier(sql);
		tbl.setComment("this is a comment");
		tbl.setPreserveQuotes(true);
		assertEquals("\"catalog\"", tbl.getCatalog());
		assertEquals("\"schema\"", tbl.getSchema());
		assertEquals("\"table\"", tbl.getTableName());

		assertEquals(sql, tbl.getTableExpression());

		TableIdentifier t2 = tbl.createCopy();
		assertEquals("\"catalog\"", t2.getCatalog());
		assertEquals("\"schema\"", t2.getSchema());
		assertEquals("\"table\"", t2.getTableName());
		assertEquals(sql, t2.getTableExpression());
		assertEquals(true, tbl.equals(t2));
		assertEquals("this is a comment", t2.getComment());

		tbl = new TableIdentifier("foo");
		tbl.getSourceOptions().setTypeModifier("TEMP");

		t2 = tbl.createCopy();
		assertEquals(tbl.getSourceOptions().getTypeModifier(), t2.getSourceOptions().getTypeModifier());

		tbl.setUseInlinePK(true);
		t2 = tbl.createCopy();
		assertTrue(t2.getUseInlinePK());
	}

	@Test
	public void testNameOnly()
	{
		TableIdentifier foo = new TableIdentifier("myschema", "foo");
		foo.setUseNameOnly(true);
		assertThat(foo.getTableExpression(), is("foo"));
	}

	@Test
	public void testEqualsAndHashCode()
	{
		TableIdentifier one = new TableIdentifier("person");
		TableIdentifier two = new TableIdentifier("person");
		TableIdentifier three = new TableIdentifier("address");
		Set<TableIdentifier> ids = new HashSet<>();
		ids.add(one);
		ids.add(two);
		ids.add(three);
		assertEquals("Too many entries", 2, ids.size());

		Set<TableIdentifier> tids = new TreeSet<>();
		tids.add(one);
		tids.add(two);
		tids.add(three);
		assertEquals("Too many entries", 2, tids.size());
	}
}
