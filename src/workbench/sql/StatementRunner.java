/*
 * StatementRunner.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.interfaces.ResultLogger;
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
import workbench.sql.wbcommands.WbDefineVar;
import workbench.sql.wbcommands.WbDescribeTable;
import workbench.sql.wbcommands.WbDiff;
import workbench.sql.wbcommands.WbDisableOraOutput;
import workbench.sql.wbcommands.WbEnableOraOutput;
import workbench.sql.wbcommands.WbEndBatch;
import workbench.sql.wbcommands.WbExport;
import workbench.sql.wbcommands.WbHelp;
import workbench.sql.wbcommands.WbImport;
import workbench.sql.wbcommands.WbInclude;
import workbench.sql.wbcommands.WbListCatalogs;
import workbench.sql.wbcommands.WbListProcedures;
import workbench.sql.wbcommands.WbListTables;
import workbench.sql.wbcommands.WbListVars;
import workbench.sql.wbcommands.WbOraExecute;
import workbench.sql.wbcommands.WbRemoveVar;
import workbench.sql.wbcommands.WbSchemaReport;
import workbench.sql.wbcommands.WbStartBatch;
import workbench.sql.wbcommands.WbXslt;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.interfaces.ExecutionController;
import workbench.sql.wbcommands.WbFeedback;

/**
 *
 * @author  info@sql-workbench.net
 */
