/*
 * PostgresProcedureReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.Statement;
import workbench.util.SqlUtil;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
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
		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			String create1 = "CREATE FUNCTION fn_answer()  \n" +
             "  RETURNS integer  \n" +
             "AS $$ \n" +
             "BEGIN \n" +
             "    RETURN 42; \n" +
             "END; \n" +
             "$$ LANGUAGE plpgsql \n";
			stmt.execute(create1);
			String create2 = "CREATE FUNCTION fn_answer(boost integer)  \n" +
             "  RETURNS integer  \n" +
             "AS $$ \n" +
             "BEGIN \n" +
             "    RETURN 42 * boost;\n" +
             "END; \n" +
             "$$ LANGUAGE plpgsql \n";
			stmt.execute(create2);
			con.commit();
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
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
		assertEquals(expected, source);
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
		assertEquals("fn_answer()", f1.getDisplayName());
		assertTrue(source1.contains("RETURN 42;"));

		ProcedureDefinition f2 = procs.get(1);
		String source2 = f2.getSource(con).toString();
		assertEquals("fn_answer(integer)", f2.getDisplayName());
		assertTrue(source2.contains("RETURN 42 * boost;"));

	}
}
