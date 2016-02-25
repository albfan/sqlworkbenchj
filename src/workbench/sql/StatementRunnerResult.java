/*
 * StatementRunnerResult.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
 * @author Thomas Kellerer
 */
public class StatementRunnerResult
{
	private List<ResultSet> results;
	private long totalUpdateCount;
	private long totalRowsProcessed;
	private MessageBuffer messages;
	private List<DataStore> datastores;
	private String sourceCommand;

  private ExecutionStatus status = ExecutionStatus.Success;
  private boolean hasWarnings;
	private boolean wasCancelled;
	private boolean stopScriptExecution;
	private boolean showRowCount = true;
	private boolean ignoreUpdateCount;

	private long executionTime = -1;
	private static final DurationFormatter timingFormatter = new DurationFormatter();
	private ErrorDescriptor errorDetails;

	public StatementRunnerResult()
	{
		this.messages = new MessageBuffer();
	}

	public StatementRunnerResult(String aCmd)
	{
		this();
		this.sourceCommand = aCmd;
	}

  public boolean isIgnoreUpdateCount()
  {
    return ignoreUpdateCount;
  }

	public void ignoreUpdateCounts(boolean flag)
	{
		ignoreUpdateCount = flag;
	}

	public void setRowsProcessed(long rows)
	{
		this.totalRowsProcessed = rows;
	}

  public ExecutionStatus getStatus()
  {
    if (status == null) return ExecutionStatus.Success;
    return status;
  }

	public long getRowsProcessed()
	{
		return this.totalRowsProcessed;
	}

	public void addRowsProcessed(long rows)
	{
		this.totalRowsProcessed += rows;
	}

	public boolean stopScript()
	{
		return stopScriptExecution;
	}

	public void setStopScript(boolean flag)
	{
		this.stopScriptExecution = flag;
	}

	public boolean promptingWasCancelled()
	{
		return wasCancelled;
	}

	public void setPromptingWasCancelled()
	{
		this.wasCancelled = true;
	}

	public void setExecutionDuration(long t)
	{
		this.executionTime = t;
	}

	/**
	 * Returns the duration of the statement.
	 */
	public long getExecutionDuration()
	{
		return this.executionTime;
	}

	public void setShowRowCount(boolean flag)
	{
		showRowCount = flag;
	}

	public boolean getShowRowCount()
	{
		return showRowCount;
	}

	public String getFormattedDuration()
	{
		if (executionTime == -1) return null;
		return timingFormatter.formatDuration(executionTime, (executionTime < DurationFormatter.ONE_MINUTE));
	}

	public String getTimingMessage()
	{
		if (executionTime == -1) return null;
		StringBuilder msg = new StringBuilder(100);
		msg.append(ResourceMgr.getString("MsgExecTime"));
		msg.append(' ');
		msg.append(getFormattedDuration());
		return msg.toString();
	}

	public void appendMessages(ResultLogger log)
	{
		if (this.messages == null) return;
		messages.appendTo(log);
	}

	public void setSuccess()
	{
    this.status = ExecutionStatus.Success;
		this.errorDetails = null;
	}

	public void setFailure()
	{
		setFailure(null);
	}

	public void setFailure(ErrorDescriptor error)
	{
    this.status = ExecutionStatus.Error;
		this.errorDetails = error;
	}

	public void setWarning()
	{
    hasWarnings = true;
	}

	public boolean hasWarning()
	{
    return hasWarnings;
	}

	public ErrorDescriptor getErrorDescriptor()
	{
		return errorDetails;
	}

	public boolean isSuccess()
	{
    return status == ExecutionStatus.Success;
	}

	/**
	 * Define the SQL statement that generated this
	 * result. This is mainly used in the GUI to display
	 * a tooltip for the result-tab
	 *
	 * @param sql
	 */
	public void setSourceCommand(String sql)
	{
		sourceCommand = sql;
	}

	public String getSourceCommand()
	{
		return this.sourceCommand;
	}

	public int addDataStore(DataStore ds)
	{
		if (this.datastores == null) this.datastores = new ArrayList<>(5);
		if (ds != null)
		{
			ds.resetCancelStatus();
			this.datastores.add(ds);
		}
		return this.datastores.size();
	}

	public int addResultSet(ResultSet rs)
	{
		if (this.results == null) this.results = new LinkedList<>();
		this.results.add(rs);
		return this.results.size();
	}

	public void addUpdateCountMsg(int count)
	{
		if (ignoreUpdateCount) return;
		this.totalUpdateCount += count;
		addMessage(ResourceMgr.getFormattedString("MsgRowsAffected", count));
	}

  /**
   * Adds an error message identified by the resource key.
   *
   * This will also create an ErrorDescriptor and the status of this result will be set to failure.
   *
   * @param key
   *
   * @see ErrorDescriptor
   * @see #setFailure(workbench.sql.ErrorDescriptor)
   */
	public void addErrorMessageByKey(String key)
	{
    String msg = ResourceMgr.getString(key);
    addErrorMessage(msg);
	}

	public void addErrorMessageByKey(String key, String ... arguments)
	{
    String msg = null;
    if (arguments != null)
    {
      msg = ResourceMgr.getFormattedString(key, (Object[])arguments);
    }
    else
    {
      msg = ResourceMgr.getString(key);
    }
    addErrorMessage(msg);
	}

  /**
   * Adds an error message and creates an ErrorDescriptor with this message
   *
   * This will also create an ErrorDescriptor and the status of this result will be set to failure.
   *
   * @param key
   *
   * @see ErrorDescriptor
   * @see #setFailure(workbench.sql.ErrorDescriptor)
   */
	public void addErrorMessage(String msg)
	{
		addMessage(msg);
    ErrorDescriptor error = new ErrorDescriptor();
    error.setErrorMessage(msg);
    setFailure(error);
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

	public void addWarning(CharSequence msg)
	{
    addMessage(msg);
    hasWarnings = true;
	}

	public void addWarning(MessageBuffer messages)
	{
    addMessage(messages);
    hasWarnings = true;
	}

	public void addWarningByKey(String msgKey)
	{
    addMessageByKey(msgKey);
    hasWarnings = true;
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
	public CharSequence getMessages()
	{
		if (this.messages == null) return null;
		return messages.getBuffer();
	}

  public MessageBuffer getMessageBuffer()
  {
    return messages;
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
		this.executionTime = -1;
		this.errorDetails = null;
    this.status = ExecutionStatus.Success;
	}

}
