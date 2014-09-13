/*
 * WbCallPostgresTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbCall;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCallPostgresTest
	extends WbTestCase
{
	private static final String TEST_ID = "wbcalltest";

	public WbCallPostgresTest()
	{
		super(TEST_ID);
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"create table person (id integer, first_name varchar(50), last_name varchar(50));\n" +
			"insert into person (id, first_name, last_name) values (1, 'Arthur', 'Dent');\n" +
			"insert into person (id, first_name, last_name) values (2, 'Ford', 'Prefect');\n" +
			"commit;\n"  +
			"CREATE OR REPLACE FUNCTION refcursorfunc() \n" +
			 "  RETURNS refcursor \n" +
			 "  LANGUAGE plpgsql \n" +
			 "AS \n" +
			 "$body$ \n" +
			 "DECLARE  \n" +
			 "    mycurs refcursor;  \n" +
			 " BEGIN  \n" +
			 "    OPEN mycurs FOR SELECT * FROM person ORDER BY id;  \n" +
			 "    RETURN mycurs;  \n" +
			 " END \n" +
			 "$body$;\n" +
			"create or replace function get_answer() \n" +
			" returns integer \n" +
			 "  LANGUAGE plpgsql \n" +
			 "AS \n" +
			 "$body$ \n" +
			 " BEGIN  \n" +
			 "    RETURN 42;  \n" +
			 " END \n" +
			 "$body$\n" +
			 ";");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testWbCall()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		WbCall call = new WbCall();
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);
		String cmd = "WbCall refcursorfunc()";
		StatementRunnerResult result = call.execute(cmd);
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());
		DataStore ds = result.getDataStores().get(0);
		assertEquals(2, ds.getRowCount());
		assertEquals(Integer.valueOf(1), ds.getValue(0, 0));
		assertEquals("Arthur", ds.getValue(0, 1));
		assertEquals("Dent", ds.getValue(0, 2));
		assertEquals(Integer.valueOf(2), ds.getValue(1, 0));
		assertEquals("Ford", ds.getValue(1, 1));
		assertEquals("Prefect", ds.getValue(1, 2));

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, TEST_ID, "refcursorfunc");
		assertEquals(1, procs.size());
		ProcedureDefinition proc = procs.get(0);
		String callCmd = proc.createSql(con);
		assertEquals("WbCall refcursorfunc();", callCmd);

		DataStore params = con.getMetadata().getProcedureReader().getProcedureColumns(proc);
		assertEquals(1, params.getRowCount());
		assertTrue(ProcedureDefinition.returnsRefCursor(con, params));
	}

	@Test
	public void testRegularFunctionCall()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		WbCall call = new WbCall();
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		call.setStatementRunner(runner);
		call.setConnection(con);
		String cmd = "WbCall get_answer()";
		StatementRunnerResult result = call.execute(cmd);
		assertEquals("{? =  call get_answer()}", call.getSqlUsed());
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());
		DataStore ds = result.getDataStores().get(0);
		assertEquals(1, ds.getRowCount());
		assertEquals(42, ds.getValueAsInt(0, 1, -1));
	}

}
