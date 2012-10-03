/*
 * MySQLIndexReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import workbench.db.DbMetadata;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * An index reader to retrieve information about MySQL indexes.
 *
 * The reader updates column information for those columns that are only partially indexed,
 * e.g. when using <tt>create index idx on foo (col1(10), col2(20));</tt>
 *
 * @author Thomas Kellerer
 */
public class MySQLIndexReader
	extends JdbcIndexReader
{

	public MySQLIndexReader(DbMetadata meta)
	{
		super(meta);
	}

	@Override
	public void processIndexList(TableIdentifier table, Collection<IndexDefinition> indexList)
	{
		if (indexList.isEmpty()) return;

		ResultSet rs = null;
		Statement stmt = null;
		WbConnection conn = this.metaData.getWbConnection();

		try
		{
			String sql =
				"show index from " + SqlUtil.fullyQualifiedName(conn, table) +
				" where sub_part is not null";

			stmt = conn.createStatementForQuery();
			rs = stmt.executeQuery(sql);

			while (rs.next())
			{
				String indexName = rs.getString("Key_name");
				String col = rs.getString("Column_name");
				int part = rs.getInt("Sub_part");
				IndexDefinition def = findIndex(indexList, indexName);
				IndexColumn iCol = findColumn(def, col);
				if (iCol != null)
				{
					iCol.setColumn(col + "(" + Integer.toString(part) + ")");
				}
			}
		}
		catch (SQLException sql)
		{
			LogMgr.logError("MySQLIndexReader.processIndexList()", "Could not read indexed definition", sql);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private IndexColumn findColumn(IndexDefinition index, String colName)
	{
		if (index == null) return null;
		for (IndexColumn col : index.getColumns())
		{
			if (col.getColumn().equals(colName))
			{
				return col;
			}
		}
		return null;
	}

	private IndexDefinition findIndex(Collection<IndexDefinition> indexes, String toFind)
	{
		for (IndexDefinition ind : indexes)
		{
			if (ind.getName().equals(toFind)) return ind;
		}
		return null;
	}

}
