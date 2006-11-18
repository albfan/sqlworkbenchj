/*
 * DefaultStatementRunnerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import junit.framework.*;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.commands.DdlCommand;
import workbench.sql.commands.UpdatingCommand;
import workbench.sql.wbcommands.WbCopy;
import workbench.sql.wbcommands.WbDefineVar;
import workbench.sql.wbcommands.WbFeedback;
import workbench.sql.wbcommands.WbInclude;

/**
 *
 * @author support@sql-workbench.net
 */
public class DefaultStatementRunnerTest 
	extends TestCase
{
	private TestUtil util;
	public DefaultStatementRunnerTest(String testName)
	{
		super(testName);
		util = new TestUtil();
	}

	public void testWbCommands()
		throws Exception
	{
		try
		{
			util.prepareEnvironment();
			DefaultStatementRunner runner = util.createConnectedStatementRunner();
			WbConnection con = runner.getConnection();

			runner.setVerboseLogging(true);
			
			String sql = "--comment\n\nwbfeedback off";
			SqlCommand command = runner.getCommandToUse(sql);
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
	 * Test of getCommandToUse method, of class workbench.sql.DefaultStatementRunner.
	 */
	public void testCommands() throws Exception
	{
		String sql = "\n\ninsert into bla (col) values (1)";
		DefaultStatementRunner runner = new DefaultStatementRunner();
		SqlCommand command = runner.getCommandToUse(sql);
		assertSame(command, UpdatingCommand.INSERT);
		
		sql = "--do something\nupdate bla set col = value";
		command = runner.getCommandToUse(sql);
		assertSame(command, UpdatingCommand.UPDATE);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "  delete from bla";
		command = runner.getCommandToUse(sql);
		assertSame(command, UpdatingCommand.DELETE);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "  create table bla (col integer);";
		command = runner.getCommandToUse(sql);
		assertSame(command, DdlCommand.CREATE);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "-- comment\n\n\ncreate view bla as select * from blub;";
		command = runner.getCommandToUse(sql);
		assertSame(command, DdlCommand.CREATE);

		sql = "-- comment\n\n\ncreate \nor \nreplace \nview bla as select * from blub;";
		command = runner.getCommandToUse(sql);
		assertSame(command, DdlCommand.CREATE);

		sql = "-- comment\n\n\ncreate trigger bla;";
		command = runner.getCommandToUse(sql);
		assertSame(command, DdlCommand.CREATE);
		
		sql = "  drop table bla (col integer);";
		command = runner.getCommandToUse(sql);
		assertSame(command, DdlCommand.DROP);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "/* this is \n a comment \n*/\n-- comment\nalter table bla drop constraint xyz;";
		command = runner.getCommandToUse(sql);
		assertSame(command, DdlCommand.ALTER);
		assertEquals(true, command.isUpdatingCommand());
		
		boolean isDrop = ((DdlCommand)command).isDropCommand(sql);
		assertEquals(true, isDrop);
		
		sql = "  -- comment\n   wbvardefine x=42;";
		command = runner.getCommandToUse(sql);
		assertSame(command, WbDefineVar.DEFINE_LONG);
		assertEquals(false, command.isUpdatingCommand());

		sql = "-- bla\nwbvardef x=42;";
		command = runner.getCommandToUse(sql);
		assertSame(command, WbDefineVar.DEFINE_SHORT);
		
		sql = "   -- comment\nwbcopy -sourceprofile=x";
		command = runner.getCommandToUse(sql);
		assertTrue(command instanceof WbCopy);
		
		sql = "@file.sql";
		command = runner.getCommandToUse(sql);
		assertTrue(command instanceof WbInclude);
		
		sql = "-- run the second script\n/* bla blub */\nwbinclude -file=file.sql";
		command = runner.getCommandToUse(sql);
		assertTrue(command instanceof WbInclude);
	}
}
