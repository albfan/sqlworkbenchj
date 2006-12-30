/*
 * TableCreatorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import junit.framework.TestCase;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import workbench.TestUtil;
import workbench.util.ArrayUtil;

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
			stmt.executeUpdate("CREATE TABLE create_test (zzz integer, bbb integer, aaa integer, ccc integer)");
			TableIdentifier oldTable = new TableIdentifier("create_test");
			TableIdentifier newTable = new TableIdentifier("new_table");
			
			List<ColumnIdentifier> clist = con.getMetadata().getTableColumns(oldTable);
			
			ColumnIdentifier[] cols  = new ColumnIdentifier[clist.size()];
			int i = 0;
			for (ColumnIdentifier col : clist)
			{
				cols[i++] = col;
			}
			ColumnIdentifier c = cols[0];
			cols[0] = cols[2];
			cols[2] = c;
			
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
