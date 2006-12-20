/*
 * WbCopyTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.Statement;
import junit.framework.*;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.DefaultStatementRunner;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbCopyTest extends TestCase
{
	
	public WbCopyTest(String testName)
	{
		super(testName);
	}

	public void testExecute() throws Exception
	{
		try
		{
			TestUtil util = new TestUtil(getClass().getName()+"_testExecute");
			util.prepareEnvironment();
			
			DefaultStatementRunner runner = util.createConnectedStatementRunner();
			WbConnection con = runner.getConnection();
			
			Statement stmt = con.createStatement();
			
			stmt.executeUpdate("create table source_data (nr integer primary key, lastname varchar(50), firstname varchar(50))");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (1,'Dent', 'Arthur')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (2,'Beeblebrox', 'Zaphod')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (3,'Moviestar', 'Mary')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (4,'Perfec', 'Ford')");

			con.commit();
			
			String sql = "--copy source_data and create target\nwbcopy -sourceTable=source_data -targettable=target_data -createtarget=true";
			runner.runStatement(sql, -1, -1);
			StatementRunnerResult result = runner.getResult();
			assertEquals("Copy not successful", true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from target_data");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 4, count);
			}
			rs.close();
			rs = stmt.executeQuery("select lastname from target_data where nr = 3");
			if (rs.next())
			{
				String name = rs.getString(1);
				assertEquals("Incorrect value copied", "Moviestar", name);
			}
			else
			{
				fail("Record with nr = 3 not copied");
			}
			
			stmt.executeUpdate("update source_data set lastname = 'Prefect' where nr = 4");
			con.commit();
			
			// Allow WbCopy to find the PK columns automatically.
			//stmt.executeUpdate("alter table target_data add primary key (nr)");
			//con.commit();

			sql = "--copy source_data and create target\nwbcopy -sourceTable=source_data -targettable=target_data -mode=update";
			runner.runStatement(sql, -1, -1);
			result = runner.getResult();
			assertEquals("Copy not successful", true, result.isSuccess());
			
			rs = stmt.executeQuery("select lastname from target_data where nr = 4");
			if (rs.next())
			{
				String name = rs.getString(1);
				assertEquals("Incorrect value copied", "Prefect", name);
			}
			else
			{
				fail("Record with nr = 4 not copied");
			}
			
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
