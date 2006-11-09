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
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import javax.swing.text.NumberFormatter;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.storage.DataStore;
import workbench.util.MessageBuffer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class StatementRunnerResult
{
	// contains a list of result sets
	private ArrayList results;
	private ArrayList updateCounts;
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
		this.timingFormatter = createTimingFormatter();
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
	
	public static final DecimalFormat createTimingFormatter()
	{
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		String sep = Settings.getInstance().getProperty("workbench.gui.timining.decimal", ".");
		symb.setDecimalSeparator(sep.charAt(0));		
		DecimalFormat numberFormatter = new DecimalFormat("0.#s", symb);
		numberFormatter.setMaximumFractionDigits(2);
		return numberFormatter;
	}
	
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

	public void addUpdateCount(int count)
	{
		if (this.updateCounts == null) this.updateCounts = new ArrayList();
		this.updateCounts.add(new Integer(count));
		addUpdateCountMsg(count);
	}

	public void addUpdateCountMsg(int count)
	{
		addMessage(count + " " + ResourceMgr.getString("MsgRowsAffected"));
	}
	
	public void addMessages(String[] msg)
	{
		if (msg == null || msg.length == 0) return;
		for (int i=0; i < msg.length; i++)
		{
			this.addMessage(msg[i]);
		}
	}

	private void checkMessageBuffer()
	{
		if (messages == null) messages = new MessageBuffer(500);
	}
	public void addMessage(StringBuffer msgBuffer)
	{
		checkMessageBuffer();
		if (messages.getLength() > 0) messages.append('\n');
		messages.append(msgBuffer);
	}
	
	public void addMessage(String msg)
	{
		checkMessageBuffer();
		if (messages.getLength() > 0) messages.append('\n');
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

	public StringBuffer getMessageBuffer()
	{
		if (this.messages == null) return null;
		StringBuffer b = messages.getBuffer();
		if (b == null) 
		{
			// If we have a SoftReference but the buffer is null
			// this means at least one message was added, but 
			// the buffer was removed due to tight memory.
			// In this case a warning is returned.
			b = new StringBuffer(ResourceMgr.getString("ErrMsgBufferCollected"));
		}
		return b;
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
		this.messages = null;
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
