/*
 * MySQLTableCommentReader
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mysql;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;
import workbench.db.DbMetadata;
import workbench.db.ObjectListEnhancer;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLTableCommentReader
	implements ObjectListEnhancer
{

	@Override
	public void updateObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes)
	{
		if (Settings.getInstance().getBoolProperty("workbench.db.mysql.tablecomments.retrieve", false))
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

		Map<String, String> remarks = readRemarks(con, catalog, object, requestedTypes);

		for (int row=0; row < result.getRowCount(); row++)
		{
			String tblName = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String tblSchema = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
			String remark = remarks.get(getNameKey(tblSchema, tblName));
			if (remark != null && !remark.equals("VIEW"))
			{
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, remark);
			}
		}
	}

	private String getNameKey(String schema, String objectname)
	{
		if (schema != null && objectname != null)
		{
			return schema.trim() + "." + objectname.trim();
		}
		else if (objectname != null)
		{
			return objectname.trim();
		}
		return null;
	}

	public Map<String, String> readRemarks(WbConnection con, String catalog, String object, String[] requestedTypes)
	{
		String sql = "select table_schema, table_name, table_comment from information_schema.tables";

		boolean whereAdded = false;

		if (StringUtil.isNonBlank(object))
		{
			sql += " WHERE table_name = '" + object + "'";
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(catalog))
		{
			if (whereAdded) sql += " AND ";
			else sql += " WHERE ";

			sql += " table_schema = '" + catalog + "'";
			whereAdded = true;
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("MySQLTableCommentReader.updateObjectRemarks()", "Using query=\n" + sql);
		}

		Statement stmt = null;
		ResultSet rs = null;

		Map<String, String> remarks = new TreeMap<String, String>(CaseInsensitiveComparator.INSTANCE);
		try
		{
				stmt = con.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next())
				{
					String schema = rs.getString(1);
					String objectname = rs.getString(2);
					String remark = rs.getString(3);
					if (objectname != null && StringUtil.isNonBlank(remark))
					{
						remarks.put(getNameKey(schema, objectname), remark);
					}
				}
		}
		catch (Exception e)
		{
			LogMgr.logError("MySQLTableCommentReader.updateObjectRemarks()", "Error retrieving remarks", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return remarks;
	}
}
