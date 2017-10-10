/*
 * WbFeedbackTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
    StatementRunnerResult result = runner.runStatement(sql);
    assertEquals("Echo command not run", true, result.isSuccess());
    assertEquals("Feedback not turned off", false, runner.getVerboseLogging());

    sql = "--this is a test\n\techo\t    on";
    result = runner.runStatement(sql);
    assertEquals("Echo command not run", true, result.isSuccess());
    assertEquals("Feedback not turned off", true, runner.getVerboseLogging());

    sql = "--this is a test\n\techo\t    bla";
    result = runner.runStatement(sql);
    assertEquals("Echo command did not report an error", false, result.isSuccess());

    sql = "--this is a test\n\techo";
    result = runner.runStatement(sql);
    String msg = result.getMessages().toString().trim();
    String expected = ResourceMgr.getString("MsgFeedbackEnabled");
    assertEquals("Wrong message returned", expected, msg);
    assertEquals("Echo command not successful", true, result.isSuccess());
  }

}
