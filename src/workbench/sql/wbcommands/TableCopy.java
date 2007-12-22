/*
 * TableCopy.java
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

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.datacopy.DataCopier;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableCopy 
	implements CopyTask
{
	private WbConnection sourceConnection;
	private WbConnection targetConnection;
	private MessageBuffer messages;
	private DataCopier copier;
	
	public TableCopy()
	{
	}
	
	public void copyData()
		throws SQLException, Exception
	{
		this.copier.startCopy();
	}

	public boolean init(WbConnection source, WbConnection target, StatementRunnerResult result, ArgumentParser cmdLine, RowActionMonitor monitor)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		
		String sourcetable = cmdLine.getValue(WbCopy.PARAM_SOURCETABLE);
		String sourcequery = cmdLine.getValue(WbCopy.PARAM_SOURCEQUERY);
		String targettable = cmdLine.getValue(WbCopy.PARAM_TARGETTABLE);
		
		boolean delete = cmdLine.getBoolean(WbCopy.PARAM_DELETETARGET);
		boolean cont = cmdLine.getBoolean(CommonArgs.ARG_CONTINUE);
		
		boolean createTable = cmdLine.getBoolean(WbCopy.PARAM_CREATETARGET);
		boolean dropTable = cmdLine.getBoolean(WbCopy.PARAM_DROPTARGET);
		String keys = cmdLine.getValue(WbCopy.PARAM_KEYS);

		this.copier = new DataCopier();
		copier.setTransactionControl(cmdLine.getBoolean(CommonArgs.ARG_TRANS_CONTROL, true));
		copier.setKeyColumns(keys);

		String mode = cmdLine.getValue(CommonArgs.ARG_IMPORT_MODE);
		if (mode != null)
		{
			if (!this.copier.setMode(mode))
			{
				result.addMessage(ResourceMgr.getString("ErrInvalidModeIgnored").replaceAll("%mode%", mode));
				result.setWarning(true);
			}
		}

		CommonArgs.setProgressInterval(copier, cmdLine);
		copier.setRowActionMonitor(monitor);
		copier.setContinueOnError(cont);
		
		CommonArgs.setCommitAndBatchParams(copier, cmdLine);
		
		copier.setDeleteTarget(delete);
		
		TableIdentifier targetId = new TableIdentifier(targettable);
		targetId.setNewTable(createTable);

		if (sourcetable != null)
		{
			TableIdentifier srcTable = new TableIdentifier(sourcetable);
			String where = cmdLine.getValue(WbCopy.PARAM_SOURCEWHERE);
			Map<String, String> mapping = this.parseMapping(cmdLine);
			copier.copyFromTable(sourceConnection, targetConnection, srcTable, targetId, mapping, where, createTable, dropTable);
		}
		else
		{
			ColumnIdentifier[] cols = this.parseColumns(cmdLine, sourcequery, sourceConnection);
			copier.copyFromQuery(sourceConnection, targetConnection, sourcequery, targetId, cols, createTable, dropTable);
		}

		return true;
	}

	public boolean isSuccess()
	{
		if (this.copier == null) return true;
		return copier.isSuccess();
	}

	public CharSequence getMessages()
	{
		if (this.copier == null) return null;
		return copier.getAllMessages();
	}

	public void cancel()
	{
		if (this.copier != null)
		{
			this.copier.cancel();
		}
	}
	
	private ColumnIdentifier[] parseColumns(ArgumentParser cmdLine, String sourceQuery, WbConnection sourceCon)
	{
		// First read the defined columns from the passed parameter
		String cols = cmdLine.getValue(WbCopy.PARAM_COLUMNS);
		List l = StringUtil.stringToList(cols, ",");
		int count = l.size();
		ColumnIdentifier[] result = new ColumnIdentifier[count];
		for (int i=0; i < count; i++)
		{
			String c = (String)l.get(i);
			result[i] = new ColumnIdentifier(c);
		}

		// now try to read the column definitions from the query
		// if a matching column is found, the definition from the query
		// is used (because it will/should contain the correct datatype information
		try
		{
			List colsFromQuery = SqlUtil.getResultSetColumns(sourceQuery, sourceCon);
			for (int i=0; i < count; i++)
			{
				int idx = colsFromQuery.indexOf(result[i]);
				if (idx > -1)
				{
					ColumnIdentifier c = (ColumnIdentifier)colsFromQuery.get(idx);
					result[i] = c;
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.parseColumns()", "Error retrieving column definition from source query", e);
		}
		return result;
	}

	private Map<String, String> parseMapping(ArgumentParser cmdLine)
	{
		String cols = cmdLine.getValue(WbCopy.PARAM_COLUMNS);
		if (cols == null || cols.length() == 0) return null;

		List l = StringUtil.stringToList(cols, ",");
		int count = l.size();
		
		// Use a LinkedHashMap to make sure the order of the columns
		// are preserved (in case -createTable) was also specified
		Map<String, String> mapping = new LinkedHashMap<String, String>();
		for (int i=0; i < count; i++)
		{
			String s = (String)l.get(i);
			int pos = s.indexOf('/');
			if (pos == -1)
			{
				// No mapping just a list of columns
				mapping.put(s, null);
			}
			else
			{
				String scol = s.substring(0, pos).trim();
				String tcol = s.substring(pos + 1).trim();
				mapping.put(scol, tcol);
			}
		}
		return mapping;
	}
	
}
