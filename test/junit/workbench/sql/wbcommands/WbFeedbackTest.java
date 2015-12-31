/*
 * WbFeedbackTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		String msg = result.getMessages().toString().trim();
		String expected = ResourceMgr.getString("MsgFeedbackEnabled");
		assertEquals("Wrong message returned", expected, msg);
		assertEquals("Echo command not successful", true, result.isSuccess());
	}

}
