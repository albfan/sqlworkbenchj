/*
 * DdlCommandTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.interfaces.StatementRunner;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author support@sql-workbench.net
 */
public class DdlCommandTest 
	extends TestCase
{
	
	public DdlCommandTest(String testName)
	{
		super(testName);
	}

	public void testGetTypeAndObject() throws Exception
	{
		try
		{
			// detection of the type is already tested for SqlUtil.getCreateType()
			// so we only need to test getName();
			String sql = "-- test\ncreate or \t replace\n\nprocedure bla";
			String name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("bla", name);
			
			sql = "-- test\ncreate \n\ntrigger test_trg for mytable";
			name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("test_trg", name);
			
			sql = "-- test\ncreate function \n\n myfunc\n as something";
			name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("myfunc", name);

			sql = "-- test\ncreate or replace package \n\n some_package \t\t\n as something";
			name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("some_package", name);
			
			sql = "-- test\ncreate package body \n\n some_body \t\t\n as something";
			name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("some_body", name);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testIgnoreDropErrors()
	{
		try
		{
			TestUtil util = new TestUtil("ignoreDrop");
			StatementRunner runner = util.createConnectedStatementRunner();
			String sql = "drop table does_not_exist";
			runner.setIgnoreDropErrors(true);
			runner.runStatement(sql, 0, 0);
			StatementRunnerResult result = runner.getResult();
			assertTrue(result.isSuccess());
			
			runner.setIgnoreDropErrors(false);
			runner.setUseSavepoint(true);
			runner.runStatement(sql, 0, 0);
			result = runner.getResult();
			assertFalse(result.isSuccess());
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	
}
