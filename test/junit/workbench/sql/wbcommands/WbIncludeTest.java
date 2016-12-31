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
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;

import workbench.util.EncodingUtil;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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

      Writer w = EncodingUtil.createWriter(scriptFile, encoding, false);
      w.write("insert into include_test (some_name) values ('one');\n");
      w.close();

      String sql = "WbInclude -ifDefined=foobar -file='" + scriptFile.getAbsolutePath() + "'";

      runner.runStatement(sql);
      StatementRunnerResult result = runner.getResult();
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Number cnt = (Number)TestUtil.getSingleQueryValue(con, "select count(*) from include_test");
      int count = cnt.intValue();
      assertEquals(0, count);

      VariablePool.getInstance().setParameterValue("foobar", "test");
      runner.runStatement(sql);
      result = runner.getResult();
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      cnt = (Number)TestUtil.getSingleQueryValue(con, "select count(*) from include_test");
      count = cnt.intValue();
      assertEquals(1, count);

      TestUtil.executeScript(con,
        "delete from include_test;" +
        "commit;");

      VariablePool.getInstance().setParameterValue("debug", "false");
      sql = "WbInclude -ifEquals='debug=true' -file='" + scriptFile.getAbsolutePath() + "'";
      runner.runStatement(sql);
      result = runner.getResult();
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      cnt = (Number)TestUtil.getSingleQueryValue(con, "select count(*) from include_test");
      count = cnt.intValue();
      assertEquals(0, count);

      VariablePool.getInstance().setParameterValue("debug", "foobar");
      sql = "WbInclude -ifEquals='debug=foobar' -file='" + scriptFile.getAbsolutePath() + "'";
      runner.runStatement(sql);
      result = runner.getResult();
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

      Statement stmt = con.createStatement();
      stmt.execute("create table include_test (some_name varchar(100))");
      con.commit();

      String encoding = "ISO-8859-1";
      File scriptFile = new File(util.getBaseDir(), "test_1.sql");

      Writer w = EncodingUtil.createWriter(scriptFile, encoding, false);
      w.write("insert into include_test (some_name) values ('one');\n");
      w.close();

      scriptFile = new File(util.getBaseDir(), "test_2.sql");

      w = EncodingUtil.createWriter(scriptFile, encoding, false);
      w.write("insert into include_test (some_name) values ('two');\n");
      w.write("commit;\n");
      w.close();

      String sql = "WbInclude -file='" + util.getBaseDir() + "/test*.sql'";

      runner.runStatement(sql);
      StatementRunnerResult result = runner.getResult();
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Object o = TestUtil.getSingleQueryValue(con, "select count(*) from include_test");
      if (o instanceof Number)
      {
        int count = ((Number)o).intValue();
        assertEquals(2, count);
      }
      else
      {
        fail("No count returned");
      }
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

      Statement stmt = con.createStatement();
      stmt.execute("create table include_test (file_name varchar(100))");
      con.commit();

      String encoding = "ISO-8859-1";
      File scriptFile = new File(util.getBaseDir(), "test.sql");

      Writer w = EncodingUtil.createWriter(scriptFile, encoding, false);
      w.write("insert into include_test (file_name) values ('" + scriptFile.getAbsolutePath() + "');\n");
      w.write("commit;\n");
      w.close();

      String sql = "-- comment\n\n@test.sql\n";
      runner.runStatement(sql);
      StatementRunnerResult result = runner.getResult();
      assertEquals("Statement not executed", true, result.isSuccess());

      ResultSet rs = stmt.executeQuery("select count(*) from include_test");

      if (rs.next())
      {
        int count = rs.getInt(1);
        assertEquals("Rows not inserted", 1, count);
      }
      else
      {
        fail("Select failed");
      }
      rs.close();
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
      runner.runStatement("WbInclude -file=/this/will/not/be/there/i_hope.sql");
      StatementRunnerResult result = runner.getResult();
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

      Statement stmt = con.createStatement();
      stmt.execute("create table include_test (file_name varchar(100))");
      con.commit();

      String encoding = "ISO-8859-1";
      Writer w = EncodingUtil.createWriter(include1, encoding, false);
      w.write("insert into include_test (file_name) values ('" + include1.getAbsolutePath() + "');\n");
      w.write("commit;\n");
      w.close();

      File main = new File(util.getBaseDir(), "main.sql");
      w = EncodingUtil.createWriter(main, encoding, false);
      w.write("insert into include_test (file_name) values ('" + main.getAbsolutePath() + "');\n");
      w.write("commit;\n");
      w.write("@./" + subdir1.getName() + "/" + include1.getName() + "\n");
      w.close();

      runner.runStatement("wbinclude -file='" + main.getAbsolutePath() + "';\n");
      StatementRunnerResult result = runner.getResult();
      assertEquals("Runner not successful", true, result.isSuccess());

      ResultSet rs = stmt.executeQuery("select * from include_test");
      List files = new ArrayList();
      while (rs.next())
      {
        files.add(rs.getString(1));
      }
      rs.close();
      assertEquals("Not enough values retrieved", 2, files.size());
      assertEquals("Main file not run", true, files.contains(main.getAbsolutePath()));
      assertEquals("Second file not run", true, files.contains(include1.getAbsolutePath()));
      stmt.close();
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

}
