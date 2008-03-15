/*
 * TableCreatorTest.java
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

import java.util.ArrayList;
import junit.framework.TestCase;
import java.sql.Statement;
import java.util.List;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableCreatorTest extends TestCase
{
	private TestUtil util;
	
	public TableCreatorTest(String testName)
	{
		super(testName);
		try
		{
			util = new TestUtil(testName);
			util.prepareEnvironment();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void setUp()
		throws Exception
	{
		super.setUp();
		util.emptyBaseDirectory();
	}
	
	public void testCreateTable() 
		throws Exception
	{
		
		try
		{
			WbConnection con = util.getConnection();
			Statement stmt = con.createStatement();
			// Include a keyword as a column name to make sure TableCreator is properly quoting those names
			stmt.executeUpdate("CREATE TABLE create_test (zzz integer, bbb integer, aaa integer, \"PRIMARY\" integer)");
			TableIdentifier oldTable = new TableIdentifier("create_test");
			TableIdentifier newTable = new TableIdentifier("new_table");
			
			List<ColumnIdentifier> clist = con.getMetadata().getTableColumns(oldTable);
			
			// Make sure the table is created with the same column 
			// ordering as the original table.
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
			
			for (ColumnIdentifier col : clist)
			{
				cols.add(col);
			}
			ColumnIdentifier c1 = cols.get(0);
			ColumnIdentifier c2 = cols.get(2);
			cols.set(0, c2);
			cols.set(2, c1);
			
			TableCreator creator = new TableCreator(con, newTable, cols);
			creator.createTable();
			
			clist = con.getMetadata().getTableColumns(newTable);
			assertEquals(4, clist.size());
			
			assertEquals("ZZZ", clist.get(0).getColumnName());
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
	
}
