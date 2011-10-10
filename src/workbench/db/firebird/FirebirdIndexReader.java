/*
 * FirebirdIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.firebird;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import workbench.db.*;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 * A class to retrieve the index information for Firebird 2.5.
 *
 * This class will not work with earlier Firebird releases and should not be instantiated for them.
 * @author Thomas Kellerer
 * @see ReaderFactory#getIndexReader(workbench.db.DbMetadata)
 */
public class FirebirdIndexReader
	extends JdbcIndexReader
{
	private PreparedStatement indexStatement;

	// This is the basic statement from the Jaybird driver, enhanced to support
	// function based indexes.
	private static final String GET_INDEX_INFO =
		"SELECT  NULL as TABLE_CAT \n" +
		"      , NULL as TABLE_SCHEM \n" +
		"      , trim(ind.RDB$RELATION_NAME) AS TABLE_NAME \n" +
		"      , ind.RDB$UNIQUE_FLAG AS NON_UNIQUE \n" +
		"      , NULL as INDEX_QUALIFIER \n" +
		"      , trim(ind.RDB$INDEX_NAME) as INDEX_NAME \n" +
		"      , NULL as \"TYPE\" \n" +
		"      , coalesce(ise.rdb$field_position,0) +1 as ORDINAL_POSITION \n" +
		"      , trim(coalesce(ise.rdb$field_name, ind.rdb$expression_source)) as COLUMN_NAME \n" +
		"      , case \n" +
		"           when ind.rdb$expression_source is not null then null  \n" +
		"           when ind.RDB$INDEX_TYPE = 1 then 'D'  \n" +
		"        else 'A' end as ASC_OR_DESC" +
		"      , 0 as CARDINALITY " +
		"      , 0 as \"PAGES\" \n" +
		"      , null as FILTER_CONDITION \n" +
		"FROM rdb$indices ind " +
		" LEFT JOIN rdb$index_segments ise ON ind.rdb$index_name = ise.rdb$index_name " +
		"WHERE ind.rdb$relation_name = ? " +
		"ORDER BY 4, 6, 8";

	public FirebirdIndexReader(DbMetadata meta)
	{
		super(meta);
	}

	@Override
	public ResultSet getIndexInfo(TableIdentifier table, boolean unique)
		throws SQLException
	{
		if (this.indexStatement != null)
		{
			LogMgr.logWarning("FirebirdIndexReader.getIndexInfo()", "getIndexInfo() called with pending results!");
			indexInfoProcessed();
		}
		WbConnection con = this.metaData.getWbConnection();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("FirebirdIndexReader.getIndexInfo()", "Using SQL:\n " + SqlUtil.replaceParameters(GET_INDEX_INFO, table.getTableName()));
		}

		this.indexStatement = con.getSqlConnection().prepareStatement(GET_INDEX_INFO);
		this.indexStatement.setString(1, table.getRawTableName());
		ResultSet rs = this.indexStatement.executeQuery();
		return rs;
	}

	@Override
	public void processIndexList(TableIdentifier table, Collection<IndexDefinition> indexList)
	{
		for (IndexDefinition index : indexList)
		{
			List<IndexColumn> columns = index.getColumns();
			boolean isComputed = false;
			for (IndexColumn col : columns)
			{
				isComputed = col.getDirection() == null;
				if (isComputed) break;
			}
			if (isComputed)
			{
				String expr = "COMPUTED BY " + index.getExpression();
				index.setIndexExpression(expr);
			}
		}
	}

	@Override
	public void indexInfoProcessed()
	{
		SqlUtil.closeStatement(indexStatement);
		indexStatement = null;
	}

}
