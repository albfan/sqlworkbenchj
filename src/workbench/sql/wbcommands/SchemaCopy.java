/*
 * SchemaCopy.java
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
import workbench.db.DropType;
import workbench.db.importer.DataReceiver;
import workbench.db.importer.TableDependencySorter;

import workbench.storage.RowActionMonitor;

import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.util.MessageBuffer;
import workbench.util.StringUtil;


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
	private DropType dropTable;
	private boolean ignoreDropError;
	private boolean checkDependencies;
	private boolean skipTargetCheck;
  private Boolean adjustNames = null;

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

  public void setAdjustNameCase(boolean flag)
  {
    adjustNames = Boolean.valueOf(flag);
  }

	@Override
	public void setAdjustSequences(boolean flag)
	{
		if (copier != null)
		{
			copier.setAdjustSequences(flag);
		}
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

				try
				{
					copier.copyFromTable(sourceConnection, targetConnection, sourceTable, targetTable, null, null, createTableType, dropTable, ignoreDropError, skipTargetCheck);
					totalRows += copier.startCopy();
					this.messages.append(copier.getMessageBuffer());
				}
				catch (Exception ex)
				{
					this.messages.append(copier.getMessageBuffer());
					messages.appendNewLine();
					if (copier.getContinueOnError())
					{
						messages.appendMessageKey("MsgSkipTbl");
						messages.appendNewLine();
						LogMgr.logWarning("SchemaCopy.copyData()", "Skipping table " + sourceTable + " due to previous error");
					}
					else
					{
						throw ex;
					}
				}
			}
			this.success = copier.isSuccess();
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

  private boolean adjustTargetName(TableIdentifier sourceTable)
  {
    if (adjustNames != null)
    {
      return adjustNames.booleanValue();
    }

    String tname = sourceTable.getRawTableName();

    boolean isLower = tname.toLowerCase().equals(tname);
    if (isLower && sourceConnection.getMetadata().storesLowerCaseIdentifiers())
    {
      return !targetConnection.getMetadata().storesLowerCaseIdentifiers();
    }

    boolean isUpper = tname.toUpperCase().equals(tname);
    if (isUpper && sourceConnection.getMetadata().storesUpperCaseIdentifiers())
    {
      return !targetConnection.getMetadata().storesUpperCaseIdentifiers();
    }

    boolean isMixed = StringUtil.isMixedCase(tname);
    if (isMixed && sourceConnection.getMetadata().storesMixedCaseIdentifiers())
    {
      return !targetConnection.getMetadata().storesMixedCaseIdentifiers();
    }

    if (sourceTable.wasQuoted())
    {
      return false;
    }

    return true;
  }

	private void mapTables()
	{
		targetToSourceMap = new HashMap<>(sourceTables.size());

		String currentTargetSchema = this.targetConnection.getMetadata().getCurrentSchema();
		String currentTargetCatalog = this.targetConnection.getMetadata().getCurrentCatalog();
		String schema = this.targetSchema != null ? targetConnection.getMetadata().adjustSchemaNameCase(targetSchema) : currentTargetSchema;
		String catalog = this.targetCatalog != null ? targetConnection.getMetadata().adjustObjectnameCase(targetCatalog) : currentTargetCatalog;

    String[] types = targetConnection.getMetadata().getTablesAndViewTypes();
		for (TableIdentifier sourceTable : sourceTables)
		{
			TableIdentifier targetTable = sourceTable.createCopy();
			targetTable.setNeverAdjustCase(false);

			if (this.targetSchema != null || targetCatalog != null)
			{
				targetTable.setSchema(schema);
				targetTable.setCatalog(catalog);
			}

			if (adjustTargetName(sourceTable))
      {
        targetTable.adjustCase(targetConnection);
      }
      else
      {
        targetTable.setNeverAdjustCase(true);
      }

			if (createTargetTable())
			{
				targetTable.setCatalog(currentTargetCatalog);
			}
			else
			{
				targetTable = this.targetConnection.getMetadata().findTable(targetTable, types);
				if (targetTable == null && targetSchema == null && targetCatalog == null)
				{
					// if the table was not found using the schema/catalog as specified in the source
					// table, try the default schema and catalog.
					targetTable = new TableIdentifier(sourceTable.getTableName(), sourceConnection);
					if (sourceTable.getSchema() == null)
					{
						targetTable.setSchema(currentTargetSchema);
					}
					if (sourceTable.getCatalog() == null)
					{
						targetTable.setCatalog(currentTargetCatalog);
					}
					targetTable = this.targetConnection.getMetadata().findTable(targetTable, types);
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

			if (cancel)
			{
				System.out.println("#### Cancelling!");
				break;
			}
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

			List<TableIdentifier> tables = new ArrayList<>(targetToSourceMap.keySet());
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

		boolean createTable = cmdLine.getBoolean(WbCopy.PARAM_CREATETARGET, false);
		if (createTable)
		{
			createTableType = cmdLine.getValue(WbCopy.PARAM_TABLE_TYPE, DbSettings.DEFAULT_CREATE_TABLE_TYPE);
		}
		else
		{
			createTableType = null;
		}

		dropTable = CommonArgs.getDropType(cmdLine);
		ignoreDropError = cmdLine.getBoolean(AppArguments.ARG_IGNORE_DROP, false);
		skipTargetCheck = cmdLine.getBoolean(WbCopy.PARAM_SKIP_TARGET_CHECK, false);

		this.rowMonitor = monitor;

		this.copier = WbCopy.createDataCopier(cmdLine, target.getDbSettings());

		cmdLineMode = cmdLine.getValue(CommonArgs.ARG_IMPORT_MODE);
		if (!this.copier.setMode(cmdLineMode))
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrImpInvalidMode", cmdLineMode));
			result.setFailure();
			return false;
		}

		checkDependencies = cmdLine.getBoolean(CommonArgs.ARG_CHECK_FK_DEPS);

		copier.setRowActionMonitor(rowMonitor);
		this.doSyncDelete = cmdLine.getBoolean(WbCopy.PARAM_DELETE_SYNC, false) && (!createTargetTable());
		mapTables();

		if (cancel) return false;

		if (checkDependencies && !doSyncDelete)
		{
			List<TableIdentifier> targetTables = new ArrayList<>(targetToSourceMap.keySet());

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
	public boolean hasWarnings()
	{
		if (copier == null) return false;
		return copier.hasWarnings();
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
		this.messages.appendMessageKey("MsgCopyCancelled");
		if (this.copier != null)
		{
			this.copier.cancel();
		}
	}

}
