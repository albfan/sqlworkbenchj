/*
 * TableIdentifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import junit.framework.*;
import workbench.TestUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableIdentifierTest
	extends TestCase
{
	public TableIdentifierTest(String testName)
	{
		super(testName);
	}

	public void testRemoveCollection()
	{
		List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
		tables.add(new TableIdentifier("SCHEMA", "TABLE_1"));
		tables.add(new TableIdentifier("SCHEMA", "TABLE_2"));
		tables.add(new TableIdentifier("SCHEMA", "TABLE_3"));

		List<TableIdentifier> toRemove = new ArrayList<TableIdentifier>();
		toRemove.add(new TableIdentifier("SCHEMA", "TABLE_2"));
		tables.removeAll(toRemove);

		assertEquals(2, tables.size());
	}
	
	public void testSQLServerNaming()
	{
		String name = "[linked_server].[some_catalog].dbo.some_table";
		TableIdentifier tbl = new TableIdentifier(name);

		assertEquals("some_table", tbl.getTableName());
		assertEquals("dbo", tbl.getSchema());
		assertEquals("[some_catalog]", tbl.getCatalog());
		assertEquals("[linked_server]", tbl.getServerPart());

		TableIdentifier copy = tbl.createCopy();
		assertEquals("some_table", copy.getTableName());
		assertEquals("dbo", copy.getSchema());
		assertEquals("[some_catalog]", copy.getCatalog());
		assertEquals("[linked_server]", copy.getServerPart());

		assertEquals(tbl, copy);
	}

	public void testDropName()
	{
		try
		{
			TestUtil util = new TestUtil("tableIdentifierTest");
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
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
		
	}
	public void testQuoteSpecialChars()
	{
		try
		{
			TestUtil util = new TestUtil("tableIdentifierTest");
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
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

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
	}

	public void testEqualsAndHashCode()
	{
		TableIdentifier one = new TableIdentifier("person");
		TableIdentifier two = new TableIdentifier("person");
		TableIdentifier three = new TableIdentifier("address");
		Set<TableIdentifier> ids = new HashSet<TableIdentifier>();
		ids.add(one);
		ids.add(two);
		ids.add(three);
		assertEquals("Too many entries", 2, ids.size());

		Set<TableIdentifier> tids = new TreeSet<TableIdentifier>();
		tids.add(one);
		tids.add(two);
		tids.add(three);
		assertEquals("Too many entries", 2, tids.size());
	}
}
