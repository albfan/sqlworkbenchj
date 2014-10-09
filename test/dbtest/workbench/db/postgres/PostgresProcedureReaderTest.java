/*
 * PostgresProcedureReaderTest.java
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

import workbench.db.GenericObjectDropper;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresProcedureReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "procreadertest";

	public PostgresProcedureReaderTest()
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

		String sql =
			"CREATE AGGREGATE array_accum (anyelement) \n" +
			 "( \n" +
			 "  sfunc = array_append, \n" +
			 "  stype = anyarray, \n" +
			 "  initcond = '{}' \n" +
			 ");\n" +
			 "commit;\n";
		TestUtil.executeScript(con, sql);

		String fnCreate =
			"CREATE FUNCTION fn_answer()  \n" +
			"  RETURNS integer  \n" +
			"AS $$ \n" +
			"BEGIN \n" +
			"    RETURN 42; \n" +
			"END; \n" +
			"$$ LANGUAGE plpgsql; \n" +
			"\n"  +
			"CREATE FUNCTION fn_answer(boost integer)  \n" +
			"  RETURNS integer  \n" +
			"AS $$ \n" +
			"BEGIN \n" +
			"    RETURN 42 * boost;\n" +
			"END; \n" +
			"$$ LANGUAGE plpgsql; \n" +
			"\n "  +
			"commit;";
		TestUtil.executeScript(con, fnCreate);

		String tables =
			"create schema s1; \n" +
			"create schema s2; \n" +
			" \n" +
			"create table s1.customer (id integer, customer_name varchar); \n" +
			"create table s2.customer (id integer, customer_name varchar); \n" +
			"commit;";
		TestUtil.executeScript(con, tables);

	String fullNames =
			"create or replace function public.fullname(cust s1.customer) \n" +
			"  returns varchar \n" +
			"as \n" +
			"$body$ \n" +
			"  select 'Name_1: '||cust.customer_name; \n" +
			"$body$ \n" +
			"language sql; \n" +
			" \n" +
			"create or replace function public.fullname(cust s2.customer) \n" +
			"  returns varchar \n" +
			"as \n" +
			"$body$ \n" +
			"  select 'Name_2: '||cust.customer_name; \n" +
			"$body$ \n" +
			"language sql; \n" +
			"commit;\n";

		TestUtil.executeScript(con, fullNames);

		String tableFunc =
			"CREATE OR REPLACE FUNCTION table_func(arg1 integer)\n" +
			"  RETURNS TABLE(col1 integer, col2 integer)\n" +
			"  LANGUAGE plpgsql\n" +
			"AS\n" +
			"$body$\n" +
			"BEGIN\n" +
			"  return query select arg1, arg1 * 2;\n" +
			"END;\n" +
			"$body$\n" +
			" VOLATILE\n" +
			" COST 100\n" +
			" ROWS 1000;\n";
		TestUtil.executeScript(con, tableFunc);

		String setof =
			"CREATE OR REPLACE FUNCTION fn_get_data(pid integer, title varchar, some_output text)\n" +
			"  RETURNS SETOF record\n" +
			"  LANGUAGE sql\n" +
			"AS\n" +
			"$body$\n" +
			"select 'Arthur', 'Dent';\n" +
			"$body$\n" +
			" VOLATILE\n" +
			" COST 100\n" +
			" ROWS 1000\n";
		TestUtil.executeScript(con, setof);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testSetof()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		PostgresProcedureReader reader = new PostgresProcedureReader(con);
		List<ProcedureDefinition> procs = reader.getProcedureList(null, TEST_ID, "fn_get_data");
		assertEquals(1, procs.size());
		ProcedureDefinition def = procs.get(0);
		assertEquals("fn_get_data", def.getProcedureName());
		String source = def.getSource(con).toString();
		String expected =
			"CREATE OR REPLACE FUNCTION " + TEST_ID + ".fn_get_data(pid integer, title character varying, some_output text)\n" +
			"  RETURNS SETOF record\n" +
			"  LANGUAGE sql\n" +
			"AS\n" +
			"$body$\n" +
			"select 'Arthur', 'Dent';\n" +
			"$body$\n" +
			" VOLATILE\n" +
			" COST 100\n" +
			" ROWS 1000;";
//		System.out.println(source);
		assertEquals(expected, source.trim());
	}

	@Test
	public void testTableFunc()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		PostgresProcedureReader reader = new PostgresProcedureReader(con);
		List<ProcedureDefinition> procs = reader.getProcedureList(null, TEST_ID, "table_func");
		assertEquals(1, procs.size());
		ProcedureDefinition def = procs.get(0);
		assertEquals("table_func", def.getProcedureName());
		assertEquals("table_func(integer)", def.getDisplayName());
		String source = def.getSource(con).toString();
		String expected =
			"CREATE OR REPLACE FUNCTION " + TEST_ID + ".table_func(arg1 integer)\n" +
			"  RETURNS TABLE(col1 integer, col2 integer)\n" +
			"  LANGUAGE plpgsql\n" +
			"AS\n" +
			"$body$\n" +
			"BEGIN\n" +
			"  return query select arg1, arg1 * 2;\n" +
			"END;\n" +
			"$body$\n" +
			" VOLATILE\n" +
			" COST 100\n" +
			" ROWS 1000;";
//		System.out.println("--- expected --- \n" + expected + "\n--- actual ---\n"  + source.trim() + "\n-------");
		assertEquals(expected, source.trim());
	}

	@Test
	public void testGetAggregateSource()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		PostgresProcedureReader reader = new PostgresProcedureReader(con);
		List<ProcedureDefinition> procs = reader.getProcedureList(null, TEST_ID, "array%");
		assertEquals(1, procs.size());
		ProcedureDefinition def = procs.get(0);
		assertEquals("array_accum", def.getProcedureName());
		assertEquals("aggregate", def.getDbmsProcType());
		assertEquals("array_accum(anyelement)", def.getDisplayName());
		String source = def.getSource(con).toString();
		String expected =
			"CREATE AGGREGATE array_accum(anyelement)\n" +
			"(\n"+
			"  sfunc = array_append,\n" +
			"  stype = anyarray,\n" +
			"  initcond = '{}'\n" +
			");";
		assertEquals(expected, source.trim());
		GenericObjectDropper dropper = new GenericObjectDropper();
		dropper.setConnection(con);
		String drop = dropper.getDropForObject(def).toString();
		assertEquals("DROP AGGREGATE array_accum(anyelement)", drop);
	}

	@Test
	public void testOverloaded()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		PostgresProcedureReader reader = new PostgresProcedureReader(con);
		List<ProcedureDefinition> procs = reader.getProcedureList(null, TEST_ID, "fn_answer%");
		assertEquals(2, procs.size());

		ProcedureDefinition f1 = procs.get(0);
		String source1 = f1.getSource(con).toString();
		DataStore cols1 = reader.getProcedureColumns(f1);
		assertEquals("fn_answer()", f1.getDisplayName());
		assertTrue(source1.contains("RETURN 42;"));
		assertEquals(1, cols1.getRowCount()); // the result is returned a one column
		String type = cols1.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
		assertEquals("RETURN", type);

		ProcedureDefinition f2 = procs.get(1);
		String source2 = f2.getSource(con).toString();
		DataStore cols2 = reader.getProcedureColumns(f2);
		assertEquals("fn_answer(integer)", f2.getDisplayName());
		assertTrue(source2.contains("RETURN 42 * boost;"));
		assertEquals(2, cols2.getRowCount()); // one parameter column plus the result column
		// row 0 is the result "column", so the actual column should be in row 1
		String name= cols2.getValueAsString(1, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
		assertEquals("boost", name);
	}

	@Test
	public void testQualifiedParams()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		PostgresProcedureReader reader = new PostgresProcedureReader(con);
		List<ProcedureDefinition> procs = reader.getProcedureList(null, "public", "fullname%");
		assertEquals(2, procs.size());

		ProcedureDefinition def1 = procs.get(0);
		String source1 = def1.getSource(con).toString();
		assertTrue(source1.contains("'Name_1: '"));

		ProcedureDefinition def2 = procs.get(1);
		String source2 = def2.getSource(con).toString();
		assertTrue(source2.contains("'Name_2: '"));
	}

	@Test
	public void testProcSource()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		TestUtil.executeScript(con,
			"create schema foo;\n" +
			"create function foo.bar(p_in integer) returns integer as $$ select 42; $$ language sql;\n" +
			"commit;");

		TableIdentifier object = new TableIdentifier("foo.bar(integer)", con);
		object.adjustCase(con);

		ProcedureReader reader = con.getMetadata().getProcedureReader();
		ProcedureDefinition def = new ProcedureDefinition(object.getCatalog(), object.getSchema(), object.getObjectName());
		assertTrue(reader.procedureExists(def));
		CharSequence sql = def.getSource(con);
		assertNotNull(sql);
		assertTrue(sql.toString().contains("FUNCTION foo.bar(p_in integer)"));
	}
}
