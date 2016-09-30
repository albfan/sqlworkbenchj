/*
 * WbDataDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import java.io.Reader;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.parser.ScriptParser;

import workbench.util.ArgumentParser;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.WbFile;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDataDiffTest
  extends WbTestCase
{
  private WbConnection source;
  private WbConnection target;
  private TestUtil util;

  public WbDataDiffTest()
  {
    super("WbDataDiffTest");
    util = new TestUtil("dataDiffTest");
  }

  @After
  public void tearDown()
    throws Exception
  {
    getTestUtil().emptyBaseDirectory();
  }
  
  @Test
  public void testIsConnectionRequired()
  {
    WbDataDiff diff = new WbDataDiff();
    assertFalse(diff.isConnectionRequired());
  }

  private void setupConnections()
    throws Exception
  {
    this.source = util.getConnection("dataDiffSource");
    this.target = util.getConnection("dataDiffTarget");
  }

  @Test
  public void testExclude()
    throws Exception
  {
    String script = "drop all objects;\n" +
      "create table some_data (id integer primary key, code varchar(10), firstname varchar(100), lastname varchar(100), foo varchar(20), bar varchar(20));\n" +
      "commit;\n";

    setupConnections();

    try
    {
      util.prepareEnvironment();
      TestUtil.executeScript(source, script);
      TestUtil.executeScript(target, script);

      TestUtil.executeScript(source,
        "insert into some_data (id, code, firstname, lastname, foo) \n" +
        "values (1, 'ad', 'Arthur', 'Dent', 'foo');\n" +
        "insert into some_data (id, code, firstname, lastname, foo) \n" +
        "values (2, 'fp', 'Ford', 'Prefect', 'foobar');\n" +
        "insert into some_data (id, code, firstname, lastname, foo) \n" +
        "values (3, 'zb', 'Zaphod', 'Beeblebrox', 'bar');\n " +
        "commit;\n");

      TestUtil.executeScript(target,
        "insert into some_data (id, code, firstname, lastname, foo) \n" +
        "values (100, 'ad', 'Arthur', 'Dent', 'foox');\n" +
        "insert into some_data (id, code, firstname, lastname, foo) \n" +
        "values (200, 'fp', 'Ford', 'Prefect', 'foobar');\n" +
        "insert into some_data (id, code, firstname, lastname, foo) \n" +
        "values (300, 'zb', 'Zaphod2', 'Beeblebrox', 'bar');\n " +
        "commit;\n");

      util.emptyBaseDirectory();

      StatementRunner runner = new StatementRunner();
      runner.setBaseDir(util.getBaseDir());

      String sql = "WbDataDiff " +
        " -referenceProfile=dataDiffSource " +
        " -targetProfile=dataDiffTarget " +
        " -includeDelete=false -excludeRealPK=true " +
        " -alternateKey='some_data=code' -ignoreColumns='foo,bar' "  +
        " -singleFile=true " +
        " -file=sync_ex.sql -encoding=UTF8";
      runner.runStatement(sql);

      StatementRunnerResult result = runner.getResult();
      assertTrue(result.getMessages().toString(), result.isSuccess());

      WbFile main = new WbFile(util.getBaseDir(), "sync_ex.sql");
      assertTrue(main.exists());
      String diffScript = FileUtil.readFile(main, "UTF-8");
      ScriptParser parser = new ScriptParser(diffScript);
      assertEquals(2, parser.getSize()); // 1 update plus a commit

      String upd = TestUtil.cleanupSql(parser.getCommand(0));
      assertEquals("UPDATE SOME_DATA SET FIRSTNAME = 'Zaphod' WHERE CODE = 'zb'", upd);
      assertTrue(main.delete());

      TestUtil.executeScript(source,
        "insert into some_data (id, code, firstname, lastname, foo, bar) \n" +
        "values (4, 'tm', 'Tricia', 'McMillan', 'foo', 'bar');\n " +
        "commit;\n");

      sql = "WbDataDiff " +
        " -referenceProfile=dataDiffSource " +
        " -targetProfile=dataDiffTarget " +
        " -includeDelete=false -excludeRealPK=true " +
        " -alternateKey='some_data=code' -ignoreColumns='foo,bar' "  +
        " -singleFile=true " +
        " -excludeIgnored=true " +
        " -file=sync_ex.sql -encoding=UTF8";
      runner.runStatement(sql);

      result = runner.getResult();
      assertTrue(result.getMessages().toString(), result.isSuccess());
      diffScript = FileUtil.readFile(main, "UTF-8");
      parser = new ScriptParser(diffScript);
      assertEquals(3, parser.getSize()); // 1 update, 1 insert plus a commit
      String ins = TestUtil.cleanupSql(parser.getCommand(0));
      assertEquals("INSERT INTO SOME_DATA ( CODE, FIRSTNAME, LASTNAME ) VALUES ( 'tm', 'Tricia', 'McMillan' )", ins);
    }
    finally
    {
      source.disconnect();
      target.disconnect();
    }
  }

  @Test
  public void testNoPkTables()
    throws Exception
  {
    String script = "drop all objects;\n" +
      "create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" +
      "create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
      "create table person_address (person_id integer, address_id integer, primary key (person_id, address_id)); \n" +
      "create table foobar (the_answer integer); \n" +
      "alter table person_address add constraint fk_adr foreign key (address_id) references address (address_id);\n" +
      "alter table person_address add constraint fk_per foreign key (person_id) references person (person_id);\n";

    setupConnections();

    Statement srcStmt;
    Statement targetStmt;

    try
    {
      util.prepareEnvironment();
      TestUtil.executeScript(source, script);
      TestUtil.executeScript(target, script);

      srcStmt = source.createStatement();
      insertData(srcStmt);
      source.commit();

      targetStmt = target.createStatement();
      insertData(targetStmt);

      targetStmt.executeUpdate("DELETE FROM person_address WHERE person_id in (10,14)");
      targetStmt.executeUpdate("DELETE FROM address WHERE address_id in (10, 14)");
      targetStmt.executeUpdate("DELETE FROM person WHERE person_id in (10, 14)");
      target.commit();

      TestUtil.executeScript(source, "insert into foobar values (42);");

      util.emptyBaseDirectory();

      StatementRunner runner = new StatementRunner();
      runner.setBaseDir(util.getBaseDir());
      String sql = "WbDataDiff -referenceProfile=dataDiffSource -targetProfile=dataDiffTarget -includeDelete=true -checkDependencies=true -file=sync.sql -encoding=UTF8";
      runner.runStatement(sql);

//      StatementRunnerResult result = runner.getResult();
//      System.out.println(result.getMessages());
      WbFile main = new WbFile(util.getBaseDir(), "sync.sql");
      assertTrue(main.exists());

      Reader r = EncodingUtil.createReader(main, "UTF-8");
//      String sync = FileUtil.readCharacters(r);
      //System.out.println(sync);
    }
    finally
    {
      source.disconnect();
      target.disconnect();
    }
  }

  @Test
  public void testExecute()
    throws Exception
  {
    String script = "drop all objects;\n" +
      "create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" +
      "create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
      "create table person_address (person_id integer, address_id integer, primary key (person_id, address_id)); \n" +
      "alter table person_address add constraint fk_adr foreign key (address_id) references address (address_id);\n" +
      "alter table person_address add constraint fk_per foreign key (person_id) references person (person_id);\n";

    setupConnections();

    Statement srcStmt;
    Statement targetStmt;

    try
    {
      util.prepareEnvironment();
      TestUtil.executeScript(source, script);
      TestUtil.executeScript(target, script);

      srcStmt = source.createStatement();
      insertData(srcStmt);
      source.commit();

      targetStmt = target.createStatement();
      insertData(targetStmt);

      // Delete rows so that the diff needs to create INSERT statements

      // as person_id and address are always equal in the test data I don't need to specify both
      targetStmt.executeUpdate("DELETE FROM person_address WHERE person_id in (10,14)");
      targetStmt.executeUpdate("DELETE FROM address WHERE address_id in (10, 14)");
      targetStmt.executeUpdate("DELETE FROM person WHERE person_id in (10, 14)");

      // Change some rows so that the diff needs to create UPDATE statements
      targetStmt.executeUpdate("UPDATE person SET firstname = 'Wrong' WHERE person_id in (17,2)");
      targetStmt.executeUpdate("UPDATE address SET city = 'Wrong' WHERE address_id in (17,2)");


      // Insert some rows so that the diff needs to create DELETE statements
      targetStmt.executeUpdate("insert into person (person_id, firstname, lastname) values " +
        "(300, 'doomed', 'doomed')");
      targetStmt.executeUpdate("insert into person (person_id, firstname, lastname) values " +
        "(301, 'doomed', 'doomed')");

      targetStmt.executeUpdate("insert into address (address_id, street, city, phone, email) values " +
        " (300, 'tobedelete', 'none', 'none', 'none')");

      targetStmt.executeUpdate("insert into address (address_id, street, city, phone, email) values " +
        " (301, 'tobedelete', 'none', 'none', 'none')");

      targetStmt.executeUpdate("insert into person_address (address_id, person_id) values (300,300)");
      targetStmt.executeUpdate("insert into person_address (address_id, person_id) values (301,301)");
      target.commit();

      util.emptyBaseDirectory();

      StatementRunner runner = new StatementRunner();
      runner.setBaseDir(util.getBaseDir());
      String sql = "WbDataDiff -referenceProfile=dataDiffSource -targetProfile=dataDiffTarget -includeDelete=true -checkDependencies=true -file=sync.sql -encoding=UTF8";
      runner.runStatement(sql);

      WbFile main = new WbFile(util.getBaseDir(), "sync.sql");
      assertTrue(main.exists());

      Reader r = EncodingUtil.createReader(main, "UTF-8");
      String sync = FileUtil.readCharacters(r);
      ScriptParser parser = new ScriptParser();
      parser.setScript(sync);
      assertEquals(10, parser.getSize());

      String[] expectedFiles = new String[]
      {
        "address_$delete.sql",
        "address_$insert.sql",
        "address_$update.sql",
        "person_$delete.sql",
        "person_$insert.sql",
        "person_$update.sql",
        "person_address_$delete.sql",
        "person_address_$insert.sql"
      };

      for (String fname : expectedFiles)
      {
        WbFile f = new WbFile(util.getBaseDir(), fname);
        assertTrue(f.exists());
      }

      TestUtil.executeScript(source, "update person set lastname = '<name>' where person_id = 10;commit;");

      sql = "WbDataDiff -type=xml -referenceProfile=dataDiffSource -targetProfile=dataDiffTarget -includeDelete=true -checkDependencies=true -file=sync.xml -encoding=UTF8";
      runner.runStatement(sql);

      main = new WbFile(util.getBaseDir(), "sync.xml");
      assertTrue(main.exists());

      expectedFiles = new String[]
      {
        "address_$delete.xml",
        "address_$insert.xml",
        "address_$update.xml",
        "person_$delete.xml",
        "person_$insert.xml",
        "person_$update.xml",
        "person_address_$delete.xml",
        "person_address_$insert.xml"
      };

      String xml = FileUtil.readCharacters(EncodingUtil.createReader(main, "UTF-8"));
      String result = TestUtil.getXPathValue(xml, "count(/data-diff/summary/mapping)");
      assertEquals("3", result);

      xml = FileUtil.readCharacters(EncodingUtil.createReader(new WbFile(util.getBaseDir(), "person_$update.xml"), "UTF-8"));
      result = TestUtil.getXPathValue(xml, "count(/table-data-diff/update)");
      assertEquals("2", result);

      result = TestUtil.getXPathValue(xml, "/table-data-diff[@name='PERSON']/update[1]/col[2]/text()");
      assertEquals("first2", result);

      result = TestUtil.getXPathValue(xml, "/table-data-diff[@name='PERSON']/update[2]/col[2]/text()");
      assertEquals("first17", result);

      xml = FileUtil.readCharacters(EncodingUtil.createReader(new WbFile(util.getBaseDir(), "person_$insert.xml"), "UTF-8"));
      result = TestUtil.getXPathValue(xml, "count(/table-data-diff/insert)");
      assertEquals("2", result);

      result = TestUtil.getXPathValue(xml, "/table-data-diff[@name='PERSON']/insert[1]/col[@name='LASTNAME']/text()");
      assertEquals("<name>", result);

      for (String fname : expectedFiles)
      {
        WbFile f = new WbFile(util.getBaseDir(), fname);
        if (!f.delete())
        {
          fail("Could not delete " + f.getFullPath());
        }
      }

      if (!main.delete())
      {
        fail("Could not delete " + main.getFullPath());
      }
    }
    finally
    {
      source.disconnect();
      target.disconnect();
    }
  }

  @Test
  public void testXml()
    throws Exception
  {
    String sql =
      "create table foo1 (id integer primary key, some_data varchar(100)); \n" +
      "create table foo2 (id integer primary key, some_data varchar(100)); \n" +
      " \n" +
      "insert into foo1 values (1, 'one'); \n" +
      "insert into foo1 values (2, 'two'); \n" +
      "insert into foo1 values (3, 'three'); \n" +
      " \n" +
      " \n" +
      "insert into foo2 values (1, 'one-'); \n" +
      "insert into foo2 values (2, 'two-');";

    WbConnection conn = util.getConnection();
    TestUtil.executeScript(conn, sql);
    WbDataDiff diff = new WbDataDiff();
    diff.setConnection(conn);
    File xml = new File(util.getBaseDir(), "diff.xml");
    StatementRunnerResult result = diff.execute("WbDataDiff -file='" + xml.getAbsolutePath() + "'  -type=xml -referenceTables=foo1 -targetTables=foo2");

    assertTrue(result.isSuccess());
    String[] expectedFiles = new String[]
      {
        "diff.xml",
        "foo2_$insert.xml",
        "foo2_$update.xml",
      };

    for (String fname : expectedFiles)
    {
      File f = new File(util.getBaseDir(), fname);
      assertTrue(f.exists());
    }
    String content = FileUtil.readCharacters(EncodingUtil.createReader(xml, "UTF-8"));
    String value = TestUtil.getXPathValue(content, "count(/data-diff/summary/mapping)");
    assertEquals("1", value);

    content= FileUtil.readCharacters(EncodingUtil.createReader(new WbFile(util.getBaseDir(), "foo2_$update.xml"), "UTF-8"));
    value = TestUtil.getXPathValue(content, "count(/table-data-diff/update)");
    assertEquals("2", value);

    value = TestUtil.getXPathValue(content, "/table-data-diff[@name='FOO2']/update[1]/col[@name='SOME_DATA']/text()");
    assertEquals("one", value);

    value = TestUtil.getXPathValue(content, "/table-data-diff[@name='FOO2']/update[2]/col[@name='SOME_DATA']/text()");
    assertEquals("two", value);
  }

  private void insertData(Statement stmt)
    throws SQLException
  {
    int rowCount = 20;
    for (int i=0; i < rowCount; i++)
    {
      stmt.executeUpdate("insert into person (person_id, firstname, lastname) values (" + i + ", 'first" + i + "', 'last" + i + "')");
    }
    for (int i=0; i < rowCount; i++)
    {
      stmt.executeUpdate("insert into address (address_id, street, city, phone, email) values (" + i + ", 'street" + i + "', 'city" + i + "', 'phone" + i + "', 'email"+i + "')");
    }
    for (int i=0; i < rowCount; i++)
    {
      stmt.executeUpdate("insert into person_address (address_id, person_id) values (" +i + ", " + i + ")");
    }
  }

  @Test
  public void testMissingColumns()
    throws Exception
  {
    setupConnections();

    try
    {
      TestUtil.executeScript(source, "DROP ALL OBJECTS;\n"
        + "create table person (id integer primary key, name varchar(50), nickname varchar(50));\n"
        + "insert into person values (1, 'Arthur Dent', 'Earthling');\n"
        + "insert into person values (2, 'Zaphod Beeblebrox', 'President');\n"
        + "commit;\n");

      TestUtil.executeScript(target, "DROP ALL OBJECTS;\n"
        + "create table person (id integer primary key, name varchar(50));\n"
        + "insert into person values (1, 'Arthur');\n"
        + "insert into person values (2, 'Zaphod Beeblebrox');\n"
        + "commit;\n");

      StatementRunner runner = new StatementRunner();
      runner.setBaseDir(util.getBaseDir());
      util.emptyBaseDirectory();

      String sql = "WbDataDiff -referenceProfile=dataDiffSource -targetProfile=dataDiffTarget -file=sync.sql -encoding=UTF8";
      runner.runStatement(sql);
      StatementRunnerResult result = runner.getResult();
      assertTrue(result.isSuccess());
      assertTrue(result.hasWarning());
      CharSequence msg = result.getMessages();
      assertNotNull(msg);
      assertTrue(msg.toString().indexOf("The columns from the table PERSON do not match the columns of the target table PERSON") > -1);

      util.emptyBaseDirectory();
      sql = "WbDataDiff -referenceProfile=dataDiffTarget -targetProfile=dataDiffSource -file=sync.sql -encoding=UTF8";
      runner.runStatement(sql);
      result = runner.getResult();
      assertTrue(result.isSuccess());
      assertFalse(result.hasWarning());
      msg = result.getMessages();
      assertNotNull(msg);
      assertTrue(msg.toString().indexOf("The columns from the table PERSON do not match the columns of the target table PERSON") == -1);

    }
    finally
    {
      source.disconnect();
      target.disconnect();
    }
  }

  @Test
  public void testQuoteIdentifier()
    throws Exception
  {
    setupConnections();

    try
    {
      TestUtil.executeScript(source, "DROP ALL OBJECTS;\n" +
        "create table person (id integer primary key, \"p_Name\" varchar(50), \"NickName\" varchar(50));\n" +
        "insert into person values (1, 'Arthur Dent', 'Earthling');\n" +
        "insert into person values (2, 'Zaphod Beeblebrox', 'President');\n" +
        "commit;\n");

      TestUtil.executeScript(target, "DROP ALL OBJECTS;\n"
        + "create table person (id integer primary key, \"p_Name\" varchar(50), \"NickName\" varchar(50));\n"
        + "insert into person values (1, 'Arthur Dent2', 'Earthling');\n"
        + "insert into person values (2, 'Zaphod Beeblebrox2', 'President');\n"
        + "insert into person values (3, 'Ford Prefect', 'Hitchhiker');\n"
        + "commit;\n");

      StatementRunner runner = new StatementRunner();
      runner.setBaseDir(util.getBaseDir());
      util.emptyBaseDirectory();

      String sql = "WbDataDiff -alternateKey='person=\"NickName\"' -referenceProfile=dataDiffSource -targetProfile=dataDiffTarget -file=sync.sql -encoding=UTF8";
      runner.runStatement(sql);
      StatementRunnerResult result = runner.getResult();
      String msg = result.getMessages().toString();
      assertTrue(msg, result.isSuccess());

      String[] expectedFiles = new String[]
      {
        "sync.sql",
        "person_$update.sql",
        "person_$delete.sql",
      };

      for (String fname : expectedFiles)
      {
        File f = new File(util.getBaseDir(), fname);
        assertTrue(f.exists());
      }

      File update = new File(util.getBaseDir(), "person_$update.sql");
      ScriptParser p = new ScriptParser();
      p.setFile(update);
      assertEquals(2, p.getSize());
      assertTrue(SqlUtil.getSqlVerb(p.getCommand(0)).equals("UPDATE"));
      assertTrue(SqlUtil.getSqlVerb(p.getCommand(1)).equals("UPDATE"));
    }
    finally
    {
      source.disconnect();
      target.disconnect();
    }
  }


  @Test
  public void testSingleTable()
    throws Exception
  {
    String script = "drop all objects;\n" +
      "create schema difftest;\n" +
      "set schema difftest;\n" +
      "create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" +
      "create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
      "create table person_address (person_id integer, address_id integer, primary key (person_id, address_id)); \n" +
      "create table dummy (some_id integer); \n" +
      "alter table person_address add constraint fk_adr foreign key (address_id) references address (address_id);\n" +
      "alter table person_address add constraint fk_per foreign key (person_id) references person (person_id);\n" +
      "commit;\n";

    setupConnections();

    Statement srcStmt;
    Statement targetStmt;

    try
    {
      util.prepareEnvironment();
      TestUtil.executeScript(source, script);
      TestUtil.executeScript(target, script);

      srcStmt = source.createStatement();
      insertData(srcStmt);
      source.commit();

      targetStmt = target.createStatement();
      insertData(targetStmt);

      // Delete rows so that the diff needs to create INSERT statements

      // as person_id and address are always equal in the test data I don't need to specify both
      targetStmt.executeUpdate("DELETE FROM person_address WHERE person_id in (10,14)");
      targetStmt.executeUpdate("DELETE FROM address WHERE address_id in (10, 14)");
      targetStmt.executeUpdate("DELETE FROM person WHERE person_id in (10, 14)");

      // Change some rows so that the diff needs to create UPDATE statements
      targetStmt.executeUpdate("UPDATE person SET firstname = 'Wrong' WHERE person_id in (17,2)");
      targetStmt.executeUpdate("UPDATE address SET city = 'Wrong' WHERE address_id in (17,2)");


      // Insert some rows so that the diff needs to create DELETE statements
      targetStmt.executeUpdate("insert into person (person_id, firstname, lastname) values " +
        "(300, 'doomed', 'doomed')");
      targetStmt.executeUpdate("insert into person (person_id, firstname, lastname) values " +
        "(301, 'doomed', 'doomed')");

      targetStmt.executeUpdate("insert into address (address_id, street, city, phone, email) values " +
        " (300, 'tobedelete', 'none', 'none', 'none')");

      targetStmt.executeUpdate("insert into address (address_id, street, city, phone, email) values " +
        " (301, 'tobedelete', 'none', 'none', 'none')");

      targetStmt.executeUpdate("insert into person_address (address_id, person_id) values (300,300)");
      targetStmt.executeUpdate("insert into person_address (address_id, person_id) values (301,301)");
      target.commit();

      util.emptyBaseDirectory();

      StatementRunner runner = new StatementRunner();
      runner.setBaseDir(util.getBaseDir());
      String sql = "WbDataDiff -includeDelete=true -referenceProfile=dataDiffSource -targetProfile=dataDiffTarget -referenceSchema=difftest -targetSchema=difftest -referenceTables=person -file=sync.sql -encoding=UTF8";
      runner.runStatement(sql);
      StatementRunnerResult result = runner.getResult();
      assertTrue(result.getMessages().toString(), result.isSuccess());

      String[] expectedFiles = new String[]
      {
        "person_$delete.sql",
        "person_$insert.sql",
        "person_$update.sql",
      };

      for (String fname : expectedFiles)
      {
        WbFile f = new WbFile(util.getBaseDir(), fname);
        assertTrue("File " + f.getFileName() + " does not exist", f.exists());
      }

      for (String fname : expectedFiles)
      {
        WbFile f = new WbFile(util.getBaseDir(), fname);
        assertTrue(f.delete());
      }
    }
    finally
    {
      source.disconnect();
      target.disconnect();
    }
  }

  @Test
  public void testAlternateKeyParameter()
  {
    WbDataDiff diff = new WbDataDiff();
    ArgumentParser cmdLine = diff.getArgumentParser();
    String arguments = " -alternateKey='person=id2,id3' -alternateKey='foobar=col1,col2,col3'";
    cmdLine.parse(arguments);
    StatementRunnerResult result = new StatementRunnerResult();
    Map<String, Set<String>> keys = diff.getAlternateKeys(cmdLine, result);
    assertNotNull(keys);
    assertEquals(2, keys.size());
    Set<String> cols = keys.get("person");
    assertNotNull(cols);
    assertEquals(2, cols.size());
    assertTrue(cols.contains("id2"));
    assertTrue(cols.contains("id3"));

    Set<String> foocols = keys.get("foobar");
    assertNotNull(foocols);
    assertEquals(3, foocols.size());
    assertTrue(foocols.contains("col1"));
    assertTrue(foocols.contains("col2"));
    assertTrue(foocols.contains("col3"));

    arguments = " -alternateKey='person=\"id2\",\"id3\"'";
    cmdLine.parse(arguments);
    keys = diff.getAlternateKeys(cmdLine, result);
    assertEquals(1, keys.size());
    assertEquals(2, keys.get("person").size());
  }
}
