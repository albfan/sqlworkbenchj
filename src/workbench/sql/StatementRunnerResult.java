/*
 * StatementRunnerResult.java
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

import java.sql.ResultSet;
import java.util.ArrayList;

import workbench.storage.DataStore;

/**
 *
 * @author  info@sql-workbench.net
 */
public class StatementRunnerResult
{
	// contains a list of result sets
	private ArrayList results;
	private ArrayList updateCounts;
	private ArrayList messages;
	private ArrayList datastores;
	private String sourceCommand;
	
	private boolean success = true;
	private boolean hasWarning = false;

	public StatementRunnerResult()
	{
	}
	
	public StatementRunnerResult(String aCmd)
	{
		this.sourceCommand = aCmd;
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
		this.datastores.add(ds);
		return this.datastores.size();
	}

	public int addResultSet(ResultSet rs)
	{
		if (this.results == null) this.results = new ArrayList();
		this.results.add(rs);
		return this.results.size();
	}

	public void addUpdateCount(int aCount)
	{
		if (this.updateCounts == null) this.updateCounts = new ArrayList();
		this.updateCounts.add(new Integer(aCount));
	}

	public void addMessages(String[] msg)
	{
		if (this.messages == null) this.messages = new ArrayList();
		if (msg == null || msg.length == 0) return;
		for (int i=0; i < msg.length; i++)
		{
			this.messages.add(msg[i]);
		}
	}

	public void addMessage(String aMessage)
	{
		if (this.messages == null) this.messages = new ArrayList();
		this.messages.add(aMessage);
	}

	public boolean hasData()
	{
		return (this.hasResultSets() || this.hasDataStores());
	}

	public boolean hasMessages()
	{
		return (this.messages != null && this.messages.size() > 0);
	}

	public boolean hasUpdateCounts()
	{
		return (this.updateCounts != null && this.updateCounts.size() > 0);
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

	public String[] getMessages()
	{
		if (this.messages == null) return null;
		int size = this.messages.size();
		String[] msgs = new String[size];
		for (int i=0; i< size; i++)
		{
			msgs[i] = (String)this.messages.get(i);
		}
		return msgs;
	}

	public long getTotalUpdateCount()
	{
		if (this.updateCounts == null) return 0;
		int size = this.updateCounts.size();
		long result = 0;
		for (int i=0; i< size; i++)
		{
			result += ((Integer)this.updateCounts.get(i)).intValue();
		}
		return result;
	}

	public int[] getUpdateCounts()
	{
		if (this.updateCounts == null) return new int[0];
		int size = this.updateCounts.size();
		int[] counts = new int[size];
		for (int i=0; i< size; i++)
		{
			counts[i] = ((Integer)this.updateCounts.get(i)).intValue();
		}
		return counts;
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

	public void clear()
	{
		if (this.datastores != null)
		{
			this.datastores.clear();
		}
		this.clearResultSets();
		if (this.messages != null) this.messages.clear();
		if (this.updateCounts !=null) this.updateCounts.clear();
		this.sourceCommand = null;
		this.hasWarning = false;
	}
	
}
