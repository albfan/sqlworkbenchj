/*
 * StatementRunnerResult.java
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

import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import workbench.gui.sql.DwStatusBar;
import workbench.resource.ResourceMgr;

import workbench.storage.DataStore;
import workbench.util.MessageBuffer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class StatementRunnerResult
{
	private List<ResultSet> results;
	private List<Integer> updateCounts;
	private MessageBuffer messages;
	private List<DataStore> datastores;
	private String sourceCommand;
	
	private boolean success = true;
	private boolean hasWarning = false;
	private boolean wasCancelled = false;

	private long executionTime = -1;
	private DecimalFormat timingFormatter;
	
	public StatementRunnerResult()
	{
		this.timingFormatter = DwStatusBar.createTimingFormatter();
		this.messages = new MessageBuffer();
	}
	
	public StatementRunnerResult(String aCmd)
	{
		this();
		this.sourceCommand = aCmd;
	}

	public boolean promptingWasCancelled() { return wasCancelled; }
	public void setPromptingWasCancelled() { this.wasCancelled = true; }
	
	public void setExecutionTime(long t) { this.executionTime = t; }
	public long getExecutionTime() { return this.executionTime; }
	
	public String getTimingMessage()
	{
		if (executionTime == -1) return null;
		StringBuilder msg = new StringBuilder(100);
		msg.append(ResourceMgr.getString("MsgExecTime"));
		msg.append(' ');
		double time = ((double)executionTime) / 1000.0;
		msg.append(timingFormatter.format(time));
		return msg.toString();
	}
	
	public void setSuccess() { this.success = true; }
	public void setFailure() { this.success = false; }
	public void setWarning(boolean flag) { this.hasWarning = flag; }
	public boolean hasWarning() { return this.hasWarning; }

	public boolean isSuccess() { return this.success; }
	public String getSourceCommand() { return this.sourceCommand; }

	public int addDataStore(DataStore ds)
	{
		if (this.datastores == null) this.datastores = new LinkedList<DataStore>();
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

	public void addUpdateCount(int count)
	{
		if (this.updateCounts == null) this.updateCounts = new LinkedList<Integer>();
		this.updateCounts.add(new Integer(count));
	}

	public void addUpdateCountMsg(int count)
	{
		addUpdateCount(count);
		addMessage(count + " " + ResourceMgr.getString("MsgRowsAffected"));
	}
	
	public void addMessage(MessageBuffer buffer)
	{
		this.messages.append(buffer);
	}
	
	public void addMessages(String[] msg)
	{
		if (msg == null || msg.length == 0) return;
		for (int i=0; i < msg.length; i++)
		{
			this.messages.append(msg[i]);
		}
	}

	public void addMessage(CharSequence msgBuffer)
	{
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

	public DataStore[] getDataStores()
	{
		if (this.datastores == null) return null;
		int size = this.datastores.size();
		DataStore[] ds = new DataStore[size];
		for (int i=0; i< size; i++)
		{
			ds[i] = (DataStore)this.datastores.get(i);
		}
		return ds;
	}

	public ResultSet[] getResultSets()
	{
		if (this.results == null) return null;

		int size = this.results.size();
		ResultSet[] rs = new ResultSet[size];
		for (int i=0; i< size; i++)
		{
			rs[i] = (ResultSet)this.results.get(i);
		}
		return rs;
	}

	public StringBuilder getMessageBuffer()
	{
		if (this.messages == null) return null;
		return messages.getBuffer();
	}
	
	public long getTotalUpdateCount()
	{
		if (this.updateCounts == null || this.updateCounts.size() == 0) return 0;
		Iterator<Integer> itr = updateCounts.iterator();
		long result = 0;
		for (Integer value : updateCounts)
		{
			result += value.intValue();
		}
		return result;
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
					try { rs.clearWarnings(); } catch (Throwable th) {}
					try { rs.close(); } catch (Throwable th) {}
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
		// be removed and as they are re-used by 
		// We only want to free the list itself.
		if (this.datastores != null)
		{
			this.datastores.clear();
		}
		this.clearResultSets();
		clearMessageBuffer();
		if (this.updateCounts !=null) this.updateCounts.clear();
		this.sourceCommand = null;
		this.hasWarning = false;
		this.executionTime = -1;
	}
	
}
