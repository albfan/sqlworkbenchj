/*
 * TableCopy.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import workbench.AppArguments;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbSettings;
import workbench.db.DropType;
import workbench.db.TableIdentifier;
import workbench.db.TableNotFoundException;
import workbench.db.WbConnection;
import workbench.db.datacopy.DataCopier;

import workbench.storage.RowActionMonitor;

import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Handles a WbCopy call for a single table.
 *
 * @author Thomas Kellerer
 */
class TableCopy
	implements CopyTask
{
	private DataCopier copier;

	@Override
	public long copyData()
		throws SQLException, Exception
	{
		return this.copier.startCopy();
	}

	@Override
	public void setAdjustSequences(boolean flag)
	{
		copier.setAdjustSequences(flag);
	}

	@Override
	public boolean init(WbConnection sourceConnection, WbConnection targetConnection, StatementRunnerResult result, ArgumentParser cmdLine, RowActionMonitor monitor)
		throws SQLException
	{

		String sourcetable = cmdLine.getValue(WbCopy.PARAM_SOURCETABLE);
		String sourcequery = SqlUtil.trimSemicolon(cmdLine.getValue(WbCopy.PARAM_SOURCEQUERY));
		String targettable = cmdLine.getValue(WbCopy.PARAM_TARGETTABLE);
		if (targettable == null)
		{
			targettable = sourcetable;
		}

		boolean createTable = cmdLine.getBoolean(WbCopy.PARAM_CREATETARGET);
		DropType dropType = CommonArgs.getDropType(cmdLine);
		boolean ignoreDropError = cmdLine.getBoolean(AppArguments.ARG_IGNORE_DROP, false);
		boolean skipTargetCheck = cmdLine.getBoolean(WbCopy.PARAM_SKIP_TARGET_CHECK, false);

		this.copier = WbCopy.createDataCopier(cmdLine, targetConnection.getDbSettings());
		String keys = cmdLine.getValue(WbCopy.PARAM_KEYS);
		copier.setKeyColumns(keys);

		String mode = cmdLine.getValue(CommonArgs.ARG_IMPORT_MODE);
		if (!this.copier.setMode(mode, targetConnection))
		{
			result.addErrorMessageByKey("ErrImpInvalidMode", mode);
			return false;
		}

		copier.setRowActionMonitor(monitor);

		String createTableType = null;
		TableIdentifier targetId = null;
		if (createTable)
		{
			targetId = new TableIdentifier(targettable, targetConnection);
			targetId.setNewTable(true);
			createTableType = cmdLine.getValue(WbCopy.PARAM_TABLE_TYPE, DbSettings.DEFAULT_CREATE_TABLE_TYPE);
		}
		else
		{
      String[] types = targetConnection.getMetadata().getTablesAndViewTypes();
			targetId = targetConnection.getMetadata().findTable(new TableIdentifier(targettable, targetConnection), types);
		}

		if (targetId == null)
		{
      if (skipTargetCheck)
      {
        targetId = new TableIdentifier(targettable, targetConnection);
      }
      else
      {
        throw new TableNotFoundException(targettable);
      }
		}

		if (sourcetable != null)
		{
			TableIdentifier srcTable = new TableIdentifier(sourcetable, sourceConnection);
			String where = cmdLine.getValue(WbCopy.PARAM_SOURCEWHERE);
			Map<String, String> mapping = this.parseMapping(cmdLine);

			copier.copyFromTable(sourceConnection, targetConnection, srcTable, targetId, mapping, where, createTableType, dropType, ignoreDropError, skipTargetCheck);
		}
		else
		{
			List<ColumnIdentifier> cols = this.parseColumns(cmdLine);
			List<ColumnIdentifier> queryCols = SqlUtil.getResultSetColumns(sourcequery, sourceConnection);
			if (cols != null)
			{
				if (queryCols.size() != cols.size())
				{
					result.addErrorMessage("Columns in query does not match number of columns in -columns parameter");
					return false;
				}
				// when -columns=... is specified this can be used to rename the columns from the query
				// so we need to adjust the names that are passed to the DataCopier because it
				// expects the names to match the target table, not the source
				for (int i=0; i < cols.size(); i++)
				{
					queryCols.get(i).setColumnName(cols.get(i).getColumnName());
					queryCols.get(i).setColumnAlias(null);
				}
			}
			copier.copyFromQuery(sourceConnection, targetConnection, sourcequery, targetId, queryCols, createTableType, dropType, ignoreDropError, skipTargetCheck);
		}

		boolean doSyncDelete = cmdLine.getBoolean(WbCopy.PARAM_DELETE_SYNC, false) && !createTable;
		copier.setDoDeleteSync(doSyncDelete);

		return true;
	}

	@Override
	public void setTargetSchemaAndCatalog(String schema, String catalog)
	{
	}

	@Override
	public boolean isSuccess()
	{
		if (this.copier == null) return true;
		return copier.isSuccess();
	}

	@Override
	public boolean hasWarnings()
	{
		if (copier == null) return false;
		return copier.hasWarnings();
	}

	@Override
	public CharSequence getMessages()
	{
		if (this.copier == null) return null;
		return copier.getAllMessages();
	}

	@Override
	public void cancel()
	{
		if (this.copier != null)
		{
			this.copier.cancel();
		}
	}

	protected List<ColumnIdentifier> parseColumns(ArgumentParser cmdLine)
	{
		// First read the defined columns from the passed parameter
		String cols = cmdLine.getValue(WbCopy.PARAM_COLUMNS);
		if (StringUtil.isBlank(cols)) return null;

		List<String> l = StringUtil.stringToList(cols, ",", true, true, false, true);
		int count = l.size();
		List<ColumnIdentifier> result = CollectionUtil.sizedArrayList(count);
		for (String c : l)
		{
			if (c.indexOf('/') > -1)
			{
				copier.addError(ResourceMgr.getString("MsgCopyErrIllegalMapping"));
				return null;
			}
			result.add(new ColumnIdentifier(c));
		}
		return result;
	}

	protected Map<String, String> parseMapping(ArgumentParser cmdLine)
	{
		String cols = cmdLine.getValue(WbCopy.PARAM_COLUMNS);
		if (cols == null || cols.length() == 0) return null;

		List<String> l = StringUtil.stringToList(cols, ",", true, true, false, true);
		int count = l.size();

		// Use a LinkedHashMap to make sure the order of the columns
		// is preserved (in case -createTable) was also specified
		Map<String, String> mapping = new LinkedHashMap<>();
		for (int i=0; i < count; i++)
		{
			String s = l.get(i);
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
