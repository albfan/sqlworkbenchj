/*
 * OracleObjectListEnhancer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import workbench.db.DbMetadata;
import workbench.db.ObjectListEnhancer;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * A class to "cleanup" the reported table type for MATERIALZED VIEWS.
 *
 * The JDBC driver returns MVIEWS with the type "TABLE" which is not useful when displaying
 * objects in the DbExplorer.
 *
 * This class processes the retrieved objects and updates the object type accordingly
 *
 * @author Thomas Kellerer
 */
public class OracleObjectListEnhancer
	implements ObjectListEnhancer
{

	private boolean canRetrieveSnapshots = true;

	@Override
	public void updateObjectList(WbConnection con, DataStore result, String catalogPattern, String schema, String objectNamePattern, String[] types)
	{
		boolean checkSnapshots = Settings.getInstance().getBoolProperty("workbench.db.oracle.detectsnapshots", true) && DbMetadata.typeIncluded("TABLE", types);
		if (!checkSnapshots) return;

		Set<String> snapshots = getSnapshots(con, schema);
		for (int row=0; row < result.getRowCount(); row++)
		{
			String owner = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			String name =  result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String fqName = owner + "." + name;
			if (snapshots.contains(fqName))
			{
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, DbMetadata.MVIEW_NAME);
			}
		}
	}

	/**
	 * Returns a Set with Strings identifying available Snapshots (materialized views).
	 *
	 * The names will be returned as owner.tablename
	 * In case the retrieve throws an error, this method will return
	 * an empty set in subsequent calls.
	 */
	public Set<String> getSnapshots(WbConnection connection, String schema)
	{
		if (!canRetrieveSnapshots)
		{
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<String>();
		String sql = "SELECT /* SQLWorkbench */ owner||'.'||mview_name FROM all_mviews";
		if (schema != null)
		{
			sql += " WHERE owner = ?";
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			stmt = connection.getSqlConnection().prepareStatement(sql);
			if (schema != null)
			{
				stmt.setString(1, schema);
			}
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String name = rs.getString(1);
				result.add(name);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("OracleObjectListEnhancer.getSnapshots()", "Error accessing all_mviews", e);
			// When we get an exception, most probably we cannot access the ALL_MVIEWS view.
			// To avoid further (unnecessary) calls, we are disabling the support
			// for snapshots
			this.canRetrieveSnapshots = false;
			result = Collections.emptySet();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

}
