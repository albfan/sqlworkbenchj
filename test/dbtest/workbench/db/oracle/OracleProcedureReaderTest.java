/*
 * OracleProcedureReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.DelimiterDefinition;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleProcedureReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "oraprocreader";

	public OracleProcedureReaderTest()
	{
		super(TEST_ID);
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		String sql = "create table some_info (id integer primary key, some_number number(14,3))\n" +
			"/\n" +
			"CREATE OR REPLACE PROCEDURE DATA_TYPE_TEST (some_value some_info.some_number%type ) \n" +
			"IS \n" +
			"BEGIN \n" +
			" NULL; \n" +
			"END DATA_TYPE_TEST; \n" +
			"/\n";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		sql =
			"CREATE OR REPLACE FUNCTION my_func \n" +
			"RETURN integer \n" +
			"IS \n" +
			"BEGIN \n" +
			" return 42; \n" +
			"END my_func; \n" +
			"/\n";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		sql =
			"CREATE PACKAGE proc_pckg  \n" +
			"AS  \n" +
			"  PROCEDURE process_pkg_data(some_value out number, some_id in number); \n" +
			"  FUNCTION get_answer RETURN INTEGER; \n" +
			"END proc_pckg;  \n" +
			"/ \n" +
			" \n" +
			"CREATE PACKAGE BODY proc_pckg \n" +
			"AS \n" +
			"  PROCEDURE process_pkg_data(some_value out number, some_id in number) \n" +
			"  IS  \n" +
			"  BEGIN  \n" +
			"    some_value := some_id * 2;   \n" +
			"  END process_pkg_data;   \n" +
			" \n" +
			"  FUNCTION get_answer \n" +
			"    RETURN INTEGER \n" +
			"  IS \n" +
			"  BEGIN \n" +
			"    return 42; \n" +
			"  END get_answer;\n" +
			"END proc_pckg; \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testExists()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		OracleProcedureReader reader = (OracleProcedureReader)con.getMetadata().getProcedureReader();
		assertTrue(reader.packageExists(OracleTestUtil.SCHEMA_NAME, "PROC_PCKG"));
		assertFalse(reader.packageExists(OracleTestUtil.SCHEMA_NAME, "FOOBAR"));
	}

	@Test
	public void testColumnTypes()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "DATA_TYPE_TEST");
		assertEquals(1, procs.size());
		ProcedureDefinition proc = procs.get(0);
		assertFalse(proc.isFunction());

		DataStore cols = con.getMetadata().getProcedureReader().getProcedureColumns(proc);
		assertEquals(1, cols.getRowCount());
		String colname = cols.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
		assertEquals("SOME_VALUE", colname);
		String datatype = cols.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
		assertEquals("NUMBER(14,3)", datatype);
	}

	@Test
	public void testPackagedFunctions()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "PROCESS_PKG_DATA");
		assertEquals(1, procs.size());
		ProcedureDefinition proc = procs.get(0);
		assertFalse(proc.isFunction());

		DataStore cols = con.getMetadata().getProcedureReader().getProcedureColumns(proc);
		assertEquals(2, cols.getRowCount());
		String colname = cols.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
		assertEquals("SOME_VALUE", colname);
		String inout = cols.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
		assertEquals("OUT", inout);
		colname = cols.getValueAsString(1, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
		assertEquals("SOME_ID", colname);
		inout = cols.getValueAsString(1, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
		assertEquals("IN", inout);

		procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "GET_ANSWER");
		assertEquals(1, procs.size());
		proc = procs.get(0);
		assertTrue(proc.isFunction());
		cols = con.getMetadata().getProcedureReader().getProcedureColumns(proc);
		assertEquals(1, cols.getRowCount());
	}

	@Test
	public void testInvalidProcedure()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql =
			"CREATE OR REPLACE FUNCTION my_invalid \n" +
			"RETURN integer \n" +
			"IS \n" +
			"BEGIN \n" +
			" return 42 \n" +  // this error is intended to generate an invalid procedure
			"END my_invalid; \n" +
			"/\n";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		Settings.getInstance().setProperty("workbench.db.oracle.procedures.custom_sql", true);
		DataStore ds = con.getMetadata().getProcedureReader().getProcedures(null, OracleTestUtil.SCHEMA_NAME, "MY_INVALID");
		assertEquals(1, ds.getRowCount());
		String procName1 = ds.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
		assertEquals("MY_INVALID", procName1);

		String status = ds.getValueAsString(0, OracleProcedureReader.COLUMN_IDX_PROC_LIST_ORA_STATUS);
		assertEquals("INVALID", status);

		sql =
			"CREATE PACKAGE proc_pckg2  \n" +
			"AS  \n" +
			"  PROCEDURE process_pkg_data2(some_value out number, some_id in number); \n" +
			"  FUNCTION get_answer2 RETURN INTEGER; \n" +
			"END proc_pckg2;  \n" +
			"/ \n" +
			" \n" +
			"CREATE PACKAGE BODY proc_pckg2 \n" +
			"AS \n" +
			"  PROCEDURE process_pkg_data2(some_value out number, some_id in number) \n" +
			"  IS  \n" +
			"  BEGIN  \n" +
			"    some_value = some_id * 2;   \n" + // force an invalid procedure
			"  END process_pkg_data2;   \n" +
			" \n" +
			"  FUNCTION get_answer2 \n" +
			"    RETURN INTEGER \n" +
			"  IS \n" +
			"  BEGIN \n" +
			"    return 42; \n" +
			"  END get_answer2;\n" +
			"END proc_pckg2; \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		DataStore ds2 = con.getMetadata().getProcedureReader().getProcedures(null, OracleTestUtil.SCHEMA_NAME, "PROCESS_PKG_DATA2");

		assertEquals(1, ds2.getRowCount());
		String procName2 = ds2.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
		assertEquals("PROCESS_PKG_DATA2", procName2);
		String status2 = ds2.getValueAsString(0, OracleProcedureReader.COLUMN_IDX_PROC_LIST_ORA_STATUS);
		assertEquals("INVALID", status2);
	}
}
