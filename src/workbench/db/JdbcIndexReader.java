/*
 * JdbcIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class JdbcIndexReader
	implements IndexReader
{
	protected DbMetadata metaData;
	
	public JdbcIndexReader(DbMetadata meta)
	{
		this.metaData = meta;
	}

	public void indexInfoProcessed()
	{
		// nothing to do, as we are using the driver's call
	}

	/**
	 * Return information about the indexes defined for the given table.
	 * 
	 * @throws java.sql.SQLException 
  */
	public ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException
	{
		return this.metaData.getSqlConnection().getMetaData().getIndexInfo(table.getCatalog(), table.getSchema(), table.getTableName(), unique, false);
	}
	
	/**
	 * Return the SQL to re-create the indexes defined for the table.
	 * 
	 * @param table 
	 * @param indexDefinition 
	 * @param tableNameToUse 
	 * @return SQL Script to create indexes
	 */
	public StringBuilder getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse)
	{
		if (indexDefinition == null) return null;
		int count = indexDefinition.getRowCount();
		if (count == 0) return StringUtil.emptyBuffer();
		StringBuilder idx = new StringBuilder();
		String template = this.metaData.metaSqlMgr.getIndexTemplate();
		
		int idxCount = 0;
		for (int i = 0; i < count; i++)
		{
			String idx_name = indexDefinition.getValue(i, 0).toString();
			String unique = indexDefinition.getValue(i, 1).toString();
			String is_pk  = indexDefinition.getValue(i, 2).toString();
			String definition = indexDefinition.getValue(i, 3).toString();
			String type = indexDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_TYPE);
			if (type == null || type.startsWith("NORMAL")) type = "";
			
			// TODO: some DBMS return a column list with ascending/descending information (e.g. LASTNAME A, FIRSTNAME A)
			// but for some DBMS this is not valid SQL 
			
			// But the returned definition cannot easily be parsed for this situation 
			// because it might also be a function (that contains a comma in the value list)
			// or complex expression (e.g. CASE ...) which would even be harder to analyze
			// So I'll leave the expression as it is...

			// Only add non-PK Indexes here. The indexes related to the PK constraints
			// are usually auto-created when the PK is defined, so there is no need
			// to re-create a CREATE INDEX statement for them.
			if ("NO".equalsIgnoreCase(is_pk))
			{
				idxCount ++;
				String sql = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, (tableNameToUse == null ? table.getTableName() : tableNameToUse));
				if ("YES".equalsIgnoreCase(unique))
				{
					sql = StringUtil.replace(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, "UNIQUE ");
					if ("unique".equalsIgnoreCase(type)) type = "";
				}
				else
				{
					sql = StringUtil.replace(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, "");
				}
				sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_TYPE_PLACEHOLDER, type);
				sql = StringUtil.replace(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, definition);
				sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, idx_name);
				idx.append(sql);
				idx.append(";\n");
			}
		}
		if (idxCount > 0) idx.append("\n");
		return idx;
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
	public String buildCreateIndexSql(TableIdentifier aTable, String indexName, boolean unique, String[] columnList)
	{
		if (columnList == null) return StringUtil.EMPTY_STRING;
		int count = columnList.length;
		if (count == 0) return StringUtil.EMPTY_STRING;
		String template = this.metaData.metaSqlMgr.getIndexTemplate();
		StringBuilder cols = new StringBuilder(count * 25);

		for (int i=0; i < count; i++)
		{
			if (columnList[i] == null || columnList[i].length() == 0) continue;
			if (cols.length() > 0) cols.append(',');
			cols.append(columnList[i]);
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
	public void processIndexList(TableIdentifier table, Collection<IndexDefinition> indexDefinitions)
	{
		// Nothing implemented
	}
	
}
