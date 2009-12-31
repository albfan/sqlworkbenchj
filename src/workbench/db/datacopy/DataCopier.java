/*
 * DataCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.datacopy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.GenericObjectDropper;
import workbench.db.TableCreator;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.compare.TableDeleteSync;
import workbench.db.importer.DataImporter;
import workbench.db.importer.DeleteType;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.RowDataReceiver;
import workbench.db.importer.TableStatements;
import workbench.interfaces.BatchCommitter;
import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.ObjectDropper;
import workbench.interfaces.ProgressReporter;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.CollectionUtil;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * A class to copy data from one database to another.
 * DataCopier supports copying multiple tables or just a single table. When
 * copying multiple tables (using {@link #setTableList(java.util.List)} the
 * tables between source and destination are matched by name.
 *
 * When copying a single table, source and target table need not have the same
 * name.
 *
 * The source data is always retrieved using a {@link QueryCopySource}
 *
 * @author Thomas Kellerer
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

	// columnMap maps columns from the source (table or query) to the target table
	private Map<ColumnIdentifier, ColumnIdentifier> columnMap;

	private List<ColumnIdentifier> targetColumnsForQuery;
	private MessageBuffer messages = null;
	private MessageBuffer errors = null;
	private boolean doSyncDelete = false;

	public DataCopier()
	{
		this.importer = new DataImporter();
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
		importer.clearMessages();
		messages = new MessageBuffer();
		errors = new MessageBuffer();
	}

	public void setTransactionControl(boolean flag)
	{
		this.importer.setTransactionControl(flag);
	}

	public void beginMultiTableCopy(WbConnection targetConn)
		throws SQLException
	{
		this.importer.setConnection(targetConn);
		this.importer.beginMultiTable();
	}

	public void setUseSavepoint(boolean flag)
	{
		if (importer != null) importer.setUseSavepoint(flag);
	}

	public void endMultiTableCopy()
	{
		this.importer.endMultiTable();
	}

	public RowDataProducer getSource()
	{
		return this.sourceData;
	}

	public RowDataReceiver getReceiver()
	{
		return this.importer;
	}

	public void setKeyColumns(List<ColumnIdentifier> keys)
	{
		this.importer.setKeyColumns(keys);
	}

	public void setKeyColumns(String keys)
	{
		this.importer.setKeyColumns(keys);
	}

	public void setPerTableStatements(TableStatements stmt)
	{
		this.importer.setPerTableStatements(stmt);
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

	public void setProducer(RowDataProducer source, WbConnection target, TableIdentifier targetTbl)
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
	public void copyFromTable(WbConnection source,
														WbConnection target,
														TableIdentifier sourceTable,
														TableIdentifier targetTable,
														Map<String, String> columnMapping,
														String additionalWhere,
														boolean createTable,
														boolean dropTable,
														boolean ignoreDropError)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = sourceTable;
		this.targetTable = targetTable;
		this.targetColumnsForQuery = null;

		if (!this.sourceConnection.getMetadata().objectExists(sourceTable, (String)null))
		{
			this.addError(ResourceMgr.getFormattedString("ErrCopySourceTableNotFound", sourceTable.getQualifiedName()));
			throw new SQLException("Table " + sourceTable.getTableName() + " not found in source connection");
		}

		this.initColumnMapping(columnMapping, createTable);

		if (createTable)
		{
			createTable(this.columnMap.values(), dropTable, ignoreDropError);
		}

		this.initImporterForTable(additionalWhere);
	}

	private TableIdentifier findTargetTable()
	{
		LogMgr.logDebug("DataCopier.findTargetTable()", "Looking for table " + targetTable.getQualifiedName() + " in target database");
		TableIdentifier realTable = this.targetConnection.getMetadata().findTable(targetTable);
		if (realTable == null)
		{
			TableIdentifier toFind = targetTable.createCopy();

			toFind.setSchema(toFind.getSchemaToUse(targetConnection));
			toFind.setCatalog(toFind.getCatalogToUse(targetConnection));
			LogMgr.logDebug("DataCopier.findTargetTable()", "Table " + targetTable.getQualifiedName() + " not found. Trying " + toFind.getQualifiedName());
			realTable = this.targetConnection.getMetadata().findTable(toFind);
		}
		return realTable;
	}

	private void createTable(Collection<ColumnIdentifier> columns, boolean dropIfExists, boolean ignoreError)
		throws SQLException
	{
		if (dropIfExists)
		{
			TableIdentifier toDrop = findTargetTable();
			if (toDrop != null)
			{
				LogMgr.logInfo("DataCopier.createTable()", "About to drop table " + toDrop.getQualifiedName());
				try
				{
					ObjectDropper dropper = new GenericObjectDropper();
					dropper.setObjects(CollectionUtil.arrayList(toDrop));
					dropper.setConnection(targetConnection);
					dropper.dropObjects();
					this.addMessage(ResourceMgr.getFormattedString("MsgCopyTableDropped", toDrop.getQualifiedName()));
				}
				catch (SQLException e)
				{
					String msg = ResourceMgr.getFormattedString("MsgCopyErrorCreatTable",toDrop.getTableExpression(this.targetConnection),ExceptionUtil.getDisplay(e));
					if (ignoreError)
					{
						this.addMessage(msg);
					}
					else
					{
						this.addError(msg);
						throw e;
					}
				}
			}
			else
			{
				LogMgr.logInfo("DataCopier.createTable()", "Table " + targetTable.getQualifiedName() + " not dropped because it was not found in the target database");
			}
		}

		try
		{
			List<ColumnIdentifier> targetCols = new ArrayList<ColumnIdentifier>(columns.size());
			for (ColumnIdentifier col : columns)
			{
				// When copying a table from MySQL or SQL Server to a standard compliant DBMS we must ensure
				// that wrong quoting characters are replaced with the standard characters
				ColumnIdentifier copy = col.createCopy();
				copy.adjustQuotes(sourceConnection.getMetadata(), targetConnection.getMetadata());
				targetCols.add(col);
			}

			TableCreator creator = new TableCreator(this.targetConnection, this.targetTable, targetCols);
			creator.setUseColumnAlias(true); // if an alias was specified in the original query, the new table should use that one
			creator.useDbmsDataType(this.sourceConnection.getDatabaseProductName().equals(this.targetConnection.getDatabaseProductName()));
			creator.createTable();

			TableDefinition newTable = targetConnection.getMetadata().getTableDefinition(targetTable);
			targetTable = newTable.getTable();

			// When the source of the copy is a table (defined by copyFromTable()) then a column mapping
			// is present. In that case the definition of the columns stored in the columnMap's values
			// must be updated to reflect the definitions of the just created table
			if (columnMap != null)
			{
				updateTargetColumns(newTable.getColumns(), columnMap.values());
			}
			else if (targetColumnsForQuery != null)
			{
				updateTargetColumns(newTable.getColumns(), targetColumnsForQuery);
			}

			// no need to delete rows from a newly created table
			this.setDeleteTarget(DeleteType.none);

			this.addMessage(ResourceMgr.getFormattedString("MsgCopyTableCreated", this.targetTable.getTableExpression(this.targetConnection)) + "\n");
		}
		catch (SQLException e)
		{
			//LogMgr.logError("DataCopier.copyFromTable()", "Error when creating target table", e);
			this.addError(ResourceMgr.getFormattedString("MsgCopyErrorCreatTable",targetTable.getTableExpression(this.targetConnection),ExceptionUtil.getDisplay(e)));
			throw e;
		}
	}

	/**
	 * After creating the target table, the column definitions for the newly
	 * create table are retrieved and we have to use those columns e.g.
	 * because upper/lowercase can be different now. 
	 *
	 * @param realCols
	 * @param toUpdate to column definitions to be updated
	 */
	private void updateTargetColumns(List<ColumnIdentifier> realCols, Collection<ColumnIdentifier> toUpdate)
	{
		for (ColumnIdentifier targetCol : toUpdate)
		{
			ColumnIdentifier realCol = findColumn(realCols, targetCol.getDisplayName());
			if (realCol != null)
			{
				targetCol.setColumnName(realCol.getDisplayName());
				targetCol.setDbmsType(realCol.getDbmsType());
				targetCol.setDataType(realCol.getDataType());
				targetCol.setColumnSize(realCol.getColumnSize());
				targetCol.setDecimalDigits(realCol.getDecimalDigits());
			}
		}
	}

	/**
	 *	Copy data from a SQL SELECT result to the given target table.
	 */
	public void copyFromQuery(WbConnection source,
														WbConnection target,
														String query,
														TableIdentifier aTargetTable,
														List<ColumnIdentifier> queryColumns,
														boolean createTarget,
														boolean dropTarget,
														boolean ignoreDropError)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = null;
		this.targetTable = aTargetTable;

		this.targetColumnsForQuery = new ArrayList<ColumnIdentifier>(queryColumns);
		if (createTarget)
		{
			createTable(targetColumnsForQuery, dropTarget, ignoreDropError);
		}
		this.initImporterForQuery(query);
	}

	public void setRowActionMonitor(RowActionMonitor rowMonitor)
	{
		if (rowMonitor != null)
		{
			this.importer.setRowActionMonitor(rowMonitor);
		}
	}

	public void setTableList(List<TableIdentifier> tables)
	{
		this.importer.setTableList(tables);
	}

	public void setDeleteTarget(DeleteType delete)
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

	public int getBatchSize()
	{
		return this.importer.getBatchSize();
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

	public void setDoDeleteSync(boolean flag)
	{
		this.doSyncDelete = flag;
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

			if (this.doSyncDelete)
			{
				TableDeleteSync sync = new TableDeleteSync(this.targetConnection, this.sourceConnection);
				sync.setTableName(this.sourceTable, this.targetTable);
				sync.setRowMonitor(this.importer.getRowActionMonitor());
				sync.setBatchSize(this.getBatchSize());
				sync.setReportInterval(this.importer.getReportInterval());
				sync.doSync();
				long rows = sync.getDeletedRows();
				String msg = ResourceMgr.getFormattedString("MsgCopyNumRowsDeleted", rows, targetTable.getTableName());
				this.addMessage(msg);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataCopier.start()", "Error when copying data", e);
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
		List<ColumnIdentifier> realCols = targetConnection.getMetadata().getTableColumns(targetTable);
		updateTargetColumns(realCols, targetColumnsForQuery);
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
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>(count);

		int col = 0;

		StringBuilder sql = new StringBuilder(count * 25 + 30);
		sql.append("SELECT ");

		for (Map.Entry<ColumnIdentifier, ColumnIdentifier> entry : this.columnMap.entrySet())
		{
			ColumnIdentifier sid = entry.getKey();
			ColumnIdentifier tid = entry.getValue();
			if (col > 0)
			{
				sql.append(", ");
			}
			sql.append(sourceConnection.getMetadata().quoteObjectname(sid.getColumnName()));
			cols.add(tid);
			col ++;
		}

		sql.append(" FROM ");
		sql.append(this.sourceTable.getTableExpression(this.sourceConnection));

		if (StringUtil.isNonBlank(addWhere))
		{
			sql.append(' ');
			String first = SqlUtil.getSqlVerb(addWhere);
			if (!first.equals("WHERE"))
			{
				sql.append(" WHERE ");
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
			String msg = ResourceMgr.getFormattedString("ErrCopyTargetTableNotFound", this.targetTable.getTableExpression());
			this.addMessage(msg);
			throw e;
		}
	}

	private void initQuerySource(String sql)
	{
		QueryCopySource source = new QueryCopySource(this.sourceConnection, sql);
		this.sourceData = source;
		this.importer.setProducer(source);
	}

	private List<ColumnIdentifier> getSourceColumns()
		throws SQLException
	{
		List<ColumnIdentifier> sourceCols = null;
		if (this.sourceTable != null)
		{
			DbMetadata sourceMeta = this.sourceConnection.getMetadata();
			TableIdentifier tbl = sourceMeta.resolveSynonym(this.sourceTable);
			sourceCols = sourceMeta.getTableColumns(tbl);
		}
		else if (this.targetColumnsForQuery != null)
		{
			sourceCols = new ArrayList<ColumnIdentifier>(this.targetColumnsForQuery);
		}
		return sourceCols;
	}

	private ColumnIdentifier findColumn(List<ColumnIdentifier> columns, String colname)
	{
		if (columns == null) return null;
		if (colname == null) return null;

		for (ColumnIdentifier col : columns)
		{
			String name = col.getColumnName();
			if (name.equalsIgnoreCase(colname)) return col;
		}
		return null;
	}

	private void addTargetColumn(ColumnIdentifier sourceCol, String targetName, List<ColumnIdentifier> targetCols)
	{
		if (sourceCol == null) return;
		ColumnIdentifier targetCol = findColumn(targetCols, (targetName == null ? sourceCol.getColumnName() : targetName));
		if (targetCol != null)
		{
			this.columnMap.put(sourceCol, targetCol);
		}
		else
		{
			LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + sourceCol.toString() + " not found in target table " + this.targetTable + ". Ignoring mapping!");
			String msg = ResourceMgr.getFormattedString("ErrCopyTargetColumnNotFound", sourceCol.toString());
			this.addMessage(msg);
		}
	}

	/**
	 *	Initialize the column mapping between source and target table.
	 *	If a mapping is provided, it is used (after checking that the columns
	 *	exist in both tables).
	 *	If no mapping is provided, all matching columns from both tables are copied
	 */
	private void initColumnMapping(Map<String, String> columnMapping, boolean createNew)
		throws SQLException
	{
		List<ColumnIdentifier> sourceCols = getSourceColumns();
		List<ColumnIdentifier> targetCols = null;
		if (!createNew) targetCols = this.targetConnection.getMetadata().getTableColumns(this.targetTable);

		this.columnMap = new HashMap<ColumnIdentifier, ColumnIdentifier>(sourceCols.size());

		if (columnMapping != null)
		{
			int colPos = 0;
			for (Map.Entry<String, String> entry : columnMapping.entrySet())
			{
				ColumnIdentifier sourceCol = findColumn(sourceCols, entry.getKey());
				if (sourceCol != null)
				{
					if (createNew)
					{
						// If no target column specified (e.g. using -columns=col1,col2,col3)
						// then simply use the name from the source table
						ColumnIdentifier targetCol = sourceCol.createCopy();
						if (entry.getValue() != null)
						{
							// Mapping specified, change the name of the column to the specified value
							targetCol.setColumnName(entry.getValue());
							targetCol.adjustQuotes(sourceConnection.getMetadata(), targetConnection.getMetadata());
						}

						// Make sure the order of the columns is preserved
						// when creating the table later, the columns will
						// be sorted by the position before generating the SQL
						targetCol.setPosition(colPos);
						colPos++;
						this.columnMap.put(sourceCol, targetCol);
					}
					else
					{
						addTargetColumn(sourceCol, entry.getValue(), targetCols);
					}
				}
				else
				{
					LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + entry.getKey() + " not found in source table " + this.sourceTable + ". Ignoring mapping!");
					String msg = ResourceMgr.getFormattedString("ErrCopySourceColumnNotFound", entry.getKey());
					this.addMessage(msg);
				}
			}
		}
		else
		{
			// Use all columns from the source table
			for (ColumnIdentifier scol : sourceCols)
			{
				if (createNew)
				{
					columnMap.put(scol, scol.createCopy());
				}
				else
				{
					addTargetColumn(scol, null, targetCols);
				}

			}
		}
	}

	public void addError(String msg)
	{
		if (this.errors == null) this.errors = new MessageBuffer();
		if (this.errors.getLength() > 0) this.errors.appendNewLine();
		this.errors.append(msg);
	}


	public void addMessage(String msg)
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
		if (messages != null)
		{
			buf.append(this.messages);
			messages.clear();
		}
		importer.copyMessages(buf);
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
