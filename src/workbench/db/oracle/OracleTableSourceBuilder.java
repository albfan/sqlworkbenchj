/*
 * OracleTableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.SQLException;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTableSourceBuilder
	extends TableSourceBuilder
{

	private final String INDEX_USAGE_PLACEHOLDER = "%pk_index_usage%";

	public OracleTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_partitions", true))
		{
			try
			{
				OracleTablePartition reader = new OracleTablePartition(this.dbConnection);
				reader.retrieve(table, dbConnection);
				return reader.getSourceForTableDefinition();
			}
			catch (SQLException sql)
			{
				LogMgr.logError("OracleTableSourceBuilder.getAdditionalTableOptions()", "Error retrieving partitions", sql);
			}
		}
		return null;
	}

	/**
	 * Generate the SQL to create the primary key for the table.
	 *
	 * If the primary key is supported by an index that does not have the same name
	 * as the primary key, it is assumed that the index is defined as an additional
	 * option to the ADD CONSTRAINT SQL...
	 *
	 * @param table the table for which the PK source should be created
	 * @param pkCols a List of PK column names
	 * @param pkName the name of the primary key
	 * @return the SQL to re-create the primary key
	 */
	@Override
	public CharSequence getPkSource(TableIdentifier table, List<String> pkCols, String pkName)
	{
		OracleIndexReader reader = (OracleIndexReader)dbConnection.getMetadata().getIndexReader();
		String pkIndex = reader.getPrimaryKeyIndex(table);
		String sql = super.getPkSource(table, pkCols, pkName).toString();
		IndexDefinition idx = getIndexDefinition(table, pkIndex);
		boolean isPartitioned = false;

		try
		{
			OracleIndexPartition partIndex =  new OracleIndexPartition(this.dbConnection);
			partIndex.retrieve(idx, dbConnection);
			isPartitioned = partIndex.isPartitioned();
		}
		catch (SQLException ex)
		{
			isPartitioned = false;
		}

		if (pkIndex.equals(pkName) && !isPartitioned)
		{
			sql = sql.replace(" " + INDEX_USAGE_PLACEHOLDER, "");
		}
		else
		{
			String indexSql = reader.getExtendedIndexSource(table, idx, null, "    ").toString();
			StringBuilder using = new StringBuilder(indexSql.length() + 20);
			using.append("\n   USING INDEX (\n     ");
			using.append(SqlUtil.trimSemicolon(indexSql).trim().replace("\n", "\n  "));
			using.append("\n   )");
			sql = sql.replace(INDEX_USAGE_PLACEHOLDER, using);
		}
		return sql;
	}

	private IndexDefinition getIndexDefinition(TableIdentifier table, String indexName)
	{
		OracleIndexReader reader = (OracleIndexReader)dbConnection.getMetadata().getIndexReader();
		try
		{
			IndexDefinition index = reader.getIndexDefinition(table, indexName, null);
			return index;
		}
		catch (SQLException sql)
		{
			LogMgr.logError("OracleTableSourceBuilder.getIndexDefinition()", "Could not retrieve index", sql);
		}
		return null;
	}

}
