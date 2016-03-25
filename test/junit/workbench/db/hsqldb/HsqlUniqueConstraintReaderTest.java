/*
 * PostgresUniqueConstraintReaderTest.java
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
package workbench.db.hsqldb;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.report.SchemaReporter;

import workbench.util.CollectionUtil;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class HsqlUniqueConstraintReaderTest
  extends WbTestCase
{

  public HsqlUniqueConstraintReaderTest()
  {
    super("HsqlUniqueConstraintReaderTest");
  }

  @AfterClass
  public static void tearDown()
    throws Exception
  {
    ConnectionMgr.getInstance().disconnectAll();
  }

  @Test
  public void testProcessIndexList()
    throws Exception
  {
    String sql =
      "CREATE TABLE utest\n" +
      "(\n" +
      "   id  integer   NOT NULL\n" +
      ");\n" +
      "\n" +
      "ALTER TABLE utest\n" +
      "   ADD CONSTRAINT unique_id UNIQUE (id);\n" +
      "commit;";

    WbConnection con = getTestUtil().getHSQLConnection("unique_test");
    TestUtil.executeScript(con, sql);

    TableIdentifier parent = con.getMetadata().findObject(new TableIdentifier("UTEST"));
    List<IndexDefinition> indexList = con.getMetadata().getIndexReader().getTableIndexList(parent, true);

    assertEquals(1, indexList.size());
    assertTrue(indexList.get(0).isUniqueConstraint());
    assertEquals("UNIQUE_ID", indexList.get(0).getUniqueConstraintName());
  }

  @Test
  public void testSchemaReport()
    throws Exception
  {
    String sql =
      "CREATE TABLE utest\n" +
      "(\n" +
      "   id  integer   NOT NULL\n" +
      ");\n" +
      "\n" +
      "ALTER TABLE utest\n" +
      "   ADD CONSTRAINT unique_id UNIQUE (id);\n" +
      "commit;";

    WbConnection con = getTestUtil().getHSQLConnection("unique_test");
    TestUtil.executeScript(con, sql);

    SchemaReporter reporter = new SchemaReporter(con);
    TableIdentifier parent = con.getMetadata().findObject(new TableIdentifier("UTEST"));
    reporter.setObjectList(CollectionUtil.arrayList(parent));
    String xml = reporter.getXml();
    System.out.println(xml);

    String count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def)");
    assertEquals("Incorrect table count", "1", count);
    count = TestUtil.getXPathValue(xml, "count(/schema-report/table-def[@name='UTEST']/index-def)");
    assertEquals("1", count);
    String constraint = TestUtil.getXPathValue(xml, "/schema-report/table-def[@name='UTEST']/index-def[1]/constraint-name/text()");
    assertEquals("UNIQUE_ID", constraint);
  }
}
