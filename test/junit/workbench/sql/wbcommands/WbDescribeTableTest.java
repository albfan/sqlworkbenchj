/*
 * WbDescribeTableTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.util.List;
import junit.framework.*;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbDescribeTableTest extends TestCase
{
	public WbDescribeTableTest(String testName)
	{
		super(testName);
	}

	protected void tearDown() throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
		super.tearDown();
	}

	public void testExecute() throws Exception
	{
		TestUtil util;
		StatementRunner runner;
		
		try
		{
			util = new TestUtil(getClass().getName()+"_testExecute");
			util.prepareEnvironment();
			runner = util.createConnectedStatementRunner();
			String sql = "create table describe_test (nr integer, info_text varchar(100));";
			runner.runStatement(sql, -1, -1);
			StatementRunnerResult result = runner.getResult();
			assertEquals("Could not create table", true, result.isSuccess());
			
			sql = "-- show table definition\ndesc describe_test;";
			runner.runStatement(sql, -1, -1);
			result = runner.getResult();
			assertEquals("Describe failed", true, result.isSuccess());
			
			List<DataStore> data = result.getDataStores();
			assertNotNull("No description returned", data);
			assertEquals("No data returned", 1, data.size());
			assertEquals("Wrong number of rows returned", 2, data.get(0).getRowCount());

			sql = "-- show table definition\ndescribe \"DESCRIBE_TEST\"\n-- for table;";
			runner.runStatement(sql, -1, -1);
			result = runner.getResult();
			assertEquals("Describe failed", true, result.isSuccess());
			
			data = result.getDataStores();
			assertNotNull("No description returned", data);
			assertEquals("No data returned", 1, data.size());
			
			assertEquals("Wrong number of rows returned", 2, data.get(0).getRowCount());
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
}
