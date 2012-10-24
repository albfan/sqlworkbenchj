/*
 * WbOraShowTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import workbench.db.WbConnection;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class WbOraShowTest
extends WbTestCase
{
	public WbOraShowTest()
	{
		super("WbOraShowTest");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);

		runner.runStatement("create procedure nocando as begin null end;");
		StatementRunnerResult result = runner.getResult();

		assertFalse(result.isSuccess());
		runner.runStatement("show errors nocando");
		result = runner.getResult();
		assertTrue(result.isSuccess());
		String msg = result.getMessageBuffer().toString();
//		System.out.println(msg);
		assertTrue(msg.startsWith("Errors for PROCEDURE NOCANDO"));
		assertTrue(msg.contains("PLS-00103"));
	}

}
