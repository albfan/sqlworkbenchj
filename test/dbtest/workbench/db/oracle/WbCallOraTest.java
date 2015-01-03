/*
 * WbCallOraTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import java.math.BigDecimal;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.interfaces.StatementParameterPrompter;

import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.DelimiterDefinition;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.sql.wbcommands.WbCall;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCallOraTest
	extends WbTestCase
{

	private boolean prompterCalled;

	public WbCallOraTest()
	{
		super("TestWbCallOra");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String tableSql =
			"create table address (id integer primary key, person_id integer, address_info varchar(100)); \n " +
			"create table person (id integer primary key, person_name varchar(100)); \n " +
			"insert into person values (1, 'Arthur Dent');\n" +
			"insert into person values (2, 'Ford Prefect');\n" +
			"insert into address (id, person_id, address_info) values (100, 1, 'Arthur''s Address');\n" +
			"insert into address (id, person_id, address_info) values (200, 2, 'Fords''s Address');\n" +
			"commit;\n";

		TestUtil.executeScript(con, tableSql);

		String sql =
			"CREATE OR REPLACE PROCEDURE ref_cursor_example(pid number, person_result out sys_refcursor, addr_result out sys_refcursor) is \n" +
      "begin \n" +
      "    open person_result for select id, person_name from person where id = pid;\n" +
      "    open addr_result for select a.id, a.person_id, a.address_info from address a join person p on a.person_id = p.id where p.id = pid;\n" +
      "end; \n" +
      "/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		sql =
			"create or replace function get_stuff(p_id in number) \n" +
			"return sys_refcursor \n" +
			"is \n" +
			"    p_cur sys_refcursor; \n" +
			"begin \n" +
			"  open p_cur for select * from person where id = p_id; \n" +
			"  return p_cur; " +
			"end get_stuff; \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		sql =
			"CREATE OR REPLACE PROCEDURE get_magic(errorcode out number, app_id in number, p_cur in out sys_refcursor)  \n" +
			"IS \n" +
			"BEGIN \n" +
			"  errorcode   := 42;  \n" +
			"  OPEN p_cur FOR  \n" +
			"     SELECT 42 AS MAGIC_NUMBER \n" +
			"     FROM DUAL;  \n" +
			"END GET_MAGIC;  \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		sql =
			"CREATE OR REPLACE PROCEDURE process_data(some_value out number, some_id in number)  \n" +
			"IS \n" +
			"BEGIN \n" +
			"  some_value := some_id * 2;  \n" +
			"END process_data;  \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		sql =
			"create or replace function get_status(process_status out varchar, error_flag out number, some_value integer) \n" +
			"return number \n" +
			"is \n" +
			"begin \n" +
			"  process_status := 'FINISHED'; \n" +
			"  error_flag := -1; \n" +
			"  return 42; \n" +
			"end get_status; \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		sql =
			"create or replace function get_answer \n" +
			"return number \n" +
			"is \n" +
			"begin \n" +
			"  return 42; \n" +
			"end; \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		sql =
			"CREATE PACKAGE proc_pckg  \n" +
			"AS  \n" +
			"  PROCEDURE process_pkg_data(some_value out number, some_id in number); \n" +
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
			"END proc_pckg; \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER, true);

		sql =
			"CREATE TYPE some_type AS OBJECT (some_value varchar(100), some_id integer);\n" +
			"/\n" +
			"create type some_type_table as table of some_type;\n" +
			"/";
		TestUtil.executeScript(con, sql, true);

		sql =
			"create or replace package overload_pkg \n" +
			"as \n" +
			"  procedure get_data( db_status   out    number \n" +
			"                    , app_status  out    number \n" +
			"                    , id1         in     number \n" +
			"                    , id2         in     number \n" +
			"                    , id3         in     number \n" +
			"                    , cur         in out sys_refcursor); \n" +
			" \n" +
			"   procedure get_data( db_status  out     number \n" +
			"                  , app_status    out     number \n" +
			"                  , id1           in      number \n" +
			"                  , id2           in      number \n" +
			"                  , id3           in      number \n" +
			"                  , id4           in      number \n" +
			"                  , some_filter   in      some_type_table \n" +
			"                  , cur           in out  sys_refcursor); \n" +
			"end overload_pkg; \n" +
			"/ \n" +
			" \n" +
			"create or replace package body overload_pkg \n" +
			"as \n" +
			"   \n" +
			"  procedure get_data( db_status   out    number \n" +
			"                    , app_status  out    number \n" +
			"                    , id1         in     number \n" +
			"                    , id2         in     number \n" +
			"                    , id3         in     number \n" +
			"                    , cur         in out sys_refcursor) \n" +
			"  is \n" +
			"  begin \n" +
			"    db_status := 1; \n" +
			"    app_status := 2; \n" +
			"  end get_data; \n" +
			"   \n" +
			"   procedure get_data( db_status     out    number \n" +
			"                     , app_status    out    number \n" +
			"                     , id1           in     number \n" +
			"                     , id2           in     number \n" +
			"                     , id3           in     number \n" +
			"                     , id4           in     number \n" +
			"                     , some_filter   in     some_type_table \n" +
			"                     , cur           in out sys_refcursor) \n" +
			"  is \n" +
			"  begin \n" +
			"    db_status := 10; \n" +
			"    app_status := 20; \n" +
			"  end get_data; \n" +
			"                                     \n" +
			"end overload_pkg; \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER, true);
}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testOverload()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		WbCall call = new WbCall();
		StatementRunner runner = new StatementRunner();

		StatementParameterPrompter prompter = new StatementParameterPrompter()
		{
			@Override
			public boolean showParameterDialog(StatementParameters parms, boolean showNames)
			{
				assertEquals(1, parms.getParameterCount());
				parms.setParameterValue(0, "0");
				parms.setParameterValue(1, "1");
				return true;
			}
		};
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);
		call.setParameterPrompter(prompter);

		String cmd = "WbCall overload_pkg.get_data(?,?,1,2,3,4,some_type_table(),?)";
		StatementRunnerResult result = call.execute(cmd);
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());
		assertTrue(result.hasDataStores());
		List<DataStore> results = result.getDataStores();
		assertEquals(1, results.size());

		// Person result
		DataStore data = results.get(0);
		assertEquals(10, data.getValueAsInt(0, 1, -1));
		assertEquals(20, data.getValueAsInt(1, 1, -1));
	}

	@Test
	public void testWbCall()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "REF_CURSOR_EXAMPLE");
		assertEquals(1, procs.size());

		StatementParameterPrompter prompter = new StatementParameterPrompter()
		{
			@Override
			public boolean showParameterDialog(StatementParameters parms, boolean showNames)
			{
				assertEquals(1, parms.getParameterCount());
				parms.setParameterValue(0, "1");
				return true;
			}
		};

		WbCall call = new WbCall();
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);
		call.setParameterPrompter(prompter);

		String cmd = "wbcall ref_cursor_example(?, ?, ?)";
		StatementRunnerResult result = call.execute(cmd);
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());
		List<DataStore> results = result.getDataStores();
		assertEquals(2, results.size());

		// Person result
		DataStore person = results.get(0);
		assertEquals(1, person.getRowCount());
		assertEquals(BigDecimal.valueOf(1), person.getValue(0, 0));
		assertEquals("Arthur Dent", person.getValue(0, 1));

		// Address result
		DataStore address = results.get(1);
		assertEquals(1, address.getRowCount());
		assertEquals(BigDecimal.valueOf(100), address.getValue(0, 0));
		assertEquals(BigDecimal.valueOf(1), address.getValue(0, 1));
		assertEquals("Arthur's Address", address.getValue(0, 2));

		String callSql = procs.get(0).createSql(con);
		assertEquals("-- Parameters: PID (IN), PERSON_RESULT (OUT), ADDR_RESULT (OUT)\nWbCall REF_CURSOR_EXAMPLE(?,?,?);", callSql);

		procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "GET_STUFF");
		assertEquals(1, procs.size());

		ProcedureDefinition getStuff = procs.get(0);
		assertNotNull(getStuff);

		String sql = getStuff.createSql(con);
		assertEquals("-- Parameters: P_ID (IN)\nWbCall GET_STUFF(?);", sql);

		result = call.execute("WbCall GET_STUFF(?)");
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());

		results = result.getDataStores();
		assertEquals(1, results.size());

		DataStore person2 = results.get(0);
		assertEquals(1, person2.getRowCount());
	}

	@Test
	public void testWbCallGeneration()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "GET_MAGIC");
		assertEquals(1, procs.size());

		ProcedureDefinition magic = procs.get(0);
		assertNotNull(magic);
		String generated = magic.createSql(con);
		String expected = "-- Parameters: ERRORCODE (OUT), APP_ID (IN), P_CUR (INOUT)\nWbCall GET_MAGIC(?,?,?);";
		assertEquals(expected, generated);

		procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "GET_STATUS");
		assertEquals(1, procs.size());

		ProcedureDefinition status = procs.get(0);
		assertNotNull(status);
		String answerSql = status.createSql(con);
		expected = "-- Parameters: PROCESS_STATUS (OUT), ERROR_FLAG (OUT), SOME_VALUE (IN)\nWbCall GET_STATUS(?,?,?);";
		assertEquals(expected, answerSql);
	}

	@Test
	public void testFunctionWithOutputParameter()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "GET_STATUS");
		assertEquals(1, procs.size());

		WbCall call = new WbCall();
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);

		String cmd = "wbcall get_status(?, ?, 42)";
		StatementRunnerResult result = call.execute(cmd);
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());
		List<DataStore> results = result.getDataStores();
		assertEquals(1, results.size());
		DataStore ds = results.get(0);
		assertNotNull(ds);
		assertEquals(3, ds.getRowCount());
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			String name = ds.getValueAsString(row, 0);
			if ("RETURN".equals(name))
			{
				int value = ds.getValueAsInt(row, 1, -1);
				assertEquals(42,value);
			}
			if ("PROCESS_STATUS".equals(name))
			{
				String value = ds.getValueAsString(row, 1);
				assertEquals("FINISHED", value);
			}
			if ("ERROR_FLAG".equals(name))
			{
				int value = ds.getValueAsInt(row, 1, -1);
				assertEquals(-1, value);
			}
		}
	}

	@Test
	public void testPackageProcedure()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "PROCESS_PKG_DATA");
		assertEquals(1, procs.size());
		String sql = procs.get(0).createSql(con);
		assertEquals("-- Parameters: SOME_VALUE (OUT), SOME_ID (IN)\nWbCall PROC_PCKG.PROCESS_PKG_DATA(?,?);", sql);

		StatementParameterPrompter prompter = new StatementParameterPrompter()
		{
			@Override
			public boolean showParameterDialog(StatementParameters parms, boolean showNames)
			{
				prompterCalled = true;
				return true;
			}
		};

		WbCall call = new WbCall();
		prompterCalled = false;
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);
		call.setParameterPrompter(prompter);

		String cmd = "wbcall PROC_PCKG.PROCESS_PKG_DATA(?, 21)";
		StatementRunnerResult result = call.execute(cmd);
		assertFalse(prompterCalled);
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());
		List<DataStore> results = result.getDataStores();
		assertEquals(1, results.size());
		DataStore ds = results.get(0);
		assertNotNull(ds);
		assertEquals(1, ds.getRowCount());
		String name = ds.getValueAsString(0, 0);
		int value = ds.getValueAsInt(0, 1, -1);
		assertEquals("SOME_VALUE", name);
		assertEquals(42, value);
	}

	@Test
	public void testRegularProcedure()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "PROCESS_DATA");
		assertEquals(1, procs.size());
		String sql = procs.get(0).createSql(con);
		assertEquals("-- Parameters: SOME_VALUE (OUT), SOME_ID (IN)\nWbCall PROCESS_DATA(?,?);", sql);

		StatementParameterPrompter prompter = new StatementParameterPrompter()
		{
			@Override
			public boolean showParameterDialog(StatementParameters parms, boolean showNames)
			{
				prompterCalled = true;
				return true;
			}
		};

		WbCall call = new WbCall();
		prompterCalled = false;
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);
		call.setParameterPrompter(prompter);

		String cmd = "wbcall process_data(?, 21)";
		StatementRunnerResult result = call.execute(cmd);
		assertFalse(prompterCalled);
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());
		List<DataStore> results = result.getDataStores();
		assertEquals(1, results.size());
		DataStore ds = results.get(0);
		assertNotNull(ds);
		assertEquals(1, ds.getRowCount());
		String name = ds.getValueAsString(0, 0);
		int value = ds.getValueAsInt(0, 1, -1);
		assertEquals("SOME_VALUE", name);
		assertEquals(42, value);
	}

	@Test
	public void testRegularFunction()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "GET_ANSWER");
		assertEquals(1, procs.size());

		StatementParameterPrompter prompter = new StatementParameterPrompter()
		{
			@Override
			public boolean showParameterDialog(StatementParameters parms, boolean showNames)
			{
				prompterCalled = true;
				return true;
			}
		};

		WbCall call = new WbCall();
		prompterCalled = false;
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);
		call.setParameterPrompter(prompter);

		String cmd = "wbcall get_answer()";
		StatementRunnerResult result = call.execute(cmd);
		assertFalse(prompterCalled);
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());
		List<DataStore> results = result.getDataStores();
		assertEquals(1, results.size());
		DataStore ds = results.get(0);
		assertNotNull(ds);
		assertEquals(1, ds.getRowCount());
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			String name = ds.getValueAsString(row, 0);
			if ("RETURN".equals(name))
			{
				int value = ds.getValueAsInt(row, 1, -1);
				assertEquals(42,value);
			}
		}
	}

	@Test
	public void testSystemProcedure()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "GET_ANSWER");
		assertEquals(1, procs.size());

		StatementParameterPrompter prompter = new StatementParameterPrompter()
		{
			@Override
			public boolean showParameterDialog(StatementParameters parms, boolean showNames)
			{
				prompterCalled = true;
				return true;
			}
		};

		WbCall call = new WbCall();
		prompterCalled = false;
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);
		call.setParameterPrompter(prompter);

		String cmd = "wbcall dbms_utility.get_parameter_value('STATISTICS_LEVEL', ?, ?, 1)";
		StatementRunnerResult result = call.execute(cmd);
		assertTrue(prompterCalled);
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());
		List<DataStore> results = result.getDataStores();
		assertEquals(1, results.size());
		DataStore ds = results.get(0);
		assertNotNull(ds);
		assertEquals(3, ds.getRowCount());
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			String name = ds.getValueAsString(row, 0);
			if ("RETURN".equals(name))
			{
				int value = ds.getValueAsInt(row, 1, -1);
				assertEquals(1,value);
			}
			if ("STRVAL".equals(name))
			{
				assertEquals("TYPICAL", ds.getValueAsString(row, 1));
			}
		}
	}
}
