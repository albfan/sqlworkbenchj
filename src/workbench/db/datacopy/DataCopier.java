/*
 * DataCopier.java
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import workbench.db.importer.DataImporter;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.RowDataReceiver;
import workbench.exception.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.WbThread;
import workbench.db.*;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DataCopier
	implements RowDataProducer
{
	private WbConnection sourceConnection;
	private WbConnection targetConnection;

	private RowDataProducer sourceData;
	
	private TableIdentifier sourceTable;
	private TableIdentifier targetTable;

	private DataImporter importer;

	// the columnMap will contain elements of type ColumnIdentifier
	private HashMap columnMap;

	private Statement retrieveStatement;
	private String retrieveSql;
	private boolean keepRunning = true;
	private String addWhere;

	// used when the source is a SQL and not a table
	private boolean useQuery = false;
	private ColumnIdentifier[] targetColumnsForQuery;
	private StringBuffer messages = null;
	private StringBuffer errors = null;
	private boolean abortOnError = false;

	public DataCopier()
	{
		this.importer = new DataImporter();
		this.importer.setProducer(this);
	}

	public void copyFromTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier aTargetTable)
		throws SQLException
	{
		this.copyFromTable(source, target, aSourceTable, aTargetTable, (Map)null, null, false, false);
	}

	public void copyFromFile(RowDataProducer source, WbConnection target, TableIdentifier targetTable)
	{
		this.sourceConnection = null;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.targetTable = targetTable;
		this.useQuery = false;
		this.targetColumnsForQuery = null;
		this.sourceData = source;
		this.importer.setProducer(source);
	}
	
	/**
	 *	Define the source table, the target table and the column mapping
	 *	for the copy process.
	 *	If the columnMapping is null, the matching columns from both tables are used.
	 *	It is expected that the mapping contains String objects. The key is the name of the
	 *	source column, the mapped value is the name of the target column
	 */
	public void copyFromTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier aTargetTable, Map columnMapping, String additionalWhere, boolean createTable, boolean dropTable)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = aSourceTable;
		this.targetTable = aTargetTable;
		this.useQuery = false;
		this.targetColumnsForQuery = null;
		
		if (!this.sourceConnection.getMetadata().tableExists(aSourceTable))
		{
			this.addError(ResourceMgr.getString("ErrorCopySourceTableNotFound").replaceAll("%name%", aTargetTable.getTable()));
			throw new SQLException("Table " + aTargetTable.getTable() + " not found in target connection");
		}
		
		this.setSourceTableWhere(additionalWhere);
		boolean exists = this.targetConnection.getMetadata().tableExists(aTargetTable);

		if (exists && dropTable && createTable)
		{
			this.targetConnection.getMetadata().dropTable(aTargetTable);
			this.addMessage(ResourceMgr.getString("MsgCopyTableDropped").replaceAll("%name%", aTargetTable.getTable()));
			exists = false;
		}

		if (exists)
		{
			this.initColumnMapping(columnMapping);
		}
		else if (createTable)
		{
			List sourceCols = source.getMetadata().getTableColumns(aSourceTable);

			this.columnMap = new HashMap(columnMapping.size());
			Iterator itr = columnMapping.entrySet().iterator();
			while (itr.hasNext())
			{
				Map.Entry entry = (Map.Entry)itr.next();
				ColumnIdentifier scol = new ColumnIdentifier((String)entry.getKey());
				int index = sourceCols.indexOf(scol);
				if (index > -1)
				{
					ColumnIdentifier sourceCol = (ColumnIdentifier)sourceCols.get(index);
					ColumnIdentifier targetCol = sourceCol.createCopy();
					String tcol = (String)entry.getValue();
					targetCol.setColumnName(tcol);
					this.columnMap.put(sourceCol, targetCol);
				}
			}
			int count = this.columnMap.size();
			ColumnIdentifier[] cols = new ColumnIdentifier[count];
			itr = this.columnMap.values().iterator();
			for (int i=0; i < count; i++)
			{
				cols[i] = (ColumnIdentifier)itr.next();
			}
			try
			{
				TableCreator creator = new TableCreator(this.targetConnection, this.targetTable, cols);
				creator.createTable();
				String msg = creator.getMessages();
				if (msg != null)
				{
					this.addMessage(msg);
				}
				this.addMessage(ResourceMgr.getString("MsgCopyTableCreated").replaceAll("%name%", aTargetTable.getTable()));
			}
			catch (SQLException e)
			{
				LogMgr.logError("DataCopier.copyFromTable()", "Error when creating target table", e);
				this.addError(ResourceMgr.getString("MsgCopyErrorCreatTable").replaceAll("%name%", aTargetTable.getTable()));
				this.addError(ExceptionUtil.getDisplay(e));
				throw e;
			}
		}
		else
		{
			this.addError(ResourceMgr.getString("ErrorCopyTargetTableNotFound").replaceAll("%name%", aTargetTable.getTable()));
			throw new SQLException("Table " + aTargetTable.getTable() + " not found in target connection");
		}
		this.initImporterForTable();
	}

	public void setAbortOnError(boolean flag)
	{
		this.abortOnError = flag;
	}

	public void setKeyColumns(List keys)
	{
		this.importer.setKeyColumns(keys);
	}

	public void setKeyColumns(String keys)
	{
		this.importer.setKeyColumns(keys);
	}
	
	/**
	 *	Forwards the setMode() call to the DataImporter.
	 *	@see workbench.db.importer.DataImporter#setMode(String)
	 */
	public boolean setMode(String mode)
	{
		return this.importer.setMode(mode);
	}

	public void setReportInterval(int value)
	{
		this.importer.setReportInterval(value);
	}
	
	/**
	 *	Initialize the DataCopier to create a copy of the source table by creating
	 *	a new table in the target database.
	 */
	public void copyToNewTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier newTableName, String additionalWhere)
		throws SQLException
	{
		this.copyToNewTable(source, target, aSourceTable, newTableName, null, additionalWhere);
	}

	/**
	 *	Set the definition to copy a table from source to target.
	 *	The table will be created in the target connection. If the table
	 *	already exists, an Exception will be thrown.
	 */
	public void copyToNewTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier newTableName, ColumnIdentifier[] sourceColumns, String additionalWhere)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = aSourceTable;
		this.targetTable = newTableName;
		this.useQuery = false;
		this.targetColumnsForQuery = null;
		this.setSourceTableWhere(additionalWhere);

		if (target.getMetadata().tableExists(newTableName))
		{
			LogMgr.logInfo("DataCopier.copyToNewTable()", "New table " + newTableName.getTableExpression() + " does already exist!");
			List requestedCols = null;
			if (sourceColumns != null)
			{
				int count = sourceColumns.length;
				requestedCols = new ArrayList(count);
				for (int i=0; i < count; i++)
				{
					requestedCols.add(sourceColumns[i]);
				}
			}
			this.readColumnDefinition(requestedCols);
		}
		else
		{
			this.initNewTable(sourceColumns);
		}

		this.initImporterForTable();
	}

	/**
	 *	Create a new table in the target database, and initialize the column mapping
	 *  so that the requested columns are copied.
	 *	If a column list is passed, the new table will contain only the passed columns,
	 *	otherwise it will contain all columns from the sourcetable.
	 *	The method expecst sourceConnect, targetConnection, sourceTable and targetTable
	 *	to be initialized accordingly
	 */
	private void initNewTable(ColumnIdentifier[] sourceColumns)
		throws SQLException
	{
		List sourceCols = null;
		if (this.sourceTable != null)
		{
			sourceCols = this.sourceConnection.getMetadata().getTableColumns(this.sourceTable);
		}
		else
		{
			int count = this.targetColumnsForQuery.length;
			sourceCols = new ArrayList(count);
			for (int i=0; i < count; i++)
			{
				sourceCols.add(this.targetColumnsForQuery[i]);
			}
		}

		// the names of the target columns are copied into
		// a List, in order to preserver the column order
		// which is either defined by the user (through the sourceColumns list)
		// or by the order of the existing table.
		List targetCols = null;
		if (sourceColumns == null)
		{
			int count = sourceCols.size();
			targetCols = new ArrayList(count);
			this.columnMap = new HashMap(count);
			for (int i=0; i < count; i++)
			{
				this.columnMap.put(sourceCols.get(i), sourceCols.get(i));
				targetCols.add(sourceCols.get(i));
			}
		}
		else
		{
			int count = sourceColumns.length;
			this.columnMap = new HashMap(count);
			targetCols = new ArrayList(count);
			for (int i=0; i < count; i++)
			{
				int index = sourceCols.indexOf(sourceColumns[i]);
				if (index > -1)
				{
					ColumnIdentifier col = (ColumnIdentifier)sourceCols.get(i);
					this.columnMap.put(col, col);
					targetCols.add(col);
				}
			}
		}

		int count = targetCols.size();
		ColumnIdentifier[] realCols = new ColumnIdentifier[count];
		for (int i=0; i < count; i++)
		{
			realCols[i] = (ColumnIdentifier)targetCols.get(i);
		}

		try
		{
			TableCreator creator = new TableCreator(this.targetConnection, this.targetTable, realCols);
			creator.createTable();

			// no need to delete rows from a newly created table
			this.setDeleteTarget(false);

			String msg = creator.getMessages();
			if (msg != null)
			{
				this.addMessage(msg);
			}
			this.addMessage(ResourceMgr.getString("MsgCopyTableCreated").replaceAll("%name%", this.targetTable.getTable()));
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataCopier.copyFromTable()", "Error when creating target table", e);
			this.addError(ResourceMgr.getString("MsgCopyErrorCreatTable").replaceAll("%name%", targetTable.getTable()));
			this.addError(ExceptionUtil.getDisplay(e));
			throw e;
		}
	}

	/**
	 *	Copy data from a SQL SELECT result to the given target table.
	 */
	public void copyFromQuery(WbConnection source, WbConnection target, String aSourceQuery, TableIdentifier aTargetTable, ColumnIdentifier[] queryColumns)
		throws SQLException
	{
		if (queryColumns == null || queryColumns.length == 0)
		{
			throw new IllegalArgumentException("Source and target column identifiers must be specified when using a SQL query!");
		}
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = null;
		this.targetTable = aTargetTable;
		this.retrieveSql = aSourceQuery;
		this.useQuery = true;
		this.targetColumnsForQuery = queryColumns;

		if (aTargetTable.isNewTable())
		{
			if (!target.getMetadata().tableExists(aTargetTable))
			{
				this.initNewTable(targetColumnsForQuery);
			}
		}
		this.initImporterForQuery();
	}

	/**
	 *	Define the source table, the target table and the column mapping
	 *	for the copy process.
	 *	This version of setDefinition allows to pass ColumnIdentifiers directly, if
	 *  they have been retrieved somewhere else (e.g. the DataPumper)
	 */
	public void copyFromTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier aTargetTable,
	                          ColumnIdentifier[] sourceColumns, ColumnIdentifier targetColumns[])
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = aSourceTable;
		this.targetTable = aTargetTable;
		this.useQuery = false;
		this.targetColumnsForQuery = null;
		if (sourceColumns == null || sourceColumns.length == 0 ||
		    targetColumns == null || targetColumns.length == 0 ||
		    targetColumns.length != sourceColumns.length)
		{
			this.readColumnDefinition();
		}
		else
		{
			int count = sourceColumns.length;

			this.columnMap = new HashMap(count);
			for (int i=0; i < count; i++)
			{
				this.columnMap.put(sourceColumns[i], targetColumns[i]);
			}
		}
		this.initImporterForTable();
	}

	public void setSourceTableWhere(String aWhere)
	{
		if (aWhere == null || aWhere.trim().length() == 0)
		{
			this.addWhere = null;
		}
		else
		{
			this.addWhere = aWhere; //SqlUtil.makeCleanSql(aWhere, false);
			String verb = SqlUtil.getSqlVerb(this.addWhere).toUpperCase();
			if ("SELECT".equals(verb))
			{
				LogMgr.logWarning("DataCopier", "Ignoring additional WHERE statement: " + this.addWhere);
				this.addWhere = null;
			}
		}
	}

	public void setRowActionMonitor(RowActionMonitor rowMonitor)
	{
		if (rowMonitor != null)
		{
			this.importer.setRowActionMonitor(rowMonitor);
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_COPY);
		}
	}

	public void setDeleteTarget(boolean delete)
	{
		this.importer.setDeleteTarget(delete);
	}

	public void setContinueOnError(boolean cont)
	{
		this.importer.setContinueOnError(cont);
	}

	public String getSourceTableWhere()
	{
		return this.addWhere;
	}

	public void setUseBatch(boolean flag)
	{
		this.importer.setUseBatch(flag);
	}
	public void setCommitEvery(int interval)
	{
		this.importer.setCommitEvery(interval);
	}

	public void startBackgroundCopy()
	{
		Thread t = new WbThread("DataCopier Thread")
		{
			public void run()
			{
				try 
				{ 
					// can't use start() because
					// this would refer to the start() method
					// of the thread, and not the from the DataCopier
					DataCopier.this.start(); 
				} 
				catch (Throwable th) 
				{ 
				}
			}
		};
		t.start();
	}

	public long getAffectedRows()
	{
		return this.importer.getAffectedRows();
	}

	public boolean isSuccess()
	{
		return this.importer.isSuccess();
	}

	public boolean hasWarnings()
	{
		return this.importer.hasWarning();
	}

	public String getErrorMessage()
	{
		if (this.errors == null) return null;
		return this.errors.toString();
	}

	public String[] getImportErrors()
	{
		return this.importer.getErrors();
	}

	public String[] getImportWarnings()
	{
		return this.importer.getWarnings();
	}

	public void start()
		throws Exception
	{
		ResultSet rs = null;

		String step = null;
		try
		{
			step = "retrieve";
			if (this.sourceData == null)
			{
				this.retrieveStatement = this.sourceConnection.createStatementForQuery();
				rs = this.retrieveStatement.executeQuery(this.retrieveSql);
				int colCount = rs.getMetaData().getColumnCount();
				Object[] rowData = new Object[colCount];
				step = "copy";
				while (this.keepRunning && rs.next())
				{
					for (int i=0; i < colCount; i++)
					{
						if (!keepRunning) break;
						rowData[i] = rs.getObject(i + 1);
					}
					if (this.keepRunning) this.importer.processRow(rowData);
				}
			}
			else
			{
				step = "copy";
				this.sourceData.start();
			}

			step = "cleanup";
			if (this.sourceData == null) this.importer.importFinished();
			String msg = this.getRowsInsertedMessage();
			if (msg != null) this.addMessage(msg);
			msg = this.getRowsUpdatedMessage();
			if (msg != null) this.addMessage(msg);

			LogMgr.logInfo("DataCopier.start()", "Copying of data finished. " + this.importer.getInsertedRows() + " row(s) inserted. " + this.importer.getUpdatedRows() + " row(s) updated.");
		}
		catch (Exception e)
		{
			LogMgr.logError("DataCopier.start()", "Error when copying data", e);
			String msg = null;
			if ("retrieve".equals(step))
			{
				msg = ResourceMgr.getString("ErrorCopyRetrieving");
			}
			else if ("copy".equals(step))
			{
				msg = ResourceMgr.getString("ErrorCopyInsert");
			}
			else if ("cleanup".equals(step))
			{
				msg = ResourceMgr.getString("ErrorCopyCleanup");
			}
			this.addError(msg + ": " + ExceptionUtil.getDisplay(e, false));
			this.importer.importCancelled();
			throw e;
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { this.retrieveStatement.close(); } catch (Throwable th) {}
		}
	}

	public String getRowsUpdatedMessage()
	{
		String msg = null;
		long rows = this.importer.getUpdatedRows();
		if (rows > 0)
		{
			msg = rows + " " + ResourceMgr.getString("MsgCopyNumRowsUpdated");
		}
		return msg;
	}

	public String getRowsInsertedMessage()
	{
		long rows = this.importer.getInsertedRows();
		String msg = null;

		if (rows > 0)
		{
			msg = rows + " " + ResourceMgr.getString("MsgCopyNumRowsInserted");
		}
		return msg;
	}

	public void setReceiver(RowDataReceiver receiver)
	{
		// ignore this call, as we know that it comes from the DataImporter
		// which is created in the constructor
	}

	public void cancel()
	{
		this.keepRunning = false;
		if (this.sourceData != null) 
		{
			this.sourceData.cancel();
		}
		this.importer.importCancelled();
	}

	private void initImporterForQuery()
		throws SQLException
	{
		if (!this.useQuery || this.targetColumnsForQuery == null) return;
		this.importer.setTargetTable(this.targetTable.getTableExpression(), this.targetColumnsForQuery);
	}

	/**
	 *	Send the definition of the target table to the DataImporter, and creates
	 *	the approriate SELECT statement to retrieve the data from the source
	 */
	private void initImporterForTable()
		throws SQLException
	{
		if (this.columnMap == null || this.columnMap.size() == 0)  return;
		int count = this.columnMap.size();
		ColumnIdentifier[] cols = new ColumnIdentifier[count];

		int col = 0;
		Iterator itr = this.columnMap.entrySet().iterator();

		StringBuffer sql = new StringBuffer(200);
		sql.append("SELECT ");

		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			ColumnIdentifier sid = (ColumnIdentifier)entry.getKey();
			ColumnIdentifier tid = (ColumnIdentifier)entry.getValue();
			if (col > 0)
			{
				sql.append("\n       , ");
			}
			sql.append(sid.getColumnName());
			cols[col] = tid;
			col ++;
		}
		sql.append(" \nFROM ");
		sql.append(this.sourceTable.getTableExpression());

		if (this.addWhere != null)
		{
			if (!this.addWhere.toUpperCase().startsWith("WHERE"))
			{
				sql.append(" \nWHERE ");
			}
			else
			{
				sql.append("\n ");
			}
			sql.append(this.addWhere);
		}
		this.retrieveSql = sql.toString();
		LogMgr.logDebug("DataCopier.initImporter()", "Using retrieve statement\n" + this.retrieveSql);
		try
		{
			this.importer.setTargetTable(this.targetTable.getTableExpression(), cols);
		}
		catch (SQLException e)
		{
			String msg = ResourceMgr.getString("ErrorCopyTargetTableNotFound").replaceAll("%table%", this.targetTable.getTableExpression());
			this.addMessage(msg);
			throw e;
		}
	}

	/**
	 *	Initialize the column mapping between source and target table.
	 *	If a mapping is provided, it is used (after checking that the columns
	 *	exist in both tables).
	 *	If no mapping is provided, all matching columns from both tables are copied
	 */
	private void initColumnMapping(Map aMapping)
		throws SQLException
	{
		if (this.sourceConnection == null || this.targetConnection == null ||
		    this.sourceTable == null || this.targetTable == null) return;

		// if no mapping is specified, read the matching columns from
		// the source and the target table
		if (aMapping == null || aMapping.size() == 0)
		{
			this.readColumnDefinition();
			return;
		}

		List sourceCols = this.sourceConnection.getMetadata().getTableColumns(this.sourceTable);
		List targetCols = this.targetConnection.getMetadata().getTableColumns(this.targetTable);

		Iterator itr = aMapping.entrySet().iterator();
		this.columnMap = new HashMap(targetCols.size());

		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String sc = (String)entry.getKey();
			String tc = (String)entry.getValue();
			if (sc == null || sc.trim().length() == 0 || tc == null || tc.trim().length() == 0) continue;

			// we are creating the Identifier without a type, so that when
			// comparing the ID's the type will not be considered
			ColumnIdentifier sid = new ColumnIdentifier(sc);
			ColumnIdentifier tid = new ColumnIdentifier(tc);

			// now check if the columns are actually present in the specified tables
			int sidx = sourceCols.indexOf(sid);
			int tidx = targetCols.indexOf(tid);

			if (sidx < 0)
			{
				LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + sc + " not found in table " + this.sourceTable + ". Ignoring mapping!");
				String msg = ResourceMgr.getString("ErrorCopySourceColumnNotFound").replaceAll("%name%", sc);
				this.addMessage(msg);
			}
			if (tidx < 0)
			{
				LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + tc + " not found in table " + this.targetTable + ". Ignoring mapping!");
				String msg = ResourceMgr.getString("ErrorCopyTargetColumnNotFound").replaceAll("%name%", tc);
				this.addMessage(msg);
			}

			if (sidx > -1 && tidx > -1)
			{
				LogMgr.logInfo("DataCopier.initColumnMapping()", "Copying " + this.sourceTable + "." + sc + " to " + this.targetTable + "." + tc);
				this.columnMap.put(sourceCols.get(sidx), targetCols.get(tidx));
			}

		}
	}

	private void readColumnDefinition()
		throws SQLException
	{
		this.readColumnDefinition(null);
	}
	/*
	 *	Initialize the internal column mapping.
	 *  This is done by reading the columns of the source and target table.
	 *  The columns which have the same name, will be copied
	 *	If a list of requested columns is passed only, those columns
	 *	from that list will be used from the source table
	 */
	private void readColumnDefinition(List requestedCols)
		throws SQLException
	{
		if (this.sourceConnection == null || this.targetConnection == null ||
		this.sourceTable == null || this.targetTable == null) return;

		List cols = this.sourceConnection.getMetadata().getTableColumns(this.sourceTable);
		List sourceCols = null;
		if (requestedCols != null)
		{
			int count = requestedCols.size();
			sourceCols = new ArrayList(count);
			for (int i=0; i < count; i++)
			{
				int index = cols.indexOf(requestedCols.get(i));
				if (index > -1)
				{
					sourceCols.add(cols.get(index));
				}
			}
		}
		else
		{
			sourceCols = cols;
		}
		
		List targetCols = this.targetConnection.getMetadata().getTableColumns(this.targetTable);

		int count = targetCols.size();
		this.columnMap = new HashMap(count);
		LogMgr.logInfo("DataCopier.readColumnDefinition()", "Copying matching columns from " + this.sourceTable + " to " + this.targetTable);
		StringBuffer usedColumns = new StringBuffer(100);
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier column = (ColumnIdentifier)targetCols.get(i);
			// ColumnIdentifier's equals() method checks the name and the data type!
			// so only columns where the name and the data type match, will be copied
			// note that this can be overridden by explicitly defining the column mapping
			if (sourceCols.contains(column))
			{
				LogMgr.logInfo("DataCopier.readColumnDefinition()", "Including column: " + column);
				this.columnMap.put(column, column);
				if (i > 0) usedColumns.append(',');
				usedColumns.append(column.getColumnName());
			}
		}
		if (requestedCols == null)
		{
			String msg = ResourceMgr.getString("MsgCopyColumnsUsed") + ": " + usedColumns.toString();
			this.addMessage(msg);
		}
	}

	private void addError(String msg)
	{
		if (this.errors == null) this.errors = new StringBuffer(250);
		if (this.errors.length() > 0) this.errors.append('\n');
		this.errors.append(msg);
	}


	private void addMessage(String msg)
	{
		if (this.messages == null) this.messages = new StringBuffer(250);
		if (this.messages.length() > 0) this.messages.append('\n');
		this.messages.append(msg);
	}

	public String getMessages()
	{
		if (this.messages == null) return null;
		return this.messages.toString();
	}

	public String getAllMessages()
	{
		StringBuffer log = new StringBuffer(250);

		String[] msg = this.getImportWarnings();
		int count = 0;
		if (msg != null)
		{
			count = msg.length;
			for (int i=0; i < count; i++)
			{
				if (msg[i] != null) log.append(msg[i]);
				log.append("\n");
			}
		}
		msg = this.getImportErrors();
		if (msg != null)
		{
			count = msg.length;
			if (count > 0) log.append("\n");
			for (int i=0; i < count; i++)
			{
				if (msg[i] != null) log.append(msg[i]);
				log.append("\n");
			}
		}

		String errmsg = this.getErrorMessage();
		if (errmsg != null) log.append(errmsg);

		return log.toString();
	}
}
