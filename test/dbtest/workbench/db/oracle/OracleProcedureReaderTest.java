/*
 * OracleProcedureReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import workbench.db.ProcedureReader;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.sql.DelimiterDefinition;
import workbench.storage.DataStore;
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
		if (con == null) return;

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
}
