/*
 * StatementRunner.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import java.util.List;
import workbench.db.ConnectionProfile;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.interfaces.ParameterPrompter;
import workbench.interfaces.ResultLogger;
import workbench.resource.Settings;
import workbench.sql.wbcommands.WbEndBatch;
import workbench.sql.wbcommands.WbStartBatch;
import workbench.storage.RowActionMonitor;
import workbench.db.WbConnection;
import workbench.interfaces.ExecutionController;
import workbench.interfaces.ResultSetConsumer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class StatementRunner
	implements PropertyChangeListener
{
	// used to restore the "real" connection if WbConnect changes the "current"
	// connection during script execution
	private WbConnection mainConnection;

	private WbConnection currentConnection;
	private StatementRunnerResult result;

	private SqlCommand currentCommand;
	private ResultSetConsumer currentConsumer;
	private String baseDir;
	
	private RowActionMonitor rowMonitor;
	private ExecutionController controller;
	private WbStartBatch batchCommand;
	private ResultLogger resultLogger;
	private boolean verboseLogging;
	private boolean hideWarnings;
	private boolean fullErrorReporting = false;
	private ParameterPrompter prompter;
	private boolean ignoreDropErrors = false;
	protected CommandMapper cmdMapper;
	private boolean useSavepoint;
	private Savepoint savepoint;
	private boolean returnOnlyErrorMessages = false;
	private List<PropertyChangeListener> changeListeners = new ArrayList<PropertyChangeListener>();
	private int maxRows = -1;
	private int queryTimeout = -1;
	private boolean showDataLoadingProgress = true;
	
	public StatementRunner()
	{
		this.verboseLogging = !Settings.getInstance().getConsolidateLogMsg();
		Settings.getInstance().addPropertyChangeListener(this, "workbench.gui.log.consolidate");
		this.cmdMapper = new CommandMapper();
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
	
	public void propertyChange(PropertyChangeEvent evt)
	{
		if ("workbench.gui.log.consolidate".equals(evt.getPropertyName()))
		{
			this.verboseLogging = !Settings.getInstance().getConsolidateLogMsg();
		}
	}

	public void setShowDataLoadingProgress(boolean flag)
	{
		this.showDataLoadingProgress = false;
	}
	
	public void setFullErrorReporting(boolean flag)
	{
		this.fullErrorReporting = flag;
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
	 * For testing purposes, to that non-default commands can be added 
	 * during a JUnit test
	 */
	public void addCommand(SqlCommand command)
	{
		cmdMapper.addCommand(command);
	}

	public Collection<String> getAllWbCommands()
	{
		return cmdMapper.getAllWbCommands();
	}
	
	/**
	 * Controls the type of error reporting. 
	 * If this is set to true, no additional messages will be reported, only
	 * the errors returned by the server. 
	 * 
	 * @param flag
	 * @see SqlCommand#setReturnOnlyErrorMessages(boolean) 
	 */
	public void setReturnOnlyErrorMessages(boolean flag)
	{
		returnOnlyErrorMessages = flag;
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

	public void preCloseConnection()
	{
		
	}
	
	public void setConnection(WbConnection aConn)
	{
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

		if (this.currentConnection == null && this.currentCommand.isConnectionRequired())
		{
			String verb = SqlUtil.getSqlVerb(aSql);
			throw new SQLException("Cannot execute command '" + verb + "' without a connection!");
		}
		
		this.currentCommand.setStatementRunner(this);
		this.currentCommand.setRowMonitor(this.rowMonitor);
		this.currentCommand.setResultLogger(this.resultLogger);
		if (currentConsumer == null)
		{
			this.currentCommand.setMaxRows(maxRows);
		}
		else
		{
			this.currentCommand.setMaxRows(0);
		}
		this.currentCommand.setQueryTimeout(queryTimeout);
		this.currentCommand.setConnection(this.currentConnection);
		this.currentCommand.setParameterPrompter(this.prompter);
		this.currentCommand.setReturnOnlyErrorMessages(this.returnOnlyErrorMessages);
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
		
		boolean oldReporting = this.currentCommand.getFullErrorReporting();
		
		this.currentCommand.setFullErrorReporting(this.fullErrorReporting);
		
		long sqlExecStart = System.currentTimeMillis();
		
		this.result = this.currentCommand.execute(realSql);
		
		this.currentCommand.setFullErrorReporting(oldReporting);

		if (this.currentCommand instanceof WbStartBatch && result.isSuccess())
		{
			this.batchCommand = (WbStartBatch)this.currentCommand;
		}
		else if (this.batchCommand != null && this.currentCommand instanceof WbEndBatch)
		{
			this.result = this.batchCommand.executeBatch();
		}
		long time = (System.currentTimeMillis() - sqlExecStart);
		result.setExecutionTime(time);
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
		if (this.currentCommand != null && currentCommand != currentConsumer)
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

	public void done()
	{
		if (this.result != null) this.result.clear();
		this.result = null;
		this.releaseSavepoint();
		this.statementDone();
		this.currentConsumer = null;
		this.restoreMainConnection();
		this.currentConnection.clearWarnings();
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
