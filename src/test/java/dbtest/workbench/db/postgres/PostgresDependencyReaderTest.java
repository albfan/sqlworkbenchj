/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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
package workbench.db.postgres;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresDependencyReaderTest
  extends WbTestCase
{

  public PostgresDependencyReaderTest()
  {
    super("PgDependencyTest");
  }

  @Before
  public void setUp()
  {
  }

  @After
  public void tearDown()
  {
    WbConnection conn = PostgresTestUtil.getPostgresConnection();
    PostgresTestUtil.dropAllObjects(conn);
  }

  @Test
  public void testDependencies()
    throws Exception
  {
    WbConnection conn = PostgresTestUtil.getPostgresConnection();
    assertNotNull(conn);

    TestUtil.executeScript(conn,
      "create table t1 (id serial); \n" +
      "create view v1 as select * from t1;\n" +
      "create view v2 as select t1.id as id1, v1.id as id2 from v1 cross join t1;\n" +
      "commit;");

    TableIdentifier t1 = conn.getMetadata().findObject(new TableIdentifier("t1"));
    TableIdentifier v1 = conn.getMetadata().findObject(new TableIdentifier("v1"));
    TableIdentifier v2 = conn.getMetadata().findObject(new TableIdentifier("v2"));

    PostgresDependencyReader reader = new PostgresDependencyReader();
    List<DbObject> usedBy = reader.getUsedBy(conn, t1);
    assertNotNull(usedBy);
    assertEquals(2, usedBy.size());
    assertEquals("v1", usedBy.get(0).getObjectName());
    assertEquals("v2", usedBy.get(1).getObjectName());

    List<DbObject> usedObjects = reader.getUsedObjects(conn, v1);
    assertNotNull(usedObjects);
    assertEquals(1, usedObjects.size());
    assertEquals("t1", usedObjects.get(0).getObjectName());

    List<DbObject> v2Uses = reader.getUsedObjects(conn, v2);
    assertNotNull(v2Uses);
    assertEquals(2, v2Uses.size());
    assertEquals("t1", v2Uses.get(0).getObjectName());
    assertEquals("v1", v2Uses.get(1).getObjectName());

    List<DbObject> t1Uses = reader.getUsedObjects(conn, t1);
    assertNotNull(v2Uses);
    assertEquals(1, t1Uses.size());
    assertEquals("t1_id_seq", t1Uses.get(0).getObjectName());
  }

  @Test
  public void testSupportsDependencies()
  {
    PostgresDependencyReader reader = new PostgresDependencyReader();
    List<String> types = CollectionUtil.arrayList("view", "table", "sequence");
    for (String type : types)
    {
      assertTrue(reader.supportsDependencies(type));
    }
  }

}
