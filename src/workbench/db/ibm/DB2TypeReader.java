/*
 * DB2TypeReader.java
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
package workbench.db.ibm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DataTypeResolver;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListEnhancer;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An ObjectListExtender to read OBJECT TYPEs
 * does not return a value in the TABLE_TYPE column
 *
 * @author Thomas Kellerer
 */
public class DB2TypeReader
	implements ObjectListExtender, ObjectListEnhancer
{
	private final Map<String, Integer> typeNameMap = new HashMap<>(10);

	public DB2TypeReader()
	{
		typeNameMap.put("VARCHAR", Types.VARCHAR);
		typeNameMap.put("CHAR", Types.CHAR);
		typeNameMap.put("SMALLINT", Types.SMALLINT);
		typeNameMap.put("BIGINT", Types.BIGINT);
		typeNameMap.put("INTEGER", Types.INTEGER);
		typeNameMap.put("DECIMAL", Types.DECIMAL);
		typeNameMap.put("DOUBLE", Types.DOUBLE);
		typeNameMap.put("CHARACTER", Types.CHAR);
		typeNameMap.put("LONG VARCHAR", Types.LONGVARCHAR);
		typeNameMap.put("BLOB", Types.BLOB);
		typeNameMap.put("CLOB", Types.CLOB);
		typeNameMap.put("DATE", Types.DATE);
		typeNameMap.put("TIME", Types.TIME);
		typeNameMap.put("BOOLEAN", Types.BOOLEAN);
		typeNameMap.put("XML", Types.SQLXML);
		typeNameMap.put("VARBINARY", Types.VARBINARY);
		typeNameMap.put("REAL", Types.REAL);
		typeNameMap.put("ARRAY", Types.ARRAY);
		typeNameMap.put("BINARY", Types.BINARY);
	}

	@Override
	public boolean isDerivedType()
	{
		return false;
	}

	@Override
	public void updateObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes)
	{
	}

	@Override
	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schemaPattern, String objectPattern, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("TYPE", requestedTypes)) return false;

		List<DB2ObjectType> types = getTypes(con, schemaPattern, objectPattern);
		if (types.isEmpty()) return false;

		for (DB2ObjectType type : types)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, type.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, type.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, type.getObjectType());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, type.getComment());
			result.getRow(row).setUserObject(type);
		}
		return true;
	}

	public List<DB2ObjectType> getTypes(WbConnection con, String schemaPattern, String namePattern)
	{
		List<DB2ObjectType> result = new ArrayList<>();
		String select =
			"select typeschema,  \n" +
			"       typename,   \n" +
			"       remarks,   \n" +
			"       sourcename, \n" +
			"       metatype, \n" +
			"       length,  \n" +
			"       array_length,  \n" +
			"       scale   \n" +
			"from syscat.datatypes  \n" +
			"where ownertype = 'U' ";

		if (StringUtil.isNonBlank(schemaPattern))
		{
			if (schemaPattern.indexOf('%') > -1)
			{
				select += " AND typeschema LIKE '" + schemaPattern + "' ";
			}
			else
			{
				select += " AND typeschema = '" + schemaPattern + "' ";
			}
		}

		if (StringUtil.isNonBlank(namePattern))
		{
			if (namePattern.indexOf('%') > -1)
			{
				select += " AND typename LIKE '" + namePattern + "' ";
			}
			else
			{
				select += " AND typename = '" + namePattern + "' ";
			}
		}

		select += " ORDER BY typeschema, typename ";
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DB2TypeReader.getTypes()", "Query to retrieve TYPE: " + select);
		}

		Statement stmt = null;
		ResultSet rs = null;
		DataTypeResolver resolver = con.getMetadata().getDataTypeResolver();

		try
		{
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(select);
			while (rs.next())
			{
				String schema = rs.getString("typeschema");
				String name = rs.getString("typename");
				String remarks = rs.getString("REMARKS");
				String meta = rs.getString("METATYPE");
				String baseType = rs.getString("SOURCENAME");
				int len = rs.getInt("length");
				int scale = rs.getInt("scale");
				int arrayLength = rs.getInt("array_length");
				DB2ObjectType object = new DB2ObjectType(schema, name);
				object.setComment(remarks);
				object.setMetaType(meta);

				if (object.getMetaType() != DB2ObjectType.MetaType.structured)
				{
					int jdbcType = db2TypeToJDBC(baseType);
					object.setBaseType(resolver.getSqlTypeDisplay(baseType, jdbcType, len, scale));
					object.setArrayLength(arrayLength);
				}
				result.add(object);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DB2TypeReader.getTypes()", "Error retrieving object types", e);
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
		if (object == null) return null;
		if (!handlesType(object.getObjectType())) return null;

		DB2ObjectType type = getObjectDefinition(con, object);
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

	@Override
	public DB2ObjectType getObjectDefinition(WbConnection con, DbObject name)
	{
		List<DB2ObjectType> objects = getTypes(con, name.getSchema(), name.getObjectName());
		if (CollectionUtil.isEmpty(objects)) return null;
		DB2ObjectType type = objects.get(0);
		List<ColumnIdentifier> attr = getAttributes(con, type);
		type.setAttributes(attr);
		return type;
	}

	@Override
	public String getObjectSource(WbConnection con, DbObject object)
	{
		DB2ObjectType type = getObjectDefinition(con, object);
		if (type == null) return null;
		StringBuilder sql = new StringBuilder(50 + type.getNumberOfAttributes() * 50);
		sql.append("CREATE TYPE ");
		sql.append(type.getObjectName());

		DB2ObjectType.MetaType metaType = type.getMetaType();

		switch (metaType)
		{
			case cursor:
				sql.append(" AS CURSOR;");
				break;

			case distinct:
				sql.append(" AS ");
				sql.append(type.getBaseType());
				if (!type.getBaseType().endsWith("LOB"))
				{
					sql.append(" WITH COMPARISONS");
				}
				sql.append(';');
				break;

			case array:
				sql.append(" AS ");
				sql.append(type.getBaseType());
				sql.append(" ARRAY[");
				sql.append(type.getArrayLength());
				sql.append("]");
				sql.append(';');
				break;

			default:
				if (metaType == DB2ObjectType.MetaType.row)
				{
					sql.append(" AS ROW \n(\n");
				}
				else
				{
					sql.append(" AS\n(\n");
				}
				List<ColumnIdentifier> columns = type.getAttributes();
				int maxLen = ColumnIdentifier.getMaxNameLength(columns);
				for (int i = 0; i < columns.size(); i++)
				{
					sql.append("  ");
					sql.append(StringUtil.padRight(columns.get(i).getColumnName(), maxLen + 2));
					sql.append(columns.get(i).getDbmsType());
					if (i < columns.size() - 1)
					{
						sql.append(",\n");
					}
				}
				sql.append("\n);\n");
		}
		return sql.toString();
	}

	public List<ColumnIdentifier> getAttributes(WbConnection con, DB2ObjectType type)
	{
		if (type == null) return null;

		String sql = null;

		if (type.getMetaType() == DB2ObjectType.MetaType.row)
		{
			sql =
				"select fieldname,  \n" +
				"       fieldtypename, \n" +
				"       length, \n" +
				"       scale  \n" +
				"from syscat.rowfields \n";
		}
		else
		{
			sql =
				"select attr_name,  \n" +
				"       attr_typename, \n" +
				"       length, \n" +
				"       scale  \n" +
				"from syscat.attributes  \n";
		}

		sql += " WHERE typename = '" + type.getObjectName() + "' \n";
		sql += " AND typeschema = '" + type.getSchema() + "' \n";
		sql += " ORDER BY ordinal";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DB2TypeReader.getAttributes()", "Using SQL: " + sql);
		}

		Statement stmt = null;
		ResultSet rs = null;
		List<ColumnIdentifier> result = new ArrayList<>(type.getNumberOfAttributes());
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
				int jdbcType = db2TypeToJDBC(dataType);
				ColumnIdentifier col = new ColumnIdentifier(colname, jdbcType);

				if (SqlUtil.isCharacterTypeWithLength(jdbcType))
				{
					col.setDbmsType(resolver.getSqlTypeDisplay(dataType, jdbcType, length, 0));
				}
				else
				{
					col.setDbmsType(resolver.getSqlTypeDisplay(dataType, jdbcType, length, scale));
				}
				result.add(col);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("DB2TypeReader.getAttributes()", "Error retrieving attributes", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private int db2TypeToJDBC(String typeName)
	{
		if (typeName == null) return Types.OTHER;
		Integer type = typeNameMap.get(typeName);
		if (type == null)
		{
			return Types.OTHER;
		}
		return type.intValue();
	}
}
