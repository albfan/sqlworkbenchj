/*
 * PostgresIndexReader.java
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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A extension to the JdbcIndexReader to construct the Postgres specific syntax
 * for indexes.
 *
 * This class does not actually construct the CREATE INDEX based on the information
 * available from the JDBC API, but retrieves the CREATE INDEX directly from the database
 * as Postgres stores the full command in the table <tt>pg_indexes</tt>.
 *
 * @author  Thomas Kellerer
 */
public class PostgresIndexReader
	extends JdbcIndexReader
{
	public PostgresIndexReader(DbMetadata meta)
	{
		super(meta);
	}

	/**
	 * Return the SQL for all indexes defined in indexDefinition.
	 *
	 * @param table the table for which to retrieve the indexes
	 * @param indexDefinition the indexes to retrieve
	 * @param tableNameToUse an optional alternative tablename to use
	 * @return
	 */
	@Override
	public StringBuilder getIndexSource(TableIdentifier table, List<IndexDefinition> indexList)
	{
		if (CollectionUtil.isEmpty(indexList)) return null;

		WbConnection con = this.metaData.getWbConnection();
		Statement stmt = null;
		ResultSet rs = null;

		// The full CREATE INDEX Statement is stored in pg_indexes for each
		// index. So all we need to do, is retrieve the indexdef value
		// from that table for all indexes defined for this table.

		int count = indexList.size();
		if (count == 0) return StringUtil.emptyBuffer();

		StringBuilder sql = new StringBuilder(50 + count * 20);
		sql.append("SELECT indexdef FROM pg_indexes WHERE indexname in (");

		String nl = Settings.getInstance().getInternalEditorLineEnding();

		StringBuilder source = new StringBuilder(count * 50);

		Savepoint sp = null;
		int indexCount = 0;
		try
		{
			for (IndexDefinition index : indexList)
			{
				String idxName = index.getObjectExpression(con);

				if (index.isPrimaryKeyIndex()) continue;

				if (index.isUniqueConstraint())
				{
					String constraint = getUniqueConstraint(table, index);
					source.append(constraint);
					source.append(nl);
				}
				else
				{
					if (indexCount > 0) sql.append(',');
					sql.append('\'');
					sql.append(idxName);
					sql.append('\'');
					indexCount++;
				}
			}
			sql.append(')');

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresIndexReader.getIndexSource1()", "Using sql: " + sql.toString());
			}

			if (indexCount > 0)
			{
				sp = con.setSavepoint();
				stmt = con.createStatementForQuery();

				rs = stmt.executeQuery(sql.toString());
				while (rs.next())
				{
					source.append(rs.getString(1));
					source.append(';');
					source.append(nl);
				}
				con.releaseSavepoint(sp);
			}
		}
		catch (Exception e)
		{
			con.rollback(sp);
			LogMgr.logError("PostgresIndexReader.getIndexSource1()", "Error retrieving source", e);
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		if (source.length() > 0) source.append(nl);

		return source;
	}

	@Override
	public CharSequence getIndexSource(TableIdentifier table, IndexDefinition indexDefinition)
	{
		if (indexDefinition == null) return null;
		if (table == null) return null;
		if (Settings.getInstance().getBoolProperty("workbench.db.postgresql.default.indexsource", false))
		{
			return super.getIndexSource(table, indexDefinition);
		}

		if (indexDefinition.isUniqueConstraint())
		{
			return getUniqueConstraint(table, indexDefinition);
		}

		WbConnection con = this.metaData.getWbConnection();

		PreparedStatement stmt = null;
		ResultSet rs = null;

		// The full CREATE INDEX Statement is stored in pg_indexes for each
		// index. So all we need to do, is retrieve the indexdef value
		// from that table for all indexes defined for this table.

		String result = null;

		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT indexdef FROM pg_indexes WHERE indexname = ?");
		boolean hasSchema = false;
		if (indexDefinition.getSchema() != null)
		{
			sql.append(" AND schemaname = ? ");
			hasSchema = true;
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresIndexReader.getIndexSource2()", "Using SQL:\n " + sql);
		}

		Savepoint sp = null;
		try
		{
			sp = con.setSavepoint();
			stmt = con.getSqlConnection().prepareStatement(sql.toString());
			stmt.setString(1, indexDefinition.getName());
			if (hasSchema)
			{
				stmt.setString(2, indexDefinition.getSchema());
			}
			rs = stmt.executeQuery();
			if (rs.next())
			{
				result = rs.getString(1);
			}
			con.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			con.rollback(sp);
			LogMgr.logError("PostgresIndexReader.getIndexSource2()", "Error when retrieving index", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

}
