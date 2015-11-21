/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbSearchPath;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.PkDefinition;
import workbench.db.ReaderFactory;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class UpdateTableDetector
{
	private TableIdentifier updateTable;
	private List<ColumnIdentifier> missingPkcolumns;
	private WbConnection conn;
	private boolean checkPkOnly;
	private boolean logDuration;

	public UpdateTableDetector(WbConnection db)
	{
		conn = db;
		logDuration = Settings.getInstance().getBoolProperty("workbench.db.updatetable.check.logduration", false);
	}

  /**
   * Control if only the primary key should be retrieved.
   *
   * If set to false (the default), checkUpdateTable() will also match the columns of the result with the
   * columns of the update table.
   *
   * For performance reasons the column check can be disabled through this method.
   *
   * @param flag  if true, the columns of the table will not be matched against the result
   */
	public void setCheckPKOnly(boolean flag)
	{
		this.checkPkOnly = flag;
	}

	public TableIdentifier getUpdateTable()
	{
		return updateTable;
	}

	public List<ColumnIdentifier> getMissingPkColumns()
	{
		return missingPkcolumns;
	}

	public void checkUpdateTable(TableIdentifier table, ResultInfo resultInfo)
	{
		updateTable = null;
		resultInfo.setUpdateTable(null);
		missingPkcolumns = null;

		if (table == null)
		{
			return;
		}

		DbMetadata meta = conn.getMetadata();
		if (meta == null) return;

		TableIdentifier tbl = getFullyQualifiedTable(table);
    if (tbl == null) return;

		tbl.adjustCase(conn);

		if (checkPkOnly)
		{
			LogMgr.logDebug("UpdateTableDetector.setUpdateTable()", "Only checking the PK definition for " + tbl.getTableExpression());
			checkPkOnlyForUpdateTable(tbl, resultInfo);
			if (updateTable != null)
			{
				LogMgr.logDebug("UpdateTableDetector.setUpdateTable()", "Using update table: " + updateTable.getTableExpression());
				return;
			}

			// the table either has no PK or unique index at all or does not exist
			// if it does not exist, this might be a synonym
			tbl = findTable(tbl);
		}

		List<ColumnIdentifier> columns = null;

		try
		{

			// try to use the table name as-is and retrieve the table definition directly
			// as this should cover the majority of all cases it should usually be a bit faster
			// than first searching for the table - especially for Oracle with its dead slow catalog views
			TableDefinition def = getDefinition(tbl);
			if (def != null && def.getColumnCount() > 0 && def.getTable() != null)
			{
				columns = def.getColumns();
				if (meta.isSynonym(tbl))
				{
					updateTable = tbl;
				}
				else
				{
					// using the table identifier returned ensures that the table name is fully qualified with schema and catalog
					updateTable = def.getTable();
				}
			}
			else
			{
				updateTable = findTable(tbl);
				columns = getColumns(tbl);
			}

			int realColumns = 0;

			// check the columns which are in the new table so that we can refuse any changes to columns
			// which do not derive from that table.
			// Note that this does not work, if the columns were renamed via an alias in the select statement

			if (columns != null)
			{
				this.missingPkcolumns = new ArrayList<>(columns.size());

				for (ColumnIdentifier column : columns)
				{
					int index = findColumn(column.getColumnName(), resultInfo);
					if (index > -1)
					{
            syncResultColumn(index, column, resultInfo);
            realColumns++;
					}
					else if (column.isPkColumn())
					{
						this.missingPkcolumns.add(column);
					}
				}
			}

			if (realColumns == 0 && updateTable != null)
			{
				LogMgr.logWarning("UpdateTableDetector.setUpdateTable()", "No columns from the table " + this.updateTable.getTableExpression() + " could be found in the current result set!");
			}

			if (!resultInfo.hasPkColumns() && meta.getDbSettings().checkUniqueIndexesForPK())
			{
				checkUniqueIndexesFor(updateTable, resultInfo);
			}
		}
		catch (Exception e)
		{
			this.updateTable = null;
			LogMgr.logError("UpdateTableDetector.setUpdateTable()", "Could not read table definition", e);
		}

		LogMgr.logDebug("UpdateTableDetector.setUpdateTable()", "Using update table: " + updateTable);

		resultInfo.setUpdateTable(updateTable);
	}

  private int findColumn(String columnName, ResultInfo info)
  {
    if (info.isColumnTableDetected() && updateTable != null)
    {
      for (int i=0; i < info.getColumnCount(); i++)
      {
        String resultCol = info.getColumn(i).getColumnName();
        if (columNamesAreEqual(columnName, resultCol) && columnBelongsToUpdateTable(info.getColumn(i), info))
        {
          return i;
        }
      }
    }

    int index = info.findColumn(columnName, conn.getMetadata());
    if (index > -1 && !isUniqueColumnName(columnName, info))
    {
      LogMgr.logWarning("UpdateTableDetector.findColumn()", "Column " + columnName + " is not unique. Ignoring column!");
      return -1;
    }
    return index;
  }

  private boolean isUniqueColumnName(String column, ResultInfo info)
  {
    int count = 0;
    for (int col=0; col < info.getColumnCount(); col++)
    {
      if (columNamesAreEqual(info.getColumnName(col), column))
      {
        count ++;
      }
      if (count > 1) break;
    }
    return count == 1;
  }

  private boolean columnBelongsToUpdateTable(ColumnIdentifier column, ResultInfo info)
  {
    if (column.getSourceTableName() == null) return false;

    TableIdentifier tbl = new TableIdentifier(column.getSourceTableName());
    return updateTable.compareNames(tbl);
  }

  private boolean columNamesAreEqual(String col1, String col2)
  {
    col1 = conn.getMetadata().removeQuotes(col1);
    col2 = conn.getMetadata().removeQuotes(col2);
    return col1.equalsIgnoreCase(col2);
  }

	private List<ColumnIdentifier> getColumns(TableIdentifier table)
		throws SQLException
	{
		// If the object that was used in the original SELECT is
		// a synonym we have to get the definition of the underlying
		// table in order to find the primary key columns
		TableIdentifier synCheck = getFullyQualifiedTable(table);

		// if the passed table is not a synonym resolveSynonym
		// will return the passed table
		TableIdentifier toCheck = getSynonymTable(synCheck);

		List<ColumnIdentifier> columns = null;

		if (conn.getDbSettings().useCompletionCacheForUpdateTableCheck())
		{
			columns = conn.getObjectCache().getColumns(toCheck);
		}
		else
		{
			columns = conn.getMetadata().getTableColumns(toCheck);
		}
		return columns;
	}

	private TableIdentifier getSynonymTable(TableIdentifier toCheck)
	{
		if (!conn.getMetadata().supportsSynonyms()) return toCheck;

		if (conn.getDbSettings().useCompletionCacheForUpdateTableCheck())
		{
			return conn.getObjectCache().getSynonymTable(toCheck);
		}
		return conn.getMetadata().resolveSynonym(toCheck);
	}

	private void checkPkOnlyForUpdateTable(TableIdentifier tbl, ResultInfo resultInfo)
	{
		DbMetadata meta = conn.getMetadata();
		if (meta == null) return;
		if (resultInfo == null) return;

		PkDefinition pk = null;
		long start = System.currentTimeMillis();

		if (conn.getDbSettings().useCompletionCacheForUpdateTableCheck())
		{
			pk = conn.getObjectCache().getPrimaryKey(tbl);
		}
		else
		{
			pk = meta.getIndexReader().getPrimaryKey(tbl);
		}

		long duration = System.currentTimeMillis() - start;
		if (logDuration)
		{
			LogMgr.logDebug("UpdateTableDetector.checkPkOnlyForUpdateTable()", "Retrieving primary key for table " + tbl.getTableExpression() + " took: " + duration + "ms");
		}

		this.missingPkcolumns = new ArrayList<>(1);

		if (pk == null || CollectionUtil.isEmpty(pk.getColumns()))
		{
			checkUniqueIndexesFor(tbl, resultInfo);
		}
		else
		{
			for (String colName : pk.getColumns())
			{
				int index = resultInfo.findColumn(colName, conn.getMetadata());
				if (index > -1)
				{
					resultInfo.setIsPkColumn(index, true);
					resultInfo.setIsNullable(index, false);
				}
				else
				{
					missingPkcolumns.add(new ColumnIdentifier(colName));
				}
			}
		}
		if (resultInfo.hasPkColumns())
		{
			this.updateTable = tbl;
			resultInfo.setUpdateTable(updateTable);
		}
	}

	private void checkUniqueIndexesFor(TableIdentifier tableToUse, ResultInfo result)
	{
		if (tableToUse == null || result == null) return;
		LogMgr.logInfo("UpdateTableDetector.checkUniqueIndexesForPK()", "No PK found for table " + tableToUse.getTableName()+ " Trying to find an unique index.");
		List<IndexDefinition> indexes = null;

		long start = System.currentTimeMillis();
		if (conn.getDbSettings().useCompletionCacheForUpdateTableCheck())
		{
			indexes = conn.getObjectCache().getUniqueIndexes(tableToUse);
		}
		else
		{
			IndexReader reader = ReaderFactory.getIndexReader(conn.getMetadata());
			indexes = reader.getUniqueIndexes(tableToUse);
		}
		long duration = System.currentTimeMillis() - start;

		if (logDuration)
		{
			LogMgr.logDebug("UpdateTableDetector.checkPkOnlyForUpdateTable()", "Retrieving unique indexes for table " + tableToUse.getTableExpression() + " took: " + duration + "ms");
		}

		if (CollectionUtil.isEmpty(indexes)) return;

		IndexDefinition idx = findSuitableIndex(indexes, result);
		if (idx == null) return;
		LogMgr.logInfo("UpdateTableDetector.checkUniqueIndexesForPK()", "Using unique index " + idx.getObjectName() + " as a surrogate PK for table: " + tableToUse.getTableExpression());

		List<IndexColumn> columns = idx.getColumns();
		for (IndexColumn col : columns)
		{
			int index = result.findColumn(col.getColumn(), conn.getMetadata());
			if (index > -1)
			{
				result.setIsPkColumn(index, true);
				result.setIsNullable(index, false);
			}
			else
			{
				missingPkcolumns.add(new ColumnIdentifier(col.getColumn()));
			}
		}
	}

	/**
	 * Search for the first index where all columns are defined as NOT NULL
	 *
	 * @param indexes the indexes to search
	 * @return the index to use, null if no such index could be found
	 */
	private IndexDefinition findSuitableIndex(List<IndexDefinition> indexes, ResultInfo info)
	{
		for (IndexDefinition index : indexes)
		{
			if (!hasNullableColumns(index, info)) return index;
		}
		return null;
	}

	private boolean hasNullableColumns(IndexDefinition index, ResultInfo info)
	{
		for (IndexColumn col : index.getColumns())
		{
			int colIdx = info.findColumn(col.getColumn(), conn.getMetadata());
			if (colIdx > -1 && info.getColumn(colIdx).isNullable())
			{
				return true;
			}
		}
		return false;
	}

	public void syncResultColumn(int index, ColumnIdentifier column, ResultInfo info)
	{
		boolean canUpdate = true;
		if (!column.isAutoGenerated() && column.getComputedColumnExpression() != null)
		{
			canUpdate = false;
			LogMgr.logDebug("UpdateTableDetector.syncResultColumn()", "Column " + column.getColumnName() + " can not be updated because it is a computed column");
		}

		if (info.getColumn(index).isReadonly())
		{
			LogMgr.logDebug("UpdateTableDetector.syncResultColumn()", "Column " + column.getColumnName() + " was marked as read-only by the driver!");
		}

		info.setUpdateable(index, canUpdate);
		info.setIsPkColumn(index, column.isPkColumn());
		info.setIsNullable(index, column.isNullable());
		ColumnIdentifier resultCol = info.getColumn(index);
		resultCol.setIsAutoincrement(column.isAutoincrement());
		resultCol.setComputedColumnExpression(column.getComputedColumnExpression());
	}

	private TableDefinition getDefinition(TableIdentifier toFind)
		throws SQLException
	{
		TableDefinition def = null;
		if (conn.getDbSettings().useCompletionCacheForUpdateTableCheck())
		{
			List<ColumnIdentifier> columns = conn.getObjectCache().getColumns(toFind);
			if (columns != null)
			{
				TableIdentifier tbl = conn.getObjectCache().getTable(toFind);
				def = new TableDefinition(tbl, columns);
			}
		}

		if (def == null)
		{
			TableIdentifier tbl = getFullyQualifiedTable(toFind);
			def = conn.getMetadata().getTableDefinition(tbl);
		}
		return def;
	}

	private TableIdentifier getFullyQualifiedTable(TableIdentifier table)
	{
		TableIdentifier tbl = table.createCopy();
		if (tbl.getSchema() == null)
		{
			DbSearchPath handler = DbSearchPath.Factory.getSearchPathHandler(conn);

			if (handler.isRealSearchPath())
			{
				List<String> path = handler.getSearchPath(conn, null);
				if (path.size() > 1)
				{
					return findTable(table);
				}
			}
			tbl.setSchema(conn.getMetadata().getCurrentSchema());
		}

		if (tbl.getCatalog() == null)
		{
			tbl.setCatalog(conn.getMetadata().getCurrentCatalog());
		}
		return tbl;
	}

	private TableIdentifier findTable(TableIdentifier table)
	{
		TableIdentifier tbl = null;
		if (conn.getDbSettings().useCompletionCacheForUpdateTableCheck())
		{
			tbl = conn.getObjectCache().getOrRetrieveTable(table);
		}

		if (tbl == null)
		{
			tbl = conn.getMetadata().searchSelectableObjectOnPath(table);
		}
		return tbl;
	}
}
