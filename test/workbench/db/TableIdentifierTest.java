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

import java.sql.Connection;
import java.sql.DriverManager;
import junit.framework.*;

/**
 *
 * @author <a href="mailto:thomas.kellerer@mgm-tp.com">Thomas Kellerer</a>
 */
public class TableIdentifierTest extends TestCase
{
	
	public TableIdentifierTest(String testName)
	{
		super(testName);
	}

	protected void setUp() throws Exception
	{
	}

	protected void tearDown() throws Exception
	{
	}

	public void testIdentifier()
	{
		String sql = "BDB_IE.dbo.tblBDBMMPGroup";
		TableIdentifier tbl = new TableIdentifier(sql);
		assertEquals("BDB_IE", tbl.getCatalog());
		assertEquals("dbo", tbl.getSchema());
		assertEquals("BDB_IE.dbo.tblBDBMMPGroup", tbl.getTableExpression());
		
	}
	
}
