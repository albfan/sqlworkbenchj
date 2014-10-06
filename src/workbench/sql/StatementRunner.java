/*
 * StatementRunner.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.sql;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.WbManager;
import workbench.interfaces.ExecutionController;
import workbench.interfaces.ParameterPrompter;
import workbench.interfaces.ResultLogger;
import workbench.interfaces.ResultSetConsumer;
import workbench.interfaces.SqlHistoryProvider;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionProfile;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.TransactionChecker;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;

import workbench.sql.commands.AlterSessionCommand;
import workbench.sql.commands.SetCommand;
import workbench.sql.commands.SingleVerbCommand;
import workbench.sql.wbcommands.WbEndBatch;
import workbench.sql.wbcommands.WbStartBatch;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class StatementRunner
	implements PropertyChangeListener
{
	public static final String SERVER_MSG_PROP = "server_messages";

	// used to restore the "real" connection if WbConnect changes the "current"
	// connection during script execution
	private WbConnection mainConnection;

	private WbConnection currentConnection;
	private StatementRunnerResult result;

	private SqlCommand currentCommand;
	private StatementHook statementHook = StatementHookFactory.DEFAULT_HOOK;
	private ResultSetConsumer currentConsumer;
	private String baseDir;

	private RowActionMonitor rowMonitor;
	private ExecutionController controller;
	private WbStartBatch batchCommand;
	private ResultLogger resultLogger;
	private boolean verboseLogging;
	private boolean hideWarnings;
	private ErrorReportLevel errorLevel;
	private ParameterPrompter prompter;
	private boolean ignoreDropErrors;
	protected CommandMapper cmdMapper;
	private boolean useSavepoint;
	private boolean logAllStatements;
	private OutputPrinter messageOutput;
	private boolean traceStatements;
	private Savepoint savepoint;
	private final List<PropertyChangeListener> changeListeners = new ArrayList<>();
	private int maxRows = -1;
	private int queryTimeout = -1;
	private boolean showDataLoadingProgress = true;
	private EndReadOnlyTrans endTransType;
	private TransactionChecker transactionChecker;
	private final Map<String, String> sessionAttributes = new TreeMap<>();
	private final	RemoveEmptyResultsAnnotation removeAnnotation = new RemoveEmptyResultsAnnotation();

	// The history provider is here to give SqlCommands access to the command history.
	// Currently this is only used in WbHistory to show a list of executed statements.
	private SqlHistoryProvider history;

	public StatementRunner()
	{
		verboseLogging = !Settings.getInstance().getConsolidateLogMsg();
		errorLevel = Settings.getInstance().getStatementErrorReportLevel();
		cmdMapper = new CommandMapper();
		logAllStatements = Settings.getInstance().getLogAllStatements();
		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_CONSOLIDATE_LOG_MESSAGES, Settings.PROPERTY_LOG_ALL_SQL, Settings.PROPERTY_ERROR_STATEMENT_LOG_LEVEL);
	}

	public void setHistoryProvider(SqlHistoryProvider provider)
	{
		this.history = provider;
	}

	public SqlHistoryProvider getHistoryProvider()
	{
		return this.history;
	}

	public void dispose()
	{
		Settings.getInstance().removePropertyChangeListener(this);
	}

	public void addChangeListener(PropertyChangeListener l)
	{
		this.changeListeners.add(l);
	}

	public void removeChangeListener(PropertyChangeListener l)
	{
		this.changeListeners.remove(l);
	}

	public void fireConnectionChanged()
	{
		PropertyChangeEvent evt = new PropertyChangeEvent(this, "connection", null, this.currentConnection);
		for (PropertyChangeListener l : changeListeners)
		{
			l.propertyChange(evt);
		}
	}

	public boolean getTraceStatements()
	{
		return traceStatements;
	}

	public void setTraceStatements(boolean flag)
	{
		this.traceStatements = flag;
	}

	public void setMessagePrinter(OutputPrinter output)
	{
		this.messageOutput = output;
	}

	public void setSessionProperty(String name, String value)
	{
		sessionAttributes.put(name, value);
	}

	public void removeSessionProperty(String name)
	{
		sessionAttributes.remove(name);
	}

	public String getSessionAttribute(String name)
	{
		return sessionAttributes.get(name);
	}

	public boolean getBoolSessionAttribute(String name)
	{
		String value = sessionAttributes.get(name);
		return StringUtil.stringToBool(value);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.PROPERTY_CONSOLIDATE_LOG_MESSAGES.equals(evt.getPropertyName()))
		{
			this.verboseLogging = !Settings.getInstance().getConsolidateLogMsg();
		}
		else if (Settings.PROPERTY_LOG_ALL_SQL.equals(evt.getPropertyName()))
		{
			logAllStatements = Settings.getInstance().getLogAllStatements();
		}
		else if (Settings.PROPERTY_ERROR_STATEMENT_LOG_LEVEL.equals(evt.getPropertyName()))
		{
			errorLevel = Settings.getInstance().getStatementErrorReportLevel();
		}
	}

	public void setShowDataLoadingProgress(boolean flag)
	{
		this.showDataLoadingProgress = false;
	}

	public void setErrorReportLevel(ErrorReportLevel level)
	{
		this.errorLevel = level;
	}

	public ExecutionController getExecutionController()
	{
		return this.controller;
	}

	public void setExecutionController(ExecutionController control)
	{
		this.controller = control;
	}

	public boolean getHideWarnings()
	{
		return this.hideWarnings;
	}

	public void setHideWarnings(boolean flag)
	{
		this.hideWarnings = flag;
	}

	public void setIgnoreDropErrors(boolean flag)
	{
		this.ignoreDropErrors = flag;
	}

	public boolean getIgnoreDropErrors()
	{
		return this.ignoreDropErrors;
	}

	/**
	 * For testing purposes only, so that non-default commands can be added during a JUnit test.
	 */
	public void addCommand(SqlCommand command)
	{
		cmdMapper.addCommand(command);
	}

	public Collection<String> getAllWbCommands()
	{
		return cmdMapper.getAllWbCommands();
	}

	public void setMaxRows(int rows)
	{
		this.maxRows = rows;
	}

	public void setQueryTimeout(int timeout)
	{
		this.queryTimeout = timeout;
	}

	public void setParameterPrompter(ParameterPrompter filter)
	{
		this.prompter = filter;
	}

	public void setBaseDir(String dir)
	{
		this.baseDir = dir;
	}

	public String getBaseDir()
	{
		return this.baseDir;
	}

	public WbConnection getConnection()
	{
		return this.currentConnection;
	}

	public void restoreMainConnection()
	{
		if (mainConnection != null)
		{
			this.currentConnection.disconnect();
			this.setConnection(this.mainConnection);
			this.mainConnection = null;
		}
	}

	/**
	 * Temporarily change the connection, but keep the old connection open.
	 * If changeConnection() has already been called once, the current connection
	 * is closed
	 * @param newConn
	 */
	public void changeConnection(WbConnection newConn)
	{
		if (newConn == null) return;
		if (newConn == currentConnection) return;

		if (mainConnection == null)
		{
			this.mainConnection = currentConnection;
		}
		else
		{
			this.currentConnection.disconnect();
		}
		this.setConnection(newConn);
	}

	public void setConnection(WbConnection aConn)
	{
		if (statementHook != null)
		{
			statementHook.close(aConn);
		}

		this.releaseSavepoint();
		this.cmdMapper.setConnection(aConn);
		this.currentConnection = aConn;

		fireConnectionChanged();

		if (currentConnection == null) return;

    ConnectionProfile profile = currentConnection.getProfile();
    if (profile != null)
    {
      this.ignoreDropErrors = profile.getIgnoreDropErrors();
      this.hideWarnings = profile.isHideWarnings();
    }

		DbMetadata meta = this.currentConnection.getMetadata();
		DbSettings db = (meta != null ? meta.getDbSettings() : null);
		setUseSavepoint(db == null ? false : db.useSavePointForDML());
		statementHook = StatementHookFactory.getStatementHook(this);
		sessionAttributes.clear();
		initEndReadOnlyTransaction();
	}

	private boolean shouldEndTransactionForCommand(SqlCommand command)
	{
		if (command == null) return false;
		if (command.isUpdatingCommand()) return false;
		if (command instanceof SingleVerbCommand) return false; // commit or rollback
		if (command instanceof AlterSessionCommand) return false; 
		if (command instanceof SetCommand) return false;
		if (command.isWbCommand()) return false;
		return true;
	}

	private void endReadOnlyTransaction()
	{
		if (currentConnection == null) return;
		if (currentConnection.getAutoCommit()) return;
		if (endTransType == EndReadOnlyTrans.never) return;

		if (!shouldEndTransactionForCommand(currentCommand)) return;

		if (!transactionChecker.hasUncommittedChanges(currentConnection))
		{
			LogMgr.logInfo("StatementRunner.endReadOnlyTransaction()", "Sending a " + endTransType.name() + " to end the current transaction started by: " + currentCommand);
			try
			{
				if (endTransType == EndReadOnlyTrans.commit)
				{
					currentConnection.commit();
				}
				else
				{
					currentConnection.rollback();
				}
			}
			catch (Exception ex)
			{
				LogMgr.logWarning("StatementRunner.endReadOnlyTransaction()", "Could not " + endTransType.name(), ex);
			}
		}
	}

	private void initEndReadOnlyTransaction()
	{
		if (this.currentConnection == null)
		{
			endTransType = EndReadOnlyTrans.never;
		}
		else
		{
			endTransType = currentConnection.getDbSettings().getAutoCloseReadOnlyTransactions();
		}

		if (endTransType ==  EndReadOnlyTrans.never)
		{
			transactionChecker = TransactionChecker.NO_CHECK;
		}
		else
		{
			transactionChecker = TransactionChecker.Factory.createChecker(currentConnection);
		}
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

	public SqlCommand getCommandToUse(String sql)
	{
		return this.cmdMapper.getCommandToUse(sql);
	}

	public void runStatement(String aSql)
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
				this.result = new StatementRunnerResult(aSql);
				this.result.setPromptingWasCancelled();
				return;
			}
		}

		this.currentCommand = this.cmdMapper.getCommandToUse(aSql);

		if (this.currentCommand == null)
		{
			this.result = null;
			return;
		}

		if (!this.currentCommand.isModeSupported(WbManager.getInstance().getRunMode()))
		{
			result = new StatementRunnerResult();
			result.setSuccess();
			LogMgr.logWarning("StatementRunner.runStatement()", currentCommand.getVerb() + " not supported in mode " + WbManager.getInstance().getRunMode().toString() + ". The statement has been ignored.");
			return;
		}

		if (this.currentConnection == null && this.currentCommand.isConnectionRequired())
		{
			String verb = SqlUtil.getSqlVerb(aSql);
			throw new SQLException("Cannot execute command '" + verb + "' without a connection!");
		}

		this.currentCommand.setStatementRunner(this);
		this.currentCommand.setRowMonitor(this.rowMonitor);
		this.currentCommand.setResultLogger(this.resultLogger);
		if (currentConsumer != null && currentConsumer.ignoreMaxRows())
		{
			this.currentCommand.setMaxRows(0);
		}
		else
		{
			this.currentCommand.setMaxRows(maxRows);
		}
		this.currentCommand.setQueryTimeout(queryTimeout);
		this.currentCommand.setConnection(this.currentConnection);
		this.currentCommand.setParameterPrompter(this.prompter);
		this.currentCommand.setErrorReportLevel(errorLevel);
		this.currentCommand.setShowDataLoading(this.showDataLoadingProgress);

		String realSql = aSql;
		if (VariablePool.getInstance().getParameterCount() > 0)
		{
			realSql = VariablePool.getInstance().replaceAllParameters(aSql);
		}

		if (!currentCommand.isModificationAllowed(currentConnection, realSql))
		{
			ConnectionProfile target = currentCommand.getModificationTarget(currentConnection, aSql);
			String profileName = (target == null ? "" : target.getName());
			this.result = new StatementRunnerResult();
			String verb = SqlUtil.getSqlVerb(aSql);
			String msg = ResourceMgr.getFormattedString("MsgReadOnlyMode", profileName, verb);
			LogMgr.logWarning("DefaultStatementRunner.runStatement()", "Statement " + verb + " ignored because connection is set to read only!");
			this.result.addMessage(msg);
			this.result.setWarning(true);
			this.result.setSuccess();
			return;
		}

		if (controller != null && currentCommand.needConfirmation(currentConnection, realSql))
		{
			boolean doExecute = this.controller.confirmStatementExecution(realSql);
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

		realSql = statementHook.preExec(this, realSql);
		if (traceStatements && messageOutput != null)
		{
			messageOutput.printMessage(realSql);
		}

		long sqlExecStart = System.currentTimeMillis();

		if (realSql == null)
		{
			// this can happen when the statement hook signalled to not execute the statement
			this.result = new StatementRunnerResult();
		}
		else
		{
			this.result = this.currentCommand.execute(realSql);
		}

		if (this.currentCommand instanceof WbStartBatch && result.isSuccess())
		{
			this.batchCommand = (WbStartBatch)this.currentCommand;
		}
		else if (this.batchCommand != null && this.currentCommand instanceof WbEndBatch)
		{
			this.result = this.batchCommand.executeBatch();
		}

		removeEmptyResults(result, realSql);

		if (this.currentConsumer != null && currentCommand != currentConsumer && result.isSuccess())
		{
			this.currentConsumer.consumeResult(result);
		}

		long time = (System.currentTimeMillis() - sqlExecStart);
		statementHook.postExec(this, realSql, result);
		result.setExecutionDuration(time);

		if (logAllStatements)
		{
			logStatement(realSql, time, currentConnection);
		}
	}

	private void removeEmptyResults(StatementRunnerResult result, String sql)
	{
		if (removeAnnotation.containsAnnotation(sql))
		{
			Iterator<DataStore> itr = result.getDataStores().iterator();
			while (itr.hasNext())
			{
				DataStore ds = itr.next();
				if (ds.getRowCount() == 0)
				{
					itr.remove();
				}
			}
		}
	}

	public static void logStatement(String sql, long time, WbConnection conn)
	{
		StringBuilder msg = new StringBuilder(sql.length() + 25);
		msg.append("Executed: ");
		if (conn != null)
		{
			msg.append('(');
			msg.append(conn.getCurrentUser());
			msg.append('@');
			msg.append(conn.getUrl());
			msg.append(')');
		}

		if (Settings.getInstance().getBoolProperty("workbench.sql.log.statements.clean", false))
		{
			msg.append(SqlUtil.makeCleanSql(sql, false, true));
			msg.append(' ');
		}
		else
		{
			msg.append('\n');
			msg.append(sql);
			msg.append('\n');
		}

		if (time > -1)
		{
			msg.append('(');
			msg.append(Long.toString(time));
			msg.append("ms)");
		}
		LogMgr.logInfo("StatementRunner.execute()", msg);
	}

	public StatementHook getStatementHook()
	{
		return statementHook;
	}

	public ResultSetConsumer getConsumer()
	{
		return currentConsumer;
	}

	public void setConsumer(ResultSetConsumer consumer)
	{
		this.currentConsumer = consumer;
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
		endReadOnlyTransaction();
		if (this.currentCommand != null && currentCommand != currentConsumer)
		{
			this.currentCommand.done();
			this.currentCommand = null;
		}
	}

	public void cancel()
	{
		synchronized (this)
		{
			try
			{
				if (this.currentConsumer != null)
				{
					this.currentConsumer.cancel();
				}

				if (currentConnection != null && Settings.getInstance().useOracleNativeCancel())
				{
					currentConnection.oracleCancel();
				}

				if (this.currentCommand != null)
				{
					this.currentCommand.cancel();
				}

			}
			catch (Exception th)
			{
				LogMgr.logWarning("StatementRunner.cancel()", "Error when cancelling statement", th);
			}
		}
	}

	public void abort()
	{
		if (this.result != null) this.result.clear();
		this.result = null;
		this.savepoint = null;
		this.currentCommand = null;
		this.currentConsumer = null;

		if (mainConnection != null)
		{
			this.currentConnection = mainConnection;
			mainConnection = null;
		}
	}

	public void done()
	{
		synchronized (this)
		{
			if (this.result != null) this.result.clear();
			this.result = null;
			this.releaseSavepoint();
			this.currentConsumer = null;
			this.restoreMainConnection();
			if (currentConnection != null)
			{
				this.currentConnection.clearWarnings();
			}
		}
	}

	public void setUseSavepoint(boolean flag)
	{
		this.useSavepoint = flag;
	}

	public void setSavepoint()
	{
		if (!useSavepoint) return;
		if (this.savepoint != null) return;
		try
		{
			this.savepoint = this.currentConnection.setSavepoint();
		}
		catch (SQLException e)
		{
			LogMgr.logError("DefaultStatementRunner.setSavepoint()", "Error creating savepoint", e);
			this.savepoint = null;
		}
		catch (Throwable th)
		{
			LogMgr.logError("DefaultStatementRunner.setSavepoint()", "Savepoints not supported!", th);
			this.savepoint = null;
			this.useSavepoint = false;
		}
	}

	public void releaseSavepoint()
	{
		if (this.savepoint == null || this.currentConnection == null) return;
		try
		{
			this.currentConnection.releaseSavepoint(savepoint);
		}
		finally
		{
			this.savepoint = null;
		}
	}

	public void rollbackSavepoint()
	{
		if (this.savepoint == null) return;
		try
		{
			this.currentConnection.rollback(savepoint);
		}
		finally
		{
			this.savepoint = null;
		}
	}

}
