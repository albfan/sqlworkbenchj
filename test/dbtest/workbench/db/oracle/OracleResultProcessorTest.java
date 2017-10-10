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
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.DelimiterDefinition;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleResultProcessorTest
  extends WbTestCase
{

	public OracleResultProcessorTest()
	{
		super("OracleResultProcessorTest");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

  @Test
	public void testEmbeddedResult()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		String sql =
      "create or replace function get_numbers(p_start number, p_end number)\n" +
      "    return sys_refcursor\n" +
      "as\n" +
      "    l_stmt   varchar2(32767);\n" +
      "    l_col    integer := 0;\n" +
      "    l_result sys_refcursor;\n" +
      "begin\n" +
      "    l_stmt := 'select ';\n" +
      "    for idx in p_start..p_end loop\n" +
      "        if l_col > 0 then \n" +
      "          l_stmt := l_stmt || ', ';\n" +
      "        end if;\n" +
      "        l_stmt := l_stmt || to_char(p_start + l_col);\n" +
      "        l_col := l_col + 1;\n" +
      "        l_stmt := l_stmt || ' as col_' || to_char(l_col);\n" +
      "    end loop;\n" +
      "    l_stmt := l_stmt || ' from dual';\n" +
      "    open l_result for l_stmt;\n" +
      "    return l_result;\n" +
      "end;\n" +
      "/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      stmt = con.createStatement();
      rs = stmt.executeQuery("select get_numbers(1,5) from dual");
      assertNotNull(rs);
      ResultSetMetaData rsm = rs.getMetaData();
      assertEquals(5,rsm.getColumnCount());
      for (int i=0; i < 5; i++)
      {
        assertEquals("COL_" + (i + 1), rsm.getColumnName(i+1));
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

}
