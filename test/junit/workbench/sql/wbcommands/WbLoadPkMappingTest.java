/*
 * WbLoadPkMappingTest.java
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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import org.junit.AfterClass;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.storage.PkMapping;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class WbLoadPkMappingTest
  extends WbTestCase
{

  public WbLoadPkMappingTest()
  {
    super("WbLoadPkMappingTest");
  }

  @AfterClass
  public static void tearDown()
  {
    PkMapping.getInstance().clear();
  }

  @Test
  public void testExecute()
    throws Exception
  {
    TestUtil util = getTestUtil();
    StatementRunner runner;

    try
    {
      util.prepareEnvironment();
      runner = util.createConnectedStatementRunner();

      File f = new File(util.getBaseDir(), "pkmapping.def");
      PrintWriter w = new PrintWriter(new FileWriter(f));
      w.println("junitpk=id,name");
      w.close();

      String sql = "-- load mapping from a file \n     " + WbLoadPkMapping.VERB + "\n -file='" + f.getAbsolutePath() + "'";
      SqlCommand command = runner.getCommandToUse(sql);
      assertTrue(command instanceof WbLoadPkMapping);
      runner.runStatement(sql);
      StatementRunnerResult result = runner.getResult();
      assertEquals("Loading not successful", true, result.isSuccess());

      Map mapping = PkMapping.getInstance().getMapping();
      String cols = (String)mapping.get("junitpk");
      assertEquals("Wrong pk mapping stored", "id,name", cols);
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

}
