/*
 * DefaultStatementRunner.java
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.interfaces.ParameterPrompter;
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
import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.interfaces.ExecutionController;
import workbench.sql.wbcommands.WbFeedback;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DefaultStatementRunner
	implements PropertyChangeListener, StatementRunner
{
	private WbConnection dbConnection;
	private StatementRunnerResult result;
	private VariablePool parameterPool;

	private HashMap cmdDispatch;
	private ArrayList dbSpecificCommands;

	private SqlCommand currentCommand;
	private SqlCommand currentConsumer;
	private String baseDir;
	
	private RowActionMonitor rowMonitor;
	private ExecutionController controller;
	private WbStartBatch batchCommand;
	private ResultLogger resultLogger;
	private boolean verboseLogging;
	private boolean supportsSelectInto = false;
	private boolean removeComments;
	private boolean fullErrorReporting = false;
	private ParameterPrompter prompter;
	//private boolean logStatements = Settings.getInstance().getBoolProperty("workbench.sql.execution.log", false);
	private boolean removeNewLines = false;
	
	public DefaultStatementRunner()
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
		
		sql = new WbSchemaDiff();
		cmdDispatch.put(sql.getVerb(), sql);
		cmdDispatch.put("WBDIFF", sql);

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

	public void setFullErrorReporting(boolean flag) { this.fullErrorReporting = flag; }
	
	public void setExecutionController(ExecutionController control)
	{
		this.controller = control;
	}

	/**
	 * For testing purposes, to that non-default commands can be added 
	 * during a JUnit test
	 */
	public void addCommand(SqlCommand command)
	{
		cmdDispatch.put(command.getVerb(), command);
	}
	
	public void setParameterPrompter(ParameterPrompter filter) { this.prompter = filter; }
	public void setBaseDir(String dir) { this.baseDir = dir; }
	public String getBaseDir() { return this.baseDir; }
	
	public WbConnection getConnection()
	{
		return this.dbConnection;
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
		this.supportsSelectInto = false;
		
		if (aConn == null) return;
		
		DbMetadata meta = this.dbConnection.getMetadata();
		
		if (meta.isOracle())
		{
			this.cmdDispatch.put(WbOraExecute.EXEC.getVerb(), WbOraExecute.EXEC);
			this.cmdDispatch.put(WbOraExecute.EXECUTE.getVerb(), WbOraExecute.EXECUTE);

			EchoCommand echo = new EchoCommand();
			this.cmdDispatch.put(echo.getVerb(), echo);

			this.dbSpecificCommands.add(WbOraExecute.EXEC.getVerb());
			this.dbSpecificCommands.add(WbOraExecute.EXECUTE.getVerb());
			this.dbSpecificCommands.add(echo.getVerb());
		}
		else if (meta.isSqlServer() || meta.isMySql() || meta.supportsCatalogs())
		{
			UseCommand cmd = new UseCommand();
			this.cmdDispatch.put(cmd.getVerb(), cmd);
			this.dbSpecificCommands.add(cmd.getVerb());
		}
		else if (meta.isFirebird())
		{
			this.cmdDispatch.put(DdlCommand.RECREATE.getVerb(), DdlCommand.RECREATE);
			this.cmdDispatch.put(WbInclude.INCLUDE_FB.getVerb(), WbInclude.INCLUDE_FB);
			this.dbSpecificCommands.add(WbInclude.INCLUDE_FB.getVerb());
			this.dbSpecificCommands.add(DdlCommand.RECREATE.getVerb());
		}

		String verbs = Settings.getInstance().getProperty("workbench.db.ignore." + meta.getDbId(), "");
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
		this.supportsSelectInto = meta.supportsSelectIntoNewTable();
		this.removeComments = dbConnection.getProfile().getRemoveComments();
		this.removeNewLines = Settings.getInstance().getBoolProperty("workbench.db." + meta.getDbId() + ".removenewlines", false);
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

	public void runStatement(String aSql, int maxRows, int queryTimeout)
		throws SQLException, Exception
	{
		if (this.result != null)
		{
			this.result.clear();
		}
        
		if (this.prompter != null)
		{
			boolean goOn = this.prompter.processParameterPrompts(aSql);
			if (!goOn)
			{
				this.result = new StatementRunnerResult();
				this.result.setPromptingWasCancelled();
				return;
			}				
		}
		
		if (removeComments || removeNewLines)
		{
			aSql = SqlUtil.makeCleanSql(aSql, !removeNewLines, !removeComments, '\'');
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
		this.currentCommand.setParameterPrompter(this.prompter);

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
				this.result = new StatementRunnerResult();
				String msg = ResourceMgr.getString("MsgStatementCancelled");
				this.result.addMessage(msg);
				this.result.setWarning(true);
				this.result.setSuccess();
				return;
			}
		}
		
		long sqlExecStart = System.currentTimeMillis();
		boolean oldReporting = this.currentCommand.getFullErrorReporting();
		this.currentCommand.setFullErrorReporting(this.fullErrorReporting);
		this.result = this.currentCommand.execute(this.dbConnection, realSql);
		this.currentCommand.setFullErrorReporting(oldReporting);

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
		long time = (System.currentTimeMillis() - sqlExecStart);
		result.setExecutionTime(time);
	}

	/**
	 * Check for a SELECT ... INTO syntax for Informix which actually
	 * creates a table. In that case we will simply pretend it's a
	 * CREATE statement.
	 * In all other casese, the approriate SqlCommand from commanDispatch will be used
	 * This is made public in order to be accessible from a JUnit test
	 */
	public SqlCommand getCommandToUse(String sql)
	{
		String verb = SqlUtil.getSqlVerb(sql);
		if (this.supportsSelectInto && !verb.equalsIgnoreCase(WbSelectBlob.VERB) && this.dbConnection != null && this.dbConnection.getMetadata().isSelectIntoNewTable(sql))
		{
			LogMgr.logDebug("StatementRunner.getCommandToUse()", "Found 'SELECT ... INTO new_table'");
			// use the generic SqlCommand implementation for this and not the SelectCommand
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
			if (this.currentConsumer != null)
			{
				this.currentConsumer.cancel();
			}
			else if (this.currentCommand != null)
			{
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
