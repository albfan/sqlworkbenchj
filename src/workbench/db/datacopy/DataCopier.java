/*
 * DataCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.datacopy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import workbench.interfaces.BatchCommitter;
import workbench.interfaces.ObjectDropper;
import workbench.interfaces.ProgressReporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.DropType;
import workbench.db.GenericObjectDropper;
import workbench.db.TableCreator;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;
import workbench.db.compare.TableDeleteSync;
import workbench.db.compare.TableDiffStatus;
import workbench.db.importer.DataImporter;
import workbench.db.importer.DataReceiver;
import workbench.db.importer.DeleteType;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.TableStatements;

import workbench.storage.RowActionMonitor;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.MessageBuffer;
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
	private MessageBuffer messages;
	private MessageBuffer errors;
	private boolean doSyncDelete;
	private boolean ignoreColumnDefaultsForCreate;
	private boolean trimCharData;

	public DataCopier()
	{
		this.importer = new DataImporter();
	}

	public void reset()
	{
		sourceData = null;
		sourceTable = null;
		targetTable = null;
		sourceConnection = null;
		targetColumnsForQuery = null;
		targetConnection = null;
		columnMap = null;
		importer.clearMessages();
		messages = new MessageBuffer();
		errors = new MessageBuffer();
	}

	public void setTrimCharData(boolean flag)
	{
		this.trimCharData = flag;
	}

	public void setAdjustSequences(boolean flag)
	{
		this.importer.setAdjustSequences(flag);
	}

	/**
	 * Controls if column defaults should be used when creating the targt table.
	 * @param flag
	 */
	public void setIgnoreColumnDefaults(boolean flag)
	{
		this.ignoreColumnDefaultsForCreate = flag;
	}

	public void setIgnoreIdentityColumns(boolean flag)
	{
		this.importer.setIgnoreIdentityColumns(flag);
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

	public DataReceiver getReceiver()
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

	@Override
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
	 *	The key is the name of the source column, the mapped value is the name of the target column
	 *
	 * @param source the source connection
	 * @param target the target connection
	 * @param sourceTbl  the table to copy (in the database defined by source)
	 * @param targetTbl  the table write to (in the database defined by target)
	 * @param columnMapping define a mapping from source columns to target columns (may be null)
	 * @param additionalWhere a WHERE condition to be appended to the SELECT that retrieves the data from the source table
	 * @param createTableType if not null, the target table will be created with the template defined by this type
	 * @param dropTable if true, the target table will be dropped
	 * @param ignoreDropError if true, an error during drop will not terminate the copy
	 * @param skipTargetCheck  use the columns from the source table for the target table.
	 *                                  This can be useful if the JDBC driver does not return information
	 *                                  about the target table e.g. because it is a temporary table.
	 * @see DbSettings#getCreateTableTemplate(java.lang.String)
	 */
	public void copyFromTable(WbConnection source,
														WbConnection target,
														TableIdentifier sourceTbl,
														TableIdentifier targetTbl,
														Map<String, String> columnMapping,
														String additionalWhere,
														String createTableType,
														DropType dropTable,
														boolean ignoreDropError,
														boolean skipTargetCheck)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = sourceTbl;
		this.targetTable = targetTbl;
		this.targetColumnsForQuery = null;

		if (!this.sourceConnection.getMetadata().objectExists(sourceTbl, (String)null))
		{
			this.addError(ResourceMgr.getFormattedString("ErrCopySourceTableNotFound", sourceTbl.getQualifiedName()));
			throw new SQLException("Table " + sourceTbl.getTableName() + " not found in source connection");
		}

		this.initColumnMapping(columnMapping, createTableType != null, skipTargetCheck);

		if (createTableType != null)
		{
			createTable(this.columnMap.values(), dropTable, ignoreDropError, createTableType, skipTargetCheck);
		}

		// this flag must be set before calling initImporterForTable!
		importer.skipTargetCheck(skipTargetCheck);

		this.initImporterForTable(additionalWhere);

	}

	private TableIdentifier findTargetTable()
	{
		LogMgr.logDebug("DataCopier.findTargetTable()", "Looking for table " + targetTable.getQualifiedName() + " in target database");
		TableIdentifier realTable = this.targetConnection.getMetadata().findTable(targetTable, false);
		if (realTable == null)
		{
			TableIdentifier toFind = targetTable.createCopy();

			toFind.setSchema(toFind.getSchemaToUse(targetConnection));
			toFind.setCatalog(toFind.getCatalogToUse(targetConnection));
			LogMgr.logDebug("DataCopier.findTargetTable()", "Table " + targetTable.getQualifiedName() + " not found. Trying " + toFind.getQualifiedName());
			realTable = this.targetConnection.getMetadata().findTable(toFind, false);
		}
		return realTable;
	}

	private void createTable(Collection<ColumnIdentifier> columns, DropType dropIfExists, boolean ignoreError, String createType, boolean skipTargetCheck)
		throws SQLException
	{
		if (dropIfExists == DropType.cascaded || dropIfExists == DropType.regular)
		{
			TableIdentifier toDrop = null;
			if (skipTargetCheck)
			{
				toDrop = targetTable.createCopy();
				targetTable.adjustCase(targetConnection);
			}
			else
			{
				toDrop = findTargetTable();
			}

			if (toDrop != null)
			{
				LogMgr.logInfo("DataCopier.createTable()", "About to drop table " + toDrop.getQualifiedName());
				try
				{
					ObjectDropper dropper = new GenericObjectDropper();
					dropper.setObjects(Collections.singletonList(toDrop));
					dropper.setCascade(dropIfExists == DropType.cascaded);
					dropper.setConnection(targetConnection);
					dropper.dropObjects();
					this.addMessage(ResourceMgr.getFormattedString("MsgCopyTableDropped", toDrop.getQualifiedName()));
				}
				catch (SQLException e)
				{
					String msg = ResourceMgr.getFormattedString("MsgCopyErrorDropTable",toDrop.getTableExpression(this.targetConnection),ExceptionUtil.getDisplay(e));
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
			List<ColumnIdentifier> pkCols = this.importer.getKeyColumns();

			List<ColumnIdentifier> targetCols = new ArrayList<>(columns.size());
			for (ColumnIdentifier col : columns)
			{
				// When copying a table from MySQL or SQL Server to a standard compliant DBMS we must ensure
				// that wrong quoting characters are removed
				ColumnIdentifier copy = col.createCopy();
				String name = col.getColumnName();

				if (skipTargetCheck)
				{
					name = sourceConnection.getMetadata().removeQuotes(name);
					name = targetConnection.getMetadata().adjustObjectnameCase(name);
					copy.setColumnName(name);
				}
				else
				{
					copy.adjustQuotes(sourceConnection.getMetadata(), targetConnection.getMetadata());
				}
				if (pkCols.size() > 0)
				{
					boolean isPK = findColumn(pkCols, copy.getColumnName()) != null;
					col.setIsPkColumn(isPK);
				}
				targetCols.add(copy);
			}

			TableCreator creator = new TableCreator(this.targetConnection, createType, this.targetTable, targetCols);
			creator.setUseColumnAlias(true); // if an alias was specified in the original query, the new table should use that one
			creator.useDbmsDataType(this.sourceConnection.getDatabaseProductName().equals(this.targetConnection.getDatabaseProductName()));
			creator.setRemoveDefaults(ignoreColumnDefaultsForCreate);
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
				targetCol.setColumnName(realCol.getColumnName());
				targetCol.setColumnAlias(realCol.getDisplayName());
				targetCol.setDbmsType(realCol.getDbmsType());
				targetCol.setDataType(realCol.getDataType());
				targetCol.setColumnSize(realCol.getColumnSize());
				targetCol.setDecimalDigits(realCol.getDecimalDigits());
			}
		}
	}

	/**
	 *	Copy data from a SQL SELECT result to the given target table.
	 *
	 * @param source          the source connection
	 * @param target          the target connection
	 * @param query           the query to be executed on the source connection
	 * @param aTargetTable    the table write to (in the database defined by target)
	 * @param queryColumns    the columns from the query
	 * @param createTableType if not null, the target table will be created with the template defined by this type
	 * @param dropTarget      if true, the target table will be dropped
	 * @param ignoreDropError if true, an error during drop will not terminate the copy
	 * @param skipTargetCheck if true the existence of the target table will not be checked.
	 *
	 * @see DbSettings#getCreateTableTemplate(java.lang.String)
	 */
	public void copyFromQuery(WbConnection source,
														WbConnection target,
														String query,
														TableIdentifier aTargetTable,
														List<ColumnIdentifier> queryColumns,
														String createTableType,
														DropType dropTarget,
														boolean ignoreDropError,
														boolean skipTargetCheck)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.importer.setConnection(target);
		this.sourceTable = null;
		this.targetTable = aTargetTable;

		this.targetColumnsForQuery = new ArrayList<>(queryColumns);
		if (createTableType != null)
		{
			createTable(targetColumnsForQuery, dropTarget, ignoreDropError, createTableType, skipTargetCheck);
		}
		// this flag must be set before calling initImporterForTable!
		importer.skipTargetCheck(skipTargetCheck);

		initImporterForQuery(query, skipTargetCheck);
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

	public boolean getContinueOnError()
	{
		return this.importer.getContinueOnError();
	}

	public void setContinueOnError(boolean cont)
	{
		this.importer.setContinueOnError(cont);
	}

	@Override
	public void setUseBatch(boolean flag)
	{
		this.importer.setUseBatch(flag);
	}

	@Override
	public void setCommitBatch(boolean flag)
	{
		this.importer.setCommitBatch(flag);
	}

	@Override
	public int getBatchSize()
	{
		return this.importer.getBatchSize();
	}

	@Override
	public void setBatchSize(int size)
	{
		this.importer.setBatchSize(size);
	}

	@Override
	public void commitNothing()
	{
		this.importer.commitNothing();
	}

	@Override
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
			@Override
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

	public long startCopy()
		throws Exception
	{
		long totalRows = 0;
		try
		{
			// this will call start() or sourceData.start()
			// depending on which source we set for the importer
			this.sourceData.setAbortOnError(!this.importer.getContinueOnError());
			this.importer.startImport();
			totalRows = importer.getAffectedRows();

			if (this.doSyncDelete)
			{
				TableDeleteSync sync = new TableDeleteSync(this.targetConnection, this.sourceConnection);

        Set<String> keys = CollectionUtil.caseInsensitiveSet();
        for (ColumnIdentifier col : importer.getKeyColumns())
        {
          keys.add(col.getColumnName());
        }
        TableDiffStatus status = sync.setTableName(this.sourceTable, this.targetTable, keys);
        if (status == TableDiffStatus.OK)
        {
          sync.setRowMonitor(this.importer.getRowActionMonitor());
          sync.setBatchSize(this.getBatchSize());
          sync.setReportInterval(this.importer.getReportInterval());
          sync.doSync();
          long rows = sync.getDeletedRows();
          String msg = ResourceMgr.getFormattedString("MsgCopyNumRowsDeleted", rows, targetTable.getTableName());
          addMessage(msg);
        }
        else
        {
          addError(ResourceMgr.getFormattedString("ErrDataDiffNoPK", targetTable.getTableName()));
        }
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataCopier.start()", "Error when copying data", e);
			this.addError(ExceptionUtil.getDisplay(e));
			this.importer.tableImportError();
			throw e;
		}
		return totalRows;
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
		if (sourceData != null) this.sourceData.cancel();
	}

	private void initImporterForQuery(String query, boolean skipTargetCheck)
		throws SQLException
	{
		if (this.targetColumnsForQuery == null) return;

		if (!skipTargetCheck)
		{
			List<ColumnIdentifier> realCols = targetConnection.getMetadata().getTableColumns(targetTable);
			updateTargetColumns(realCols, targetColumnsForQuery);
		}
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
		if (this.columnMap == null || this.columnMap.isEmpty())
		{
			throw new SQLException("No columns defined");
		}
		int count = this.columnMap.size();

		List<ColumnIdentifier> cols = new ArrayList<>(count);
		List<ColumnIdentifier> sourceCols = new ArrayList<>(count);
		TableSelectBuilder builder = new TableSelectBuilder(sourceConnection, "export", TableSelectBuilder.TABLEDATA_TEMPLATE_NAME);

		StringBuilder sql = new StringBuilder(count * 25 + 30);

		for (Map.Entry<ColumnIdentifier, ColumnIdentifier> entry : this.columnMap.entrySet())
		{
			ColumnIdentifier sid = entry.getKey();
			ColumnIdentifier tid = entry.getValue();
			sourceCols.add(sid);
			cols.add(tid);
		}

		String select = builder.getSelectForColumns(sourceTable, sourceCols, -1);
		sql.append(select);

		if (StringUtil.isNonBlank(addWhere))
		{
			sql.append(' ');
			String first = this.sourceConnection.getParsingUtil().getSqlVerb(addWhere);
			if (!first.equals("WHERE"))
			{
				sql.append(" WHERE ");
			}
			sql.append(addWhere);
		}
		initQuerySource(sql.toString());

		this.importer.setTargetTable(this.targetTable, cols);
	}

	private void initQuerySource(String sql)
	{
		QueryCopySource source = new QueryCopySource(this.sourceConnection, sql);
		source.setTrimCharData(trimCharData);
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
			sourceCols = sourceMeta.getTableColumns(tbl, false);
		}
		else if (this.targetColumnsForQuery != null)
		{
			sourceCols = new ArrayList<>(this.targetColumnsForQuery);
		}
		return sourceCols;
	}

	private ColumnIdentifier findColumn(List<ColumnIdentifier> columns, String colname)
	{
		return ColumnIdentifier.findColumnInList(columns, colname);
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
			LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + sourceCol.getColumnName() + " not found in target table " + this.targetTable + ". Ignoring mapping!");
			String msg = ResourceMgr.getFormattedString("ErrCopyTargetColumnNotFound", sourceCol.getColumnName());
			this.addMessage(msg);
		}
	}

	/**
	 *	Initialize the column mapping between source and target table.
	 *	If a mapping is provided, it is used (after checking that the columns
	 *	exist in both tables).
	 *	If no mapping is provided, all matching columns from both tables are copied
	 */
	private void initColumnMapping(Map<String, String> columnMapping, boolean createNew, boolean useSourceColumns)
		throws SQLException
	{
		List<ColumnIdentifier> sourceCols = getSourceColumns();
		List<ColumnIdentifier> targetCols = null;
		if (!createNew)
		{
			if (useSourceColumns)
			{
				targetCols =  this.sourceConnection.getMetadata().getTableColumns(this.sourceTable);
				for (ColumnIdentifier col : targetCols)
				{
					String colname = sourceConnection.getMetadata().removeQuotes(col.getColumnName());
					col.setColumnName(colname);
					String alias = col.getColumnAlias();
					if (alias != null)
					{
						col.setColumnAlias(sourceConnection.getMetadata().removeQuotes(alias));
					}
				}
			}
			else
			{
				targetCols =  this.targetConnection.getMetadata().getTableColumns(this.targetTable);
			}
		}

		this.columnMap = new HashMap<>(sourceCols.size());

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

}
