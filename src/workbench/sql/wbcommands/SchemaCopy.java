/*
 * SchemaCopy.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.datacopy.DataCopier;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
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
	private boolean continueOnError = false;

	private List<TableIdentifier> sourceTables;
	private int currentTable = 0;
	private RowActionMonitor rowMonitor;
	private boolean cancel = false;
	private boolean deleteTarget = false;
	private ArgumentParser arguments;
	private String copyMode;
	
	public SchemaCopy(List<TableIdentifier> tables)
	{
		this.sourceTables = tables;
	}
	
	public void copyData()
		throws SQLException, Exception
	{
		//this.copier.setTableCount(count);
		cancel = false;
		currentTable = 0;

		for (TableIdentifier table : sourceTables)
		{
			if (this.cancel)
			{
				break;
			}

			currentTable++;
			copier.reset();
			copier.setContinueOnError(continueOnError);
			copier.setDeleteTarget(deleteTarget);
			copier.setRowActionMonitor(rowMonitor);
			this.copier.setMode(copyMode);
			
			CommonArgs.setProgressInterval(copier, arguments);
			CommonArgs.setCommitAndBatchParams(copier, arguments);
		
			// By creating a new identifier, we are stripping of any schema information
			// or other source connection specific stuff.
			TableIdentifier targetTable = new TableIdentifier(table.getTableName());
			if (!createTable)
			{
				// check if the target table exists. DataCopier will throw an exception if 
				// it doesn't but in SchemaCopy we want to simply ignore non-existing tables
				boolean exists = this.targetConnection.getMetadata().tableExists(targetTable);
				if (!exists)
				{
					this.messages.append(ResourceMgr.getFormattedString("MsgCopyTableIgnored", targetTable.getTableName()));
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
			this.messages.appendNewLine();
		}
		this.success = true;
	}

	
	public boolean init(WbConnection source, WbConnection target, StatementRunnerResult result, ArgumentParser cmdLine, RowActionMonitor monitor)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		
		this.arguments = cmdLine;
		
		deleteTarget = cmdLine.getBoolean(WbCopy.PARAM_DELETETARGET);
		continueOnError = cmdLine.getBoolean(CommonArgs.ARG_CONTINUE);
		createTable = cmdLine.getBoolean(WbCopy.PARAM_CREATETARGET);
		dropTable = cmdLine.getBoolean(WbCopy.PARAM_DROPTARGET);

		this.copier = new DataCopier();
		this.rowMonitor = monitor;
		
		String mode = cmdLine.getValue(CommonArgs.ARG_IMPORT_MODE);
		if (mode != null)
		{
			if (!this.copier.setMode(mode))
			{
				result.addMessage(ResourceMgr.getString("ErrInvalidModeIgnored").replaceAll("%mode%", mode));
				result.setWarning(true);
			}
			else
			{
				this.copyMode = mode;
			}
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
