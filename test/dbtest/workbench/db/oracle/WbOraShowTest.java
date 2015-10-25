/*
 * WbOraShowTest.java
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

import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

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

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}


	@Test
	public void testExecute()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);

		StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);

		runner.runStatement("create procedure nocando as begin null end;");
		StatementRunnerResult result = runner.getResult();

		assertFalse(result.isSuccess());
		runner.runStatement("show errors nocando");
		result = runner.getResult();
		assertTrue(result.isSuccess());
		String msg = result.getMessages().toString();
//		System.out.println(msg);
		assertTrue(msg.startsWith("Errors for PROCEDURE NOCANDO"));
		assertTrue(msg.contains("PLS-00103"));
	}

}
