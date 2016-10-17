/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.ResultProcessor;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresResultProcessorTest
  extends WbTestCase
{

  public PostgresResultProcessorTest()
  {
    super("PostgresResultProcessorTest");
  }

  @BeforeClass
  public static void setUp()
    throws Exception
  {
    WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

    PostgresTestUtil.initTestCase("result_processor");

    TestUtil.executeScript(con,
      "create table person (id integer, first_name varchar(50), last_name varchar(50));\n" +
      "insert into person (id, first_name, last_name) values (1, 'Arthur', 'Dent');\n" +
      "insert into person (id, first_name, last_name) values (2, 'Ford', 'Prefect');\n" +
      "commit;\n" +
      "CREATE OR REPLACE FUNCTION refcursorfunc1() \n" +
      "  RETURNS refcursor \n" +
      "  LANGUAGE plpgsql \n" +
      "AS \n" +
      "$body$ \n" +
      "DECLARE  \n" +
      "    c1 refcursor;  \n" +
      " BEGIN  \n" +
      "    OPEN c1 FOR SELECT * FROM person ORDER BY id; \n" +
      "    RETURN c1;  \n" +
      " END \n" +
      "$body$;\n" +
      "\n" +
      "CREATE OR REPLACE FUNCTION refcursorfunc2() \n" +
      "  RETURNS setof refcursor \n" +
      "  LANGUAGE plpgsql \n" +
      "AS \n" +
      "$body$ \n" +
      "DECLARE  \n" +
      "    c1 refcursor;  \n" +
      "    c2 refcursor;  \n" +
      "BEGIN  \n" +
      "    OPEN c1 FOR SELECT * FROM person where id = 1;\n" +
      "    RETURN NEXT c1;  \n" +
      "    OPEN c2 FOR SELECT * FROM person where id = 2;\n" +
      "    RETURN NEXT c2;  \n" +
      "END \n" +
      "$body$;\n" +
      "commit;"
    );
  }

  @AfterClass
  public static void tearDown()
    throws Exception
  {
    PostgresTestUtil.cleanUpTestCase();
  }

  @Test
  public void testSimpleSelect()
    throws Exception
  {

    WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatement();

      ResultSet firstResult = stmt.executeQuery("select * from person order by id");
      ResultProcessor proc = new ResultProcessor(stmt, firstResult, con);
      rs = proc.getResult();
      assertNotNull(rs);

      int row = 0;
      while (rs.next())
      {
        row ++;
        int id = rs.getInt(1);
        assertEquals(row, id);
      }
      assertEquals(row, 2);

      firstResult = stmt.executeQuery("select daterange(current_date, current_date + 1)");
      proc = new ResultProcessor(stmt, firstResult, con);
      rs = proc.getResult();
      assertNotNull(rs);
      assertTrue(rs.next());
      Object range = rs.getObject(1);
      assertNotNull(range);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

  }
  @Test
  public void testSingleRefCursor()
    throws Exception
  {

    WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatement();

      // test regular statement
      ResultSet firstResult = stmt.executeQuery("select * from refcursorfunc1()");
      ResultProcessor proc = new ResultProcessor(stmt, firstResult, con);
      rs = proc.getResult();
      assertNotNull(rs);
      int row = 1;
      while (rs.next())
      {
        int id = rs.getInt(1);
        assertEquals(row, id);
        row ++;
      }
      assertFalse(proc.hasMoreResults());

    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  @Test
  public void testSimpleMutlipeRefCursor()
    throws Exception
  {

    WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatement();

      ResultSet firstResult = stmt.executeQuery("select * from refcursorfunc2()");
      ResultProcessor proc = new ResultProcessor(stmt, firstResult, con);
      rs = proc.getResult();
      assertNotNull(rs);
      int count = 0;
      while (rs.next())
      {
        int id = rs.getInt(1);
        assertEquals(1, id);
        count ++;
      }
      assertEquals(1, count);

      assertTrue(proc.hasMoreResults());
      rs = proc.getResult();
      assertNotNull(rs);

      count = 0;
      while (rs.next())
      {
        int id = rs.getInt(1);
        assertEquals(2, id);
        count ++;
      }
      assertEquals(1, count);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

}
