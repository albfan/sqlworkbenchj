/*
 * SqlServerObjectListEnhancer
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;
import workbench.db.DbMetadata;
import workbench.db.JdbcUtils;
import workbench.db.ObjectListEnhancer;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerObjectListEnhancer
	implements ObjectListEnhancer
{

	@Override
	public void updateObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes)
	{
		if (Settings.getInstance().getBoolProperty("workbench.db.microsoft_sql_server.remarks.object.retrieve", false))
		{
			updateObjectRemarks(con, result, aCatalog, aSchema, objects, requestedTypes);
		}
	}

	protected void updateObjectRemarks(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (result == null) return;
		if (result.getRowCount() == 0) return;

		String object = null;
		if (result.getRowCount() == 1)
		{
			object = result.getValueAsString(0, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		}

		Map<String, String> remarks = readRemarks(con, schema, object, requestedTypes);

		for (int row=0; row < result.getRowCount(); row++)
		{
			String name = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String remark = remarks.get(name);
			if (remark != null)
			{
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, remark);
			}
		}
	}


	public Map<String, String> readRemarks(WbConnection con, String schema, String object, String[] requestedTypes)
	{
		String propName = Settings.getInstance().getProperty("workbench.db.microsoft_sql_server.remarks.propertyname", "MS_DESCRIPTION");
		String sql = null;

		if (JdbcUtils.hasMinimumServerVersion(con, "9.0"))
		{
			sql = "SELECT objtype, objname, cast(value as varchar) as value \n" +
      "FROM fn_listextendedproperty ('" + propName + "','schema', ?, ?, ";
		}
		else
		{
			sql = "SELECT objname, cast(value as varchar) as value \n" +
      "FROM ::fn_listextendedproperty ('" + propName + "','user', ?, ?, ";
		}

		if (object == null)
		{
			sql += "null, null, null)";
		}
		else
		{
			sql += "'" + object + "', null, null)";
		}

		if (requestedTypes == null)
		{
			requestedTypes = new String[] { "TABLE", "VIEW", "SYNONYM", "TYPE" };
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("SqlServerObjectListEnhancer.updateObjectRemarks()", "Using query=\n" + sql);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;

		Map<String, String> remarks = new TreeMap<String, String>(CaseInsensitiveComparator.INSTANCE);
		try
		{
			for (String type : requestedTypes)
			{
				stmt = con.getSqlConnection().prepareStatement(sql);
				stmt.setString(1, schema);
				stmt.setString(2, type);
				rs = stmt.executeQuery();
				while (rs.next())
				{
					String objectname = rs.getString(2);
					String remark = rs.getString(3);
					if (objectname != null && remark != null)
					{
						remarks.put(objectname.trim(), remark);
					}
				}
				SqlUtil.closeResult(rs);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlServerColumnEnhancer.updateObjectRemarks()", "Error retrieving remarks", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return remarks;
	}
}
