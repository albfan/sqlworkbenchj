/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.hsqldb;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlSynonymReaderTest
  extends WbTestCase
{

  public HsqlSynonymReaderTest()
  {
    super("HsqlSynonymReaderTest");
  }

  @Before
  public void setUp()
  {
  }

  @After
  public void tearDown()
  {
    ConnectionMgr.getInstance().disconnectAll();
  }

  @Test
  public void testGetSynonymList()
    throws Exception
  {
    WbConnection conn = getTestUtil().getHSQLConnection("syntest");
    if (!JdbcUtils.hasMinimumServerVersion(conn, "2.3.4"))
    {
      System.out.println("HSQLDB version used does not support synonyms");
      return;
    }
    
    TestUtil.executeScript(conn,
      "create table person (id integer);\n" +
      "create synonym s_person for person;");

    HsqlSynonymReader reader = new HsqlSynonymReader();
    List<TableIdentifier> result = reader.getSynonymList(conn, null, "PUBLIC", "%");
    assertEquals(1, result.size());

    TableIdentifier syn = result.get(0);

    assertEquals("S_PERSON", syn.getTableName());

    TableIdentifier target = reader.getSynonymTable(conn, syn.getCatalog(), syn.getSchema(), syn.getTableName());
    assertNotNull(target);
    assertEquals("PERSON", target.getTableName());
  }


}
