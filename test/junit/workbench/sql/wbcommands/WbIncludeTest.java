/*
 * WbIncludeTest.java
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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class WbIncludeTest
  extends WbTestCase
{
  private TestUtil util;
  private StatementRunner runner;

  public WbIncludeTest()
  {
    super("WbIncludeTest");
    util = getTestUtil();
  }

  @Before
  public void setUp()
    throws Exception
  {
    util.emptyBaseDirectory();
    runner = util.createConnectedStatementRunner();
  }

  @Test
  public void testConditionalInclude()
    throws Exception
  {
    try
    {
      WbConnection con = runner.getConnection();

      TestUtil.executeScript(con,
        "create table include_test (some_name varchar(100));\n" +
        "commit;");

      String encoding = "ISO-8859-1";
      File scriptFile = new File(util.getBaseDir(), "test_1.sql");

      TestUtil.writeFile(scriptFile, "insert into include_test (some_name) values ('one');", encoding);

      String sql = "WbInclude -ifDefined=foobar -file='" + scriptFile.getAbsolutePath() + "'";

      StatementRunnerResult result = runner.runStatement(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Number cnt = (Number)TestUtil.getSingleQueryValue(con, "select count(*) from include_test");
      int count = cnt.intValue();
      assertEquals(0, count);

      VariablePool.getInstance().setParameterValue("foobar", "test");
      result = runner.runStatement(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      cnt = (Number)TestUtil.getSingleQueryValue(con, "select count(*) from include_test");
      count = cnt.intValue();
      assertEquals(1, count);

      TestUtil.executeScript(con,
        "delete from include_test;" +
        "commit;");

      VariablePool.getInstance().setParameterValue("debug", "false");
      sql = "WbInclude -ifEquals='debug=true' -file='" + scriptFile.getAbsolutePath() + "'";
      result = runner.runStatement(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      cnt = (Number)TestUtil.getSingleQueryValue(con, "select count(*) from include_test");
      count = cnt.intValue();
      assertEquals(0, count);

      VariablePool.getInstance().setParameterValue("debug", "foobar");
      sql = "WbInclude -ifEquals='debug=foobar' -file='" + scriptFile.getAbsolutePath() + "'";
      result = runner.runStatement(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      cnt = (Number)TestUtil.getSingleQueryValue(con, "select count(*) from include_test");
      count = cnt.intValue();
      assertEquals(1, count);
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

  @Test
  public void testMultipleFiles()
    throws Exception
  {
    try
    {
      WbConnection con = runner.getConnection();

      TestUtil.executeScript(con,
        "create table include_test (some_name varchar(100));\n" +
        "commit;");

      String encoding = "ISO-8859-1";
      File scriptFile = new File(util.getBaseDir(), "test_1.sql");

      TestUtil.writeFile(scriptFile, "insert into include_test (some_name) values ('one');", encoding);

      scriptFile = new File(util.getBaseDir(), "test_2.sql");

      TestUtil.writeFile(scriptFile,
        "insert into include_test (some_name) values ('two');\n"+
        "commit;\n", encoding);

      String sql = "WbInclude -file='" + util.getBaseDir() + "/test*.sql'";

      StatementRunnerResult result = runner.runStatement(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      int count = TestUtil.getNumberValue(con, "select count(*) from include_test");
      assertEquals(2, count);
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

  @Test
  public void testAlternateInclude()
    throws Exception
  {
    try
    {
      WbConnection con = runner.getConnection();

      TestUtil.executeScript(con,
        "create table include_test (file_name varchar(100));\n" +
        "commit;");

      String encoding = "ISO-8859-1";
      File scriptFile = new File(util.getBaseDir(), "test.sql");

      TestUtil.writeFile(scriptFile,
        "insert into include_test (file_name) values ('" + scriptFile.getAbsolutePath() + "');\n"+
        "commit;\n", encoding);

      String sql = "-- comment\n\n@test.sql\n";
      StatementRunnerResult result = runner.runStatement(sql);
      assertEquals("Statement not executed", true, result.isSuccess());

      int count = TestUtil.getNumberValue(con, "select count(*) from include_test");
      assertEquals("Rows not inserted", 1, count);
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

  @Test
  public void testFileNotFound()
    throws Exception
  {
    try
    {
      StatementRunnerResult result = runner.runStatement("WbInclude -file=/this/will/not/be/there/i_hope.sql");
      assertFalse("Runner was successful", result.isSuccess());
      String msg = result.getMessages().toString();
      assertTrue("Wrong error", msg.indexOf("not found") > -1);
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

  @Test
  public void testNestedInclude()
    throws Exception
  {
    try
    {
      WbConnection con = runner.getConnection();

      File subdir1 = new File(util.getBaseDir(), "subdir1");
      subdir1.mkdir();

      File include1 = new File(subdir1, "include1.sql");

      TestUtil.executeScript(con,
        "create table include_test (id integer, file_name varchar(100));\n" +
        "commit;\n");

      String encoding = "ISO-8859-1";

      TestUtil.writeFile(include1,
        "insert into include_test (id, file_name) values (1, '" + include1.getAbsolutePath() + "');\n" +
        "commit;\n", encoding);

      File main = new File(util.getBaseDir(), "main.sql");

      TestUtil.writeFile(main,
        "insert into include_test (id, file_name) values (2, '" + main.getAbsolutePath() + "');\n" +
        "commit;\n" +
        "@./" + subdir1.getName() + "/" + include1.getName() + "\n", encoding);

      StatementRunnerResult result = runner.runStatement("wbinclude -file='" + main.getAbsolutePath() + "';\n");
      assertEquals("Runner not successful", true, result.isSuccess());

      DataStore ds = TestUtil.getQueryResult(con, "select file_name from include_test order by id");
      assertEquals("Not enough values retrieved", 2, ds.getRowCount());
      assertEquals("Main file not run", main.getAbsolutePath(), ds.getValueAsString(1, 0));
      assertEquals("Second file not run", include1.getAbsolutePath(), ds.getValueAsString(0, 0));
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

}
