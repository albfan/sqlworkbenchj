/*
 * StatementRunnerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.sql.ResultSet;
import java.sql.Statement;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.ExecutionController;
import workbench.sql.commands.DdlCommand;
import workbench.sql.commands.UpdatingCommand;
import workbench.sql.wbcommands.WbCopy;
import workbench.sql.wbcommands.WbDefineVar;
import workbench.sql.wbcommands.WbFeedback;
import workbench.sql.wbcommands.WbInclude;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class StatementRunnerTest 
	extends TestCase
{
	private TestUtil util;
	private boolean confirmExecution = false;
	private boolean controllerCalled = false;
	
	public StatementRunnerTest(String testName)
	{
		super(testName);
		util = new TestUtil(testName);
	}

	public void testReadOnly()
	{
		try
		{
			util.prepareEnvironment();
			
			WbConnection con = util.getConnection("readOnlyTest");
			StatementRunner runner = new StatementRunner();
			runner.setBaseDir(util.getBaseDir());
			runner.setConnection(con);
			con.getProfile().setReadOnly(false);
			
			StatementRunnerResult result = null;
			
			runner.runStatement("create table read_only_test (id integer, data varchar(100))", -1, -1);
			result = runner.getResult();
			assertTrue(result.isSuccess());
			
			runner.runStatement("insert into read_only_test (id, data) values (1, 'test')", -1, -1);
			result = runner.getResult();
			assertTrue(result.isSuccess());
			
			runner.runStatement("commit", -1, -1);
			result = runner.getResult();
			assertTrue(result.isSuccess());
			
			boolean exists = con.getMetadata().tableExists(new TableIdentifier("read_only_test"));
			assertTrue(exists);
			
			con.getProfile().setReadOnly(true);
			runner.runStatement("insert into read_only_test (id, data) values (2, 'test')", -1, -1);
			result = runner.getResult();
			assertTrue(result.hasWarning());
			
			runner.runStatement("commit", -1, -1);
			result = runner.getResult();
			assertTrue(result.hasWarning());
			
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from read_only_test");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			SqlUtil.closeResult(rs);
			assertEquals(1, count);
			
			ExecutionController controller = new ExecutionController() 
			{

				public boolean confirmExecution(String command)
				{
					controllerCalled = true;
					return confirmExecution;
				}
			};
			
			con.getProfile().setReadOnly(false);
			con.getProfile().setConfirmUpdates(true);
			runner.setExecutionController(controller);
			
			controllerCalled = false;
			runner.runStatement("select count(*) from read_only_test", -1, -1);
			result = runner.getResult();
			assertFalse(controllerCalled);
			assertTrue(result.isSuccess());
			assertTrue(result.hasDataStores());
			DataStore ds = result.getDataStores().get(0);
			assertEquals(1, ds.getRowCount());
			
			controllerCalled = false;
			confirmExecution = true;
			runner.runStatement("insert into read_only_test (id, data) values (2, 'test')", -1, -1);
			result = runner.getResult();
			assertTrue(controllerCalled);
			assertTrue(result.isSuccess());
			
			controllerCalled = false;
			confirmExecution = false;
			runner.runStatement("insert into read_only_test (id, data) values (3, 'test')", -1, -1);
			result = runner.getResult();
			assertTrue(controllerCalled);
			assertTrue(result.hasWarning());
		
			rs = stmt.executeQuery("select count(*) from read_only_test");
			count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			SqlUtil.closeResult(rs);
			assertEquals(2, count);
			
			SqlUtil.closeStatement(stmt);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	
	
	public void testWbCommands()
		throws Exception
	{
		try
		{
			util.prepareEnvironment();
			StatementRunner runner = util.createConnectedStatementRunner();
			WbConnection con = runner.getConnection();

			runner.setVerboseLogging(true);
			
			String sql = "--comment\n\nwbfeedback off";
			SqlCommand command = runner.cmdMapper.getCommandToUse(sql);
			assertTrue(command instanceof WbFeedback);
			runner.runStatement(sql, -1, -1);

			boolean verbose = runner.getVerboseLogging();
			assertEquals("Feedback not executed", false, verbose);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	
	/**
	 * Test of getCommandToUse method, of class workbench.sql.StatementRunner.
	 */
	public void testCommands() throws Exception
	{
		String sql = "\n\ninsert into bla (col) values (1)";
		StatementRunner runner = new StatementRunner();
		SqlCommand command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, UpdatingCommand.INSERT);
		
		sql = "--do something\nupdate bla set col = value";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, UpdatingCommand.UPDATE);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "  delete from bla";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, UpdatingCommand.DELETE);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "  create table bla (col integer);";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, DdlCommand.CREATE);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "-- comment\n\n\ncreate view bla as select * from blub;";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, DdlCommand.CREATE);

		sql = "-- comment\n\n\ncreate \nor \nreplace \nview bla as select * from blub;";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, DdlCommand.CREATE);

		sql = "-- comment\n\n\ncreate trigger bla;";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, DdlCommand.CREATE);
		
		sql = "  drop table bla (col integer);";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, DdlCommand.DROP);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "/* this is \n a comment \n*/\n-- comment\nalter table bla drop constraint xyz;";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, DdlCommand.ALTER);
		assertEquals(true, command.isUpdatingCommand());
		
		boolean isDrop = ((DdlCommand)command).isDropCommand(sql);
		assertEquals(true, isDrop);
		
		sql = "  -- comment\n   wbvardefine x=42;";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, WbDefineVar.DEFINE_LONG);
		assertEquals(false, command.isUpdatingCommand());

		sql = "-- bla\nwbvardef x=42;";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertSame(command, WbDefineVar.DEFINE_SHORT);
		
		sql = "   -- comment\nwbcopy -sourceprofile=x";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertTrue(command instanceof WbCopy);
		
		sql = "@file.sql";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertTrue(command instanceof WbInclude);
		
		sql = "-- run the second script\n/* bla blub */\nwbinclude -file=file.sql";
		command = runner.cmdMapper.getCommandToUse(sql);
		assertTrue(command instanceof WbInclude);
	}
}
