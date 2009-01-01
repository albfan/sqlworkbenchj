/*
 * TableIdentifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import junit.framework.*;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableIdentifierTest
	extends TestCase
{
	public TableIdentifierTest(String testName)
	{
		super(testName);
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
			System.out.println("****" + exp);

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

	}

	public void testCopy()
	{
		String sql = "\"catalog\".\"schema\".\"table\"";
		TableIdentifier tbl = new TableIdentifier(sql);
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
