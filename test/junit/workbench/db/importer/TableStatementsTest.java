/*
 * TableStatementsTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import org.junit.Test;
import workbench.db.TableIdentifier;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableStatementsTest
{

	@Test
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
