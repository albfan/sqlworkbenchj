/*
 * WbModeTest.java
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
package workbench.sql.wbcommands;


import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.interfaces.ExecutionController;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.sql.BatchRunner;

import workbench.util.SqlUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbModeTest
	extends WbTestCase
{

	public WbModeTest()
	{
		super("WbModeTest");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();

		try
		{
			WbConnection con = util.getConnection();
			TestUtil.executeScript(con,
				"create table mode_test (id integer primary key, some_value varchar(100));\n" +
				"insert into mode_test values (1, 'one');\n" +
				"insert into mode_test values (2, 'two');\n" +
				"commit;\n"
			);

			BatchRunner runner = new BatchRunner();
			runner.setConnection(con);
			runner.setVerboseLogging(false);
			runner.runScript(
				"WbMode readonly;\n" +
				"delete from mode_test;\n" +
				"commit;\n"
			);

			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from mode_test");
			assertTrue(rs.next());
			int count = rs.getInt(1);
			assertEquals(count, 2);
			SqlUtil.closeResult(rs);

			Controller controll = new Controller();
			runner.setExecutionController(controll);

			runner.runScript(
				"WbMode confirm;\n" +
				"delete from mode_test;\n" +
				"commit;\n"
			);

			assertEquals(2, controll.confirmStatementCalled);

			rs = stmt.executeQuery("select count(*) from mode_test");
			assertTrue(rs.next());
			count = rs.getInt(1);
			assertEquals(count, 0);
			SqlUtil.closeResult(rs);
			SqlUtil.closeStatement(stmt);

			con.getProfile().setReadOnly(true);
			con.getProfile().setConfirmUpdates(false);

			runner.runScript(
				"WbMode confirm;\n"
			);
			assertTrue(con.confirmUpdatesInSession());
			assertFalse(con.isSessionReadOnly());

			runner.runScript(
				"WbMode normal;\n"
			);
			assertFalse(con.confirmUpdatesInSession());
			assertFalse(con.isSessionReadOnly());

			runner.runScript(
				"WbMode reset;\n"
			);
			assertFalse(con.confirmUpdatesInSession());
			assertTrue(con.isSessionReadOnly());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	private class Controller
		implements ExecutionController
	{
		public int confirmStatementCalled;
		public int confirmCalled;

		Controller()
		{
			confirmStatementCalled = 0;
			confirmCalled = 0;
		}

		@Override
		public boolean confirmStatementExecution(String command)
		{
			confirmStatementCalled ++;
			return true;
		}

		@Override
		public boolean confirmExecution(String prompt, String yes, String no)
		{
			confirmCalled ++;
			return true;
		}

		@Override
		public String getPassword(String prompt)
		{
			return "";
		}

		@Override
		public String getInput(String prompt)
		{
			return "";
		}

	}
}
