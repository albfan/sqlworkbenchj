/*
 * WbCopyTest.java
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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.MetaDataSqlManager;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

import workbench.util.SqlUtil;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCopyTest
  extends WbTestCase
{

  public WbCopyTest()
  {
    super("WbCopyTest");
  }

  @Test
  public void testIsConnectionRequired()
  {
    WbCopy copy = new WbCopy();
    assertFalse(copy.isConnectionRequired());
  }

  @After
  public void tearDown()
    throws Exception
  {
    ConnectionMgr.getInstance().disconnectAll();
  }


  @Test
  public void testImportIntoView()
    throws Exception
  {
    TestUtil util = getTestUtil();

    util.prepareEnvironment();
    WbConnection source = util.getHSQLConnection("viewCopySource");
    WbConnection target = util.getHSQLConnection("viewCopyTarget");

    try
    {
      TestUtil.executeScript(source,
        "create table foo (id integer primary key, data varchar(100));\n" +
        "insert into foo (id, data) values (1, 'foo');\n" +
        "insert into foo (id, data) values (2, 'bar');\n" +
        "commit;");

      TestUtil.executeScript(target,
        "create table foo (id integer primary key, data varchar(100));\n" +
        "create view v_foo as select * from foo;");

      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(source);

      String sql =
        "wbcopy -sourceTable=foo " +
        "       -targetTable=v_foo " +
        "       -sourceProfile='viewCopySource' " +
        "       -targetProfile='viewCopyTarget'";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      int rows = TestUtil.getNumberValue(target, "select count(*) from v_foo");
      assertEquals(2, rows);

      TestUtil.executeScript(source,
        "drop table foo;\n" +
        "create table v_foo (id integer primary key, data varchar(100));\n" +
        "insert into v_foo (id, data) values (1, 'foo');\n" +
        "insert into v_foo (id, data) values (2, 'bar');\n" +
        "commit;");

      TestUtil.executeScript(target,
        "delete from foo;\n" +
        "commit;");

      sql =
        "wbcopy -sourceTable=* " +
        "       -sourceProfile='viewCopySource' " +
        "       -targetProfile='viewCopyTarget'";

      result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      rows = TestUtil.getNumberValue(target, "select count(*) from v_foo");
      assertEquals(2, rows);
    }
    finally
    {
      source.disconnect();
      target.disconnect();
    }
  }

  @Test
  public void testTrimData()
    throws Exception
  {
    TestUtil util = getTestUtil();

    util.prepareEnvironment();
    WbConnection source = util.getHSQLConnection("namedSchemaCopySource");
    WbConnection target = util.getConnection("namedSchemaCopyTarget");
    target.getProfile().setTrimCharData(false);
    source.getProfile().setTrimCharData(false);

    try
    {
      TestUtil.executeScript(source,
        "create table test (some_data char(5), id integer); \n" +
        "insert into test values ('42', 1); \n" +
        "commit;");

      TestUtil.executeScript(target,
        "create table test (some_data varchar(5), id integer); \n" +
        "commit;");

      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(source);

      String sql =
        "wbcopy -sourceTable=test " +
        "       -sourceProfile='namedSchemaCopySource' " +
        "       -targetProfile='namedSchemaCopyTarget'";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());
      String data = (String)TestUtil.getSingleQueryValue(target, "select some_data from test");
      assertEquals("42   ", data);

      TestUtil.executeScript(target,
        "delete from test;\n" +
        "commit;");

      sql =
        "wbcopy -sourceTable=test -trimCharData=true " +
        "       -sourceProfile='namedSchemaCopySource' " +
        "       -targetProfile='namedSchemaCopyTarget'";

       result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());
      data = (String)TestUtil.getSingleQueryValue(target, "select some_data from test");
      assertEquals("42", data);
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

  @Test
  public void testSchemaCopy()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest");
    util.prepareEnvironment();

    WbConnection source = util.getConnection("namedSchemaCopySource");
    WbConnection target = util.getHSQLConnection("namedSchemaCopyTarget");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(source);

      TestUtil.executeScript(source,
        "create schema scott;\n" +
        "create schema tiger;\n" +
        "set schema scott;\n" +
        "create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob);\n" +
        "create table address (person_id integer, address_details varchar(100));\n" +

        "insert into person (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01');\n"+
        "insert into person (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202');\n" +
        "insert into person (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303');\n" +
        "insert into person (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404');\n" +

        "insert into address (person_id, address_details) values (1, 'Arlington');\n" +
        "insert into address (person_id, address_details) values (2, 'Heart of Gold');\n" +
        "insert into address (person_id, address_details) values (3, 'Sleepy by Lane');\n" +
        "insert into address (person_id, address_details) values (4, 'Betelgeuse');\n"+
        "commit;\n" +
        "set schema scott;\n" +
        "create table tiger.person (nr integer not null primary key, lastname varchar(50), firstname varchar(50));\n" +
        "insert into tiger.person (nr, lastname, firstname) values (100,'Dent-x', 'Arthur');\n"+
        "insert into tiger.person (nr, lastname, firstname) values (200,'Beeblebrox-x', 'Zaphod');\n" +
        "insert into tiger.person (nr, lastname, firstname) values (300,'Moviestar-x', 'Mary');\n" +
        "insert into tiger.person (nr, lastname, firstname) values (400,'Perfect-x', 'Ford');\n" +
        "commit;");

      TestUtil.executeScript(target,
        "create schema scott;\n" +
        "create schema tiger;\n" +
        "create table tiger.person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob);\n" +
        "create table tiger.address (person_id integer, address_details varchar(100));\n" +
        "create schema other;\n" +
        "alter user sa set initial schema other;\n" +
        "create table scott.person (nr integer not null primary key, lastname varchar(50), firstname varchar(50));\n" +
        "insert into scott.person (nr, lastname, firstname) values (100,'Dent', 'Arthur');\n"+
        "insert into scott.person (nr, lastname, firstname) values (2,'Beeblebrox', 'Zaphod');\n" +
        "commit;");

      String sql =
        "wbcopy -deleteTarget=true " +
        "       -sourceSchema=scott " +
        "       -targetSchema=tiger " +
        "       -deleteTarget=true " +
        "       -sourceProfile='namedSchemaCopySource' " +
        "       -targetProfile='namedSchemaCopyTarget'";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Statement ttstmt = target.createStatement();
      ResultSet rs = ttstmt.executeQuery("select nr, lastname, firstname from tiger.person");
      int count = 0;
      while (rs.next())
      {
        count ++;
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);
      assertEquals(4, count);
      rs = ttstmt.executeQuery("select count(*) from scott.person");
      if (rs.next())
      {
        count = rs.getInt(1);
      }
      SqlUtil.closeResult(rs);
      assertEquals(2, count);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(source.getProfile());
      ConnectionMgr.getInstance().removeProfile(target.getProfile());
    }
  }

  @Test
  public void testTableWithSchema()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest");
    util.prepareEnvironment();

    WbConnection source = util.getConnection("namedSchemaCopySource");
    WbConnection target = util.getHSQLConnection("namedSchemaCopyTarget");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(source);

      TestUtil.executeScript(source,
        "create schema scott;\n" +
        "set schema scott;\n" +
        "create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob);\n" +
        "create table address (person_id integer, address_details varchar(100));\n" +

        "insert into person (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01');\n"+
        "insert into person (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202');\n" +
        "insert into person (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303');\n" +
        "insert into person (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404');\n" +

        "insert into address (person_id, address_details) values (1, 'Arlington');\n" +
        "insert into address (person_id, address_details) values (2, 'Heart of Gold');\n" +
        "insert into address (person_id, address_details) values (3, 'Sleepy by Lane');\n" +
        "insert into address (person_id, address_details) values (4, 'Betelgeuse');\n"+
        "commit;");

      TestUtil.executeScript(target,
        "create schema scott;\n" +
        "create table scott.person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob);\n" +
        "create table scott.address (person_id integer, address_details varchar(100));\n" +
        "create schema other;\n" +
        "alter user sa set initial schema other;\n" +
        "commit;");

      String sql =
        "wbcopy -deleteTarget=true " +
        "       -sourceTable=scott.person,scott.address " +
        "       -sourceProfile='namedSchemaCopySource' " +
        "       -targetProfile='namedSchemaCopyTarget'";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Statement ttstmt = target.createStatement();
      ResultSet rs = ttstmt.executeQuery("select nr, lastname, firstname from scott.person");
      int count = 0;
      while (rs.next())
      {
        count ++;
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);
      assertEquals(4, count);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(source.getProfile());
      ConnectionMgr.getInstance().removeProfile(target.getProfile());
    }
  }

  @Test
  public void testCopyWithSyncDelete()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testExecute");
    util.prepareEnvironment();

    StatementRunner runner = util.createConnectedStatementRunner();
    WbConnection con = runner.getConnection();

    Statement stmt = con.createStatement();

    stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50))");
    stmt.executeUpdate("create table target_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50))");

    for (int i=0; i < 50; i++)
    {
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (" +  i + ",'Lastname" + i + "', 'Arthur" + i + ",')");
    }

    for (int i=0; i < 37; i++)
    {
      stmt.executeUpdate("insert into target_data (nr, lastname, firstname) values (" +  (i + 1000) + ",'Lastname" + i + "', 'Arthur" + i + ",')");
    }

    con.commit();

    String sql = "wbcopy -sourceTable=source_data " +
                 "       -targettable=target_data " +
                 "       -createTarget=false " +
                 "       -syncDelete=true " +
                 "       -batchSize=10";

    StatementRunnerResult result = runner.runStatement(sql);
    assertEquals(result.getMessages().toString(), true, result.isSuccess());

    ResultSet rs = stmt.executeQuery("select count(*) from target_data");
    int count = -1;
    if (rs.next())
    {
      count = rs.getInt(1);
    }
    SqlUtil.closeResult(rs);
    assertEquals("Wrong rowcount", 50, count);

    rs = stmt.executeQuery("select count(*) from target_data where nr >= 1000");
    count = -1;
    if (rs.next())
    {
      count = rs.getInt(1);
    }
    SqlUtil.closeResult(rs);
    assertEquals("Rows not deleted", 0, count);
    ConnectionMgr.getInstance().removeProfile(con.getProfile());
  }


  @Test
  public void testSyncDeleteWithAlternateKey()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testExecute");
    util.prepareEnvironment();

    StatementRunner runner = util.createConnectedStatementRunner();
    WbConnection con = runner.getConnection();

    Statement stmt = con.createStatement();

    stmt.executeUpdate("create table source_data (nr integer not null, lastname varchar(50), firstname varchar(50))");
    stmt.executeUpdate("create table target_data (nr integer not null, lastname varchar(50), firstname varchar(50))");

    for (int i=0; i < 50; i++)
    {
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (" +  i + ",'Lastname" + i + "', 'Arthur" + i + ",')");
    }

    for (int i=0; i < 37; i++)
    {
      stmt.executeUpdate("insert into target_data (nr, lastname, firstname) values (" +  (i + 1000) + ",'Lastname" + i + "', 'Arthur" + i + ",')");
    }

    con.commit();

    String sql = "wbcopy -sourceTable=source_data " +
                 "       -targettable=target_data " +
                 "       -createTarget=false " +
                 "       -keyColumns=nr " +
                 "       -syncDelete=true " +
                 "       -batchSize=10";

    StatementRunnerResult result = runner.runStatement(sql);
    assertEquals(result.getMessages().toString(), true, result.isSuccess());

    ResultSet rs = stmt.executeQuery("select count(*) from target_data");
    int count = -1;
    if (rs.next())
    {
      count = rs.getInt(1);
    }
    SqlUtil.closeResult(rs);
    assertEquals("Wrong rowcount", 50, count);

    rs = stmt.executeQuery("select count(*) from target_data where nr >= 1000");
    count = -1;
    if (rs.next())
    {
      count = rs.getInt(1);
    }
    SqlUtil.closeResult(rs);
    assertEquals("Rows not deleted", 0, count);
    ConnectionMgr.getInstance().removeProfile(con.getProfile());
  }

  @Test
  public void testCopy() throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testExecute");
    util.prepareEnvironment();

    StatementRunner runner = util.createConnectedStatementRunner();
    WbConnection con = runner.getConnection();

    try
    {
      Statement stmt = con.createStatement();

      stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
      stmt.executeUpdate("create table target_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

      con.commit();

      String sql = "--copy source_data and create target\n" +
        "wbcopy -sourceTable=source_data " +
        "-targettable=target_data -createTarget=false";

      StatementRunnerResult result = runner.runStatement(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      ResultSet rs = stmt.executeQuery("select count(*) from target_data");
      if (rs.next())
      {
        int count = rs.getInt(1);
        assertEquals("Incorrect number of rows copied", 4, count);
      }
      rs.close();
      rs = stmt.executeQuery("select lastname from target_data where nr = 3");
      if (rs.next())
      {
        String name = rs.getString(1);
        assertEquals("Incorrect value copied", "Moviestar", name);
      }
      else
      {
        fail("Record with nr = 3 not copied");
      }
      rs.close();
      rs = stmt.executeQuery("select nr, binary_data from target_data");
      while (rs.next())
      {
        int id = rs.getInt(1);
        Object blob = rs.getObject(2);
        assertNotNull("No blob data imported", blob);
        if (blob instanceof byte[])
        {
          byte[] retrievedData = (byte[])blob;
          assertEquals("Wrong blob size imported", id, retrievedData.length);
          assertEquals("Wrong content of blob data", id, retrievedData[0]);
        }
      }

      stmt.executeUpdate("update source_data set lastname = 'Prefect' where nr = 4");
      con.commit();

      sql = "wbcopy -sourceTable=source_data -targettable=target_data -mode=update";
      result = runner.runStatement(sql);
      assertEquals("Copy not successful", true, result.isSuccess());

      rs = stmt.executeQuery("select lastname from target_data where nr = 4");
      if (rs.next())
      {
        String name = rs.getString(1);
        assertEquals("Incorrect value copied", "Prefect", name);
      }
      else
      {
        fail("Record with nr = 4 not copied");
      }
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testRemoveDefaults() throws Exception
  {
    TestUtil util = new TestUtil("WbCopyRemoveDefault");
    util.prepareEnvironment();

    StatementRunner runner = util.createConnectedStatementRunner();
    WbConnection con = runner.getConnection();

    try
    {
      TestUtil.executeScript(con,
        "create table source_data (nr integer not null primary key, lastname varchar(50) default 'Dent', firstname varchar(50));\n" +
        "insert into source_data (nr, lastname, firstname) values (1,'Dent', 'Arthur');\n" +
        "commit;");

      String sql = "wbcopy -sourceTable=source_data -targettable=target_data -createTarget=true -removeDefaults=true";
      StatementRunnerResult result = runner.runStatement(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Number count = (Number)TestUtil.getSingleQueryValue(con, "select count(*) from target_data");
      assertEquals(1, count.intValue());
      List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(new TableIdentifier("target_data"));
      ColumnIdentifier col = columns.get(1);
      assertEquals("LASTNAME", col.getColumnName());
      assertNull(col.getDefaultValue());
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testCreateWithMap()
    throws Exception
  {
    TestUtil util = new TestUtil("CreateOrderedTest");
    util.prepareEnvironment();

    StatementRunner runner = util.createConnectedStatementRunner();
    WbConnection con = runner.getConnection();

    try
    {
      Statement stmt = con.createStatement();

      stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50))");

      stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (1,'Dent', 'Arthur')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (2,'Beeblebrox', 'Zaphod')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (3,'Moviestar', 'Mary')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (4,'Perfect', 'Ford')");

      con.commit();

      String sql = "wbcopy -sourceTable=source_data " +
                  "-targetTable=target_data " +
                  "-columns=lastname/nachname, firstname/vorname, nr/id "+
                  "-createTarget=true";

      StatementRunnerResult result = runner.runStatement(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      try (ResultSet rs = stmt.executeQuery("select count(*) from target_data"))
      {
        if (rs.next())
        {
          int count = rs.getInt(1);
          assertEquals("Incorrect number of rows copied", 4, count);
        }
      }

      // Make sure the order in the column mapping is preserved when creating the table
      List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(new TableIdentifier("TARGET_DATA"));
      for (ColumnIdentifier col : columns)
      {
        if (col.getColumnName().equalsIgnoreCase("NACHNAME"))
        {
          assertEquals(1, col.getPosition());
        }
        else if (col.getColumnName().equalsIgnoreCase("VORNAME"))
        {
          assertEquals(2, col.getPosition());
        }
        else if (col.getColumnName().equalsIgnoreCase("ID"))
        {
          assertEquals(3, col.getPosition());
        }
        else
        {
          fail("Wrong column " + col.getColumnName() + " created");
        }
      }
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testWithColumnMap()
    throws Exception
  {
    TestUtil util = new TestUtil("CopyWithMapTest");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("mappedCopyTest");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(con);

      Statement stmt = con.createStatement();

      stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

      stmt.executeUpdate("create table target_data (tnr integer not null primary key, tlastname varchar(50), tfirstname varchar(50), tbinary_data blob)");
      stmt.executeUpdate("insert into target_data (tnr, tlastname, tfirstname) values (42,'Gaga', 'Radio')");

      con.commit();

      String sql = "wbcopy -sourceTable=source_data " +
        "-targetTable=target_data " +
        "-deleteTarget=true " +
        "-columns=lastname/tlastname, firstname/tfirstname, nr/tnr";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals("Copy not successful", true, result.isSuccess());

      ResultSet rs = stmt.executeQuery("select count(*) from target_data where tbinary_data is null");
      if (rs.next())
      {
        int count = rs.getInt(1);
        assertEquals("Incorrect number of rows copied", 4, count);
      }
      SqlUtil.closeResult(rs);

      rs = stmt.executeQuery("select tfirstname, tlastname from target_data where tnr = 3");
      if (rs.next())
      {
        String s = rs.getString(1);
        assertEquals("Incorrect firstname", "Mary", s);
        s = rs.getString(2);
        assertEquals("Incorrect firstname", "Moviestar", s);
      }
      else
      {
        fail("Nothing copied");
      }
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testCreateTargetFromQuery1()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testCreateTargetFromQuery");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("queryCopyTest");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(con);

      Statement stmt = con.createStatement();

      stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

      con.commit();

      String sql = "wbcopy -sourceQuery='select firstname, nr, lastname from source_data where nr < 3' " +
        "-targetTable=target_data -createTarget=true";

      StatementRunnerResult result = copyCmd.execute(sql);
      String msg = result.getMessages().toString();
      assertEquals(msg, true, result.isSuccess());

      ResultSet rs = stmt.executeQuery("select count(*) from target_data");
      if (rs.next())
      {
        int count = rs.getInt(1);
        assertEquals("Incorrect number of rows copied", 2, count);
      }
      SqlUtil.closeResult(rs);

      rs = stmt.executeQuery("select nr, firstname, lastname from target_data");
      while (rs.next())
      {
        int id = rs.getInt(1);
        String fname = rs.getString(2);
        String lname = rs.getString(3);
        if (id == 1)
        {
          assertEquals("Wrong firstname for id=1", "Arthur", fname);
          assertEquals("Wrong lastname for id=1", "Dent", lname);
        }
        else if (id == 2)
        {
          assertEquals("Wrong firstname for id=2", "Zaphod", fname);
          assertEquals("Wrong lastname for id=1", "Beeblebrox", lname);
        }
        else
        {
          fail("Wrong ID " + id + " copied");
        }
      }
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testCreateTargetFromQuery2()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testCreateTargetFromQuery");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("queryCopyTest");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(con);

      Statement stmt = con.createStatement();

      stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

      con.commit();

      String sql = "wbcopy -sourceQuery='select firstname, nr, lastname from source_data where nr < 3' " +
        "-targetTable=target_data -createTarget=true -columns=tfirstname, tnr, tlastname";

      StatementRunnerResult result = copyCmd.execute(sql);
      String msg = result.getMessages().toString();
      assertEquals(msg, true, result.isSuccess());

      ResultSet rs = stmt.executeQuery("select count(*) from target_data");
      if (rs.next())
      {
        int count = rs.getInt(1);
        assertEquals("Incorrect number of rows copied", 2, count);
      }
      SqlUtil.closeResult(rs);

      rs = stmt.executeQuery("select tnr, tfirstname, tlastname from target_data");
      while (rs.next())
      {
        int id = rs.getInt(1);
        String fname = rs.getString(2);
        String lname = rs.getString(3);
        if (id == 1)
        {
          assertEquals("Wrong firstname for id=1", "Arthur", fname);
          assertEquals("Wrong lastname for id=1", "Dent", lname);
        }
        else if (id == 2)
        {
          assertEquals("Wrong firstname for id=2", "Zaphod", fname);
          assertEquals("Wrong lastname for id=1", "Beeblebrox", lname);
        }
        else
        {
          fail("Wrong ID " + id + " copied");
        }
      }
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testQueryCopy()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testExecute");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("queryCopyTest");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(con);

      Statement stmt = con.createStatement();

      stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

      stmt.executeUpdate("create table target_data (tnr integer not null primary key, tlastname varchar(50), tfirstname varchar(50), tbinary_data blob)");

      con.commit();

      String sql = "wbcopy -sourceQuery='select firstname, nr, lastname from source_data where nr < 3;' " +
        "-targetTable=target_data " +
        "-columns=tfirstname, tnr, tlastname";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals("Copy not successful", true, result.isSuccess());

      ResultSet rs = stmt.executeQuery("select count(*) from target_data where tbinary_data is null");
      if (rs.next())
      {
        int count = rs.getInt(1);
        assertEquals("Incorrect number of rows copied", 2, count);
      }
      SqlUtil.closeResult(rs);

      rs = stmt.executeQuery("select tfirstname, tlastname from target_data where tnr = 1");
      if (rs.next())
      {
        String s = rs.getString(1);
        assertEquals("Incorrect firstname", "Arthur", s);
        s = rs.getString(2);
        assertEquals("Incorrect firstname", "Dent", s);
      }
      else
      {
        fail("Nothing copied");
      }
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testQueryCopyWithComments()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testExecute");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("queryCopyTest");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(con);

      TestUtil.executeScript(con,
        "create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), some_data varchar(20));\n" +
        "insert into source_data (nr, lastname, firstname, some_data) values (1,'Dent', 'Arthur', '01');\n" +
        "insert into source_data (nr, lastname, firstname, some_data) values (2,'Beeblebrox', 'Zaphod', null);\n" +
        "insert into source_data (nr, lastname, firstname, some_data) values (3,'Moviestar', 'Mary', '03');\n" +
        "insert into source_data (nr, lastname, firstname, some_data) values (4,'Perfect', 'Ford', null);\n" +
        "create table target_data (tnr integer not null primary key, tlastname varchar(50), tfirstname varchar(50), tsome_data varchar(20));\n" +
        "commit;\n");

      String sql =
        "wbcopy -sourceQuery=\"select firstname, nr, lastname, coalesce(some_data, '--- missing! ---') as some_data from source_data\" " +
        "       -targetTable=target_data " +
        "       -columns=tfirstname, tnr, tlastname, tsome_data";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals("Copy not successful", true, result.isSuccess());

      int count = TestUtil.getNumberValue(con, "select count(*) from target_data");
      assertEquals("Incorrect number of rows copied", 4, count);

      String data = (String)TestUtil.getSingleQueryValue(con, "select tsome_data from target_data where tnr = 4");
      assertEquals("--- missing! ---", data);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testQueryCopyNoColumns()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testExecute");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("queryCopyTest");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(con);

      TestUtil.executeScript(con,
      "create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50));\n" +
      "insert into source_data (nr, lastname, firstname) values (1,'Dent', 'Arthur');\n" +
      "insert into source_data (nr, lastname, firstname) values (2,'Beeblebrox', 'Zaphod');\n" +
      "insert into source_data (nr, lastname, firstname) values (3,'Moviestar', 'Mary');\n" +
      "insert into source_data (nr, lastname, firstname) values (4,'Perfect', 'Ford');\n" +
      "create table target_data (tnr integer not null primary key, tlastname varchar(50), tfirstname varchar(50));\n" +
      "commit;\n");


      String sql =
        "wbcopy -sourceQuery='select firstname as tfirstname, nr as tnr, lastname as tlastname from source_data' " +
        "-targetTable=target_data";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals("Copy not successful", true, result.isSuccess());

      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery("select count(*) from target_data");
      if (rs.next())
      {
        int count = rs.getInt(1);
        assertEquals("Incorrect number of rows copied", 4, count);
      }
      SqlUtil.closeResult(rs);

      rs = stmt.executeQuery("select tfirstname, tlastname from target_data where tnr = 1");
      if (rs.next())
      {
        String s = rs.getString(1);
        assertEquals("Incorrect firstname", "Arthur", s);
        s = rs.getString(2);
        assertEquals("Incorrect firstname", "Dent", s);
      }
      else
      {
        fail("Nothing copied");
      }
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testQueryCopyNoPK()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testExecute");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("queryCopyTest");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(con);

      Statement stmt = con.createStatement();

      stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
      stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Prefect', 'Ford', '04040404')");

      stmt.executeUpdate("create table target_data (tnr integer, tlastname varchar(50), tfirstname varchar(50), tbinary_data blob)");
      stmt.executeUpdate("insert into target_data (tnr, tlastname, tfirstname) values (1, 'Dend', 'Artur')");
      stmt.executeUpdate("insert into target_data (tnr, tlastname, tfirstname) values (2, 'Biblebrox', 'Zaphod')");

      con.commit();

      String sql = "wbcopy -sourceQuery='select firstname, nr, lastname from source_data' " +
        "-targetTable=target_data " +
        "-mode=update,insert " +
        "-keyColumns=tnr " +
        "-columns=tfirstname, tnr, tlastname";

      StatementRunnerResult result = copyCmd.execute(sql);
      if (!result.isSuccess())
      {
        String msg = result.getMessages().toString();
        System.out.println("***********");
        System.out.println(msg);
        System.out.println("***********");
      }

      assertEquals("Copy not successful", true, result.isSuccess());

      ResultSet rs = stmt.executeQuery("select tnr, tfirstname, tlastname from target_data");
      int count = 0;
      while (rs.next())
      {
        count ++;
        int id = rs.getInt(1);
        String fname = rs.getString(2);
        String lname = rs.getString(3);

        if (id == 1)
        {
          assertEquals("Incorrect firstname", "Arthur", fname);
          assertEquals("Incorrect firstname", "Dent", lname);
        }
        else if (id == 2)
        {
          assertEquals("Incorrect firstname", "Zaphod", fname);
          assertEquals("Incorrect firstname", "Beeblebrox", lname);
        }
        else if (id == 3)
        {
          assertEquals("Incorrect firstname", "Mary", fname);
          assertEquals("Incorrect firstname", "Moviestar", lname);
        }
        else if (id == 4)
        {
          assertEquals("Incorrect firstname", "Ford", fname);
          assertEquals("Incorrect firstname", "Prefect", lname);
        }
      }
      assertEquals(4, count);
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
    }
  }

  @Test
  public void testCopySchema()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest_testExecute");
    util.prepareEnvironment();

    WbConnection source = util.getConnection("schemaCopySource");
    WbConnection target = util.getConnection("schemaCopyTarget");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(source);

      Statement stmt = source.createStatement();
      stmt.executeUpdate("CREATE SCHEMA copy_src");
      stmt.executeUpdate("SET SCHEMA copy_src");

      stmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
      stmt.executeUpdate("create table address (person_id integer not null primary key, address_details varchar(100))");
      stmt.executeUpdate("create table some_data (id integer, some_details varchar(100))");
      stmt.executeUpdate("alter table address add foreign key (person_id) references person(nr)");

      stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
      stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
      stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
      stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

      stmt.executeUpdate("insert into address (person_id, address_details) values (1, 'Arlington')");
      stmt.executeUpdate("insert into address (person_id, address_details) values (2, 'Heart of Gold')");
      stmt.executeUpdate("insert into address (person_id, address_details) values (3, 'Sleepy by Lane')");
      stmt.executeUpdate("insert into address (person_id, address_details) values (4, 'Betelgeuse')");

      source.commit();

      Statement tstmt = target.createStatement();
      tstmt.executeUpdate("CREATE SCHEMA copy_target");
      tstmt.executeUpdate("SET SCHEMA copy_target");
      tstmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
      tstmt.executeUpdate("create table address (person_id integer not null primary key, address_details varchar(100))");
      tstmt.executeUpdate("alter table address add foreign key (person_id) references person(nr)");

      tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1000, 'Tend', 'Ruhtra', null)");
      tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1001, 'Tcefrep', 'Drof', null)");
      tstmt.executeUpdate("insert into address (person_id, address_details) values (1000, 'Notgnilra')");
      tstmt.executeUpdate("insert into address (person_id, address_details) values (1001, 'Esuegleteb')");
      target.commit();

      String sql = "WbCopy " +
        "-sourceTable=some_data,address,person " +
        "-mode=insert,update " +
        "-checkDependencies=true " +
        "-sourceProfile='schemaCopySource' " +
        "-targetProfile='schemaCopyTarget' " +
        "-syncDelete=true";

      StatementRunnerResult result = copyCmd.execute(sql);
      String msg = result.getMessages().toString();
      assertEquals(msg, true, result.isSuccess());

      ResultSet rs = tstmt.executeQuery("select nr, lastname, firstname from person");
      int personCount = 0;
      while (rs.next())
      {
        personCount ++;
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);
      assertEquals(4, personCount);

      Number addressCount = (Number)TestUtil.getSingleQueryValue(target, "select count(*) from address");
      assertEquals("Wrong number of rows copied to address table", 4, addressCount.intValue());
      SqlUtil.closeResult(rs);


      // Now test deleting the target first
      sql = "WbCopy " +
        "-sourceTable=* " +
        "-mode=insert " +
        "-deleteTarget=true " +
        "-checkDependencies=true " +
        "-sourceProfile='schemaCopySource' " +
        "-targetProfile='schemaCopyTarget' ";

      result = copyCmd.execute(sql);
      msg = result.getMessages().toString();
      assertEquals(msg, true, result.isSuccess());

      rs = tstmt.executeQuery("select nr, lastname, firstname from person");
      while (rs.next())
      {
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(source.getProfile());
      ConnectionMgr.getInstance().removeProfile(target.getProfile());
    }
  }

  @Test
  public void testCreateTarget()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyCreateTest");
    util.prepareEnvironment();

    WbConnection source = util.getConnection("copyCreateTestSource");
    WbConnection target = util.getHSQLConnection("copyCreateTestTarget");

    try
    {
      TestUtil.executeScript(source,
      "create table person (nr integer not null primary key, \"Lastname\" varchar(50), firstname varchar(50));\n" +
      "insert into person (nr, \"Lastname\", firstname) values (1,'Dent', 'Arthur');\n" +
      "insert into person (nr, \"Lastname\", firstname) values (2,'Beeblebrox', 'Zaphod');\n" +
      "insert into person (nr, \"Lastname\", firstname) values (3,'Moviestar', 'Mary');\n" +
      "insert into person (nr, \"Lastname\", firstname) values (4,'Perfect', 'Ford');\n" +
      "commit");

      String sql =
        "WbCopy -createTarget=true " +
        "-sourceTable=person " +
        "-targetTable=participants " +
        "-columns='nr/person_id, firstname/firstname, \"Lastname\"/\"Lastname\"' " +
        "-sourceProfile='copyCreateTestSource' " +
        "-targetProfile='copyCreateTestTarget' ";

      WbCopy copyCmd = new WbCopy();
      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Statement ttstmt = target.createStatement();
      ResultSet rs = ttstmt.executeQuery("select person_id, \"Lastname\", firstname from participants");
      while (rs.next())
      {
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);

      target.commit();

      // Now test the table creation without columns
      sql =
        "WbCopy -createTarget=true " +
        "-dropTarget=true " +
        "-sourceTable=person " +
        "-targetTable=participants " +
        "-sourceProfile='copyCreateTestSource' " +
        "-targetProfile='copyCreateTestTarget' ";

      result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      rs = ttstmt.executeQuery("select nr, \"Lastname\", firstname from participants");
      while (rs.next())
      {
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(source.getProfile());
      ConnectionMgr.getInstance().removeProfile(target.getProfile());
    }
  }

  @Test
  public void testCreateTypedTarget()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyCreateTest");
    util.prepareEnvironment();

    WbConnection source = util.getConnection("copyCreateTestSource"); // H2
    WbConnection target = util.getHSQLConnection("copyCreateTestTarget");

    try
    {
      target.getDbSettings().setCreateTableTemplate("junit_type",
        "CREATE TEMPORARY TABLE " + MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER  +
        " ( " + MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER + " ) ");

      Statement sourceStmt = source.createStatement();

      sourceStmt.executeUpdate("create table person (nr integer not null primary key, \"Lastname\" varchar(50), firstname varchar(50))");
      sourceStmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (1,'Dent', 'Arthur')");
      sourceStmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (2,'Beeblebrox', 'Zaphod')");
      sourceStmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (3,'Moviestar', 'Mary')");
      sourceStmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (4,'Perfect', 'Ford')");
      source.commit();

      // Now test the table creation without columns
      String sql = "wbcopy -createTarget=true " +
        "-sourceTable=person " +
        "-targetTable=participants " +
        "-tableType=junit_type " +
        "-skipTargetCheck=true " +
        "-sourceProfile='copyCreateTestSource' " +
        "-targetProfile='copyCreateTestTarget' ";

      WbCopy copyCmd = new WbCopy();
      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Statement targetStmt = target.createStatement();

      ResultSet rs = targetStmt.executeQuery("select nr, Lastname, firstname from participants");
      while (rs.next())
      {
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(source.getProfile());
      ConnectionMgr.getInstance().removeProfile(target.getProfile());
    }
  }

  @Test
  public void testCreateTypedFromQuery()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyCreateTest");
    util.prepareEnvironment();

    WbConnection source = util.getConnection("copyCreateTestSource"); // H2
    WbConnection target = util.getHSQLConnection("copyCreateTestTarget");

    try
    {
      target.getDbSettings().setCreateTableTemplate("junit_type",
        "CREATE TEMPORARY TABLE " + MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER  +
        " ( " + MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER + " ) ");

      Statement sourceStmt = source.createStatement();

      sourceStmt.executeUpdate("create table person (nr integer not null primary key, \"Lastname\" varchar(50), firstname varchar(50))");
      sourceStmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (1,'Dent', 'Arthur')");
      sourceStmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (2,'Beeblebrox', 'Zaphod')");
      sourceStmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (3,'Moviestar', 'Mary')");
      sourceStmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (4,'Perfect', 'Ford')");
      source.commit();

      String sql = "wbcopy -createTarget=true " +
        "-sourceQuery='select nr as person_id, \"Lastname\" as last_name, firstname as first_name from person' " +
        "-" + CommonArgs.ARG_TRANS_CONTROL + "=false " +
        "-targetTable=participants " +
        "-tableType=junit_type " +
        "-skipTargetCheck=true " +
        "-sourceProfile='copyCreateTestSource' " +
        "-targetProfile='copyCreateTestTarget' ";

      WbCopy copyCmd = new WbCopy();

      // make sure the target connection is not phyiscally closed by WbCopy otherwise
      // the temporary will not return the correct results.
      copyCmd.setConnection(target);

      StatementRunnerResult result = copyCmd.execute(sql);

      assertEquals(result.getMessages().toString(), true, result.isSuccess());
      long participants = copyCmd.getAffectedRows();
      assertEquals(4, participants);

      participants = 0;
      Statement targetStmt = target.createStatement();
      ResultSet rs = targetStmt.executeQuery("select person_id, last_name, first_name from participants");
      while (rs.next())
      {
        participants ++;
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);
      assertEquals(4, participants);
      target.commit();

      sql = "wbcopy -sourceQuery='select nr as person_id, \"Lastname\" as last_name, firstname as first_name from person' " +
        "-targetTable=person_2 " +
        "-skipTargetCheck=true " +
        "-sourceProfile='copyCreateTestSource' " +
        "-targetProfile='copyCreateTestTarget' ";

      TestUtil.executeScript(target,
        "create table person_2 (person_id integer not null primary key, last_name varchar(50), first_name varchar(50));\n" +
        "commit;\n");

      result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Object count = TestUtil.getSingleQueryValue(target, "select count(*) from person_2");
      assertEquals(4, ((Number)count).intValue());

      Object lastName = TestUtil.getSingleQueryValue(target, "select last_name from person_2 where person_id = 3");
      assertNotNull(lastName);
      assertEquals("Moviestar", lastName);

      TestUtil.executeScript(target,
        "delete from person_2;\n" +
        "commit;\n");

      sql = "wbcopy -sourceQuery='select nr, \"Lastname\", firstname from person' " +
        "-targetTable=person_2 " +
        "-skipTargetCheck=true " +
        "-sourceProfile='copyCreateTestSource' " +
        "-targetProfile='copyCreateTestTarget' " +
        "-columns=person_id,last_name,first_name ";

      result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      lastName = TestUtil.getSingleQueryValue(target, "select last_name from person_2 where person_id = 3");
      assertNotNull(lastName);
      assertEquals("Moviestar", lastName);

    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(source.getProfile());
      ConnectionMgr.getInstance().removeProfile(target.getProfile());
    }
  }


  @Test
  public void testCopySchemaCreateTable()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("schemaCopyCreateSource");
    WbConnection target = util.getHSQLConnection("schemaCopyCreateTarget");

    try
    {
      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(con);

      Statement tstmt = con.createStatement();

      tstmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
      tstmt.executeUpdate("create table address (person_id integer, address_details varchar(100))");

      tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
      tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
      tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
      tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

      tstmt.executeUpdate("insert into address (person_id, address_details) values (1, 'Arlington')");
      tstmt.executeUpdate("insert into address (person_id, address_details) values (2, 'Heart of Gold')");
      tstmt.executeUpdate("insert into address (person_id, address_details) values (3, 'Sleepy by Lane')");
      tstmt.executeUpdate("insert into address (person_id, address_details) values (4, 'Betelgeuse')");

      con.commit();

      String sql = "wbcopy -createTarget=true -sourceTable=person,address -sourceProfile='schemaCopyCreateSource' -targetProfile='schemaCopyCreateTarget'";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertEquals(result.getMessages().toString(), true, result.isSuccess());

      Statement ttstmt = target.createStatement();
      ResultSet rs = ttstmt.executeQuery("select nr, lastname, firstname from person");
      int count = 0;
      while (rs.next())
      {
        count ++;
        int nr = rs.getInt(1);
        String ln = rs.getString(2);
        String fn = rs.getString(3);
        if (nr == 1)
        {
          assertEquals("Incorrect data copied", "Dent", ln);
          assertEquals("Incorrect data copied", "Arthur", fn);
        }
        else if (nr == 2)
        {
          assertEquals("Incorrect data copied", "Beeblebrox", ln);
          assertEquals("Incorrect data copied", "Zaphod", fn);
        }
      }
      SqlUtil.closeResult(rs);
      assertEquals(4, count);
    }
    finally
    {
      ConnectionMgr.getInstance().removeProfile(con.getProfile());
      ConnectionMgr.getInstance().removeProfile(target.getProfile());
    }
  }

  @Test
  public void testQuotedColumns()
    throws Exception
  {
    TestUtil util = new TestUtil("WbCopyTest");
    util.prepareEnvironment();

    WbConnection con = util.getConnection("schemaCopyCreateSource");
    TestUtil.executeScript(con,
      "CREATE TABLE source_table (id integer, firstname varchar(20), lastname varchar(20));\n" +
      "insert into source_table values (1, 'Arthur', 'Dent');\n" +
      "commit;\n" +
      "CREATE TABLE target_table (id integer, \"FirstName\" varchar(20), \"LastName\" varchar(20));\n" +
      "commit;\n");

    String sql = "wbcopy -sourceTable=source_table -targetTable=target_table -columns='id/id, firstname/\"FirstName\", lastname/\"LastName\" ";

    WbCopy copyCmd = new WbCopy();
    copyCmd.setConnection(con);

    StatementRunnerResult result = copyCmd.execute(sql);
    assertEquals(result.getMessages().toString(), true, result.isSuccess());
    try (Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT count(*) from target_table"))
    {
      assertTrue(rs.next());
      int count = rs.getInt(1);
      assertEquals(1, count);
    }
  }

  @Test
  public void testContinueOnError()
    throws Exception
  {
    TestUtil util = getTestUtil();

    util.prepareEnvironment();
    WbConnection source = util.getHSQLConnection("namedSchemaCopySource");
    WbConnection target = util.getConnection("namedSchemaCopyTarget");

    try
    {
      TestUtil.executeScript(source,
        "create table test_1 (id integer not null, some_data varchar(10)); \n" +
        "create table test_2 (id integer not null primary key, some_data varchar(10)); \n" +
        "create table test_3 (id integer not null primary key, some_data varchar(10)); \n" +
        "create table test_4 (id integer not null, some_data varchar(10)); \n" +
        "create table test_5 (id integer not null, some_data varchar(10)); \n" +
        "insert into test_1 values (1, 'foo'); \n" +
        "insert into test_1 values (2, 'foo'); \n" +
        "insert into test_2 values (1, 'foo'); \n" +
        "insert into test_2 values (2, 'foo'); \n" +
        "insert into test_3 values (1, 'foo'); \n" +
        "insert into test_3 values (2, 'foo'); \n" +
        "insert into test_4 values (1, 'foo'); \n" +
        "insert into test_4 values (2, 'foo'); \n" +
        "insert into test_5 values (1, 'foo'); \n" +
        "insert into test_5 values (2, 'foo'); \n" +
        "commit;");

      TestUtil.executeScript(target,
        "create table test_1 (id integer not null, some_data varchar(10)); \n" +
        "create table test_2 (id integer not null primary key, some_data varchar(10)); \n" +
        "create table test_3 (id integer not null primary key, some_data varchar(10)); \n" +
        "create table test_4 (id integer not null, some_data varchar(10)); \n" +
        "create table test_5 (id integer not null, some_data varchar(10)); \n" +
        "commit;");

      WbCopy copyCmd = new WbCopy();
      copyCmd.setConnection(source);

      String sql =
        "wbcopy -sourceTable=* " +
        "       -mode=update,insert " +
        "       -continueOnError=true " +
        "       -sourceProfile='namedSchemaCopySource' " +
        "       -targetProfile='namedSchemaCopyTarget'";

      StatementRunnerResult result = copyCmd.execute(sql);
      assertTrue(result.isSuccess());
      assertTrue(result.hasWarning());
//      String msg = result.getMessages().toString();
//      System.out.println(msg);
      Number count = (Number)TestUtil.getSingleQueryValue(target, "select count(*) from test_2");
      assertEquals(2, count.intValue());
      count = (Number)TestUtil.getSingleQueryValue(target, "select count(*) from test_3");
      assertEquals(2, count.intValue());

      count = (Number)TestUtil.getSingleQueryValue(target, "select count(*) from test_1");
      assertEquals(0, count.intValue());
      count = (Number)TestUtil.getSingleQueryValue(target, "select count(*) from test_4");
      assertEquals(0, count.intValue());
      count = (Number)TestUtil.getSingleQueryValue(target, "select count(*) from test_5");
      assertEquals(0, count.intValue());
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

}
