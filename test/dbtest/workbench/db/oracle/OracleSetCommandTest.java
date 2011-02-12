/*
 * OracleSetCommandTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import static org.junit.Assert.*;
/**
 *
 * @author Thomas Kellerer
 */
public class OracleSetCommandTest
	extends WbTestCase
{

	public OracleSetCommandTest()
	{
		super("OracleSetCommandTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
	}

	@Test
	public void testExecute()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);
		runner.runStatement("set serveroutput on");

		StatementRunnerResult result = runner.getResult();
		assertTrue(result.isSuccess());

		runner.runStatement(
			"begin \n" +
			"  dbms_output.put_line('Hello, World'); \n" +
			"end;\n");

		result = runner.getResult();
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg.contains("Hello, World"));
	}

}