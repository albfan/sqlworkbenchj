/*
 * ColumnDropperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class ColumnDropperTest
	extends TestCase
{
	public ColumnDropperTest(String testName)
	{
		super(testName);
	}

	public void testDropObjects()
		throws Exception
	{
		TestUtil util = new TestUtil("dropColumn");
		WbConnection con = util.getConnection();
		
		try
		{
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20), dummy1 integer, dummy2 date)");
			con.commit();
			TableIdentifier table = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
			cols.add(new ColumnIdentifier("DUMMY1"));
			cols.add(new ColumnIdentifier("DUMMY2"));
			
			ColumnDropper dropper = new ColumnDropper(con, table, cols);
			dropper.dropObjects();
			
			List<ColumnIdentifier> tableCols = con.getMetadata().getTableColumns(table);
			assertEquals(3, tableCols.size());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
