/*
 * EchoCommandTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import junit.framework.*;
import workbench.resource.ResourceMgr;
import workbench.sql.DefaultStatementRunner;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author support@sql-workbench.net
 */
public class EchoCommandTest extends TestCase
{
	
	public EchoCommandTest(String testName)
	{
		super(testName);
	}

	public void testExecute() throws Exception
	{
		try
		{
			DefaultStatementRunner runner = new DefaultStatementRunner();
			EchoCommand echo = new EchoCommand();
			runner.addCommand(echo);
			String sql = "--this is a test\n\techo\t    off";
			runner.runStatement(sql, 0, 0);
			StatementRunnerResult result = runner.getResult();
			assertEquals("Echo command not run", true, result.isSuccess());
			assertEquals("Feedback not turned off", false, runner.getVerboseLogging());

			sql = "--this is a test\n\techo\t    on";
			runner.runStatement(sql, 0, 0);
			result = runner.getResult();
			assertEquals("Echo command not run", true, result.isSuccess());
			assertEquals("Feedback not turned off", true, runner.getVerboseLogging());

			sql = "--this is a test\n\techo\t    bla";
			runner.runStatement(sql, 0, 0);
			result = runner.getResult();
			assertEquals("Echo command did not report an error", false, result.isSuccess());

			sql = "--this is a test\n\techo";
			runner.runStatement(sql, 0, 0);
			result = runner.getResult();
			String msg = result.getMessageBuffer().toString().trim();
			String expected = ResourceMgr.getString("MsgFeedbackEnabled");
			assertEquals("Echo command not successful", true, result.isSuccess());
			assertEquals("Wrong message returned", expected, msg);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
}
