/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017 Thomas Kellerer.
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

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlDataTypeResolverTest
  extends WbTestCase
{

  public HsqlDataTypeResolverTest()
  {
    super("HsqlDataTypeResolverTest");
  }

  @BeforeClass
  public static void setUpClass()
    throws Exception
  {
  }

  @AfterClass
  public static void tearDownClass()
    throws Exception
  {
    ConnectionMgr.getInstance().disconnectAll();
  }

  @Test
  public void testUpdateColumnDefinition()
    throws Exception
  {
    TestUtil util = getTestUtil();
    WbConnection con = util.getHSQLConnection("data_type_resolver_test");
    TestUtil.executeScript(con,
      "CREATE TABLE bit_test \n" +
      "(\n" +
      "  c1 bit,  \n" +
      "  c2 bit(42) \n "+
      ");\n");
    TableDefinition tbl = con.getMetadata().getTableDefinition(new TableIdentifier(null, "PUBLIC", "BIT_TEST"));
    assertNotNull(tbl);
    List<ColumnIdentifier> cols = tbl.getColumns();
    assertEquals(2, cols.size());
    for (ColumnIdentifier col : cols)
    {
      String name = col.getColumnName();
      if ("c1".equalsIgnoreCase(name))
      {
        assertEquals("BIT(1)", col.getDbmsType());
      }
      if ("c2".equalsIgnoreCase(name))
      {
        assertEquals("BIT(42)", col.getDbmsType());
      }
    }

  }

}
