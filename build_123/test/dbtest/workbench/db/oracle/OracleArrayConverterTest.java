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
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleArrayConverterTest
  extends WbTestCase
{
  public OracleArrayConverterTest()
  {
    super("OracleArrayConverterTest");
  }

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();

		WbConnection con = OracleTestUtil.getOracleConnection();
		Assume.assumeNotNull(con);

    String sql =
      "create or replace type number_list as varray(10) of number;\n" +
      "/\n" +
      "create table varray_table (numbers number_list);\n" +
      "insert into varray_table values (number_list(1,2,3));\n" +
      "commit;\n" +
      "create table testgeo (shape  sdo_geometry);\n" +
      "insert into testgeo values (sdo_geometry(2003,null,null,sdo_elem_info_array(1,1003,3),sdo_ordinate_array(1,1, 5,7)));\n" +
      "commit;\n ";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

  @Test
  public void testVArray()
    throws Exception
  {
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);

		String select = "SELECT numbers FROM varray_table";
		Statement stmt = null;
		ResultSet rs = null;
    try
    {
      stmt = con.createStatement();
      rs = stmt.executeQuery(select);
      DataStore ds = new DataStore(rs, con, true);
      assertEquals(1, ds.getRowCount());
      String value = ds.getValueAsString(0, 0);
      assertEquals(OracleTestUtil.SCHEMA_NAME + ".NUMBER_LIST(1,2,3)", value);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  @Test
  public void testGeometry()
    throws Exception
  {
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);

		String select = "SELECT shape FROM testgeo";
		Statement stmt = null;
		ResultSet rs = null;
    try
    {
      stmt = con.createStatement();
      rs = stmt.executeQuery(select);
      DataStore ds = new DataStore(rs, con, true);
      assertEquals(1, ds.getRowCount());
      String value = ds.getValueAsString(0, 0);
      assertEquals("MDSYS.SDO_GEOMETRY(2003, NULL, NULL, MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,3), MDSYS.SDO_ORDINATE_ARRAY(1,1,5,7))", value);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

  }
}
