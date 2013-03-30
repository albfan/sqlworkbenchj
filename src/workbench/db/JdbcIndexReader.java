/*
 * JdbcIndexReader.java
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
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.ibm.DB2UniqueConstraintReader;
import workbench.db.mssql.SqlServerUniqueConstraintReader;
import workbench.db.oracle.OracleUniqueConstraintReader;
import workbench.db.postgres.PostgresUniqueConstraintReader;
import workbench.db.sqltemplates.TemplateHandler;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import static workbench.db.IndexReader.COLUMN_IDX_TABLE_INDEXLIST_TYPE;

/**
 * An implementation of the IndexReader interface that uses the standard JDBC API
 * to get the index information.
 *
 * @author Thomas Kellerer
 */
public class JdbcIndexReader
	implements IndexReader
{
	protected DbMetadata metaData;
	protected String pkIndexNameColumn;

	public JdbcIndexReader(DbMetadata meta)
	{
		this.metaData = meta;
	}

	/**
	 * This method is called after the ResultSet obtained from getIndexInfo() has been processed.
	 *
	 * This is a hook for sub-classes that overwrite getIndexInfo() and need to close the
	 * returned result set.
	 *
	 * @see #getIndexInfo(workbench.db.TableIdentifier, boolean)
	 */
	@Override
	public void indexInfoProcessed()
	{
		// nothing to do, as we are using the driver's call
	}

	/**
	 * Return information about the indexes defined for the given table.
	 * If the TableIdentifier's type is VIEW null will be returned unless
	 * the current DBMS supports indexed views.
	 * <br/>
	 * This is a performance optimization when retrieving a large number
	 * of objects (such as for WbSchemaReport or WbGrepSource) in order
	 * to minimized the roundtrips to the database.
	 *
	 * @throws java.sql.SQLException
	 * @see DbSettings#supportsIndexedViews()
  */
	@Override
	public ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException
	{
		if (metaData.getDbSettings().isViewType(table.getType()) && !metaData.getDbSettings().supportsIndexedViews())
		{
			return null;
		}
		return this.metaData.getSqlConnection().getMetaData().getIndexInfo(table.getCatalog(), table.getSchema(), table.getTableName(), unique, true);
	}

	/**
	 * Return the primary key definition for the table.
	 *
	 * The definition contains the name of the primary key constraint,
	 * and optionally the name of the index supporting the primary key. It also contains
	 * all the columns that make up the primary key.
	 *
	 * @param tbl the table for which the PK should be retrieved
	 */
	@Override
	public PkDefinition getPrimaryKey(TableIdentifier tbl)
	{
		// Views don't have primary keys...
		if (metaData.getDbSettings().isViewType(tbl.getType())) return null;

		// Retrieve the name of the PK index
		String pkName = null;
		String pkIndexName = null;
		PkDefinition pk = null;

		if (this.metaData.getDbSettings().supportsGetPrimaryKeys())
		{
			String catalog = tbl.getCatalog();
			String schema = tbl.getSchema();
			List<IndexColumn> cols = new ArrayList<IndexColumn>();

			ResultSet keysRs = null;
			try
			{
				keysRs = getPrimaryKeyInfo(catalog, schema, tbl.getTableName());
				while (keysRs.next())
				{
					if (pkName == null)
					{
						pkName = keysRs.getString(6); // "PK_NAME"
					}
					if (pkIndexNameColumn != null && pkIndexName == null)
					{
						// this is supplied by our own statement that is used
						// by the OracleIndexReader
						pkIndexName = keysRs.getString(pkIndexNameColumn);
					}
					String colName = keysRs.getString(4); // "COLUMN_NAME"
					int sequence = keysRs.getInt(5); // "KEY_SEQ"
					if (sequence < 1)
					{
						LogMgr.logWarning("JdbcIndexReader.getPrimaryKey()", "Invalid column sequence '" + sequence + "' for key column " + tbl.getTableName() + "." + colName + " received!");
					}
					cols.add(new IndexColumn(colName, sequence));
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("JdbcIndexReader.getPrimaryKey()", "Error retrieving PK information", e);
			}
			finally
			{
				SqlUtil.closeResult(keysRs);
				primaryKeysResultDone();
			}

			LogMgr.logDebug("JdbcIndexreader.getPrimaryKey()", "PK Information for " + tbl.getTableName() + ", PK Name=" + pkName + ", PK Index=" + pkIndexName + ", columns=" + cols);

			if (cols.size() > 0)
			{
				pk = new PkDefinition(getPkName(pkName, pkIndexName, tbl), cols);
				pk.setPkIndexName(pkIndexName);
			}
		}

		if (pk == null && metaData.getDbSettings().pkIndexHasTableName())
		{
			LogMgr.logDebug("JdbcIndexreader.getPrimaryKey()", "No primary key returned from the driver, checking the unique indexes");
			pk = findPKFromIndexList(tbl);
		}

		if (pk != null && tbl.getPrimaryKey() == null)
		{
			tbl.setPrimaryKey(pk);
		}

		return pk;
	}

	private PkDefinition findPKFromIndexList(TableIdentifier tbl)
	{
		List<IndexDefinition> unique = getTableIndexList(tbl, true, false);
		if (CollectionUtil.isEmpty(unique)) return null;

		// see DbSettings.pkIndexHasTableName()
		// this will be checked in processIndexResult
		for (IndexDefinition idx : unique)
		{
			if (idx.isPrimaryKeyIndex())
			{
				LogMgr.logInfo("JdbcIndexreader.findPKFromIndexList()", "Using unique index " + idx.getObjectName() + " as a primary key");
				PkDefinition pk = new PkDefinition(idx.getObjectName(), idx.getColumns());
				pk.setIndexType(idx.getIndexType());
				return pk;
			}
		}

		return null;
	}

	private String getPkName(String pkName, String indexName, TableIdentifier tbl)
	{
		if (pkName != null) return pkName;
		if (indexName != null) return indexName;
		String name = "pk_" + SqlUtil.cleanupIdentifier(tbl.getRawTableName()).toLowerCase();
		LogMgr.logInfo("JdbcIndexReader.getPkName()","Using generated PK name " + name + " for " + tbl.getTableName());
		return name;
	}

	protected void primaryKeysResultDone()
	{
	}

	protected ResultSet getPrimaryKeyInfo(String catalog, String schema, String tableName)
		throws SQLException
	{
		return this.metaData.getJdbcMetaData().getPrimaryKeys(catalog, schema, tableName);
	}

	/**
	 * Return the SQL to re-create the indexes defined for the table.
	 *
	 * @param table
	 * @param indexDefinition
	 * @param tableNameToUse
	 * @return SQL Script to create indexes
	 */
	@Override
	public StringBuilder getIndexSource(TableIdentifier table, List<IndexDefinition> indexList)
	{
		if (indexList == null) return null;
		int count = indexList.size();
		if (count == 0) return StringUtil.emptyBuffer();

		StringBuilder result = new StringBuilder(count * 100);

		for (IndexDefinition definition : indexList)
		{
			// Only add non-PK Indexes here. The indexes related to the PK constraints
			// are usually auto-created when the PK is defined, so there is no need
			// to re-create a CREATE INDEX statement for them
			if (definition != null && !definition.isPrimaryKeyIndex() && !definition.isAutoGenerated())
			{
				CharSequence idx = getIndexSource(table, definition);
				if (idx != null)
				{
					result.append(idx);
					result.append('\n');
				}
			}
		}
		return result;
	}

	protected String getUniqueConstraint(TableIdentifier table, IndexDefinition indexDefinition)
	{
		String template = this.metaData.getDbSettings().getCreateUniqeConstraintSQL();
		String tableName = table.getTableExpression(this.metaData.getWbConnection());
		String sql = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, tableName);
		sql = StringUtil.replace(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, indexDefinition.getColumnList());
		sql = StringUtil.replace(sql, MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, this.metaData.quoteObjectname(indexDefinition.getUniqueConstraintName()));

		ConstraintDefinition constraint = indexDefinition.getUniqueConstraint();

		Boolean deferred = constraint == null ? Boolean.FALSE : constraint.isInitiallyDeferred();
		Boolean deferrable = constraint == null ? Boolean.FALSE : constraint.isDeferrable();

		if (deferrable == null)
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRABLE, "");
			sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRED, "");
		}
		else
		{
			if (Boolean.TRUE.equals(deferrable))
			{
				sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRABLE, "DEFERRABLE");
				if (Boolean.TRUE.equals(deferred))
				{
					sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRED, "INITIALLY DEFERRED");
				}
				else
				{
					sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRED, "INITIALLY IMMEDIATE");
				}
			}
			else
			{
				sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRABLE, "");
				sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRED, "");
			}
		}
		sql = sql.trim();

		if (constraint != null)
		{
			// currently this is only implemented for Oracle
			String disabled = metaData.getDbSettings().getDisabledConstraintKeyword();
			String novalidate = metaData.getDbSettings().getNoValidateConstraintKeyword();

			sql = TemplateHandler.replacePlaceholder(sql, MetaDataSqlManager.CONS_ENABLED, constraint.isEnabled() ? "" : disabled);
			sql = TemplateHandler.replacePlaceholder(sql, MetaDataSqlManager.CONS_VALIDATED, constraint.isValid() ? "" : novalidate);
		}

		if (!sql.endsWith(";"))
		{
			sql += ";";
		}
		return sql;
	}

	@Override
	public CharSequence getIndexSource(TableIdentifier table, IndexDefinition indexDefinition)
	{
		if (indexDefinition == null) return null;
		StringBuilder idx = new StringBuilder(100);

		String uniqueConstraint = null;
		if (indexDefinition.isUniqueConstraint())
		{
			uniqueConstraint = getUniqueConstraint(table, indexDefinition);
			if (indexDefinition.getUniqueConstraintName().equals(indexDefinition.getName()))
			{
				return uniqueConstraint;
			}
		}

		String template = this.metaData.getDbSettings().getCreateIndexSQL();
		String type = indexDefinition.getIndexType();
		type = getSQLKeywordForType(type);

		WbConnection conn = metaData.getWbConnection();
		String sql = template;

		sql = StringUtil.replace(sql, MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, table.getTableExpression(conn));
		sql = StringUtil.replace(sql, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableName());

		if (indexDefinition.isUnique())
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, "UNIQUE");
			if ("unique".equalsIgnoreCase(type)) type = "";
		}
		else
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER + " ", "");
		}

		if (StringUtil.isEmptyString(type))
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_TYPE_PLACEHOLDER + " ", type);
		}
		else
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_TYPE_PLACEHOLDER, type);
		}


		String expr = indexDefinition.getExpression();
		if (indexDefinition.isNonStandardExpression()) // currently only Firebird
		{
			sql = StringUtil.replace(sql, "(" + MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER + ")", expr);
		}
		else
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, expr);
		}

		if (!StringUtil.equalStringIgnoreCase("ASC", indexDefinition.getDirection()))
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.IDX_DIRECTION_PLACEHOLDER, indexDefinition.getDirection());
		}
		else
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.IDX_DIRECTION_PLACEHOLDER + " ", "");
		}

		sql = StringUtil.replace(sql, MetaDataSqlManager.FQ_INDEX_NAME_PLACEHOLDER, indexDefinition.getObjectExpression(metaData.getWbConnection()));
		sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, indexDefinition.getObjectName());
		idx.append(sql);
		String options = getIndexOptions(table, indexDefinition);
		if (options != null)
		{
			idx.append(options);
		}
		idx.append(";\n");

		if (uniqueConstraint != null)
		{
			idx.insert(0, '\n');
			idx.append(uniqueConstraint);
		}
		return idx;
	}

	/**
	 * Return the SQL to re-create any (non default) options for the index.
	 *
	 * The returned String has to be structured so that it can be appended
	 * after the DBMS specific basic CREATE INDEX statement
	 *
	 * @return null if not options are applicable
	 *         a SQL "fragment" to be appended at the end of the create index statement if an option is available.
	 */
	@Override
	public String getIndexOptions(TableIdentifier table, IndexDefinition type)
	{
		return null;
	}

	public String getSQLKeywordForType(String type)
	{
		if (type == null || type.startsWith("NORMAL")) return "";
		return type;
	}


	/**
	 * 	Build the SQL statement to create an Index on the given table.
	 *
	 * 	@param aTable      The table name for which the index should be constructed
	 * 	@param indexName   The name of the Index
	 * 	@param unique      unique index yes/no
	 *  @param columnList  The columns that should build the index
	 *
	 *  @return the SQL statement to create the index
	 */
	@Override
	public String buildCreateIndexSql(TableIdentifier aTable, String indexName, boolean unique, List<IndexColumn> columnList)
	{
		if (columnList == null) return StringUtil.EMPTY_STRING;
		int count = columnList.size();
		if (count == 0) return StringUtil.EMPTY_STRING;
		String template = this.metaData.getDbSettings().getCreateIndexSQL();
		StringBuilder cols = new StringBuilder(count * 25);

		for (int i=0; i < count; i++)
		{
			IndexColumn col = columnList.get(i);
			if (col == null) continue;
			if (cols.length() > 0) cols.append(',');
			cols.append(col.getExpression());
		}

		String sql = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, aTable.getTableName());
		sql = sql.replace(MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, aTable.getTableExpression(metaData.getWbConnection()));

		sql = TemplateHandler.removePlaceholder(sql, MetaDataSqlManager.INDEX_TYPE_PLACEHOLDER, true);
		if (unique)
		{
			sql = TemplateHandler.replacePlaceholder(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, "UNIQUE");
		}
		else
		{
			sql = TemplateHandler.removePlaceholder(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, true);
		}
		sql = StringUtil.replace(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, cols.toString());
		sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, indexName);
		sql = StringUtil.replace(sql, MetaDataSqlManager.FQ_INDEX_NAME_PLACEHOLDER, indexName);
		return sql;
	}

	/**
	 * A hook to post-process the index definitions after they are full retrieved.
	 *
	 * @param table     the table for which the indexlist was retrieved (never null)
	 * @param indexList the indexes retrieved (never null)
	 */
	@Override
	public void processIndexList(TableIdentifier table, Collection<IndexDefinition> indexList)
	{
		// Nothing implemented
	}

	/**
	 * Return the index information for a table as a DataStore.
	 *
	 * This is delegated to getTableIndexList() and from the resulting collection
	 * the datastore is created.
	 *
	 * @param table the table to get the indexes for
	 * @see #getTableIndexList(TableIdentifier)
	 */
	@Override
	public DataStore getTableIndexInformation(TableIdentifier table)
	{
		boolean supportsTableSpaces = this.metaData.getDbSettings().supportsTableSpaceForIndexes();
		String[] cols;
		final int types[];
		final int sizes[];

		if (supportsTableSpaces)
		{
			cols = new String[] {"INDEX_NAME", "UNIQUE", "PK", "DEFINITION", "TYPE", "TABLESPACE"};
			types= new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes= new int[] {30, 7, 6, 40, 10, 15};
		}
		else
		{
			cols = new String[] {"INDEX_NAME", "UNIQUE", "PK", "DEFINITION", "TYPE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[]  {30, 7, 6, 40, 10};
		}
		DataStore idxData = new DataStore(cols, types, sizes);
		if (table == null) return idxData;
		Collection<IndexDefinition> indexes = getTableIndexList(table);
		for (IndexDefinition idx : indexes)
		{
			int row = idxData.addRow();
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME, idx.getName());
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG, (idx.isUnique() ? "YES" : "NO"));
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG, (idx.isPrimaryKeyIndex() ? "YES" : "NO"));
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_COL_DEF, idx);
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_TYPE, idx.getIndexType());
			if (supportsTableSpaces)
			{
				idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_TBL_SPACE, idx.getTablespace());
			}
			idxData.getRow(row).setUserObject(idx);
		}
		idxData.sortByColumn(0, true);
		idxData.resetStatus();
		return idxData;
	}

	public UniqueConstraintReader getUniqueConstraintReader()
	{
		if (this.metaData.isPostgres())
		{
			return new PostgresUniqueConstraintReader();
		}
		if (this.metaData.isOracle())
		{
			return new OracleUniqueConstraintReader();
		}
		if (this.metaData.getDbId().equals("db2") || this.metaData.getDbId().equals("db2h"))
		{
			return new DB2UniqueConstraintReader();
		}
		if (this.metaData.isSqlServer())
		{
			return new SqlServerUniqueConstraintReader();
		}
		return null;
	}

	/**
	 * Returns a list of indexes defined for the given table
	 * @param table the table to get the indexes for
	 */
	@Override
	public List<IndexDefinition> getTableIndexList(TableIdentifier table)
	{
		return getTableIndexList(table, false, true);
	}

	@Override
	public List<IndexDefinition> getUniqueIndexes(TableIdentifier table)
	{
		return getTableIndexList(table, true, true);
	}

	public List<IndexDefinition> getTableIndexList(TableIdentifier table, boolean uniqueOnly, boolean checkPK)
	{
		ResultSet idxRs = null;
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(metaData.getWbConnection());

		List<IndexDefinition> result = null;

		try
		{
			PkDefinition pk = tbl.getPrimaryKey();
			if (pk == null && checkPK) pk = getPrimaryKey(tbl);

			idxRs = getIndexInfo(tbl, uniqueOnly);
			result = processIndexResult(idxRs, pk, tbl);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("JdbcIndexReader.getTableIndexInformation()", "Could not retrieve indexes", e);
			result = new ArrayList<IndexDefinition>(0);
		}
		finally
		{
			SqlUtil.closeResult(idxRs);
			indexInfoProcessed();
		}
		UniqueConstraintReader reader = getUniqueConstraintReader();
		if (reader != null && CollectionUtil.isNonEmpty(result))
		{
			reader.processIndexList(result, this.metaData.getWbConnection());
		}
		return result;
	}

	protected void processIndexResultRow(ResultSet rs, IndexDefinition index, TableIdentifier tbl)
		throws SQLException
	{
		// nothing to do
	}

	protected List<IndexDefinition> processIndexResult(ResultSet idxRs, PkDefinition pkIndex, TableIdentifier tbl)
		throws SQLException
	{
		// This will map an indexname to an IndexDefinition object
		// getIndexInfo() returns one row for each column
		// so the columns of the index are collected in the IndexDefinition
		HashMap<String, IndexDefinition> defs = new HashMap<String, IndexDefinition>();

		boolean supportsDirection = metaData.getDbSettings().supportsSortedIndex();

		while (idxRs != null && idxRs.next())
		{
			boolean nonUniqueFlag = idxRs.getBoolean(4); // "NON_UNIQUE"
			String indexName = idxRs.getString(6); // "INDEX_NAME"

			if (idxRs.wasNull() || indexName == null) continue;

			String colName = idxRs.getString(9); // "COLUMN_NAME"
			String dir = (supportsDirection ? idxRs.getString(10) : null); // "ASC_OR_DESC"

			IndexDefinition def = defs.get(indexName);
			if (def == null)
			{
				def = new IndexDefinition(tbl, indexName);
				def.setUnique(!nonUniqueFlag);
				if (metaData.getDbSettings().pkIndexHasTableName())
				{
					def.setPrimaryKeyIndex(indexName.equals(tbl.getRawTableName()));
				}
				defs.put(indexName, def);

				// The ResultSet produced by getIndexInfo() might not be 100%
				// compliant with the JDBC API as e.g. our own OracleIndexReader
				// directly returns the index type as a string not as a number
				// So the value of the type column is retrieved as an object
				// mapIndexType() will deal with that.
				Object type = idxRs.getObject("TYPE");
				def.setIndexType(metaData.getDbSettings().mapIndexType(type));
			}
			def.addColumn(colName, dir);
			processIndexResultRow(idxRs, def, tbl);
		}

		Collection<IndexDefinition> indexes = defs.values();

		// first try to find the PK index by only checking the PkDefinition
		boolean pkFound = false;

		// Because isPkIndex() will check if the PK columns are part of the index
		// expression, that should only be called if the real PK index cannot be identified
		// through the passed PkDefinition.
		// Otherwise additional indexes defined on the PK columns will be reported as PK index
		// as well, and will be left out of the table's source
		// only one index should be marked as the PK index!
		if (pkIndex != null)
		{
			for (IndexDefinition index : indexes)
			{
				if (!index.isPrimaryKeyIndex())
				{
					boolean isPK = index.getName().equals(pkIndex.getPkIndexName());
					if (isPK)
					{
						index.setPrimaryKeyIndex(true);
						pkFound = true;
						break; // don't look any further. There can only be one PK index
					}
				}
			}
		}

		if (!pkFound)
		{
			for (IndexDefinition index : indexes)
			{
				if (!index.isPrimaryKeyIndex())
				{
					boolean isPK = isPkIndex(index, pkIndex);
					index.setPrimaryKeyIndex(isPK);
					if (isPK && pkIndex != null)
					{
						pkIndex.setIndexType(index.getIndexType());
					}
					if (isPK) break;
				}
			}
		}

		processIndexList(tbl, indexes);
		return new ArrayList<IndexDefinition>(indexes);
	}

	private boolean isPkIndex(IndexDefinition toCheck, PkDefinition pkIndex)
	{
		if (toCheck == null || pkIndex == null) return false;
		if (toCheck.getName().equals(pkIndex.getPkIndexName())) return true;

		// not the same name, check if they have the same definition (same columns at the same position)
		// note that this test will yield false positives for DBMS that allow multiple identical index expressions
		List<IndexColumn> checkCols = toCheck.getColumns();
		List<String> pkCols = pkIndex.getColumns();
		int count = pkCols.size();
		if (checkCols.size() != count) return false;
		for (int col=0; col<count; col++)
		{
			if (!checkCols.get(col).getColumn().equals(pkCols.get(col))) return false;
		}
		return true;
	}

	protected IndexDefinition findIndexByName(Collection<IndexDefinition> indexList, String toFind)
	{
		if (StringUtil.isEmptyString(toFind)) return null;
		if (CollectionUtil.isEmpty(indexList)) return null;
		for (IndexDefinition index : indexList)
		{
			if (index != null && index.getName().equalsIgnoreCase(toFind)) return index;
		}
		return null;
	}

}
