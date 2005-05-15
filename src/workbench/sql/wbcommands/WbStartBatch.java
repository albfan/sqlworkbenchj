/*
 * WbStartBatch.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * @author  support@sql-workbench.net
 */
public class WbStartBatch
	extends SqlCommand
{
	private static final String VERB = "WBSTARTBATCH";
	private Statement batch; 
	public WbStartBatch()
	{
	}

	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		if (!aConnection.getMetadata().supportsBatchUpdates())
		{
			this.batch = null;
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrorJdbcBatchUpdateNotSupported"));
		}
		else
		{
			this.batch = aConnection.createStatement();
			result.setSuccess();
			result.addMessage(ResourceMgr.getString("MsgJdbcBatchProcessingStarted"));
		}
		return result;
	}
	
	public void addStatement(String sql)
		throws SQLException
	{
		if (this.currentStatement == null) throw new SQLException("Batch mode not supported");
		this.batch.addBatch(sql);
	}
	
	public StatementRunnerResult executeBatch()
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult("ExecuteBatch");
		if (this.batch == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrorJdbcBatchUpdateNotSupported"));
			return result;
		}
		long totalRows = 0;
		result.setSuccess();
		result.addMessage(ResourceMgr.getString("MsgJdbcBatchProcessingEnded"));
		
		int[] rows = this.batch.executeBatch();
		if (rows == null || rows.length == 0)
		{
			result.addMessage(ResourceMgr.getString("MsgJdbcBatchStatementNoInfo"));
		}
		else 
		{
			for (int i=0; i < rows.length; i++)
			{
				if (rows[i] != Statement.EXECUTE_FAILED)
				{
					String msg = ResourceMgr.getString("MsgJdbcBatchStatementFailed");
					result.addMessage(msg.replaceAll("%num%", Integer.toString(i)));
				}
				else if (rows[i] == Statement.SUCCESS_NO_INFO)
				{
					String msg = ResourceMgr.getString("MsgJdbcBatchStatementNoStatementInfo");
					result.addMessage(msg.replaceAll("%num%", Integer.toString(i)));
				}
				else
				{
					totalRows += rows[i];
				}
			}
			String msg = ResourceMgr.getString("MsgJdbcBatchTotalRows") + " " + Long.toString(totalRows);
			result.addMessage(msg);
		}
		return result;
	}
	
}
