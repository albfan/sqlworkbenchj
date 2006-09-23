/*
 * QueryCopySource.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.datacopy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.WbConnection;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.RowDataReceiver;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;

/**
 * @author  support@sql-workbench.net
 */
public class QueryCopySource
		implements RowDataProducer
{
	private RowDataReceiver receiver;
	private boolean keepRunning = true;
	private boolean regularStop = false;
	private WbConnection sourceConnection;
	private Statement retrieveStatement;
	private String retrieveSql;
	private boolean abortOnError;
	
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
		this.keepRunning = true;
		this.regularStop = false;
		try
		{
			this.retrieveStatement = this.sourceConnection.createStatementForQuery();
			rs = this.retrieveStatement.executeQuery(this.retrieveSql);
			ResultInfo info = new ResultInfo(rs.getMetaData(), this.sourceConnection);
			int colCount = info.getColumnCount();
			RowData row = new RowData(colCount);
			row.setUseNullValueObject(false);
			while (this.keepRunning && rs.next())
			{
				// RowData will make some transformation 
				// on the data read from the database
				// which works around some bugs in the Oracle
				// JDBC driver. Especially it will supply
				// CLOB data as a String which I hope will be
				// more flexible when copying from Oracle
				// to other systems
				row.read(rs, info);
				if (!keepRunning) break;
				try
				{
					this.receiver.processRow(row.getData());
				}
				catch (SQLException e)
				{
					if (abortOnError) throw e;
				}
			}
			
			// if keepRunning == false, cancel() was
			// called and we have to tell that the Importer
			// in order to do a rollback
			if (this.keepRunning || regularStop) 
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

	public void stop()
	{
		this.regularStop = true;
		cancel();
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
		this.abortOnError = flag;
	}

	public void setErrorHandler(JobErrorHandler handler)
	{
	}
}
