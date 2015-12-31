/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.ibm;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.ObjectListEnhancer;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2iObjectListEnhancer
	implements ObjectListEnhancer
{

	@Override
	public void updateObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes)
	{
		if (con.getDbSettings().getBoolProperty("remarks.tables.use_tabletext", false))
		{
			updateObjectRemarks(con, result, aCatalog, aSchema, objects, requestedTypes);
		}
	}

	public void updateObjectRemarks(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (result == null) return;
		if (result.getRowCount() == 0) return;

    boolean tablesRequested = DbMetadata.typeIncluded("TABLE", requestedTypes);
    if (!tablesRequested) return;

		String object = null;
		if (result.getRowCount() == 1)
		{
			object = result.getValueAsString(0, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			// no need to loop through all requested types if only a single object is requested
			requestedTypes = new String[] { result.getValueAsString(0, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE)};
		}

		Map<String, String> remarks = readRemarks(con, schema, object, requestedTypes);

		for (int row=0; row < result.getRowCount(); row++)
		{
			String name = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String objectSchema = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);

			String remark = remarks.get(objectSchema + "." + name);
			if (remark != null)
			{
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, remark);
			}
		}
	}

	private Map<String, String> readRemarks(WbConnection con, String schema, String object, String[] requestedTypes)
	{
		StringBuilder sql = new StringBuilder(50);
    sql.append(
      "select table_schema, table_name, table_text\n" +
      "from qsys2.systables t\n");

    boolean whereAdded = false;
    if (schema != null)
    {
      whereAdded = true;
      sql.append("where ");
      SqlUtil.appendExpression(sql, "table_schema", schema, con);
    }

    if (object != null)
    {
      if (whereAdded)
      {
        sql.append("\n  and ");
      }
      else
      {
        sql.append("where ");
      }
      SqlUtil.appendExpression(sql, "table_name", schema, con);
    }

		Statement stmt = null;
		ResultSet rs = null;

		if (schema == null)
		{
			schema = con.getMetadata().getCurrentSchema();
		}

		Map<String, String> remarks = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
		String type = null;
		try
		{
			for (String requestedType : requestedTypes)
			{
				type = requestedType;
				if (Settings.getInstance().getDebugMetadataSql())
				{
					LogMgr.logInfo("Db2iServerObjectListEnhancer.updateObjectRemarks()", "Retrieving table remarks using:\n" + sql);
				}
				stmt = con.createStatementForQuery();
				rs = stmt.executeQuery(sql.toString());
				while (rs.next())
				{
					String objectname = rs.getString(2);
					String remark = rs.getString(3);
					if (objectname != null && remark != null)
					{
						remarks.put(schema + "." + objectname.trim(), remark);
					}
				}
				SqlUtil.closeResult(rs);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Db2iServerObjectListEnhancer.updateObjectRemarks()", "Error retrieving remarks using:\n " + SqlUtil.replaceParameters(sql, schema, type), e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return remarks;
	}
}
