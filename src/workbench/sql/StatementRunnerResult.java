/*
 * StatementRunnerResult.java
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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import workbench.console.RowDisplay;
import workbench.interfaces.ResultLogger;
import workbench.resource.ResourceMgr;

import workbench.storage.DataStore;
import workbench.util.DurationFormatter;
import workbench.util.MessageBuffer;

/**
 * Stores the result of a SQL execution.
 * <br>
 * If the SQL command produced a result this might be available as a DataStore or as a
 * ResultSet (depending on the command).
 * <br/>
 * @see workbench.sql.SqlCommand#execute(java.lang.String)
 *
 * @author  support@sql-workbench.net
 */
public class StatementRunnerResult
{
	private List<ResultSet> results;
	private long totalUpdateCount;
	private MessageBuffer messages;
	private List<DataStore> datastores;
	private String sourceCommand;

	private boolean success = true;
	private boolean hasWarning = false;
	private boolean wasCancelled = false;
	private boolean stopScriptExecution = false;
	private boolean showRowCount = true;

	/**
	 * Controls the printing of the results in command line mode.
	 * If printRowsPerLine == true, each row should be printed on a single console line
	 * If printRowsPerLine == false, each row should be printed as a "form" (one row per column)
	 */
	private RowDisplay rowDisplay = RowDisplay.noChange;

	/**
	 * If set, indicates a temporary change of the display just for this single Result
	 */
	private RowDisplay tempRowDisplay = null;

	private long executionTime = -1;
	private final DurationFormatter timingFormatter;

	public StatementRunnerResult()
	{
		this.timingFormatter = new DurationFormatter();
		this.messages = new MessageBuffer();
	}

	public StatementRunnerResult(String aCmd)
	{
		this();
		this.sourceCommand = aCmd;
	}

	public boolean stopScript() { return stopScriptExecution; }
	public void setStopScript(boolean flag) { this.stopScriptExecution = flag; }

	public boolean promptingWasCancelled() { return wasCancelled; }
	public void setPromptingWasCancelled() { this.wasCancelled = true; }

	public void setExecutionTime(long t) { this.executionTime = t; }
	public long getExecutionTime() { return this.executionTime; }

	public void setShowRowCount(boolean flag)
	{
		showRowCount = flag;
	}

	public boolean getShowRowCount()
	{
		return showRowCount;
	}

	public RowDisplay getTemporaryDisplay()
	{
		RowDisplay result = tempRowDisplay;
		tempRowDisplay = null;
		return result;
	}

	public void setTemporaryDisplay(RowDisplay display)
	{
		tempRowDisplay = display;
	}

	/**
	 * Controls the printing of the results in command line mode.
	 * If RowDisplay.SingleLine, each row should be printed on a single console line
	 * If RowDisplay.Form, each row should be printed as a "form" (one row per column)
	 */
	public RowDisplay getRowDisplay()
	{
		return rowDisplay;
	}

	public void setRowDisplay(RowDisplay newDisplay)
	{
		rowDisplay = newDisplay;
	}

	public String getTimingMessage()
	{
		if (executionTime == -1) return null;
		StringBuilder msg = new StringBuilder(100);
		msg.append(ResourceMgr.getString("MsgExecTime"));
		msg.append(' ');
		msg.append(timingFormatter.getDurationAsSeconds(executionTime));
		return msg.toString();
	}

	public void appendMessages(ResultLogger log)
	{
		if (this.messages == null) return;
		messages.appendTo(log);
	}

	public void setSuccess() { this.success = true; }
	public void setFailure() { this.success = false; }
	public void setWarning(boolean flag) { this.hasWarning = flag; }
	public boolean hasWarning() { return this.hasWarning; }

	public boolean isSuccess() { return this.success; }
	public String getSourceCommand() { return this.sourceCommand; }

	public int addDataStore(DataStore ds)
	{
		if (this.datastores == null) this.datastores = new ArrayList<DataStore>(5);
		ds.resetCancelStatus();
		this.datastores.add(ds);
		return this.datastores.size();
	}

	public int addResultSet(ResultSet rs)
	{
		if (this.results == null) this.results = new LinkedList<ResultSet>();
		this.results.add(rs);
		return this.results.size();
	}

	public void addUpdateCountMsg(int count)
	{
		this.totalUpdateCount += count;
		addMessage(count + " " + ResourceMgr.getString("MsgRowsAffected"));
	}

	public void addMessageByKey(String key)
	{
		addMessage(ResourceMgr.getString(key));
	}
	public void addMessage(MessageBuffer buffer)
	{
		if (buffer == null) return;
		this.messages.append(buffer);
	}

	public void addMessageNewLine()
	{
		this.messages.appendNewLine();
	}

	public void addMessage(CharSequence msgBuffer)
	{
		if (msgBuffer == null) return;
		if (messages.getLength() > 0) messages.appendNewLine();
		messages.append(msgBuffer);
	}

	public boolean hasData()
	{
		return (this.hasResultSets() || this.hasDataStores());
	}

	public boolean hasMessages()
	{
		if (this.messages == null) return false;
		return (messages.getLength() > 0);
	}

	public boolean hasResultSets()
	{
		return (this.results != null && this.results.size() > 0);
	}

	public boolean hasDataStores()
	{
		return (this.datastores != null && this.datastores.size() > 0);
	}

	public List<DataStore> getDataStores()
	{
		return this.datastores;
	}

	public List<ResultSet> getResultSets()
	{
		return this.results;
	}

	/**
	 * Return the messages that have been collected for this result.
	 * This will clear the internal buffer used to store the messages.
	 *
	 * @see workbench.util.MessageBuffer#getBuffer()
	 */
	public CharSequence getMessageBuffer()
	{
		if (this.messages == null) return null;
		return messages.getBuffer();
	}

	public long getTotalUpdateCount()
	{
		return totalUpdateCount;
	}

	/**
	 * Clears stored ResultSets and DataStores. The content of
	 * the datastores will be removed!
	 * @see #clearResultSets()
	 */
	public void clearResultData()
	{
		if (this.datastores != null)
		{
			//for (int i = 0; i < datastores.size(); i++)
			for (DataStore ds : datastores)
			{
				if (ds != null) ds.reset();
			}
			this.datastores.clear();
		}
		this.clearResultSets();
	}

	/**
	 * Closes all "stored" ResultSets
	 */
	public void clearResultSets()
	{
		if (this.results != null)
		{
			for (ResultSet rs : results)
			{
				if (rs != null)
				{
					try { rs.clearWarnings(); } catch (Exception th) {}
					try { rs.close(); } catch (Exception th) {}
				}
			}
			this.results.clear();
		}
	}

	public void clearMessageBuffer()
	{
		this.messages.clear();
	}

	public void clear()
	{
		// Do not call clearResultData() !!!
		// otherwise the content of the retrieved DataStores will also
		// be removed and as they are re-used by DwPanel or TableDataPanel
		// they might still be in use.

		// We only want to free the list itself.
		if (this.datastores != null)
		{
			this.datastores.clear();
		}
		clearResultSets();
		clearMessageBuffer();
		this.totalUpdateCount = 0;
		this.sourceCommand = null;
		this.hasWarning = false;
		this.executionTime = -1;
	}

}
