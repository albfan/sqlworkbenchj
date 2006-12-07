/*
 * TableIdentifierTest.java
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import junit.framework.*;

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
	
}
