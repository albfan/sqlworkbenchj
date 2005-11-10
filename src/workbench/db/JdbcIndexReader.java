/*
 * JdbcIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;
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
	}
	
	public ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException
	{
		return this.metaData.getSqlConnection().getMetaData().getIndexInfo(table.getCatalog(), table.getSchema(), table.getTableName(), unique, false);
	}
	
	public StrBuffer getIndexSource(TableIdentifier table, DataStore indexDefinition, String tableNameToUse)
	{
		if (indexDefinition == null) return StrBuffer.EMPTY_BUFFER;
		int count = indexDefinition.getRowCount();
		if (count == 0) return StrBuffer.EMPTY_BUFFER;
		StrBuffer idx = new StrBuffer();
		String template = this.metaData.getIndexSqlTemplate();
		String sql;
		int idxCount = 0;
		for (int i = 0; i < count; i++)
		{
			String idx_name = indexDefinition.getValue(i, 0).toString();
			String unique = indexDefinition.getValue(i, 1).toString();
			String is_pk  = indexDefinition.getValue(i, 2).toString();
			String definition = indexDefinition.getValue(i, 3).toString();
			StrBuffer columns = new StrBuffer();
			StringTokenizer tok = new StringTokenizer(definition, ",");
			String col;
			int pos;
			while (tok.hasMoreTokens())
			{
				col = tok.nextToken().trim();
				if (col == null || col.length() == 0) continue;
				if (columns.length() > 0) columns.append(',');
				pos = col.indexOf(' ');
				if (pos > -1)
				{
					columns.append(col.substring(0, pos));
				}
				else
				{
					columns.append(col);
				}
			}
			// The PK's have been created with the table source, so
			// we do not need to add the corresponding index here.
			if ("NO".equalsIgnoreCase(is_pk))
			{
				idxCount ++;
				sql = StringUtil.replace(template, DbMetadata.TABLE_NAME_PLACEHOLDER, (tableNameToUse == null ? table.getTableName() : tableNameToUse));
				if ("YES".equalsIgnoreCase(unique))
				{
					sql = StringUtil.replace(sql, DbMetadata.UNIQUE_PLACEHOLDER, "UNIQUE ");
				}
				else
				{
					sql = StringUtil.replace(sql, DbMetadata.UNIQUE_PLACEHOLDER, "");
				}
				sql = StringUtil.replace(sql, DbMetadata.COLUMNLIST_PLACEHOLDER, columns.toString());
				sql = StringUtil.replace(sql, DbMetadata.INDEX_NAME_PLACEHOLDER, idx_name);
				idx.append(sql);
				idx.append(";\n");
			}
		}
		if (idxCount > 0) idx.append("\n");
		return idx;
	}

	/**
	 * 	Build the SQL statement to create an Index on the given table.
	 * 	@param aTable - The table name for which the index should be constructed
	 * 	@param indexName - The name of the Index
	 * 	@param unique - Should the index be unique
	 *  @param columnList - The columns that should build the index
	 */
	public String buildCreateIndexSql(TableIdentifier aTable, String indexName, boolean unique, String[] columnList)
	{
		if (columnList == null) return StringUtil.EMPTY_STRING;
		int count = columnList.length;
		if (count == 0) return StringUtil.EMPTY_STRING;
		String template = this.metaData.getIndexSqlTemplate();
		StrBuffer cols = new StrBuffer(count * 25);

		for (int i=0; i < count; i++)
		{
			if (columnList[i] == null || columnList[i].length() == 0) continue;
			if (cols.length() > 0) cols.append(',');
			cols.append(columnList[i]);
		}

		String sql = StringUtil.replace(template, DbMetadata.TABLE_NAME_PLACEHOLDER, aTable.getTableExpression(this.metaData.getWbConnection()));
		if (unique)
		{
			sql = StringUtil.replace(sql, DbMetadata.UNIQUE_PLACEHOLDER, "UNIQUE ");
		}
		else
		{
			sql = StringUtil.replace(sql, DbMetadata.UNIQUE_PLACEHOLDER, "");
		}
		sql = StringUtil.replace(sql, DbMetadata.COLUMNLIST_PLACEHOLDER, cols.toString());
		sql = StringUtil.replace(sql, DbMetadata.INDEX_NAME_PLACEHOLDER, indexName);
		return sql;
	}
	
}
