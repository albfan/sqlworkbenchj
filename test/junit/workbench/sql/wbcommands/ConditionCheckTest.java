/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import org.junit.Test;
import static org.junit.Assert.*;

import workbench.WbTestCase;
import workbench.sql.VariablePool;
import workbench.util.ArgumentParser;

/**
 *
 * @author Thomas Kellerer
 */
public class ConditionCheckTest
  extends WbTestCase
{


  public ConditionCheckTest()
  {
    super("ConditionCheckTest");
  }


  @Test
  public void testIfEquals()
  {
    try
    {
      VariablePool.getInstance().clear();
      VariablePool.getInstance().setParameterValue("do_update", "y");

      ArgumentParser cmdLine = new ArgumentParser();
      ConditionCheck.addParameters(cmdLine);

      cmdLine.parse("-ifEquals=\"do_update=y\"");

      ConditionCheck.Result result = ConditionCheck.checkConditions(cmdLine);
      assertTrue(result.isOK());

      VariablePool.getInstance().setParameterValue("do_update", "n");
      result = ConditionCheck.checkConditions(cmdLine);
      assertFalse(result.isOK());
      assertEquals("do_update", result.getVariable());
      assertEquals("y", result.getExpectedValue());
      assertEquals(ConditionCheck.PARAM_IF_EQUALS, result.getFailedCondition());
      String msg = ConditionCheck.getMessage("ErrInclude", result);
      assertEquals("Script not executed because variable \"do_update\" is not equal to y", msg);
    }
    finally
    {
      VariablePool.getInstance().clear();
    }
  }

}
