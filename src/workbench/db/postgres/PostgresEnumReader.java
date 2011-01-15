/*
 * PostgresEnumReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.EnumIdentifier;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read the defined ENUM types from Postgres.
 *
 * @author Thomas Kellerer
 */
public class PostgresEnumReader
	implements ObjectListExtender
{
	final	String baseSql = "select current_database() as enum_catalog, \n" +
             "       n.nspname as enum_schema,  \n" +
             "       t.typname as enum_name,  \n" +
             "       e.enumlabel as enum_value,  \n" +
             "       obj_description(t.oid) as remarks \n" +
             "from pg_type t \n" +
             "   join pg_enum e on t.oid = e.enumtypid  \n" +
             "   join pg_catalog.pg_namespace n ON n.oid = t.typnamespace";

	public EnumIdentifier getObjectDefinition(WbConnection con, DbObject obj)
	{
		if (obj == null) return null;

		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;

		String enumName = obj.getObjectName();
		String sql = "SELECT * FROM (" + baseSql + ") ei ";
		sql += " WHERE enum_name = '" + con.getMetadata().quoteObjectname(enumName) + "' ";

		String schema = obj.getSchema();
		if (StringUtil.isNonBlank(schema))
		{
			sql += " AND enum_schema = '"  + con.getMetadata().quoteObjectname(schema) + "'";
		}

		EnumIdentifier enumDef = null;

		try
		{
			sp = con.setSavepoint();
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(sql);

			if (rs.next())
			{
				String cat = rs.getString("enum_catalog");
				String eschema = rs.getString("enum_schema");
				String name = rs.getString("enum_name");
				String value = rs.getString("enum_value");
				String comment = rs.getString("remarks");
				enumDef = new EnumIdentifier(cat, eschema, name);
				enumDef.setComment(comment);
				enumDef.addEnumValue(value);
			}

			while (rs.next())
			{
				String value = rs.getString("enum_value");
				enumDef.addEnumValue(value);
			}

			con.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			con.rollback(sp);
			LogMgr.logError("PostgresEnumReader.getEnumValues()", "Could not read enum values", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return enumDef;
	}

	private String getSql(WbConnection con, String schema, String namePattern)
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 50);
		sql.append("SELECT * FROM (");
		sql.append(baseSql);
		sql.append(") ei ");

		boolean whereAdded = false;

		if (StringUtil.isNonBlank(namePattern))
		{
			sql.append(" WHERE enum_name like '");
			sql.append(con.getMetadata().quoteObjectname(namePattern));
			sql.append("%' ");
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(schema))
		{
			sql.append(whereAdded ? " AND " : " WHERE ");
			sql.append(" enum_schema = '");
			sql.append(con.getMetadata().quoteObjectname(schema));
			sql.append("'");
		}
		sql.append(" ORDER BY 2");
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresEnumReader.getSql()", "Using SQL=\n" + sql);
		}
		return sql.toString();
	}

	public Collection<EnumIdentifier> getDefinedEnums(WbConnection con, String schema, String namePattern)
	{
		Map<String, EnumIdentifier> enums = getEnumInfo(con, schema, namePattern);
		return enums.values();
	}

	public Map<String, EnumIdentifier> getEnumInfo(WbConnection con, String schemaName, String namePattern)
	{
		String sql = getSql(con, schemaName, namePattern);

		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		Map<String, EnumIdentifier> enums = new HashMap<String, EnumIdentifier>();

		try
		{
			sp = con.setSavepoint();
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String cat = rs.getString("enum_catalog");
				String schema = rs.getString("enum_schema");
				String name = rs.getString("enum_name");
				String value = rs.getString("enum_value");
				String comment = rs.getString("remarks");
				EnumIdentifier enumDef = enums.get(name);
				if (enumDef == null)
				{
					enumDef = new EnumIdentifier(cat, schema, name);
					enumDef.setComment(comment);
					enums.put(name, enumDef);
				}
				enumDef.addEnumValue(value);
			}
			con.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			con.rollback(sp);
			LogMgr.logError("PostgresEnumReader.getEnumValues()", "Could not read enum values", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return enums;
	}

	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (!handlesType(requestedTypes)) return false;
		if (!DbMetadata.typeIncluded("ENUM", requestedTypes)) return false;

		Collection<EnumIdentifier> enums = getDefinedEnums(con, schema, objects);
		if (CollectionUtil.isEmpty(enums)) return false;
		
		for (EnumIdentifier enumDef : enums)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, enumDef.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, enumDef.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, enumDef.getComment());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, enumDef.getObjectType());
			result.getRow(row).setUserObject(enumDef);
		}
		return true;
	}

	public List<String> supportedTypes()
	{
		return Collections.singletonList("ENUM");
	}

	public boolean handlesType(String type)
	{
		return StringUtil.equalStringIgnoreCase("ENUM", type);
	}

	public boolean handlesType(String[] types)
	{
		if (types == null) return true;
		for (String type : types)
		{
			if (handlesType(type)) return true;
		}
		return false;
	}

	public DataStore getObjectDetails(WbConnection con, DbObject object)
	{
		EnumIdentifier id = getObjectDefinition(con, object);
		if (id == null) return null;

		String[] columns = new String[] { "ENUM", "VALUES", "REMARKS" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 20, 30, 30 };
		DataStore result = new DataStore(columns, types, sizes);
		result.addRow();
		result.setValue(0, 0, id.getObjectName());
		result.setValue(0, 1, StringUtil.listToString(id.getValues(), ','));
		result.setValue(0, 2, id.getComment());
		return result;
	}

	public String getObjectSource(WbConnection con, DbObject object)
	{
		if (object == null) return null;
		EnumIdentifier id = getObjectDefinition(con, object);
		if (id == null) return null;

		StringBuilder result = new StringBuilder(50);
		result.append("CREATE TYPE ");
		result.append(id.getObjectName());
		result.append(" AS ENUM (");
		String values = StringUtil.listToString(id.getValues(), ",", true, '\'');
		result.append(values);
		result.append(");\n");
		if (StringUtil.isNonBlank(id.getComment()))
		{
			result.append("\nCOMMENT ON TYPE ");
			result.append(id.getObjectName());
			result.append(" IS '");
			result.append(SqlUtil.escapeQuotes(id.getComment()));
			result.append("';\n");
		}
		return result.toString();
	}
}
