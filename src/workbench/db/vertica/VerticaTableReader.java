/*
 * VerticaTableReader.java
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A TableSourceBuilder for Vertica.
 *
 * @author Tatiana Saltykova
 */
public class VerticaTableReader
	implements ObjectListExtender
{
	private String sql =
			"SELECT table_name as name, \n" +
			"       decode(is_view,true,'SYSTEM VIEW',false,'SYSTEM TABLE') as type, \n" +
			"       'v_internal' as schema, \n" +
			"       current_database as catalog, \n" +
			"       table_description as remarks \n" +
			"FROM v_internal.vs_system_tables \n" +
			"WHERE table_schema = 'v_internal' \n" +
			"ORDER BY table_name";


	@Override
	public boolean extendObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects,String[] requestedTypes)
	{
		if (!handlesType(requestedTypes)) return false;
		if (!DbMetadata.typeIncluded("SYSTEM TABLE", requestedTypes)) return false;
		if (aSchema == null || !aSchema.equals("v_internal")) return false;

		Statement stmt = null;
		ResultSet tableRs = null;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("VerticaTableReader.getInternalTables()", "Query to retrieve tables in v_internal schema:\n" + sql);
		}

		try
		{
			stmt = con.createStatementForQuery();
			tableRs = stmt.executeQuery(sql);
			while (tableRs.next())
			{
				int row = result.addRow();
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, tableRs.getString("catalog"));
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, tableRs.getString("schema"));
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, tableRs.getString("name"));
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, tableRs.getString("remarks"));
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, tableRs.getString("type"));
			}
			return true;
		}
		catch (SQLException se)
		{
			LogMgr.logError("VerticaTableReader.extendObjectList()", "Could not retrieve internal tables", se);
		}
		finally
		{
			SqlUtil.closeAll(tableRs, stmt);
		}
		return false;
	}

	@Override
	public List<String> supportedTypes()
	{
		return CollectionUtil.arrayList("SYSTEM TABLE", "SYSTEM VIEW");
	}

	@Override
	public boolean isDerivedType()
	{
		return false;
	}

	@Override
	public boolean handlesType(String type)
	{
		return supportedTypes().contains(type);
	}

	@Override
	public boolean handlesType(String[] types)
	{
		if (types == null) return true;
		for (String type : types)
		{
			if (handlesType(type))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public DataStore getObjectDetails(WbConnection con, DbObject object)
	{
		return null;
	}

	@Override
	public DbObject getObjectDefinition(WbConnection con, DbObject name)
	{
		return null;
	}

	@Override
	public String getObjectSource(WbConnection con, DbObject object)
	{
		return null;
	}


  @Override
  public List<ColumnIdentifier> getColumns(WbConnection con, DbObject object)
  {
    return null;
  }

  @Override
  public boolean hasColumns()
  {
    return false;
  }
}
