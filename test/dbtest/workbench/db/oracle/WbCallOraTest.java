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

import java.math.BigDecimal;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.interfaces.StatementParameterPrompter;
import workbench.sql.DelimiterDefinition;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.sql.wbcommands.WbCall;
import workbench.storage.DataStore;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCallOraTest
	extends WbTestCase
{

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
			"CREATE OR REPLACE procedure ref_cursor_example(pid number, person_result out sys_refcursor, addr_result out sys_refcursor) is \n" +
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
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testWbCall()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

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
		assertEquals("-- Parameters: PID, PERSON_RESULT, ADDR_RESULT\nWbCall REF_CURSOR_EXAMPLE(?,?,?);", callSql);

		procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "GET_STUFF");
		assertEquals(1, procs.size());

		ProcedureDefinition def = procs.get(0);
		assertNotNull(def);

		String sql = def.createSql(con);
		assertEquals("-- Parameters: P_ID\nWbCall GET_STUFF(?);", sql);

		result = call.execute("WbCall GET_STUFF(?)");
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(result.hasDataStores());

		results = result.getDataStores();
		assertEquals(1, results.size());

		DataStore person2 = results.get(0);
		assertEquals(1, person2.getRowCount());
	}
}
