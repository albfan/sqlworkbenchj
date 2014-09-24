/*
 * VerticaProjectionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.vertica;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;


/**
 *
 * This class retrieves information about projections from Vertica.
 *
 * @author Tatiana Saltykova
 */
public class VerticaProjectionReader
{
	private WbConnection dbConnection;

	public VerticaProjectionReader()
	{
	}

	public void setConnection(WbConnection conn)
	{
		dbConnection = conn;
	}

	public DataStore getProjectionList(TableIdentifier table)
		throws SQLException
	{
		// Views can't have projections
		if ("VIEW".equals(table.getType())) return null;

		String sql =
			"SELECT projection_basename as basename, \n" +
			"       decode(is_super_projection,true,'super') as is_super, \n" +
			"       decode(is_segmented,true,'segmented',false,'replicated') as is_segmented, \n" +
			"       decode(is_prejoin,true,'prejoin') as is_prejoin, \n" +
			"       decode(is_up_to_date,true,'up to date',false,'stale') as is_up_to_date, \n" +
			"       decode(has_statistics,true,'has stats',false,'no stats') as has_statistics, \n"+
			"       verified_fault_tolerance, \n" +
			"       count(*) \n" +
			"FROM projections \n" +
			"WHERE anchor_table_name = ? \n" +
			"  AND projection_schema = ? \n" +
			"group by 1,2,3,4,5,6,7 \n" +
			"ORDER BY 2, projection_basename";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("VerticaProjectionReader.getProjectionList()", "Query to retrieve projections for " + table.getTableExpression(dbConnection) + ":\n" + sql);
		}

		PreparedStatement projectionStatement = null;
		ResultSet rs = null;
		try
		{
			projectionStatement = this.dbConnection.getSqlConnection().prepareStatement(sql);
			projectionStatement.setString(1, table.getTableName());
			projectionStatement.setString(2, table.getSchema());
			rs = projectionStatement.executeQuery();
			DataStore ds = new DataStore(rs, true);
			return ds;
		}
		catch (Exception ex)
		{
			LogMgr.logError("Vertica", "error", ex);
		}

		return null;
	}

	public DataStore getProjectionCopies(String projectionBasename)
		throws SQLException
	{
		String sql =
			"SELECT p.projection_name as name, \n" +
			"       coalesce(p.node_name,ps.node_name) as node_name, \n" +
			"       p.created_epoch, \n" +
			"       ps.projection_column_count, \n" +
			"       ps.wos_row_count, \n" +
			"       ps.ros_row_count, \n" +
			"       ps.wos_used_bytes, \n" +
			"       ps.ros_used_bytes, \n" +
			"       ps.ros_count \n" +
			"FROM projections p \n" +
			"  left outer join projection_storage ps using (projection_id) \n" +
			"WHERE p.projection_basename = ? \n" +
			"ORDER BY p.projection_name, node_name ";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("VerticaProjectionReader.getProjectionCopies()", "Query to retrieve projection copies for " + projectionBasename + ":\n" + sql);
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, projectionBasename);
			rs = pstmt.executeQuery();
			DataStore ds = new DataStore(rs, true);
			return ds;
		}
		catch (Exception ex)
		{
			LogMgr.logDebug("VerticaProjectionReader.getProjectionColumns()", "Could not read projection columns", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return null;
	}

	public DataStore getProjectionColumns(String projectionName)
		throws SQLException
	{
		String sql =
			"SELECT pc.projection_name as name, \n" +
			"       pc.projection_column_name, \n" +
			"       decode(pc.projection_column_name,pc.table_column_name,'',pc.table_column_name) as table_column_name, \n" +
			"       pc.column_position, \n" +
			"       pc.sort_position, \n" +
			"       pc.data_type, \n" +
			"       pc.encoding_type, \n" +
			"       pc.access_rank, \n" +
			"       pc.group_id, \n" +
			"       pc.statistics_type, \n" +
			"       pc.statistics_updated_timestamp, \n" +
			"       sum(used_bytes) as bytes_total, \n" +
			"       sum(ros_count) as ros_total, \n" +
			"       avg(used_bytes) as bytes_per_node, \n" +
			"       avg(ros_count) as ros_per_node \n" +
			"FROM projection_columns pc \n" +
			"  left outer join column_storage ps using (projection_id,column_id) \n" +
			"WHERE pc.projection_id = (select projection_id FROM projections WHERE projection_basename = ? limit 1) \n" +
			"GROUP BY 1,2,3,4,5,6,7,8,9,10,11 \n" +
			"ORDER BY pc.column_position ";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("VerticaProjectionReader.getProjectionColumns()", "Query to retrieve projection columns for " + projectionName + ":\n" + sql);
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, projectionName);
			rs = pstmt.executeQuery();
			DataStore ds = new DataStore(rs, true);
			return ds;
		}
		catch (Exception ex)
		{
			LogMgr.logDebug("VerticaProjectionReader.getProjectionColumns()", "Could not read projection columns", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return null;
	}

}
