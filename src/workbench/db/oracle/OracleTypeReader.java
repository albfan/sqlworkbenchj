/*
 * OracleTypeReader.java
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
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DataTypeResolver;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve Oracle object types and their definition.
 *
 * @author Thomas Kellerer
 */
public class OracleTypeReader
	implements ObjectListExtender
{

	public OracleTypeReader()
	{
	}

	@Override
	public boolean extendObjectList(WbConnection con, DataStore result, String catalogPattern, String schemaPattern, String namePattern, String[] requestedTypes)
	{
		// if no type has been requested, the Oracle driver does not include the object types
		// if TYPE has specifically been requested, the objects are returned
		if (requestedTypes != null || !DbMetadata.typeIncluded("TYPE", requestedTypes))
		{
			updateTypes(result);
			return false;
		}

		List<OracleObjectType> types = getTypes(con, schemaPattern, namePattern);
		for (OracleObjectType type : types)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, type.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, type.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, type.getObjectType());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, type.getComment());
			result.getRow(row).setUserObject(type);
		}
		return types.size() > 0;
	}

	private void updateTypes(DataStore result)
	{
		for (int row=0; row < result.getRowCount(); row ++)
		{
			String type = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
			if ("TYPE".equals(type))
			{
				String schema = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
				String name = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
				OracleObjectType object = new OracleObjectType(schema, name);
				result.getRow(row).setUserObject(object);
			}
		}
	}

	public List<OracleObjectType> getTypes(WbConnection con, String schema, String name)
	{
		StringBuilder select = new StringBuilder(50);
		select.append(
			"SELECT owner,  \n" +
			"       type_name, " +
			"       methods, \n" +
			"       attributes \n" +
			"FROM all_types ");

		int schemaIndex = -1;
		int nameIndex = -1;

		if (StringUtil.isNonBlank(schema))
		{
			select.append(" WHERE owner = ? ");
			schemaIndex = 1;
		}

		if (StringUtil.isNonBlank(name))
		{
			if (schemaIndex != -1)
			{
				select.append(" AND ");
				nameIndex = 2;
			}
			else
			{
				select.append(" WHERE ");
				nameIndex = 1;
			}
			if (name.indexOf('%') > 0)
			{
				select.append(" type_name LIKE ? ");
				SqlUtil.appendEscapeClause(select, con, name);
				name = SqlUtil.escapeUnderscore(name, con);
			}
			else
			{
				select.append(" type_name = ? ");
			}
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleObjectTypeReader.getTypes", "Using SQL=\n" + select);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<OracleObjectType> result = new ArrayList<>();
		try
		{
			stmt = con.getSqlConnection().prepareStatement(select.toString());
			if (schemaIndex > -1)
			{
				stmt.setString(schemaIndex, schema);
			}
			if (nameIndex > -1)
			{
				stmt.setString(nameIndex, name);
			}
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String typeSchema = rs.getString(1);
				String typeName = rs.getString(2);
				int methods = rs.getInt(3);
				int attribs = rs.getInt(4);
				OracleObjectType type = new OracleObjectType(typeSchema, typeName);
				type.setNumberOfMethods(methods);
				type.setNumberOfAttributes(attribs);
				result.add(type);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTypeReader.getTypes()", "Error retrieving attributes", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	@Override
	public List<String> supportedTypes()
	{
		return CollectionUtil.arrayList("TYPE");
	}

	@Override
	public boolean handlesType(String type)
	{
		return "TYPE".equals(type);
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
	public boolean isDerivedType()
	{
		return false;
	}

	@Override
	public DataStore getObjectDetails(WbConnection con, DbObject object)
	{
		if (object == null) return null;
		if (!handlesType(object.getObjectType())) return null;

		OracleObjectType type = getObjectDefinition(con, object);
		if (type == null) return null;

		String[] columns = new String[] { "ATTRIBUTE", "DATA_TYPE" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 30, 30 };
		DataStore result = new DataStore(columns, types, sizes);
		List<ColumnIdentifier> attr = type.getAttributes();
		if (CollectionUtil.isNonEmpty(attr))
		{
			for (ColumnIdentifier col : attr)
			{
				int row = result.addRow();
				result.setValue(row, 0, col.getColumnName());
				result.setValue(row, 1, col.getDbmsType());
			}
		}
		return result;
	}

	public List<ColumnIdentifier> getAttributes(WbConnection con, OracleObjectType type)
	{
		if (type == null) return null;

		String sql =
			"SELECT attr_name,  \n" +
			"       attr_type_name, \n" +
			"       length,  \n" +
			"       precision,  \n" +
			"       scale,  \n" +
			OracleTableDefinitionReader.getDecodeForDataType("attr_type_name", true, true) + " as jdbc_type \n" +
			"FROM all_type_attrs  \n";

		sql += " WHERE type_name = '" + type.getObjectName() + "' \n";
		sql += " AND owner = '" + type.getSchema() + "' \n";
		sql += " ORDER BY attr_no";
		Statement stmt = null;
		ResultSet rs = null;
		List<ColumnIdentifier> result = new ArrayList<>(type.getNumberOfAttributes());

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleTypeReader.getAttributes()", "Using SQL: " + sql);
		}

		try
		{
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			DataTypeResolver resolver = con.getMetadata().getDataTypeResolver();

			while (rs.next())
			{
				String colname = rs.getString(1);
				String dataType = rs.getString(2);
				int length = rs.getInt(3);
				int scale = rs.getInt(4);
				int precision = rs.getInt(5);
				int jdbcType = rs.getInt(6);

				ColumnIdentifier col = new ColumnIdentifier(colname, jdbcType);

				if (SqlUtil.isCharacterTypeWithLength(jdbcType))
				{
					col.setDbmsType(resolver.getSqlTypeDisplay(dataType, jdbcType, length, 0));
				}
				else
				{
					col.setDbmsType(resolver.getSqlTypeDisplay(dataType, jdbcType, scale, precision));
					col.setDecimalDigits(precision);
				}
				result.add(col);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTypeReader.getAttributes()", "Error retrieving attributes", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	@Override
	public OracleObjectType getObjectDefinition(WbConnection con, DbObject name)
	{
		List<OracleObjectType> objects = getTypes(con, name.getSchema(), name.getObjectName());
		if (CollectionUtil.isEmpty(objects)) return null;
		OracleObjectType type = objects.get(0);
		if (type.getNumberOfAttributes() > 0)
		{
			List<ColumnIdentifier> attr = getAttributes(con, type);
			type.setAttributes(attr);
		}
		return type;
	}

	private String retrieveSource(WbConnection con, DbObject object, String schemaForSource)
	{
		if (object == null) return null;


		String sql = "select dbms_metadata.get_ddl('TYPE', ?, ?) from dual";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleTypeReader.retrieveSource()", "Using SQL: " + SqlUtil.replaceParameters(sql, object.getObjectName(), object.getSchema()));
		}


		PreparedStatement stmt = null;
		ResultSet rs = null;
		String source = null;

		try
		{
      if (StringUtil.isNonEmpty(schemaForSource))
      {
        // TODO: remap schema names
      }

			stmt = con.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, object.getObjectName());
			stmt.setString(2, object.getSchema());

			rs = stmt.executeQuery();
			if (rs.next())
			{
				source = rs.getString(1);
				if (source != null)
				{
					source = source.trim();
				}
			}

			boolean needsAlternateDelimiter = false;

			int pos = -1;
			if (source != null)
			{
				Pattern p = Pattern.compile("CREATE OR REPLACE\\s+(EDITIONABLE){0,1}\\s*TYPE BODY");
				Matcher m = p.matcher(source);
				if (m.find())
				{
					pos = m.start();
				}
			}

			// the first closing bracket with a semicolon marks the end of the type declaration
			// so we need to insert an alternate delimiter there.
			if (pos > 1)
			{
				StringBuilder fullSource = new StringBuilder(source);
				fullSource.insert(pos - 1, "\n/\n");
				fullSource.append("\n/\n");
				source = fullSource.toString();
				needsAlternateDelimiter = true;
			}

			if (!StringUtil.endsWith(source, ';') && !needsAlternateDelimiter)
			{
				source += ";\n";
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTypeReader.retrieveSource()", "Error retrieving source", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}

	@Override
	public String getObjectSource(WbConnection con, DbObject object)
  {
    return getObjectSource(con, object, null);
  }

	public String getObjectSource(WbConnection con, DbObject object, String schemaForSource)
	{
		if (object == null) return null;
		if (!handlesType(object.getObjectType())) return null;
		OracleObjectType type = getObjectDefinition(con, object);
		if (type == null) return null;

		String source = type.getSource();

		if (StringUtil.isBlank(source))
		{
			source = retrieveSource(con, object, schemaForSource);
			type.setSource(source);
		}
		return source;
	}

}
