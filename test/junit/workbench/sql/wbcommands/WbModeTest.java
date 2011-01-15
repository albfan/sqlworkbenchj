/*
 * WbModeTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.interfaces.ExecutionController;
import workbench.sql.BatchRunner;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

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
			runner.executeScript(
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

			runner.executeScript(
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

			runner.executeScript(
				"WbMode confirm;\n"
			);
			assertTrue(con.getProfile().confirmUpdatesInSession());
			assertFalse(con.getProfile().readOnlySession());

			runner.executeScript(
				"WbMode normal;\n"
			);
			assertFalse(con.getProfile().confirmUpdatesInSession());
			assertFalse(con.getProfile().readOnlySession());

			runner.executeScript(
				"WbMode reset;\n"
			);
			assertFalse(con.getProfile().confirmUpdatesInSession());
			assertTrue(con.getProfile().readOnlySession());
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

		public Controller()
		{
			confirmStatementCalled = 0;
			confirmCalled = 0;
		}

		public boolean confirmStatementExecution(String command)
		{
			confirmStatementCalled ++;
			return true;
		}

		public boolean confirmExecution(String prompt)
		{
			confirmCalled ++;
			return true;
		}

		public String getPassword(String prompt)
		{
			return "";
		}
	}
}
