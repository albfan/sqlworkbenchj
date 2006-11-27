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
import java.util.ArrayList;
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
	// contains a list of result sets
	private List results;
	private List updateCounts;
	private MessageBuffer messages;
	private ArrayList datastores;
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
		StringBuffer msg = new StringBuffer(100);
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
		if (this.datastores == null) this.datastores = new ArrayList();
		ds.resetCancelStatus();
		this.datastores.add(ds);
		return this.datastores.size();
	}

	public int addResultSet(ResultSet rs)
	{
		if (this.results == null) this.results = new ArrayList();
		this.results.add(rs);
		return this.results.size();
	}

	void dumpMessageBuffer()
	{
		System.out.println((this.messages == null ? "null" : messages.toString()));
	}
	
	public void addUpdateCount(int count)
	{
		if (this.updateCounts == null) this.updateCounts = new LinkedList();
		this.updateCounts.add(new Integer(count));
	}

	public void addUpdateCountMsg(int count)
	{
		addUpdateCount(count);
		addMessage(count + " " + ResourceMgr.getString("MsgRowsAffected"));
	}
	
	public void addMessages(String[] msg)
	{
		if (msg == null || msg.length == 0) return;
		for (int i=0; i < msg.length; i++)
		{
			this.messages.append(msg[i]);
		}
	}

	public void addMessage(StringBuffer msgBuffer)
	{
		if (messages.getLength() > 0) messages.appendNewLine();
		messages.append(msgBuffer);
	}
	
	public void addMessage(String msg)
	{
		if (messages.getLength() > 0) messages.appendNewLine();
		messages.append(msg);
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

	public StringBuffer getMessageBuffer()
	{
		if (this.messages == null) return null;
		StringBuffer b = messages.getBuffer();
		return b;
	}
	
	public long getTotalUpdateCount()
	{
		if (this.updateCounts == null || this.updateCounts.size() == 0) return 0;
		Iterator itr = updateCounts.iterator();
		long result = 0;
		while (itr.hasNext())
		{
			result += ((Integer)itr.next()).intValue();
		}
		return result;
	}

	public void clearResultData()
	{
		if (this.datastores != null)
		{
			for (int i = 0; i < datastores.size(); i++)
			{
				DataStore ds = (DataStore)datastores.get(i);
				if (ds != null) ds.reset();
			}
			this.datastores.clear();
		}
		this.clearResultSets();
	}
	
	public void clearResultSets()
	{
		if (this.results != null)
		{
			for (int i=0; i < this.results.size(); i++)
			{
				ResultSet rs = (ResultSet)this.results.get(i);
				if (rs != null)
				{
					try { rs.clearWarnings(); } catch (Throwable th) {}
					try { rs.close(); } catch (Throwable th) {}
					rs = null;
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
