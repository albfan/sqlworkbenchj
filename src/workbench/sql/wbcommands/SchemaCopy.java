/*
 * SchemaCopy.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.compare.TableDeleteSync;
import workbench.db.datacopy.DataCopier;
import workbench.db.importer.RowDataReceiver;
import workbench.db.importer.TableDependencySorter;
import workbench.db.importer.TableStatements;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.util.MessageBuffer;

/**
 *
 * @author support@sql-workbench.net
 */
public class SchemaCopy 
	implements CopyTask
{
	private WbConnection sourceConnection;
	private WbConnection targetConnection;
	private MessageBuffer messages = new MessageBuffer();
	private DataCopier copier;
	private boolean success;
	private boolean createTable = false;
	private boolean dropTable = false;
	private boolean checkDependencies = false;

	private List<TableIdentifier> sourceTables;
	private RowActionMonitor rowMonitor;
	private boolean cancel = false;
	private boolean doSyncDelete = false;
	private String cmdLineMode = null;
	
	public SchemaCopy(List<TableIdentifier> tables)
	{
		this.sourceTables = tables;
	}
	
	public void clearSchemaFromTables()
	{
		for (TableIdentifier tbl : this.sourceTables)
		{
			tbl.setSchema(null);
		}
	}
	
	public void copyData()
		throws SQLException, Exception
	{
		cancel = false;

		if (this.checkDependencies)
		{
			this.sortTablesForInsert();
		}
		
		int currentTable = 0;
		int count = sourceTables.size();
		
		RowDataReceiver receiver = this.copier.getReceiver();
		receiver.setTableCount(count);
		
		try
		{
			copier.beginMultiTableCopy();

			for (TableIdentifier table : sourceTables)
			{
				if (this.cancel)
				{
					break;
				}

				currentTable++;
				copier.reset();
				copier.setMode(cmdLineMode);
				receiver.setCurrentTable(currentTable);

				TableIdentifier targetTable = null;
				if (createTable)
				{
					// By creating a new identifier, we are stripping of any schema information
					// or other source connection specific stuff.
					targetTable = new TableIdentifier(table.getTableName());
				}
				else
				{
					targetTable = this.targetConnection.getMetadata().findTable(new TableIdentifier(table.getTableName()));
					// check if the target table exists. DataCopier will throw an exception if 
					// it doesn't but in SchemaCopy we want to simply ignore non-existing tables
					if (targetTable == null)
					{
						this.messages.append(ResourceMgr.getFormattedString("MsgCopyTableIgnored", table.getTableName()));
						this.messages.appendNewLine();
						continue;
					}
				}
				if (messages.getLength() > 0) messages.appendNewLine();
				this.messages.append(ResourceMgr.getFormattedString("MsgCopyTable", table.getTableName()));
				this.messages.appendNewLine();

				copier.copyFromTable(sourceConnection, targetConnection, table, targetTable, null, null, createTable, dropTable);
				copier.startCopy();

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
		
	}

	private void deleteRows()
		throws SQLException, IOException
	{
		if (checkDependencies)
		{
			sortTablesForDelete();
		}
		
		for (TableIdentifier sourceTable : sourceTables)
		{
			if (this.cancel)
			{
				break;
			}

			TableIdentifier targetTable = this.targetConnection.getMetadata().findTable(new TableIdentifier(sourceTable.getTableName()));
			if (targetTable == null) continue;
			
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

	private void sortTablesForDelete()
	{
		sortTables(false);
	}
	
	private void sortTablesForInsert()
	{
		sortTables(true);
	}
	
	private void sortTables(boolean forInsert)
	{
		try
		{
			if (this.rowMonitor != null)
			{
				this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
				this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgFkDeps"), -1, -1);
			}
	
			// When copying between databases, the schema in which the 
			// tables reside can be different. The tables from the commandline
			// are retrieved from the source database and will contain the schema 
			// information from that one. 
			// when sorting the tables, it is necessary to retrieve the table information
			// from the target database. So we need to replace schema information
			String targetSchema = this.targetConnection.getMetadata().getSchemaToUse();
			final String nullSchemaMarker = "-$NULL-SCHEMA$-";
			
			Map<String, String> tableSchemas = new HashMap<String, String>(this.sourceTables.size());
			for (TableIdentifier tbl : sourceTables)
			{
				String schema = tbl.getSchema();
				if (schema == null) schema = nullSchemaMarker;
				tableSchemas.put(tbl.getTableName(), schema);
				tbl.setSchema(targetSchema);
			}
			
			TableDependencySorter sorter = new TableDependencySorter(targetConnection);
			List<TableIdentifier> sorted = null;
			if (forInsert)
			{
				sorted = sorter.sortForInsert(sourceTables);
			}
			else
			{
				sorted = sorter.sortForDelete(sourceTables, false);
			}
			if (sorted != null)
			{
				this.sourceTables = sorted;
				for (TableIdentifier tbl : sourceTables)
				{
					String schema = tableSchemas.get(tbl.getTableName());
					if (nullSchemaMarker.equals(schema))
					{
						tbl.setSchema(null);
					}
					else
					{
						tbl.setSchema(schema);
					}
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SchemaCopy.sortTables()", "Error when checking FK dependencies", e);
		}
	}
	
	public boolean init(WbConnection source, WbConnection target, StatementRunnerResult result, ArgumentParser cmdLine, RowActionMonitor monitor)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		
		ArgumentParser arguments = cmdLine;

		boolean deleteTarget = cmdLine.getBoolean(WbCopy.PARAM_DELETETARGET);
		boolean continueOnError = cmdLine.getBoolean(CommonArgs.ARG_CONTINUE);
		createTable = cmdLine.getBoolean(WbCopy.PARAM_CREATETARGET);
		dropTable = cmdLine.getBoolean(WbCopy.PARAM_DROPTARGET);

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
		this.doSyncDelete = cmdLine.getBoolean(WbCopy.PARAM_DELETE_SYNC, false) && !createTable;
		if (checkDependencies && !doSyncDelete)
		{
			// The table list is needed if the -deleteTarget=true was specified
			// and checkDependencies. In that case all target tables must be deleted
			// by the importer before starting the copy process.
			copier.setTableList(this.sourceTables);
		}
		return true;
	}

	public boolean isSuccess()
	{
		return this.success;
	}

	public CharSequence getMessages()
	{
		return messages.getBuffer();
	}

	public void cancel()
	{
		this.cancel = true;
		if (this.copier != null)
		{
			this.copier.cancel();
		}
	}
	
}
