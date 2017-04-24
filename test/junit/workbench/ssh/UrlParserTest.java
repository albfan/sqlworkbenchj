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
package workbench.ssh;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class UrlParserTest
{

  public UrlParserTest()
  {
  }

  @Test
  public void testPostgres()
  {
    UrlParser parser = new UrlParser("jdbc:postgresql://someserver:5433/dbname");
    assertEquals("jdbc:postgresql://127.0.0.1:9090/dbname", parser.getLocalUrl(9090));
    assertEquals(5433, parser.getDatabasePort());
    assertEquals("someserver", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:postgresql://someserver/dbname");
    assertEquals("jdbc:postgresql://127.0.0.1:9090/dbname", parser.getLocalUrl(9090));
    assertEquals(5432, parser.getDatabasePort());
    assertEquals("someserver", parser.getDatabaseServer());
  }

  @Test
  public void testSQLServer()
  {
    UrlParser parser = new UrlParser("jdbc:sqlserver://dbserver:1433");
    assertEquals("jdbc:sqlserver://127.0.0.1:9090", parser.getLocalUrl(9090));
    assertEquals(1433, parser.getDatabasePort());
    assertEquals("dbserver", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:sqlserver://dbserver;databaseName=wb_junit");
    assertEquals("jdbc:sqlserver://127.0.0.1:9090;databaseName=wb_junit", parser.getLocalUrl(9090));
    assertEquals(1433, parser.getDatabasePort());
    assertEquals("dbserver", parser.getDatabaseServer());
  }

  @Test
  public void testOracle()
  {
    UrlParser parser = new UrlParser("jdbc:postgresql://someserver:5433/dbname");
    assertEquals("jdbc:postgresql://127.0.0.1:9090/dbname", parser.getLocalUrl(9090));
    assertEquals(5433, parser.getDatabasePort());
    assertEquals("someserver", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:postgresql://someserver/dbname");
    assertEquals("jdbc:postgresql://127.0.0.1:9090/dbname", parser.getLocalUrl(9090));
    assertEquals(5432, parser.getDatabasePort());
    assertEquals("someserver", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:sqlserver://dbserver:1433");
    assertEquals("jdbc:sqlserver://127.0.0.1:9090", parser.getLocalUrl(9090));
    assertEquals(1433, parser.getDatabasePort());
    assertEquals("dbserver", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:sqlserver://dbserver;databaseName=wb_junit");
    assertEquals("jdbc:sqlserver://127.0.0.1:9090;databaseName=wb_junit", parser.getLocalUrl(9090));
    assertEquals(1433, parser.getDatabasePort());
    assertEquals("dbserver", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:oracle:thin:@//oradev01/orcl");
    assertEquals("jdbc:oracle:thin:@//127.0.0.1:9090/orcl", parser.getLocalUrl(9090));
    assertEquals(1521, parser.getDatabasePort());
    assertEquals("oradev01", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:oracle:thin:@//oradev01:1666/orcl");
    assertEquals("jdbc:oracle:thin:@//127.0.0.1:9090/orcl", parser.getLocalUrl(9090));
    assertEquals(1666, parser.getDatabasePort());
    assertEquals("oradev01", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:oracle:thin:@oradev01:orcl");
    assertEquals("jdbc:oracle:thin:@127.0.0.1:9090:orcl", parser.getLocalUrl(9090));
    assertEquals(1521, parser.getDatabasePort());
    assertEquals("oradev01", parser.getDatabaseServer());

    parser = new UrlParser("jdbc:oracle:thin:@oradev01:1523:orcl");
    assertEquals("jdbc:oracle:thin:@127.0.0.1:9090:orcl", parser.getLocalUrl(9090));
    assertEquals(1523, parser.getDatabasePort());
    assertEquals("oradev01", parser.getDatabaseServer());
  }

}
