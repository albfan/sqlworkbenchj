/*
 * StatementRunnerResult.java
 *
 * Created on 16. November 2002, 13:44
 */

package workbench.sql;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class StatementRunnerResult
{
	// contains a list of result sets
	private ArrayList results;
	private ArrayList updateCounts;
	private ArrayList messages;
	private ArrayList datastores;

	private boolean success = true;
	
	public StatementRunnerResult()
	{
	}

	public void setSuccess() { this.success = true; }
	public void setFailure() { this.success = false; }
	public boolean isSuccess() { return this.success; }
	
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
		if (this.datastores == null) return new DataStore[0];
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
		if (this.results == null) return new ResultSet[0];
		
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
		if (this.messages == null) return new String[0];
		int size = this.messages.size();
		String[] msgs = new String[size];
		for (int i=0; i< size; i++)
		{
			msgs[i] = (String)this.updateCounts.get(i);
		}
		return msgs;
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
	
	public void clear()
	{
		if (this.datastores != null)
		{
			for (int i=0; i < this.datastores.size(); i ++)
			{
				DataStore ds = (DataStore)this.datastores.get(i);
				ds.reset();
			}
			this.datastores.clear();
		}
		
		if (this.results != null)
		{
			for (int i=0; i < this.results.size(); i++)
			{
				ResultSet rs = (ResultSet)this.results.get(i);
				if (rs != null)
				{
					try
					{
						rs.close();
					}
					catch (Exception e)
					{
					}
				}
			}
			this.results.clear();
		}
		if (this.messages != null) this.messages.clear();
		if (this.updateCounts !=null) this.updateCounts.clear();
	}
	
}
