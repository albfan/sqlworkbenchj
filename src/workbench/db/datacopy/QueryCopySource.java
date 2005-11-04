/*
 * QueryCopySource.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.datacopy;

import java.sql.ResultSet;
import java.sql.Statement;
import workbench.db.WbConnection;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.RowDataReceiver;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;

/**
 * @author  support@sql-workbench.net
 */
public class QueryCopySource
		implements RowDataProducer
{
	private RowDataReceiver receiver;
	private boolean keepRunning = true;
	private WbConnection sourceConnection;
	private Statement retrieveStatement;
	private String retrieveSql;
	
	public QueryCopySource(WbConnection source, String sql)
	{
		this.sourceConnection = source;
		this.retrieveSql = sql;
	}

	public void setReceiver(RowDataReceiver rec)
	{
		this.receiver = rec;
	}

	public void start()
		throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			this.retrieveStatement = this.sourceConnection.createStatementForQuery();
			rs = this.retrieveStatement.executeQuery(this.retrieveSql);
			int colCount = rs.getMetaData().getColumnCount();
			Object[] rowData = new Object[colCount];
			while (this.keepRunning && rs.next())
			{
				for (int i=0; i < colCount; i++)
				{
					if (!keepRunning) break;
					rowData[i] = rs.getObject(i + 1);
				}
				if (this.keepRunning) this.receiver.processRow(rowData);
			}
			if (this.keepRunning) 
			{
				this.receiver.importFinished();
			}
			else
			{
				this.receiver.importCancelled();
			}
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { retrieveStatement.close(); } catch (Throwable th) {}
		}
	}

	public void cancel()
	{
		this.keepRunning = false;
		try
		{
			this.retrieveStatement.cancel();
		}
		catch (Exception e)
		{
			LogMgr.logWarning("QueryCopySource.cancel()", "Error when cancelling retrieve", e);
		}
		
	}

	public String getMessages()
	{
		return "";
	}

	public void setAbortOnError(boolean flag)
	{
	}

	public void setErrorHandler(JobErrorHandler handler)
	{
	}
}
