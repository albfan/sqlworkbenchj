/*
 * DataCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.datacopy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableCreator;
import workbench.db.TableDropper;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.DataImporter;
import workbench.db.importer.RowDataProducer;
import workbench.interfaces.BatchCommitter;
import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.ProgressReporter;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.MessageBuffer;
import workbench.util.WbThread;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DataCopier
	implements BatchCommitter, ProgressReporter
{
	private WbConnection sourceConnection;
	private WbConnection targetConnection;

	private RowDataProducer sourceData;

	private TableIdentifier sourceTable;
	private TableIdentifier targetTable;

	private DataImporter importer;

	// the columnMap will contain elements of type ColumnIdentifier
	private HashMap<ColumnIdentifier, ColumnIdentifier> columnMap;

	private ColumnIdentifier[] targetColumnsForQuery;
	private MessageBuffer messages = null;
	private MessageBuffer errors = null;

	public DataCopier()
	{
		this.importer = new DataImporter();
	}

	public void copyFromTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier aTargetTable)
		throws SQLException
	{
		this.copyFromTable(source, target, aSourceTable, aTargetTable, (Map<String, String>)null, null, false, false);
	}

	public void reset()
	{
		sourceData = null;
		sourceTable = null;
		sourceConnection = null;
		
		targetTable = null;
		targetColumnsForQuery = null;
		targetTable = null;
		targetConnection = null;
		
		columnMap = null;
		
		messages = new MessageBuffer();
		importer = new DataImporter();
		errors = new MessageBuffer();
	}
	
	public void copyFromFile(RowDataProducer source, WbConnection target, TableIdentifier targetTbl)
	{
		this.sourceConnection = null;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.targetTable = targetTbl;
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
	public void copyFromTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier aTargetTable, Map<String, String> columnMapping, String additionalWhere, boolean createTable, boolean dropTable)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = aSourceTable;
		this.targetTable = aTargetTable;
		this.targetColumnsForQuery = null;

		if (!this.sourceConnection.getMetadata().objectExists(aSourceTable, (String)null))
		{
			this.addError(ResourceMgr.getFormattedString("ErrCopySourceTableNotFound", aSourceTable.getQualifiedName()));
			throw new SQLException("Table " + aTargetTable.getTableName() + " not found in source connection");
		}

		//this.setSourceTableWhere(additionalWhere);
		boolean exists = this.targetConnection.getMetadata().tableExists(aTargetTable);

		if (exists && dropTable && createTable)
		{
			TableDropper dropper = new TableDropper(this.targetConnection);
			dropper.dropTable(aTargetTable);
			this.addMessage(ResourceMgr.getString("MsgCopyTableDropped").replaceAll("%name%", aTargetTable.getTableExpression(this.targetConnection)));
			exists = false;
		}

		if (exists)
		{
			this.initColumnMapping(columnMapping);
		}
		else if (createTable)
		{
			List sourceCols = source.getMetadata().getTableColumns(aSourceTable);

			this.columnMap = new HashMap<ColumnIdentifier, ColumnIdentifier>(columnMapping.size());
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
			try
			{
				TableCreator creator = new TableCreator(this.targetConnection, this.targetTable, this.columnMap.values());
				creator.useDbmsDataType(this.sourceConnection.getDatabaseProductName().equals(this.targetConnection.getDatabaseProductName()));
				creator.createTable();
				this.addMessage(ResourceMgr.getString("MsgCopyTableCreated").replaceAll("%name%", aTargetTable.getTableExpression(this.targetConnection)) + "\n");
			}
			catch (SQLException e)
			{
				LogMgr.logError("DataCopier.copyFromTable()", "Error when creating target table", e);
				this.addError(ResourceMgr.getString("MsgCopyErrorCreatTable").replaceAll("%name%", aTargetTable.getTableName()));
				this.addError(ExceptionUtil.getDisplay(e));
				throw e;
			}
		}
		else
		{
			this.addError(ResourceMgr.getString("ErrCopyTargetTableNotFound").replaceAll("%name%", aTargetTable.getQualifiedName()));
			throw new SQLException("Table " + aTargetTable.getTableName() + " not found in target connection");
		}
		this.initImporterForTable(additionalWhere);
	}

	public void setKeyColumns(List<ColumnIdentifier> keys)
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
	 *	Set the definition to copy a table from source to target.
	 *	The table will be created in the target connection. If the table
	 *	already exists, an Exception will be thrown.
	 */
	public void copyToNewTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier newTableName, ColumnIdentifier[] sourceColumns, String additionalWhere, boolean drop)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = aSourceTable;
		this.targetTable = newTableName;
		this.targetColumnsForQuery = null;

		boolean tableExists = target.getMetadata().tableExists(newTableName);

		if (tableExists && !drop)
		{
			LogMgr.logInfo("DataCopier.copyToNewTable()", "New table " + newTableName.getTableExpression() + " does already exist!");
			this.readColumnDefinition(sourceColumns);
		}
		else
		{
			if (tableExists && drop)
			{
				TableDropper dropper = new TableDropper(this.targetConnection);
				dropper.dropTable(newTableName);
				this.addMessage(ResourceMgr.getString("MsgCopyTableDropped").replaceAll("%name%", newTableName.getTableExpression(this.targetConnection)));
			}
			this.initNewTable(sourceColumns);
		}
		this.initImporterForTable(additionalWhere);
	}

	/**
	 *	Create a new table in the target database, and initialize the column mapping
	 *  so that the requested columns are copied.
	 *	If a column list is passed, the new table will contain only the passed columns,
	 *	otherwise it will contain all columns from the sourcetable.
	 *	The method expects sourceConnection, targetConnection, sourceTable and targetTable
	 *	to be initialized accordingly
	 */
	private void initNewTable(ColumnIdentifier[] sourceColumns)
		throws SQLException
	{
		List<ColumnIdentifier> sourceTableCols = null;
		
		if (this.sourceTable != null)
		{
			sourceTableCols = this.sourceConnection.getMetadata().getTableColumns(this.sourceTable);
		}
		else
		{
			sourceTableCols = new ArrayList<ColumnIdentifier>(this.targetColumnsForQuery.length);
			for (ColumnIdentifier col : targetColumnsForQuery)
			{
				sourceTableCols.add(col);
			}
		}

		// the names of the target columns are copied into
		// a List, in order to preserve the column order
		// which is either defined by the user (through the sourceColumns list)
		// or by the order of the existing table.
		List<ColumnIdentifier> targetCols = null;
		if (sourceColumns == null || sourceColumns.length == 0)
		{
			int count = sourceTableCols.size();
			targetCols = new ArrayList<ColumnIdentifier>(sourceTableCols);
			this.columnMap = new HashMap<ColumnIdentifier, ColumnIdentifier>(count);
			for (int i=0; i < count; i++)
			for (ColumnIdentifier col : sourceTableCols)
			{
				this.columnMap.put(col, col);
			}
		}
		else
		{
			this.columnMap = new HashMap<ColumnIdentifier, ColumnIdentifier>(sourceColumns.length);
			targetCols = new ArrayList<ColumnIdentifier>(sourceColumns.length);
			for (ColumnIdentifier col : sourceColumns)
			{
				int index = sourceTableCols.indexOf(col);
				if (index > -1)
				{
					ColumnIdentifier tcol = sourceTableCols.get(index);
					this.columnMap.put(col, col);
					targetCols.add(col);
				}
			}
		}

		try
		{
			TableCreator creator = new TableCreator(this.targetConnection, this.targetTable, targetCols);
			creator.useDbmsDataType(this.sourceConnection.getDatabaseProductName().equals(this.targetConnection.getDatabaseProductName()));
			creator.createTable();

			// no need to delete rows from a newly created table
			this.setDeleteTarget(false);

			this.addMessage(ResourceMgr.getString("MsgCopyTableCreated").replaceAll("%name%", this.targetTable.getTableExpression(this.targetConnection)) + "\n");
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataCopier.copyFromTable()", "Error when creating target table", e);
			this.addError(ResourceMgr.getString("MsgCopyErrorCreatTable").replaceAll("%name%", targetTable.getTableExpression(this.targetConnection)));
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
		this.targetColumnsForQuery = queryColumns;

		if (aTargetTable.isNewTable())
		{
			if (!target.getMetadata().tableExists(aTargetTable))
			{
				this.initNewTable(targetColumnsForQuery);
			}
		}
		this.initImporterForQuery(aSourceQuery);
	}

	/**
	 *	Define the source table, the target table and the column mapping
	 *	for the copy process.
	 *	This version of setDefinition allows to pass ColumnIdentifiers directly, if
	 *  they have been retrieved somewhere else (e.g. the DataPumper)
	 */
	public void copyFromTable(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier aTargetTable,
	                          ColumnIdentifier[] sourceColumns, ColumnIdentifier targetColumns[], String additionalWhere)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = aSourceTable;
		this.targetTable = aTargetTable;
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

			this.columnMap = new HashMap<ColumnIdentifier, ColumnIdentifier>(count);
			for (int i=0; i < count; i++)
			{
				this.columnMap.put(sourceColumns[i], targetColumns[i]);
			}
		}
		this.initImporterForTable(additionalWhere);
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

	public void setUseBatch(boolean flag)
	{
		this.importer.setUseBatch(flag);
	}

	public void setCommitBatch(boolean flag)
	{
		this.importer.setCommitBatch(flag);
	}
	
	public void setBatchSize(int size) 
	{
		this.importer.setBatchSize(size);
	}
	
	public void commitNothing()
	{
		this.importer.commitNothing();
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
					startCopy();
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

	public String getErrorMessage()
	{
		if (this.errors == null) return null;
		return this.errors.toString();
	}

	public void startCopy()
		throws Exception
	{
		try
		{
			// this will call start() or sourceData.start()
			// depending on which source we set for the importer
			this.sourceData.setAbortOnError(!this.importer.getContinueOnError());
			this.importer.startImport();
			
			LogMgr.logInfo("DataCopier.start()", "Copying of data finished. " + this.importer.getInsertedRows() + " row(s) inserted. " + this.importer.getUpdatedRows() + " row(s) updated.");
		}
		catch (Exception e)
		{
			LogMgr.logError("DataCopier.start()", "Error when copying data", e);
			String msg = ResourceMgr.getString("ErrCopy");
			this.addError(msg + ": " + ExceptionUtil.getDisplay(e, false));
			this.importer.tableImportError();
			throw e;
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

	public void cancel()
	{
		this.sourceData.cancel();
	}

	private void initImporterForQuery(String query)
		throws SQLException
	{
		if (this.targetColumnsForQuery == null) return;
		this.importer.setTargetTable(this.targetTable, this.targetColumnsForQuery);
		initQuerySource(query);
	}

	/**
	 *	Send the definition of the target table to the DataImporter, and creates
	 *	the approriate SELECT statement to retrieve the data from the source
	 */
	private void initImporterForTable(String addWhere)
		throws SQLException
	{
		if (this.columnMap == null || this.columnMap.size() == 0)  
		{
			throw new SQLException("No columns defined");
		}
		int count = this.columnMap.size();
		ColumnIdentifier[] cols = new ColumnIdentifier[count];

		int col = 0;

		StringBuilder sql = new StringBuilder(count * 25 + 15 + (addWhere != null ? addWhere.length() : 0));
		sql.append("SELECT ");

		//while (itr.hasNext())
		for (Map.Entry<ColumnIdentifier, ColumnIdentifier> entry : this.columnMap.entrySet())
		{
			ColumnIdentifier sid = entry.getKey();
			ColumnIdentifier tid = entry.getValue();
			if (col > 0)
			{
				sql.append("\n       , ");
			}
			sql.append(sid.getColumnName());
			cols[col] = tid;
			col ++;
		}
		sql.append(" \nFROM ");
		sql.append(this.sourceTable.getTableExpression(this.sourceConnection));

		if (addWhere != null && addWhere.trim().length() > 0)
		{
			if (!addWhere.toUpperCase().startsWith("WHERE"))
			{
				sql.append(" \nWHERE ");
			}
			else
			{
				sql.append("\n ");
			}
			sql.append(addWhere);
		}
		initQuerySource(sql.toString());
		
		try
		{
			this.importer.setTargetTable(this.targetTable, cols);
		}
		catch (SQLException e)
		{
			String msg = ResourceMgr.getString("ErrCopyTargetTableNotFound").replaceAll("%table%", this.targetTable.getTableExpression());
			this.addMessage(msg);
			throw e;
		}
	}

	private void initQuerySource(String sql)
	{
		QueryCopySource source = new QueryCopySource(this.sourceConnection, sql);
		this.sourceData = source;
		this.importer.setProducer(source);
		LogMgr.logDebug("DataCopier.initImporter()", "Using retrieve statement\n" + sql);
	}
	
	/**
	 *	Initialize the column mapping between source and target table.
	 *	If a mapping is provided, it is used (after checking that the columns
	 *	exist in both tables).
	 *	If no mapping is provided, all matching columns from both tables are copied
	 */
	private void initColumnMapping(Map<String, String> aMapping)
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

		DbMetadata sourceMeta = this.sourceConnection.getMetadata();
		TableIdentifier tbl = sourceMeta.resolveSynonym(this.sourceTable);
		List<ColumnIdentifier> sourceCols = sourceMeta.getTableColumns(tbl);
		List<ColumnIdentifier> targetCols = this.targetConnection.getMetadata().getTableColumns(this.targetTable);

		this.columnMap = new HashMap<ColumnIdentifier, ColumnIdentifier>(targetCols.size());

		for (Map.Entry<String, String> entry : aMapping.entrySet())
		{
			String sc = entry.getKey();
			String tc = entry.getValue();
			if (sc == null || sc.trim().length() == 0 || tc == null || tc.trim().length() == 0) continue;

			// I'm creating the Identifier without a type, so that when
			// comparing the ID's the type will not be considered
			ColumnIdentifier sid = new ColumnIdentifier(sc);
			ColumnIdentifier tid = new ColumnIdentifier(tc);

			// now check if the columns are actually present in the specified tables
			int sidx = sourceCols.indexOf(sid);
			int tidx = targetCols.indexOf(tid);

			if (sidx < 0)
			{
				LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + sc + " not found in table " + this.sourceTable + ". Ignoring mapping!");
				String msg = ResourceMgr.getString("ErrCopySourceColumnNotFound").replaceAll("%name%", sc);
				this.addMessage(msg);
			}
			if (tidx < 0)
			{
				LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + tc + " not found in table " + this.targetTable + ". Ignoring mapping!");
				String msg = ResourceMgr.getString("ErrCopyTargetColumnNotFound").replaceAll("%name%", tc);
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
	private void readColumnDefinition(ColumnIdentifier[] requestedCols)
		throws SQLException
	{
		if (this.sourceConnection == null || this.targetConnection == null ||
		this.sourceTable == null || this.targetTable == null) return;

		List<ColumnIdentifier> cols = this.sourceConnection.getMetadata().getTableColumns(this.sourceTable);
		List<ColumnIdentifier> sourceCols = null;
		if (requestedCols != null)
		{
			sourceCols = new ArrayList<ColumnIdentifier>(requestedCols.length);
			for (ColumnIdentifier rc : requestedCols)
			{
				int index = cols.indexOf(rc);
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

		List<ColumnIdentifier> targetCols = this.targetConnection.getMetadata().getTableColumns(this.targetTable);

		int count = targetCols.size();
		this.columnMap = new HashMap<ColumnIdentifier, ColumnIdentifier>(count);
		LogMgr.logInfo("DataCopier.readColumnDefinition()", "Copying matching columns from " + this.sourceTable + " to " + this.targetTable);
		StringBuilder usedColumns = new StringBuilder(100);
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier column = targetCols.get(i);
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
			String msg = ResourceMgr.getString("MsgCopyColumnsUsed") + ": " + usedColumns.toString() + "\n";
			this.addMessage(msg);
		}
	}

	private void addError(String msg)
	{
		if (this.errors == null) this.errors = new MessageBuffer();
		if (this.errors.getLength() > 0) this.errors.appendNewLine();
		this.errors.append(msg);
	}


	private void addMessage(String msg)
	{
		if (this.messages == null) this.messages = new MessageBuffer();
		if (this.messages.getLength() > 0) this.messages.appendNewLine();
		this.messages.append(msg);
	}

	public boolean hasWarnings()
	{
		return this.importer.hasWarnings();
	}

	public MessageBuffer getMessageBuffer()
	{
		MessageBuffer buf = new MessageBuffer();
		buf.append(this.messages);
		buf.append(this.importer.getMessageBuffer());
		buf.append(this.errors);
		return buf;
	}
	
	public CharSequence getAllMessages()
	{
		StringBuilder log = new StringBuilder(2000);

		if (this.messages != null) 
		{
			log.append(messages.getBuffer());
			log.append('\n');
		}
		log.append(this.importer.getMessages());
		if (this.errors != null) log.append(this.errors.getBuffer());

		return log;
	}

	public void setErrorHandler(JobErrorHandler handler)
	{
	}
}