public class StatementRunner
	implements PropertyChangeListener
{
	private WbConnection dbConnection;
	private StatementRunnerResult result;
	private VariablePool parameterPool;

	private HashMap cmdDispatch;
	private ArrayList dbSpecificCommands;

	private SqlCommand currentCommand;
	private SqlCommand currentConsumer;

	private int maxRows;
	private boolean isCancelled;
	private boolean batchMode = false;

	private RowActionMonitor rowMonitor;
	private ExecutionController controller;
	private WbStartBatch batchCommand;
	private ResultLogger resultLogger;
	private boolean verboseLogging;
	private boolean supportsSelectInto = false;
	
	public StatementRunner()
	{
		this.verboseLogging = !Settings.getInstance().getConsolidateLogMsg();
		Settings.getInstance().addPropertyChangeListener(this);

		cmdDispatch = new HashMap();
		cmdDispatch.put("*", new SqlCommand());

		SqlCommand sql = new WbListTables();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbHelp();
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
		cmdDispatch.put( WbDefineVar.DEFINE_SHORT.getVerb(), WbDefineVar.DEFINE_SHORT);

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
		
		sql = new WbDiff();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new SetCommand();
		cmdDispatch.put(sql.getVerb(), sql);

		sql = new WbFeedback();
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

		for (int i=0; i < DdlCommand.DDL_COMMANDS.size(); i ++)
		{
			sql = (SqlCommand)DdlCommand.DDL_COMMANDS.get(i);
			cmdDispatch.put(sql.getVerb(), sql);
		}

		this.dbSpecificCommands = new ArrayList();
		this.parameterPool = VariablePool.getInstance();
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if ("workbench.gui.log.consolidate".equals(evt.getPropertyName()))
		{
			this.verboseLogging = !Settings.getInstance().getConsolidateLogMsg();
		}
	}

	public void setExecutionController(ExecutionController control)
	{
		this.controller = control;
	}

	public void setConnection(WbConnection aConn)
	{

		if (this.dbConnection != null)
		{
			int count = this.dbSpecificCommands.size();
			for (int i=0; i < count; i++)
			{
				this.cmdDispatch.remove(this.dbSpecificCommands.get(i));
			}
			this.dbSpecificCommands.clear();
		}

		this.dbConnection = aConn;
		DbMetadata meta = this.dbConnection.getMetadata();
		if (meta.isOracle())
		{
			this.cmdDispatch.put(WbOraExecute.EXEC.getVerb(), WbOraExecute.EXEC);
			this.cmdDispatch.put(WbOraExecute.EXECUTE.getVerb(), WbOraExecute.EXECUTE);

			EchoCommand echo = new EchoCommand();
			this.cmdDispatch.put(echo.getVerb(), echo);

			//this.dbSpecificCommands.add(set.getVerb());
			this.dbSpecificCommands.add(WbOraExecute.EXEC.getVerb());
			this.dbSpecificCommands.add(WbOraExecute.EXECUTE.getVerb());
			this.dbSpecificCommands.add(echo.getVerb());
		}
		else if (meta.isSqlServer())
		{
			UseCommand cmd = new UseCommand();
			this.cmdDispatch.put(cmd.getVerb(), cmd);
			this.dbSpecificCommands.add(cmd.getVerb());
		}
		else if (meta.isFirebird())
		{
			this.cmdDispatch.put(WbInclude.INCLUDE_FB.getVerb(), WbInclude.INCLUDE_FB);
			this.dbSpecificCommands.add(WbInclude.INCLUDE_FB.getVerb());
		}

		/*
		if (!meta.isPostgres())
		{
			// for non-PostgreSQL connections we can use the
			// COPY command. For PGSQL we cannot use the verb COPY, as
			// PGSQL has it's own COPY command. Oracle's COPY command
			// is a SQL*Plus command and cannot be used through JDBC,
			// so we do not need to take care of that
			SqlCommand copy = (SqlCommand)this.cmdDispatch.get(WbCopy.VERB);
			this.cmdDispatch.put("COPY", copy);
			this.dbSpecificCommands.add("COPY");
		}
		*/

		String verbs = meta.getVerbsToIgnore();
		List l = StringUtil.stringToList(verbs, ",");
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
		this.supportsSelectInto = meta.supportsSelectIntoNewTable();
	}

	public StatementRunnerResult getResult()
	{
		return this.result;
	}

	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}

	public void setResultLogger(ResultLogger logger)
	{
		this.resultLogger = logger;
	}

	public void runStatement(String aSql, int maxRows)
		throws SQLException, Exception
	{
		this.runStatement(aSql, maxRows, 0);
	}
	
	public void runStatement(String aSql, int maxRows, int queryTimeout)
		throws SQLException, Exception
	{
		// Silently ignore empty statements
		if (aSql == null || aSql.trim().length() == 0)
		{
			this.result = new StatementRunnerResult("");
			this.result.clear();
			this.result.setSuccess();
			return;
		}

		this.currentCommand = this.getCommandToUse(aSql);

		// if no mapping is found use the default implementation
		if (this.currentCommand == null)
		{
			this.currentCommand = (SqlCommand)this.cmdDispatch.get("*");
		}

		this.currentCommand.setConsumerWaiting(this.currentConsumer != null);
		this.currentCommand.setStatementRunner(this);
		this.currentCommand.setRowMonitor(this.rowMonitor);
		this.currentCommand.setResultLogger(this.resultLogger);
		this.currentCommand.setMaxRows(maxRows);
		this.currentCommand.setQueryTimeout(queryTimeout);
		this.currentCommand.setConnection(this.dbConnection);

		String realSql = aSql;
		if (parameterPool.getParameterCount() > 0)
		{
			 realSql = parameterPool.replaceAllParameters(aSql);
		}

		if (this.controller != null && this.currentCommand.isUpdatingCommand())
		{
			boolean doExecute = this.controller.confirmExecution(realSql);
			if (!doExecute)
			{
				this.result = new StatementRunnerResult(realSql);
				String msg = ResourceMgr.getString("MsgStatementCancelled");
				this.result.addMessage(msg);
				this.result.setWarning(true);
				this.result.setSuccess();
				return;
			}
		}
		this.result = this.currentCommand.execute(this.dbConnection, realSql);

		if (this.currentCommand instanceof WbStartBatch && result.isSuccess())
		{
			this.batchCommand = (WbStartBatch)this.currentCommand;
		}
		else if (this.batchCommand != null && this.currentCommand instanceof WbEndBatch)
		{
			this.result = this.batchCommand.executeBatch();
		}
		else if (this.currentCommand != null && this.currentCommand.isResultSetConsumer())
		{
			this.currentConsumer = this.currentCommand;
		}
		else
		{
			if (this.currentConsumer != null)
			{
				this.currentCommand.setConsumerWaiting(false);
				this.currentConsumer.consumeResult(this.result);
				this.currentConsumer = null;
			}
		}
	}

	/**
	 *	Check for a SELECT ... INTO syntax for Informix which actually
	 *  creates a table. In that case we will simply pretend it's a
	 *  CREATE statement.
	 *	In all other casese, the approriate SqlCommand from commanDispatch will be used
	 */
	private SqlCommand getCommandToUse(String sql)
	{
		String verb = SqlUtil.getSqlVerb(sql);
		if (this.supportsSelectInto && this.dbConnection.getMetadata().isSelectIntoNewTable(sql))
		{
			LogMgr.logDebug("StatementRunner.getCommandToUse()", "Found 'SELECT ... INTO new_table'");
			// use the generic SqlCommand implementation for this.
			return (SqlCommand)this.cmdDispatch.get("*");
		}
		else
		{
			return (SqlCommand)this.cmdDispatch.get(verb);
		}
	}

	public void setVerboseLogging(boolean flag)
	{
		this.verboseLogging = flag;
	}

	public boolean getVerboseLogging()
	{
		return this.verboseLogging;
	}

	public void statementDone()
	{
		if (this.currentCommand != null && this.currentConsumer != this.currentCommand)
		{
			this.currentCommand.done();
			this.currentCommand = null;
		}
	}

	public void cancel()
	{
		try
		{
			if (this.currentCommand != null)
			{
				this.isCancelled = true;
				this.currentCommand.cancel();
			}
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("StatementRunner.cancel()", "Error when cancelling statement", th);
		}
	}

	public void done()
	{
		if (this.result != null) this.result.clear();
		this.result = null;
		this.statementDone();
		this.currentConsumer = null;
		this.dbConnection.clearWarnings();
	}

}
