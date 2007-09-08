/*
 * CommandMapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.commands.AlterSessionCommand;
import workbench.sql.commands.DdlCommand;
import workbench.sql.commands.IgnoredCommand;
import workbench.sql.commands.SelectCommand;
import workbench.sql.commands.SetCommand;
import workbench.sql.commands.SingleVerbCommand;
import workbench.sql.commands.UpdatingCommand;
import workbench.sql.commands.UseCommand;
import workbench.sql.wbcommands.WbCall;
import workbench.sql.wbcommands.WbConfirm;
import workbench.sql.wbcommands.WbCopy;
import workbench.sql.wbcommands.WbDefinePk;
import workbench.sql.wbcommands.WbDefineVar;
import workbench.sql.wbcommands.WbDescribeTable;
import workbench.sql.wbcommands.WbDisableOraOutput;
import workbench.sql.wbcommands.WbEnableOraOutput;
import workbench.sql.wbcommands.WbEndBatch;
import workbench.sql.wbcommands.WbExport;
import workbench.sql.wbcommands.WbFeedback;
import workbench.sql.wbcommands.WbImport;
import workbench.sql.wbcommands.WbInclude;
import workbench.sql.wbcommands.WbListCatalogs;
import workbench.sql.wbcommands.WbListPkDef;
import workbench.sql.wbcommands.WbListProcedures;
import workbench.sql.wbcommands.WbListTables;
import workbench.sql.wbcommands.WbListVars;
import workbench.sql.wbcommands.WbLoadPkMapping;
import workbench.sql.wbcommands.WbRemoveVar;
import workbench.sql.wbcommands.WbSavePkMapping;
import workbench.sql.wbcommands.WbSchemaDiff;
import workbench.sql.wbcommands.WbSchemaReport;
import workbench.sql.wbcommands.WbSelectBlob;
import workbench.sql.wbcommands.WbStartBatch;
import workbench.sql.wbcommands.WbXslt;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class CommandMapper
{
	private HashMap<String, SqlCommand> cmdDispatch;
	private List<String> dbSpecificCommands;
	private boolean supportsSelectInto = false;
	private DbMetadata metaData;
	
	public CommandMapper()
	{
		cmdDispatch = new HashMap<String, SqlCommand>();
		cmdDispatch.put("*", new SqlCommand());

		SqlCommand sql = new WbListTables();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbListProcedures();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbDescribeTable();
		cmdDispatch.put(sql.getVerb(), sql);
		cmdDispatch.put("DESCRIBE", sql);

		sql = new WbEnableOraOutput();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbDisableOraOutput();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbStartBatch();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbEndBatch();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new SelectCommand();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbXslt();
		cmdDispatch.put(sql.getVerb(), sql);
		cmdDispatch.put("XSLT", sql);

		cmdDispatch.put(WbDefineVar.DEFINE_LONG.getVerb(), WbDefineVar.DEFINE_LONG);
		cmdDispatch.put(WbDefineVar.DEFINE_SHORT.getVerb(), WbDefineVar.DEFINE_SHORT);

		sql = new WbRemoveVar();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbListVars();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbExport();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbImport();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbCopy();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbSchemaReport();
		cmdDispatch.put(sql.getVerb(), sql);
		
		sql = new WbSchemaDiff();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new SetCommand();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbFeedback();
		cmdDispatch.put(sql.getVerb(), sql);
		
		sql = new WbDefinePk();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbListPkDef();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbLoadPkMapping();
		cmdDispatch.put(sql.getVerb(), sql);
		
		sql = new WbSavePkMapping();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbConfirm();
		cmdDispatch.put(sql.getVerb(), sql);
		
		sql = new WbCall();
		cmdDispatch.put(sql.getVerb(), sql);
		
		cmdDispatch.put(WbInclude.INCLUDE_LONG.getVerb(), WbInclude.INCLUDE_LONG);
		cmdDispatch.put(WbInclude.INCLUDE_SHORT.getVerb(), WbInclude.INCLUDE_SHORT);

		cmdDispatch.put(WbListCatalogs.LISTCAT.getVerb(), WbListCatalogs.LISTCAT);
		cmdDispatch.put(WbListCatalogs.LISTDB.getVerb(), WbListCatalogs.LISTDB);

		cmdDispatch.put(SingleVerbCommand.COMMIT.getVerb(), SingleVerbCommand.COMMIT);
		cmdDispatch.put(SingleVerbCommand.ROLLBACK.getVerb(), SingleVerbCommand.ROLLBACK);

		cmdDispatch.put(UpdatingCommand.DELETE.getVerb(), UpdatingCommand.DELETE);
		cmdDispatch.put(UpdatingCommand.INSERT.getVerb(), UpdatingCommand.INSERT);
		cmdDispatch.put(UpdatingCommand.UPDATE.getVerb(), UpdatingCommand.UPDATE);
		cmdDispatch.put(UpdatingCommand.TRUNCATE.getVerb(), UpdatingCommand.TRUNCATE);
		
		cmdDispatch.put(WbSelectBlob.VERB, new WbSelectBlob());
		
		for (DdlCommand cmd : DdlCommand.DDL_COMMANDS)
		{
			cmdDispatch.put(cmd.getVerb(), cmd);
		}
		this.cmdDispatch.put("CREATE OR REPLACE", DdlCommand.CREATE);

		this.dbSpecificCommands = new LinkedList<String>();
	}
	
	/**
	 * For testing purposes, to that non-default commands can be added 
	 * during a JUnit test
	 */
	public void addCommand(SqlCommand command)
	{
		cmdDispatch.put(command.getVerb(), command);
	}
	
	/**
	 * Initialize the CommandMapper with a database connection. 
	 * This will add DBMS specific commands to the internal dispatch.
	 * 
	 * This method can be called multiple times.
	 */
	public void setConnection(WbConnection aConn)
	{
		for (String cmd : dbSpecificCommands)
		{
			this.cmdDispatch.remove(cmd);
		}
		this.dbSpecificCommands.clear();
		this.supportsSelectInto = false;
		
		if (aConn == null) return;
		
		this.metaData = aConn.getMetadata();
		if (metaData == null)
		{
			LogMgr.logError("CommandMapper.setConnection()","Received connection without metaData!", null);
		}
		
		if (metaData.isOracle())
		{
			AlterSessionCommand alter = new AlterSessionCommand();
			SqlCommand wbcall = this.cmdDispatch.get(WbCall.VERB);
			
			this.cmdDispatch.put(WbCall.EXEC_VERB_LONG, wbcall);
			this.cmdDispatch.put(WbCall.EXEC_VERB_SHORT, wbcall);
			this.cmdDispatch.put(alter.getVerb(), alter);
			
			WbFeedback echo = new WbFeedback("ECHO");
			this.cmdDispatch.put(echo.getVerb(), echo);

			this.dbSpecificCommands.add(alter.getVerb());
			this.dbSpecificCommands.add(WbCall.EXEC_VERB_LONG);
			this.dbSpecificCommands.add(WbCall.EXEC_VERB_SHORT);
			this.dbSpecificCommands.add(echo.getVerb());
		}
		else if (metaData.isSqlServer() || metaData.isMySql() || metaData.supportsCatalogs())
		{
			UseCommand cmd = new UseCommand();
			this.cmdDispatch.put(cmd.getVerb(), cmd);
			this.dbSpecificCommands.add(cmd.getVerb());
		}
		else if (metaData.isFirebird())
		{
			this.cmdDispatch.put(DdlCommand.RECREATE.getVerb(), DdlCommand.RECREATE);
			this.cmdDispatch.put(WbInclude.INCLUDE_FB.getVerb(), WbInclude.INCLUDE_FB);
			this.dbSpecificCommands.add(WbInclude.INCLUDE_FB.getVerb());
			this.dbSpecificCommands.add(DdlCommand.RECREATE.getVerb());
		}
		
		if (metaData.getDbSettings().useWbProcedureCall())
		{
			SqlCommand wbcall = this.cmdDispatch.get(WbCall.VERB);
			this.cmdDispatch.put("CALL", wbcall);
			this.dbSpecificCommands.add("CALL");
		}

		String verbs = Settings.getInstance().getProperty("workbench.db.ignore." + metaData.getDbId(), "");
		List l = StringUtil.stringToList(verbs, ",", true, true);
		for (int i=0; i < l.size(); i++)
		{
			String verb = (String)l.get(i);
			if (verb == null) continue;
			verb = verb.toUpperCase();
			IgnoredCommand cmd = new IgnoredCommand(verb);
			this.cmdDispatch.put(verb, cmd);
			this.dbSpecificCommands.add(verb);
		}

		// this is stored in an instance variable for performance
		// reasons, so we can skip the call to isSelectIntoNewTable() in 
		// getCommandToUse()
		// For a single call this doesn't matter, but when executing 
		// huge scripts the repeated call to getCommandToUse should
		// be as quick as possible
		this.supportsSelectInto = metaData.supportsSelectIntoNewTable();
	}	
	
	/**
	 * Check for a SELECT ... INTO syntax for Informix which actually
	 * creates a table. In that case we will simply pretend it's a
	 * CREATE statement.
	 * In all other casese, the approriate SqlCommand from commanDispatch will be used
	 * This is made public in order to be accessible from a JUnit test
	 * 
	 * @param sql the statement to be executed
	 * @return the instance of SqlCommand to be used to run the sql, or null if the 
	 * given sql is empty or contains comments only
	 */
	public SqlCommand getCommandToUse(String sql)
	{
		String verb = SqlUtil.getSqlVerb(sql);
		if (StringUtil.isEmptyString(verb)) return null;
		
		SqlCommand cmd = null;
		
		if (this.supportsSelectInto && !verb.equalsIgnoreCase(WbSelectBlob.VERB) && this.metaData != null && this.metaData.isSelectIntoNewTable(sql))
		{
			LogMgr.logDebug("CommandMapper.getCommandToUse()", "Found 'SELECT ... INTO new_table'");
			// use the generic SqlCommand implementation for this and not the SelectCommand
			cmd = this.cmdDispatch.get("*");
		}
		else 
		{
			cmd = this.cmdDispatch.get(verb);
		}
		
		if (cmd == null)
		{
			cmd = this.cmdDispatch.get("*");
		}
		return cmd;
	}
	
}
