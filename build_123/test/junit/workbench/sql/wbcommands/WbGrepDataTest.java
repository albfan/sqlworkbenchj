/*
 * WbGrepDataTest.java
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

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.StatementRunnerResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbGrepDataTest
  extends WbTestCase
{
  private WbConnection con;

  public WbGrepDataTest()
  {
    super("WbGrepDataTest");
  }

  @Before
  public void setUp()
    throws Exception
  {
    TestUtil util = getTestUtil();
    con = util.getConnection();
    TestUtil.executeScript(con, "create table person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
      "insert into person values (1, 'Arthur', 'Dent');\n" +
      "insert into person values (2, 'Ford', 'Prefect');\n" +
      "commit;" +
      "create table address (nr integer, person_id integer, address_info varchar(100));" +
      "insert into address values (1, 1, 'Arthur''s Address');\n" +
      "insert into address values (2, 1, 'His old address');\n" +
      "insert into address values (3, 2, 'Ford''s Address');\n" +
      "insert into address values (4, 2, null);\n" +
      "commit;\n" +
      "create view v_person as select nr * 10, firstname, lastname from person;" +
      "commit;");
  }

  @After
  public void tearDown()
    throws Exception
  {
    ConnectionMgr.getInstance().disconnectAll();
  }

  @Test
  public void testExecute()
    throws Exception
  {
    String sql = "WbGrepData -tables=person -searchValue=arthur";
    WbGrepData instance = new WbGrepData();
    instance.setConnection(con);
    StatementRunnerResult result = instance.execute(sql);
    assertTrue(result.isSuccess());
    List<DataStore> data = result.getDataStores();
    assertNotNull(data);
    assertEquals(1, data.size());
    assertEquals(1, data.get(0).getRowCount());
    assertEquals(1, data.get(0).getValueAsInt(0, 0, -1));

    sql = "WbGrepData -tables=person, address -searchValue=arthur";
    result = instance.execute(sql);
    assertTrue(result.isSuccess());
    data = result.getDataStores();
    assertNotNull(data);
    assertEquals(2, data.size());
    assertEquals(1, data.get(0).getRowCount());
    assertEquals(1, data.get(1).getRowCount());

    sql = "WbGrepData -tables=person,address -compareType=isnull";
    result = instance.execute(sql);
    assertTrue(result.isSuccess());
    data = result.getDataStores();
    assertNotNull(data);
    assertEquals(1, data.size());
    assertEquals(1, data.get(0).getRowCount());
    DataStore ds = data.get(0);
    assertEquals(4, ds.getValueAsInt(0, 0, -1));

    sql = "WbGrepData -tables=%person% -searchValue=arthur -types=table,view";
    result = instance.execute(sql);
    assertTrue(result.isSuccess());
    data = result.getDataStores();
    assertNotNull(data);
    assertEquals(2, data.size());
    assertEquals(1, data.get(0).getRowCount());
    assertEquals(1, data.get(1).getRowCount());

    sql = "WbGrepData -tables=%person% -searchValue=arthur -types=view";
    result = instance.execute(sql);
    assertTrue(result.isSuccess());
    data = result.getDataStores();
    assertNotNull(data);
    assertEquals(1, data.size());
    assertEquals(1, data.get(0).getRowCount());
    assertEquals(10, data.get(0).getValueAsInt(0, 0, -1));

    sql = "WbGrepData -searchValue=arthur -types=view";
    result = instance.execute(sql);
    assertTrue(result.isSuccess());
    data = result.getDataStores();
    assertNotNull(data);
    assertEquals(1, data.size());
    assertEquals(1, data.get(0).getRowCount());
    assertEquals(10, data.get(0).getValueAsInt(0, 0, -1));

    sql = "WbGrepData -searchValue=arthur -tables=person -columns=lastname";
    result = instance.execute(sql);
    String msg = result.getMessages().toString();
    System.out.println(msg);
    assertTrue(msg, result.isSuccess());
    data = result.getDataStores();
    assertNull(data);

    sql = "WbGrepData -searchValue=arthur -tables=person -columns=firstname";
    result = instance.execute(sql);
    msg = result.getMessages().toString();
    assertTrue(msg, result.isSuccess());
    data = result.getDataStores();
    assertNotNull(data);
    assertEquals(1, data.size());
  }

}
