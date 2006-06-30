/*
 * DefaultStatementRunnerTest.java
 * JUnit based test
 *
 * Created on May 10, 2006, 10:02 PM
 */

package workbench.sql;

import junit.framework.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.interfaces.ResultLogger;
import workbench.interfaces.StatementRunner;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.commands.DdlCommand;
import workbench.sql.commands.EchoCommand;
import workbench.sql.commands.IgnoredCommand;
import workbench.sql.commands.SelectCommand;
import workbench.sql.commands.SetCommand;
import workbench.sql.commands.SingleVerbCommand;
import workbench.sql.commands.UpdatingCommand;
import workbench.sql.commands.UseCommand;
import workbench.sql.wbcommands.WbCopy;
import workbench.sql.wbcommands.WbDefinePk;
import workbench.sql.wbcommands.WbDefineVar;
import workbench.sql.wbcommands.WbDescribeTable;
import workbench.sql.wbcommands.WbSchemaDiff;
import workbench.sql.wbcommands.WbDisableOraOutput;
import workbench.sql.wbcommands.WbEnableOraOutput;
import workbench.sql.wbcommands.WbEndBatch;
import workbench.sql.wbcommands.WbExport;
import workbench.sql.wbcommands.WbHelp;
import workbench.sql.wbcommands.WbImport;
import workbench.sql.wbcommands.WbInclude;
import workbench.sql.wbcommands.WbListCatalogs;
import workbench.sql.wbcommands.WbListPkDef;
import workbench.sql.wbcommands.WbListProcedures;
import workbench.sql.wbcommands.WbListTables;
import workbench.sql.wbcommands.WbListVars;
import workbench.sql.wbcommands.WbLoadPkMapping;
import workbench.sql.wbcommands.WbOraExecute;
import workbench.sql.wbcommands.WbRemoveVar;
import workbench.sql.wbcommands.WbSavePkMapping;
import workbench.sql.wbcommands.WbSchemaReport;
import workbench.sql.wbcommands.WbSelectBlob;
import workbench.sql.wbcommands.WbStartBatch;
import workbench.sql.wbcommands.WbXslt;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.interfaces.ExecutionController;
import workbench.sql.wbcommands.WbFeedback;

/**
 *
 * @author support@sql-workbench.net
 */
public class DefaultStatementRunnerTest 
	extends TestCase
{
	public DefaultStatementRunnerTest(String testName)
	{
		super(testName);
	}

	/**
	 * Test of getCommandToUse method, of class workbench.sql.DefaultStatementRunner.
	 */
	public void testRunner() throws Exception
	{
		String sql = "insert into bla (col) values (1)";
		DefaultStatementRunner runner = new DefaultStatementRunner();
		SqlCommand command = runner.getCommandToUse(sql);
		assertSame(command, UpdatingCommand.INSERT);
		
		sql = "  update bla set col = value";
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
		
		sql = "  drop table bla (col integer);";
		command = runner.getCommandToUse(sql);
		assertSame(command, DdlCommand.DROP);
		assertEquals(true, command.isUpdatingCommand());
		
		sql = "  alter table bla drop constraint xyz;";
		command = runner.getCommandToUse(sql);
		assertSame(command, DdlCommand.ALTER);
		assertEquals(true, command.isUpdatingCommand());
		
		boolean isDrop = ((DdlCommand)command).isDropCommand(sql);
		assertEquals(true, isDrop);
		
		sql = "  wbvardefine x=42;";
		command = runner.getCommandToUse(sql);
		assertSame(command, WbDefineVar.DEFINE_LONG);
		assertEquals(false, command.isUpdatingCommand());

		sql = "-- bla\nwbvardef x=42;";
		command = runner.getCommandToUse(sql);
		assertSame(command, WbDefineVar.DEFINE_SHORT);
		
		sql = "   wbcopy -sourceprofile=x";
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
