/*
 * JdbcIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import workbench.db.ibm.DB2UniqueConstraintReader;
import workbench.db.mssql.SqlServerUniqueConstraintReader;
import workbench.db.oracle.OracleUniqueConstraintReader;
import workbench.db.postgres.PostgresUniqueConstraintReader;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

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
	 * Return the name of the index supporting the primary key.
	 * @param tbl
	 */
	@Override
	public String getPrimaryKeyIndex(TableIdentifier tbl)
	{
		// Views don't have primary keys...
		if (metaData.getDbSettings().isViewType(tbl.getType())) return StringUtil.EMPTY_STRING;

		// Retrieve the name of the PK index
		String pkName = "";

		if (this.metaData.getDbSettings().supportsGetPrimaryKeys())
		{
			String catalog = tbl.getCatalog();
			String schema = tbl.getSchema();

			ResultSet keysRs = null;
			try
			{
				keysRs = this.metaData.getJdbcMetaData().getPrimaryKeys(catalog, schema, tbl.getTableName());
				while (keysRs.next())
				{
					pkName = keysRs.getString("PK_NAME");
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("JdbcIndexReader.getPrimaryKeyIndex()", "Error retrieving PK information", e);
				pkName = "";
			}
			finally
			{
				SqlUtil.closeResult(keysRs);
			}
		}
		return pkName;
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
	public StringBuilder getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse)
	{
		if (indexDefinition == null) return null;
		int count = indexDefinition.getRowCount();
		if (count == 0) return StringUtil.emptyBuffer();

		StringBuilder result = new StringBuilder(count * 100);

		for (int i = 0; i < count; i++)
		{
			IndexDefinition definition = (IndexDefinition)indexDefinition.getValue(i, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_COL_DEF);
			// Only add non-PK Indexes here. The indexes related to the PK constraints
			// are usually auto-created when the PK is defined, so there is no need
			// to re-create a CREATE INDEX statement for them
			if (definition != null && !definition.isPrimaryKeyIndex())
			{
				CharSequence idx = getIndexSource(table, definition, tableNameToUse);
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
		sql = StringUtil.replace(sql, MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, indexDefinition.getUniqueConstraintName());

		return sql;
	}

	@Override
	public CharSequence getIndexSource(TableIdentifier table, IndexDefinition indexDefinition, String tableNameToUse)
	{
		if (indexDefinition == null) return null;
		StringBuilder idx = new StringBuilder(100);

		String tableName = tableNameToUse;
		if (tableName == null)
		{
			tableName = table.getTableExpression(this.metaData.getWbConnection());
		}

		String uniqueConstraint = null;
		if (indexDefinition.isUniqueConstraint())
		{
			uniqueConstraint = getUniqueConstraint(table, indexDefinition);
			if (indexDefinition.isUnique() && indexDefinition.getUniqueConstraintName().equals(indexDefinition.getName()))
			{
				return uniqueConstraint;
			}
		}

		String template = this.metaData.getDbSettings().getCreateIndexSQL();
		String type = indexDefinition.getIndexType();
		type = getSQLKeywordForType(type);

		String sql = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, tableName);

		if (tableNameToUse != null)
		{
			sql = sql.replace(MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, tableName);
		}

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

		sql = StringUtil.replace(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, indexDefinition.getExpression());
		sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, indexDefinition.getObjectExpression(metaData.getWbConnection()));
		idx.append(sql);
		String options = getIndexOptions(indexDefinition);
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

	@Override
	public String getIndexOptions(IndexDefinition type)
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
	 * 	@param aTable - The table name for which the index should be constructed
	 * 	@param indexName - The name of the Index
	 * 	@param unique - Should the index be unique
	 *  @param columnList - The columns that should build the index
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

		String sql = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, aTable.getTableExpression(this.metaData.getWbConnection()));
		sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_TYPE_PLACEHOLDER, "");
		if (unique)
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, "UNIQUE ");
		}
		else
		{
			sql = StringUtil.replace(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, "");
		}
		sql = StringUtil.replace(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, cols.toString());
		sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, indexName);
		return sql;
	}

	/**
	 *
	 * @param table
	 * @param indexDefinitions
	 */
	@Override
	public void processIndexList(TableIdentifier table, Collection<IndexDefinition> indexDefinitions)
	{
		// Nothing implemented
	}

	/**
	 * Return the index information for a table as a DataStore. This is
	 * delegated to getTableIndexList() and from the resulting collection
	 * the datastore is created.
	 *
	 * @param table the table to get the indexes for
	 * @see #getTableIndexList(TableIdentifier)
	 */
	@Override
	public DataStore getTableIndexInformation(TableIdentifier table)
	{
		String[] cols = {"INDEX_NAME", "UNIQUE", "PK", "DEFINITION", "TYPE"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.OTHER, Types.VARCHAR};
		final int sizes[] =   {30, 7, 6, 40, 10};
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
		if (this.metaData.getDbId().startsWith("db2"))
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
		ResultSet idxRs = null;
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(metaData.getWbConnection());

		List<IndexDefinition> result = null;

		try
		{
			String pkName = getPrimaryKeyIndex(tbl);

			idxRs = getIndexInfo(tbl, false);
			result = processIndexResult(idxRs, pkName, tbl);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("JdbcIndexReader.getTableIndexInformation()", "Could not retrieve indexes", e);
		}
		finally
		{
			SqlUtil.closeResult(idxRs);
			indexInfoProcessed();
		}
		UniqueConstraintReader reader = getUniqueConstraintReader();
		if (reader != null)
		{
			reader.processIndexList(result, this.metaData.getWbConnection());
		}
		return result;
	}

	protected List<IndexDefinition> processIndexResult(ResultSet idxRs, String pkName, TableIdentifier tbl)
		throws SQLException
	{
		// This will map an indexname to an IndexDefinition object
		// getIndexInfo() returns one row for each column
		HashMap<String, IndexDefinition> defs = new HashMap<String, IndexDefinition>();

		boolean supportsDirection = metaData.getDbSettings().supportsSortedIndex();

		while (idxRs != null && idxRs.next())
		{
			boolean unique = idxRs.getBoolean("NON_UNIQUE");
			String indexName = idxRs.getString("INDEX_NAME");
			if (idxRs.wasNull()) continue;
			if (indexName == null) continue;
			String colName = idxRs.getString("COLUMN_NAME");
			String dir = (supportsDirection ? idxRs.getString("ASC_OR_DESC") : null);

			IndexDefinition def = defs.get(indexName);
			if (def == null)
			{
				def = new IndexDefinition(tbl, indexName);
				def.setUnique(!unique);
				if (metaData.getDbSettings().pkIndexHasTableName())
				{
					def.setPrimaryKeyIndex(indexName.equals(tbl.getTableName()));
				}
				else
				{
					def.setPrimaryKeyIndex(pkName != null && indexName.equals(pkName));
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
		}

		Collection<IndexDefinition> indexes = defs.values();
		processIndexList(tbl, indexes);
		return new ArrayList<IndexDefinition>(indexes);
	}
}
