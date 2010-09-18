/*
 * WbFeedbackTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import workbench.TestUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class WbFeedbackTest
	extends WbTestCase
{

	public WbFeedbackTest()
	{
		super("WbFeedbackTest");
	}

	@Test
	public void testEcho()
		throws Exception
	{
		TestUtil util = new TestUtil("testEchoExec");
		util.prepareEnvironment();
		StatementRunner runner = new StatementRunner();
		WbFeedback echo = new WbFeedback("ECHO");
		runner.addCommand(echo);
		String sql = "--this is a test\n\techo\t    off";
		runner.runStatement(sql);
		StatementRunnerResult result = runner.getResult();
		assertEquals("Echo command not run", true, result.isSuccess());
		assertEquals("Feedback not turned off", false, runner.getVerboseLogging());

		sql = "--this is a test\n\techo\t    on";
		runner.runStatement(sql);
		result = runner.getResult();
		assertEquals("Echo command not run", true, result.isSuccess());
		assertEquals("Feedback not turned off", true, runner.getVerboseLogging());

		sql = "--this is a test\n\techo\t    bla";
		runner.runStatement(sql);
		result = runner.getResult();
		assertEquals("Echo command did not report an error", false, result.isSuccess());

		sql = "--this is a test\n\techo";
		runner.runStatement(sql);
		result = runner.getResult();
		String msg = result.getMessageBuffer().toString().trim();
		String expected = ResourceMgr.getString("MsgFeedbackEnabled");
		assertEquals("Wrong message returned", expected, msg);
		assertEquals("Echo command not successful", true, result.isSuccess());
	}

}
