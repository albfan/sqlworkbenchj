/*
 * DummyInsertTest.java
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
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class DummyInsertTest 
	extends TestCase 
{
    
	public DummyInsertTest(String testName) 
	{
		super(testName);
	}

	public void testGetSource()
		throws Exception
	{
		TestUtil util = new TestUtil("dropColumn");
		WbConnection con = util.getConnection();
		
		try
		{
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20))");
			con.commit();
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			DummyInsert insert = new DummyInsert(person);
			String sql = insert.getSource(con).toString();
			System.out.println(sql);
			String verb = SqlUtil.getSqlVerb(sql);
			assertEquals("INSERT", verb);
			assertTrue(sql.indexOf("(NR_value, 'FIRSTNAME_value', 'LASTNAME_value')") > -1);
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
