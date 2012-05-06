/*
 * SelectIntoVerifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectIntoVerifierTest
	extends WbTestCase
{
	public SelectIntoVerifierTest()
	{
		super("SelectIntoTesterTest");
	}

	@Test
	public void testIsSelectIntoNewTable()
		throws Exception
	{
		SelectIntoVerifier tester = new SelectIntoVerifier("microsoft_sql_server");

		String sql = "select * into new_table from old_table;";
		assertTrue("Pattern for SQL Server not working", tester.isSelectIntoNewTable(sql));

		sql = "-- Test\n" +
					"select * into #temp2 from #temp1;\n";
		assertTrue("Pattern for SQL Server not working", tester.isSelectIntoNewTable(sql));


		tester = new SelectIntoVerifier("postgresql");
		sql = "select * into new_table from old_table;";
		assertTrue("Pattern for Postgres not working", tester.isSelectIntoNewTable(sql));

		sql = "-- Test\n" +
					"select * into new_table from old_table;\n";
		assertTrue("Pattern for Postgres not working", tester.isSelectIntoNewTable(sql));

		tester = new SelectIntoVerifier("informix-online");
		sql = "select * from old_table into new_table";
		assertTrue(tester.isSelectIntoNewTable(sql));
	}
}
