/*
 * HsqlViewGrantReaderTest.java
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
package workbench.db.hsqldb;

import java.util.Collection;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableGrant;
import workbench.db.TableIdentifier;
import workbench.db.ViewGrantReader;
import workbench.db.WbConnection;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlViewGrantReaderTest
  extends WbTestCase
{

  public HsqlViewGrantReaderTest()
  {
    super("HsqlViewGrantReaderTest");
  }

  @Test
  public void testGetViewGrantSql()
    throws Exception
  {
    TestUtil util = getTestUtil();
    WbConnection con = util.getHSQLConnection("viewgranttest");
    con.getMetadata().clearIgnoredSchemas();
    TestUtil.executeScript(con,
      "create user someone password 'welcome';\n" +
      "create table person (id integer, name varchar(100));\n" +
      "create view v_person as select * from person;\n" +
      "grant select on v_person to someone;\n" +
      "commit;"
      );

    TableIdentifier view = new TableIdentifier("PUBLIC", "V_PERSON");
    view.setType("VIEW");
    assertEquals("V_PERSON", view.getTableName());
    ViewGrantReader reader = ViewGrantReader.createViewGrantReader(con);
    Collection<TableGrant> grants = reader.getViewGrants(con, view);
    int grantCount = 0;
    for (TableGrant grant : grants)
    {
      if ("SOMEONE".equals(grant.getGrantee()))
      {
        assertEquals("SELECT", grant.getPrivilege());
        grantCount ++;
      }
    }
    assertEquals(1, grantCount);
  }

}
