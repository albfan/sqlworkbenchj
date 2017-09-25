/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.sql.wbcommands;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.MetaDataSqlManager;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.StatementRunnerResult;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class WbRowCountTest
  extends WbTestCase
{

  public WbRowCountTest()
  {
    super("RowCountTest");
  }

  @Before
  public void setUp()
  {
  }

  @AfterClass
  public static void tearDown()
  {
    ConnectionMgr.getInstance().disconnectAll();
  }


  private void initTables(WbConnection conn)
    throws Exception
  {
    TestUtil.executeScript(conn,
      "create schema orders;\n" +
      "create schema data; \n" +
      "create table orders.t1 (id integer);\n" +
      "create table orders.a1 (id integer);\n" +
      "create table orders.a2 (id integer);\n" +
      "create table data.foo (id integer);\n" +
      "create table data.t1 (id integer);\n" +
      "create table foobar (id integer);\n" +
      "insert into orders.t1 values (1),(2),(3);\n" +
      "insert into orders.a1 values (1);\n" +
      "insert into orders.a2 values (1),(2);\n" +
      "insert into data.t1 values (1),(1),(1);\n" +
      "insert into data.foo values (1),(1),(1),(1);\n" +
      "insert into foobar values (1),(1),(1),(1),(1);\n" +
      "commit;");
  }

  @Test
  public void testExcludeColumns()
    throws Exception
  {
    WbConnection conn = getTestUtil().getConnection();
    initTables(conn);
    WbRowCount instance = new WbRowCount();
    instance.setConnection(conn);

    StatementRunnerResult result = instance.execute("WbRowCount -schema=orders -excludeColumns=type,catalog");
    assertFalse(result.getDataStores().isEmpty());
    DataStore counts = result.getDataStores().get(0);
//    DataStorePrinter printer = new DataStorePrinter(counts);
//    printer.printTo(System.out);
    assertEquals(3, counts.getRowCount());
    assertEquals(3, counts.getColumnCount());
    assertTrue("ROWCOUNT".equalsIgnoreCase(counts.getColumnName(0)));
    assertTrue("NAME".equalsIgnoreCase(counts.getColumnName(1)));
    assertTrue("SCHEMA".equalsIgnoreCase(counts.getColumnName(2)));

    result = instance.execute("WbRowCount -schema=orders -excludeColumns=type,catalog,schema");
    assertFalse(result.getDataStores().isEmpty());
    counts = result.getDataStores().get(0);
    assertEquals(3, counts.getRowCount());
    assertEquals(2, counts.getColumnCount());
    assertTrue("ROWCOUNT".equalsIgnoreCase(counts.getColumnName(0)));
    assertTrue("NAME".equalsIgnoreCase(counts.getColumnName(1)));

    result = instance.execute("WbRowCount -schema=orders -excludeColumns=type,schema");
    assertFalse(result.getDataStores().isEmpty());
    counts = result.getDataStores().get(0);
    assertEquals(3, counts.getRowCount());
    assertEquals(3, counts.getColumnCount());
    assertTrue("ROWCOUNT".equalsIgnoreCase(counts.getColumnName(0)));
    assertTrue("NAME".equalsIgnoreCase(counts.getColumnName(1)));
    assertTrue("CATALOG".equalsIgnoreCase(counts.getColumnName(2)));

    result = instance.execute("WbRowCount -schema=orders -excludeColumns=type");
    assertFalse(result.getDataStores().isEmpty());
    counts = result.getDataStores().get(0);
    assertEquals(3, counts.getRowCount());
    assertEquals(4, counts.getColumnCount());
    assertTrue("ROWCOUNT".equalsIgnoreCase(counts.getColumnName(0)));
    assertTrue("NAME".equalsIgnoreCase(counts.getColumnName(1)));
    assertTrue("CATALOG".equalsIgnoreCase(counts.getColumnName(2)));
    assertTrue("SCHEMA".equalsIgnoreCase(counts.getColumnName(3)));
  }

  @Test
  public void testExecute()
    throws Exception
  {
    WbConnection conn = getTestUtil().getConnection();
    initTables(conn);
    WbRowCount instance = new WbRowCount();
    instance.setConnection(conn);

    StatementRunnerResult result = instance.execute("WbRowCount -schema=orders");
    assertFalse(result.getDataStores().isEmpty());
    DataStore counts = result.getDataStores().get(0);
    assertEquals(3, counts.getRowCount());

    String name = counts.getValueAsString(0, 1);
    assertEquals("A1", name);
    int rows = counts.getValueAsInt(0, 0, -1);
    assertEquals(1, rows);

    name = counts.getValueAsString(1, 1);
    assertEquals("A2", name);
    rows = counts.getValueAsInt(1, 0, -1);
    assertEquals(2, rows);

    name = counts.getValueAsString(2, 1);
    assertEquals("T1", name);
    rows = counts.getValueAsInt(2, 0, -1);
    assertEquals(3, rows);

    result = instance.execute("WbRowCount -schema=orders -orderBy='rowcount;desc'");
    assertFalse(result.getDataStores().isEmpty());
    counts = result.getDataStores().get(0);
    assertEquals(3, counts.getRowCount());

//    DataStorePrinter printer = new DataStorePrinter(counts);
//    printer.printTo(System.out);

    name = counts.getValueAsString(0, 1);
    assertEquals("T1", name);
    rows = counts.getValueAsInt(0, 0, -1);
    assertEquals(3, rows);

    name = counts.getValueAsString(1, 1);
    assertEquals("A2", name);
    rows = counts.getValueAsInt(1, 0, -1);
    assertEquals(2, rows);

    name = counts.getValueAsString(2, 1);
    assertEquals("A1", name);
    rows = counts.getValueAsInt(2, 0, -1);
    assertEquals(1, rows);

    result = instance.execute("WbRowCount");
    assertFalse(result.getDataStores().isEmpty());
    counts = result.getDataStores().get(0);
    assertEquals(1, counts.getRowCount());

    result = instance.execute("WbRowCount -objects=foobar,data.foo -orderBy='rowcounts;desc'");
    assertFalse(result.getDataStores().isEmpty());
    counts = result.getDataStores().get(0);

//    DataStorePrinter printer = new DataStorePrinter(counts);
//    printer.printTo(System.out);
    assertEquals(2, counts.getRowCount());

    result = instance.execute("WbRowCount foobar,data.foo,orders.t1");
    assertFalse(result.getDataStores().isEmpty());
    counts = result.getDataStores().get(0);

//    DataStorePrinter printer = new DataStorePrinter(counts);
//    printer.printTo(System.out);
    assertEquals(3, counts.getRowCount());

  }

  @Test
  public void testRowCountTemplate()
    throws Exception
  {
    WbConnection conn = getTestUtil().getConnection();
    String dbId = conn.getDbId();

    try
    {
      System.setProperty("workbench.db." + dbId + ".tablerowcount.select", "select 42 from " + MetaDataSqlManager.TABLE_NAME_PLACEHOLDER);

      TestUtil.executeScript(conn,
        "create table foobar (id integer);\n" +
        "insert into foobar values (1),(1),(1),(1),(1);\n" +
        "commit;");
      WbRowCount counter = new WbRowCount();
      counter.setConnection(conn);
      StatementRunnerResult result = counter.execute("WbRowCount");
      assertFalse(result.getDataStores().isEmpty());
      DataStore counts = result.getDataStores().get(0);
      assertEquals(1, counts.getRowCount());
      assertEquals(42, counts.getValueAsInt(0, 0, -1));
    }
    finally
    {
      System.clearProperty("workbench.db." + dbId + ".tablerowcount.select");
    }
  }

}
