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
package workbench.db;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class JdbcUtilsTest
{

  public JdbcUtilsTest()
  {
  }

  @Test
  public void testExtractDBType()
  {
    assertEquals("jdbc:postgresql:", JdbcUtils.extractDBType("jdbc:postgresql://localhost/postgres"));
    assertEquals("jdbc:jtds:", JdbcUtils.extractDBType("jdbc:jtds:sqlserver://localhost/foobar"));
    assertEquals("jdbc:oracle:", JdbcUtils.extractDBType("jdbc:oracle:thin:@//localhost:1521/oradb"));
  }

  @Test
  public void testExtractDBID()
  {
    assertEquals(DBID.Postgres.getId(), JdbcUtils.getDbIdFromUrl("jdbc:postgresql://localhost/postgres"));
    assertEquals(DBID.HANA.getId(), JdbcUtils.getDbIdFromUrl("jdbc:sap://centos01:30015/"));
    assertEquals(DBID.SAP_DB.getId(), JdbcUtils.getDbIdFromUrl("jdbc:sapdb://127.0.0.1/dummy"));
  }
}
