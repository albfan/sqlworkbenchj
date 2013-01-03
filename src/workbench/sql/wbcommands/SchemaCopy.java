/*
 * SchemaCopy.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.AppArguments;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.compare.TableDeleteSync;
import workbench.db.datacopy.DataCopier;
import workbench.db.importer.DataReceiver;
import workbench.db.importer.DeleteType;
import workbench.db.importer.TableDependencySorter;
import workbench.db.importer.TableStatements;

import workbench.storage.RowActionMonitor;

import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.util.MessageBuffer;

/**
 * Handles a WbCopy call for a whole schema.
 *
 * @author Thomas Kellerer
 */
class SchemaCopy
	implements CopyTask
{
	private WbConnection sourceConnection;
	private WbConnection targetConnection;
	private MessageBuffer messages = new MessageBuffer();
	private DataCopier copier;
	private boolean success;
	private String createTableType;
	private boolean dropTable;
	private boolean ignoreDropError;
	private boolean checkDependencies;
	private boolean useSavepoint;
	private boolean skipTargetCheck;

	private List<TableIdentifier> sourceTables;
	private Map<TableIdentifier, TableIdentifier> targetToSourceMap;

	private RowActionMonitor rowMonitor;
	private boolean cancel;
	private boolean doSyncDelete;
	private String cmdLineMode;
	private String targetSchema;
	private String targetCatalog;

	SchemaCopy(List<TableIdentifier> tables)
	{
		this.sourceTables = tables;
	}

	@Override
	public void setTargetSchemaAndCatalog(String schema, String catalog)
	{
		this.targetSchema = schema;
		this.targetCatalog = catalog;
	}

	@Override
	public long copyData()
		throws SQLException, Exception
	{
		long totalRows = 0;
		cancel = false;

		Collection<TableIdentifier> toCopy = null;
		if (this.checkDependencies)
		{
			toCopy = sortTablesForInsert();
		}
		else
		{
			toCopy = targetToSourceMap.keySet();
		}

		int currentTable = 0;
		int count = toCopy.size();

		DataReceiver receiver = this.copier.getReceiver();
		receiver.setTableCount(count);

		Savepoint sp = null;
		// if transaction control is disabled, QueryCopySource will not rollback each implicit transaction started
		// by the select statements. In that case we should do it here in order to free resources
		if (!copier.getReceiver().isTransactionControlEnabled() && this.sourceConnection.supportsSavepoints() && this.sourceConnection.selectStartsTransaction())
		{
			sp = sourceConnection.setSavepoint();
		}

		try
		{
			copier.beginMultiTableCopy(targetConnection);

			for (TableIdentifier targetTable : toCopy)
			{
				if (this.cancel)
				{
					break;
				}

				currentTable++;
				copier.reset();
				copier.setMode(cmdLineMode);
				receiver.setCurrentTable(currentTable);

				TableIdentifier sourceTable = getSourceTable(targetTable);
				if (sourceTable == null) continue;

				if (messages.getLength() > 0) messages.appendNewLine();
				this.messages.append(ResourceMgr.getFormattedString("MsgCopyTable", sourceTable.getTableName()));
				this.messages.appendNewLine();

				copier.copyFromTable(sourceConnection, targetConnection, sourceTable, targetTable, null, null, createTableType, dropTable, ignoreDropError, skipTargetCheck);
				copier.setUseSavepoint(useSavepoint);
				totalRows += copier.startCopy();

				this.messages.append(copier.getMessageBuffer());
			}
			this.success = true;
		}
		catch (Exception e)
		{
			this.success = false;
			this.messages.append(copier.getMessageBuffer());
			throw e;
		}
		finally
		{
			copier.endMultiTableCopy();
			sourceConnection.rollback(sp);
		}

		if (doSyncDelete)
		{
			try
			{
				this.messages.appendNewLine();
				deleteRows();
			}
			catch (SQLException e)
			{
				this.success = false;
				this.messages.append(ExceptionUtil.getDisplay(e));
				this.messages.appendNewLine();
				throw e;
			}
		}
		return totalRows;
	}

	private void mapTables()
	{
		targetToSourceMap = new HashMap<TableIdentifier, TableIdentifier>(sourceTables.size());

		String schema = this.targetSchema != null ? targetConnection.getMetadata().adjustSchemaNameCase(targetSchema) : this.targetConnection.getMetadata().getCurrentSchema();
		String catalog = this.targetCatalog != null ? targetConnection.getMetadata().adjustObjectnameCase(targetCatalog) : this.targetConnection.getMetadata().getCurrentCatalog();

		for (TableIdentifier sourceTable : sourceTables)
		{
			TableIdentifier targetTable = sourceTable.createCopy();

			if (this.targetSchema != null || targetCatalog != null)
			{
				targetTable.setSchema(schema);
				targetTable.setCatalog(catalog);
			}

			if (createTargetTable())
			{
				targetTable.setCatalog(this.targetConnection.getMetadata().getCurrentCatalog());
				targetTable.adjustCase(targetConnection);
			}
			else
			{
				targetTable = this.targetConnection.getMetadata().findTable(targetTable, false);
				if (targetTable == null && targetSchema == null && targetCatalog == null)
				{
					// if the table was not found using the schema/catalog as specified in the source
					// table, try the default schema and catalog.
					targetTable = new TableIdentifier(sourceTable.getTableName(), sourceConnection);
					if (sourceTable.getSchema() == null)
					{
						targetTable.setSchema(this.targetConnection.getMetadata().getCurrentSchema());
					}
					if (sourceTable.getCatalog() == null)
					{
						targetTable.setCatalog(this.targetConnection.getMetadata().getCurrentCatalog());
					}
					targetTable = this.targetConnection.getMetadata().findTable(targetTable, false);
				}

				// check if the target table exists. DataCopier will throw an exception if
				// it doesn't but in SchemaCopy we want to simply ignore non-existing tables
				if (targetTable == null)
				{
					if (skipTargetCheck)
					{
						targetTable = sourceTable.createCopy();
						LogMgr.logWarning("SchemaCopy.mapTables()", "Table " + sourceTable.getFullyQualifiedName(sourceConnection) + " not found in target. Assuming same structure and name");
					}
					else
					{
						this.messages.append(ResourceMgr.getFormattedString("MsgCopyTableIgnored", sourceTable.getTableName()));
						this.messages.appendNewLine();
						continue;
					}
				}
			}
			LogMgr.logTrace("SchemaCopy.mapTables()", "Copying " + sourceTable.getFullyQualifiedName(sourceConnection) + " to "  + targetTable.getFullyQualifiedName(targetConnection));
			targetToSourceMap.put(targetTable, sourceTable);
		}
	}

	private void deleteRows()
		throws SQLException, IOException
	{
		Collection<TableIdentifier> toDelete = null;
		if (checkDependencies)
		{
			toDelete = sortTablesForDelete();
		}
		else
		{
			toDelete = targetToSourceMap.keySet();
		}

		for (TableIdentifier targetTable : toDelete)
		{
			if (this.cancel)
			{
				break;
			}

			TableIdentifier sourceTable = getSourceTable(targetTable);
			if (sourceTable == null) continue;

			TableDeleteSync sync = new TableDeleteSync(targetConnection, sourceConnection);
			sync.setTableName(sourceTable, targetTable);
			sync.setBatchSize(copier.getBatchSize());
			sync.doSync();
			long rows = sync.getDeletedRows();
			String msg = ResourceMgr.getFormattedString("MsgCopyNumRowsDeleted", rows, targetTable.getTableName());
			this.messages.append(msg);
			this.messages.appendNewLine();
		}
	}

	private TableIdentifier getSourceTable(TableIdentifier target)
	{
		return targetToSourceMap.get(target);
	}

	private List<TableIdentifier> sortTablesForDelete()
	{
		return sortTables(false);
	}

	private List<TableIdentifier> sortTablesForInsert()
	{
		return sortTables(true);
	}

	private List<TableIdentifier> sortTables(boolean forInsert)
	{
		try
		{
			if (this.rowMonitor != null)
			{
				this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
				this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgFkDeps"), -1, -1);
			}

			List<TableIdentifier> tables = new ArrayList<TableIdentifier>(targetToSourceMap.keySet());
			TableDependencySorter sorter = new TableDependencySorter(targetConnection);
			List<TableIdentifier> sorted = null;
			if (forInsert)
			{
				sorted = sorter.sortForInsert(tables);
			}
			else
			{
				sorted = sorter.sortForDelete(tables, false);
			}
			return sorted;
		}
		catch (Exception e)
		{
			LogMgr.logError("SchemaCopy.sortTables()", "Error when checking FK dependencies", e);
		}
		return null;
	}

	@Override
	public boolean init(WbConnection source, WbConnection target, StatementRunnerResult result, ArgumentParser cmdLine, RowActionMonitor monitor)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;

		ArgumentParser arguments = cmdLine;

		DeleteType deleteTarget = CommonArgs.getDeleteType(cmdLine);
		boolean continueOnError = cmdLine.getBoolean(CommonArgs.ARG_CONTINUE);
		boolean createTable = cmdLine.getBoolean(WbCopy.PARAM_CREATETARGET, false);
		if (createTable)
		{
			createTableType = cmdLine.getValue(WbCopy.PARAM_TABLE_TYPE, DbSettings.DEFAULT_CREATE_TABLE_TYPE);
		}
		else
		{
			createTableType = null;
		}

		dropTable = cmdLine.getBoolean(WbCopy.PARAM_DROPTARGET);
		ignoreDropError = cmdLine.getBoolean(AppArguments.ARG_IGNORE_DROP, false);
		skipTargetCheck = cmdLine.getBoolean(WbCopy.PARAM_SKIP_TARGET_CHECK, false);

		this.copier = new DataCopier();

		this.rowMonitor = monitor;

		cmdLineMode = cmdLine.getValue(CommonArgs.ARG_IMPORT_MODE);
		if (!this.copier.setMode(cmdLineMode))
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrImpInvalidMode", cmdLineMode));
			result.setFailure();
			return false;
		}

		copier.setPerTableStatements(new TableStatements(cmdLine));
		copier.setTransactionControl(cmdLine.getBoolean(CommonArgs.ARG_TRANS_CONTROL, true));

		checkDependencies = cmdLine.getBoolean(CommonArgs.ARG_CHECK_FK_DEPS);

		CommonArgs.setProgressInterval(copier, arguments);
		CommonArgs.setCommitAndBatchParams(copier, arguments);
		copier.setContinueOnError(continueOnError);
		copier.setDeleteTarget(deleteTarget);
		copier.setRowActionMonitor(rowMonitor);
		this.doSyncDelete = cmdLine.getBoolean(WbCopy.PARAM_DELETE_SYNC, false) && (!createTargetTable());
		mapTables();

		useSavepoint = cmdLine.getBoolean(WbImport.ARG_USE_SAVEPOINT, target.getDbSettings().useSavepointForImport());

		if (checkDependencies && !doSyncDelete)
		{
			List<TableIdentifier> targetTables = new ArrayList<TableIdentifier>(targetToSourceMap.keySet());

			// The table list is needed if the -deleteTarget=true was specified
			// and checkDependencies. In that case, all target tables must be deleted
			// by the importer before starting the copy process.
			copier.setTableList(targetTables);
		}
		return targetToSourceMap.size() > 0;
	}

	private boolean createTargetTable()
	{
		return createTableType != null;
	}

	@Override
	public boolean isSuccess()
	{
		return this.success;
	}

	@Override
	public CharSequence getMessages()
	{
		return messages.getBuffer();
	}

	@Override
	public void cancel()
	{
		this.cancel = true;
		if (this.copier != null)
		{
			this.copier.cancel();
		}
	}

}
