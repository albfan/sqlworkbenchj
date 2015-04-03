/*
 * HsqlTypeReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.hsqldb;


import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.BaseObjectType;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An ObjectlistEnhancer to read TYPE definitions for HSQLDB
 *
 * @author Thomas Kellerer
 */
public class HsqlTypeReader
	implements ObjectListExtender
{

	@Override
	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schemaPattern, String objectPattern, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("TYPE", requestedTypes)) return false;

		List<HsqlType> types = getTypes(con, schemaPattern, objectPattern);
		if (types.isEmpty()) return false;

		for (BaseObjectType type : types)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, type.getCatalog());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, type.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, type.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, type.getObjectType());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, type.getComment());
			result.getRow(row).setUserObject(type);
		}
		return true;
	}

	public List<HsqlType> getTypes(WbConnection con, String schemaPattern, String objectPattern)
	{
		List<HsqlType> result = new ArrayList<>();

    StringBuilder select = new StringBuilder(100);

		String baseSelect =
			"SELECT user_defined_type_catalog, user_defined_type_schema, user_defined_type_name, source_dtd_identifier \n" +
			"FROM information_schema.user_defined_types";

		select.append(baseSelect);
		boolean whereAdded = false;
		if (StringUtil.isNonBlank(schemaPattern))
		{
			select.append("\n WHERE ");
			SqlUtil.appendExpression(select, "user_defined_type_schema", schemaPattern, con);
			whereAdded = true;
		}

		if (StringUtil.isNonEmpty(objectPattern))
		{
			if (whereAdded)
			{
				select.append(" AND ");
			}
			else
			{
				select.append("\n WHERE ");
			}
			SqlUtil.appendExpression(select, "user_defined_type_name", objectPattern, con);
		}

		select.append("\n ORDER BY 2,3 ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("HsqlTypeReader.extendObjectList()", "Using SQL: " + select);
		}

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(select.toString());
			while (rs.next())
			{
				String catalog = rs.getString("user_defined_type_catalog");
				String schema = rs.getString("user_defined_type_schema");
				String name = rs.getString("user_defined_type_name");
				String datatype = rs.getString("source_dtd_identifier");
				HsqlType type = new HsqlType(schema, name);
				type.setCatalog(catalog);
				type.setDataTypeName(datatype);
				result.add(type);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("HsqlTypeReader.getTypes()", "Error retrieving object types", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	@Override
	public boolean isDerivedType()
	{
		return false;
	}

	@Override
	public List<String> supportedTypes()
	{
		return CollectionUtil.arrayList("TYPE");
	}

	@Override
	public boolean handlesType(String type)
	{
		return "TYPE".equalsIgnoreCase(type);
	}

	@Override
	public boolean handlesType(String[] types)
	{
		if (types == null) return true;
		for (String type : types)
		{
			if (handlesType(type)) return true;
		}
		return false;
	}

	@Override
	public DataStore getObjectDetails(WbConnection con, DbObject object)
	{
		try
		{
			HsqlType type = getObjectDefinition(con, object);
			if (type != null)
			{
				DataStore details = new DataStore(new String[] {"TYPE_NAME", "DATA_TYPE"}, new int[] { Types.VARCHAR, Types.VARCHAR } );
				details.addRow();
				details.setValue(0, 0, type.getObjectName());
				details.setValue(0, 1, type.getDataTypeName());
				return details;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("HsqlTypeReader.getObjectDetails()", "Cannot retrieve type columns", e);
		}
		return null;
	}

	@Override
	public HsqlType getObjectDefinition(WbConnection con, DbObject name)
	{
		try
		{
			List<HsqlType> types = getTypes(con, name.getSchema(), name.getObjectName());
			if (types.size() == 1)
			{
				return types.get(0);
			}
			return null;
		}
		catch (Exception e)
		{
			LogMgr.logError("HsqlTypeReader.getObjectDetails()", "Cannot retrieve type columns", e);
		}
		return null;
	}

	@Override
	public String getObjectSource(WbConnection con, DbObject object)
	{
		HsqlType type = getObjectDefinition(con, object);
		if (type == null) return null;
		StringBuilder sql = new StringBuilder(100);
		sql.append("CREATE TYPE ");
		sql.append(type.getObjectName());
		sql.append(" AS ");
		sql.append(type.getDataTypeName());
		sql.append(";\n");
		return sql.toString();
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
