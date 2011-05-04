/*
 * OracleMViewReader.java
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleMViewReader
{

	private String pkIndex;

	public OracleMViewReader()
	{
	}

	public CharSequence getMViewSource(WbConnection dbConnection, TableIdentifier table, DataStore indexDefinition, boolean includeDrop)
	{
		boolean alwaysUseDbmsMeta = dbConnection.getDbSettings().getUseOracleDBMSMeta("mview");

		StringBuilder result = new StringBuilder(250);

		if (includeDrop)
		{
			result.append("DROP MATERIALIZED VIEW ");
			result.append(table.getTableExpression(dbConnection));
			result.append(";\n\n");
		}

		boolean retrieved = false;
		pkIndex = null;
		if (alwaysUseDbmsMeta)
		{
			try
			{
				String sql = getSourceFromDBMSMeta(dbConnection, table);
				result.append(sql);
				retrieved = true;
			}
			catch (Exception sql)
			{
			}
		}

		if (!retrieved)
		{
			String sql = retrieveMViewQuery(dbConnection, table);
			if (includeDrop)
			{
				result.append("CREATE MATERIALIZED VIEW ");
			}
			else
			{
				result.append("CREATE OR REPLACE MATERIALIZED VIEW ");
			}
			result.append(table.getTableExpression(dbConnection));
			String options = getMViewOptions(dbConnection, table);
			if (options != null)
			{
				result.append(options);
			}
			result.append("\nAS\n");
			result.append(sql);
		}
		result.append("\n\n");

		if (indexDefinition == null)
		{
			indexDefinition = dbConnection.getMetadata().getIndexReader().getTableIndexInformation(table);
		}

		// The source for the auto-generated index that is created when using the WITH PRIMARY KEY option
		// does not need to be included in the generated SQL
		if (pkIndex != null)
		{
			int toDelete = -1;
			for (int row = 0; row < indexDefinition.getRowCount(); row++)
			{
				String name = indexDefinition.getValueAsString(row, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
				if (name.equals(pkIndex))
				{
					toDelete = row;
					break;
				}
			}
			if (toDelete > -1)
			{
				indexDefinition.deleteRow(toDelete);
			}
		}
		StringBuilder indexSource = dbConnection.getMetadata().getIndexReader().getIndexSource(table, indexDefinition, table.getTableName());

		if (indexSource != null) result.append(indexSource);
		return result.toString();
	}

	private String getMViewOptions(WbConnection dbConnection, TableIdentifier mview)
	{
		String sql =
			"select mv.rewrite_enabled, \n" +
			"       mv.refresh_mode, \n" +
			"       mv.refresh_method, \n" +
			"       mv.build_mode, \n " +
			"       mv.fast_refreshable, \n" +
			"       cons.constraint_name, \n" +
			"       cons.index_name \n" +
			"from all_mviews mv \n" +
			"  left join all_constraints cons on cons.owner = mv.owner and cons.table_name = mv.mview_name and cons.constraint_type = 'P' \n " +
			"where mv.owner = ? \n" +
			" and mv.mview_name = ? ";

		StringBuilder result = new StringBuilder(50);
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleMViewReader.getMViewOptions()", "Retrieving MVIEW options using: \n"  + SqlUtil.replaceParameters(sql, mview.getSchema(), mview.getTableName()));
			}
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, mview.getSchema());
			stmt.setString(2, mview.getTableName());
			rs = stmt.executeQuery();
			if (rs.next())
			{
				String immediate = rs.getString("BUILD_MODE");
				result.append("\n  BUILD ");
				result.append(immediate);

				String method = rs.getString("REFRESH_METHOD");
				result.append("\n  REFRESH ");
				result.append(method);

				String when = rs.getString("REFRESH_MODE");
				result.append(" ON ");
				result.append(when);

				String pk = rs.getString("constraint_name");
				if (pk != null)
				{
					result.append(" WITH PRIMARY KEY");
				}
				else
				{
					result.append(" WITH ROWID");
				}

				String rewrite = rs.getString("REWRITE_ENABLED");
				if ("Y".equals(rewrite))
				{
					result.append("\n  ENABLE QUERY REWRITE");
				}
				else
				{
					result.append("\n  DISABLE QUERY REWRITE");
				}
				pkIndex = rs.getString("INDEX_NAME");
			}
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("OracleMetadata.getSnapshotSource()", "Error accessing all_mviews", e);
			result = new StringBuilder(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result.toString();
	}

	private String retrieveMViewQuery(WbConnection dbConnection, TableIdentifier mview)
	{
		String result = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql = "SELECT query FROM all_mviews WHERE owner = ? and mview_name = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleMViewReader.generateMViewSource()", SqlUtil.replaceParameters(sql, mview.getSchema(), mview.getTableName()));
		}
		try
		{
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, mview.getSchema());
			stmt.setString(2, mview.getTableName());
			rs = stmt.executeQuery();
			if (rs.next())
			{
				result = rs.getString(1);
				if (rs.wasNull())
				{
					result = "";
				}
				else
				{
					result = result.trim();
				}

				if (!result.endsWith(";"))
				{
					result += ";";
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("OracleMetadata.getSnapshotSource()", "Error accessing all_mviews", e);
			result = ExceptionUtil.getDisplay(e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private String getSourceFromDBMSMeta(WbConnection dbConnection, TableIdentifier table)
		throws SQLException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String source = null;

		String sql = "select dbms_metadata.get_ddl('MATERIALIZED_VIEW', ?, ?) from dual";

		try
		{
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleMViewReader.getSourceFromDBMSMeta()", "Retrieving MVIEW options using: \n"  + SqlUtil.replaceParameters(sql, table.getSchema(), table.getTableName()));
			}

			stmt = dbConnection.getMetadata().getSqlConnection().prepareStatement(sql);

			stmt.setString(1, table.getObjectName());
			stmt.setString(2, table.getSchema());

			rs = stmt.executeQuery();
			if (rs.next())
			{
				source = rs.getString(1);
				if (source != null) source = source.trim();
				source += ";\n";
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleIndexReader", "Error retrieving mview via DBMS_METADATA", e);
			throw e;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}
}
