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
package workbench.db.ibm;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2iObjectListEnhancerTest
  extends WbTestCase
{

  public Db2iObjectListEnhancerTest()
  {
    super("Db2iObjectListEnhancerTest");
  }

	@AfterClass
	public static void tearDown()
    throws Exception
	{
    ConnectionMgr.getInstance().disconnectAll();
	}

  @Test
  public void testUpdateObjectList()
    throws Exception
  {
    WbConnection con = getTestUtil().getConnection();
    TestUtil.executeScript(con,
      "create schema qsys2;\n" +
      "create table qsys2.systables (table_name varchar(100), table_schema varchar(100), table_text varchar(100));\n" +
      "insert into qsys2.systables values ('FOO', 'PUBLIC', 'Foo comment');\n" +
      "insert into qsys2.systables values ('BAR', 'PUBLIC', 'Bar comment');\n" +
      "commit;");

    Db2iObjectListEnhancer reader = new Db2iObjectListEnhancer();
    DataStore tables = con.getMetadata().createTableListDataStore();
    int fooRow = tables.addRow();
    tables.setValue(fooRow, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, "PUBLIC");
    tables.setValue(fooRow, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, "CAT");
    tables.setValue(fooRow, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, "FOO");

    int barRow = tables.addRow();
    tables.setValue(barRow, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, "PUBLIC");
    tables.setValue(barRow, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, "CAT");
    tables.setValue(barRow, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, "BAR");

    reader.updateObjectRemarks(con, tables, null, "PUBLIC", null, new String[]{"TABLE"});
    assertEquals("Foo comment", tables.getValueAsString(fooRow, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS));
    assertEquals("Bar comment", tables.getValueAsString(barRow, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS));
  }

}
