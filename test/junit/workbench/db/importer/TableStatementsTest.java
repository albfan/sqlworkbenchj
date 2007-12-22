/*
 * TableStatementsTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import junit.framework.TestCase;
import workbench.db.TableIdentifier;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableStatementsTest
	extends TestCase
{
	public TableStatementsTest(String testName)
	{
		super(testName);
	}

	public void testGetTableStatement()
	{
		TableIdentifier tbl = new TableIdentifier("tsch", "address");
		
		TableStatements stmt = new TableStatements("delete from ${table.name}", null);
		String sql = stmt.getPreStatement(tbl);
		assertEquals("delete from address", sql);
		assertNull(stmt.getPostStatement(tbl));

		stmt = new TableStatements("set identity insert ${table.expression} on", "set identity insert ${table.expression} off");
		assertEquals("set identity insert tsch.address on", stmt.getPreStatement(tbl));
		assertEquals("set identity insert tsch.address off", stmt.getPostStatement(tbl));
		
	}
}
