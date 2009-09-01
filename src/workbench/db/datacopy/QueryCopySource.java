/*
 * QueryCopySource.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.ValueConverter;

/**
 * Acts as a row data producer to copy the data from a SQL query
 * to another table (and database).
 * 
 * When copying a single table, {@link DataCopier} will create the approriate
 * <tt>SELECT</tt> statement to retrieve all rows (and columns) from the source
 * table. 
 * 
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
	private boolean hasErrors = false;
	private boolean hasWarnings = false;
	private RowData currentRow;
	
	public QueryCopySource(WbConnection source, String sql)
	{
		this.sourceConnection = source;
		this.retrieveSql = sql.trim();
		if (retrieveSql.endsWith(";"))
		{
			retrieveSql = retrieveSql.substring(0, retrieveSql.length() - 1);
		}
	}

	public boolean hasErrors() { return this.hasErrors; }
	public boolean hasWarnings() { return this.hasWarnings; }
	
	public void setValueConverter(ValueConverter converter) {}
	
	public void setReceiver(RowDataReceiver rec)
	{
		this.receiver = rec;
	}

	public void start()
		throws Exception
	{
		LogMgr.logDebug("QueryCopySource.start()", "Using SQL: "+ this.retrieveSql);
		
		ResultSet rs = null;
		this.keepRunning = true;
		this.regularStop = false;
		try
		{
			this.retrieveStatement = this.sourceConnection.createStatementForQuery();
			rs = this.retrieveStatement.executeQuery(this.retrieveSql);
			ResultInfo info = new ResultInfo(rs.getMetaData(), this.sourceConnection);
			int colCount = info.getColumnCount();
			currentRow = new RowData(colCount);
			while (this.keepRunning && rs.next())
			{
				// RowData will make some transformation 
				// on the data read from the database
				// which works around some bugs in the Oracle
				// JDBC driver. Especially it will supply
				// CLOB data as a String which I hope will be
				// more flexible when copying from Oracle
				// to other systems
				// That's why I'm reading the result set into a RowData object
				currentRow.read(rs, info);
				if (!keepRunning) break;
				
				try
				{
					this.receiver.processRow(currentRow.getData());
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
				// When copying a schema, we should not send an importFinished()
				// so that the DataImporter reports the table counts correctly
				this.receiver.importFinished();
			}
			else
			{
				this.receiver.importCancelled();
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, retrieveStatement);
		}
	}

	public String getLastRecord()
	{
		if (currentRow == null) return null;
		return currentRow.toString();
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

	public boolean isCancelled()
	{
		return !keepRunning && !regularStop;
	}
	
	public MessageBuffer getMessages()
	{
		return null;
	}

	public void setAbortOnError(boolean flag)
	{
		this.abortOnError = flag;
	}

	public void setCheckDependencies(boolean flag)
	{
		// not supported
	}
	
	public void setErrorHandler(JobErrorHandler handler)
	{
	}
}
