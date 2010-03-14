/*
 * SqlServerTypeReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerTypeReader
	implements ObjectListExtender
{
	final String baseSql = "select db_name() as type_catalog, \n" +
									 "       s.name as type_schema, \n" +
									 "       t.name as type_name,  \n" +
									 "       type_name(system_type_id) as data_type, \n" +
									 "       t.is_nullable,  \n" +
									 "       t.max_length,  \n" +
									 "       t.scale,  \n" +
									 "       t.precision,  \n" +
									 "       t.collation_name \n" +
									 "from sys.types t join sys.schemas s on t.schema_id = s.schema_id \n" +
									 "where t.is_user_defined = 1";

		// the data types for which the max_length information are valid
	private Set<String> maxLengthTypes = CollectionUtil.hashSet("varchar", "nvarchar", "char", "text", "ntext", "varbinary");

		// the data types for which the scale and precision columns are valid
	private Set<String> numericTypes = CollectionUtil.hashSet("decimal", "numeric");

	public static boolean versionSupportsTypes(WbConnection con)
	{
		return JdbcUtils.hasMinimumServerVersion(con, "9.0");
	}

	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("TYPE", requestedTypes)) return false;

		List<DomainIdentifier> types = getTypeList(con, schema, objects);
		if (types.isEmpty()) return false;

		for (DomainIdentifier type : types)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, type.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, type.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, type.getComment());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, type.getObjectType());
		}
		return true;
	}

	public List<String> supportedTypes()
	{
		return CollectionUtil.arrayList("TYPE");
	}

	public boolean handlesType(String type)
	{
		return "TYPE".equalsIgnoreCase(type);
	}

	public boolean handlesType(String[] types)
	{
		if (types == null) return true;
		for (String typ : types)
		{
			if (handlesType(typ)) return true;
		}
		return false;
	}

	public List<DomainIdentifier> getTypeList(WbConnection connection, String owner, String typeName)
	{
		Statement stmt = null;
		ResultSet rs = null;
		List<DomainIdentifier> result = new ArrayList<DomainIdentifier>();
		try
		{
			stmt = connection.createStatementForQuery();
			String sql = getSql(owner, typeName);
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				DomainIdentifier domain = createTypeFromResultSet(rs);
				result.add(domain);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("SqlServerTypeReader.getTypeList()", "Could not read domains", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private DomainIdentifier createTypeFromResultSet(ResultSet rs)
		throws SQLException
	{
		String cat = rs.getString("type_catalog");
		String schema = rs.getString("type_schema");
		String name = rs.getString("type_name");
		DomainIdentifier domain = new DomainIdentifier(cat, schema, name);
		domain.setObjectType("TYPE");

		String datatype = rs.getString("data_type");
		int maxLength = rs.getInt("max_length");
		int scale = rs.getInt("scale");
		int precision = rs.getInt("precision");
		boolean nullable = rs.getBoolean("is_nullable");

		StringBuilder displayType = new StringBuilder(datatype.length() + 5);
		displayType.append(datatype);
		if (maxLengthTypes.contains(datatype))
		{
			if (maxLength == -1)
			{
				displayType.append("(max)");
			}
			else
			{
				displayType.append("(" + maxLength + ")");
			}
		}
		else if (numericTypes.contains(datatype))
		{
			displayType.append("(" + precision + "," + scale + ")");
		}
		domain.setDataType(displayType.toString());
		domain.setNullable(nullable);
		return domain;
	}

	public DataStore getObjectDetails(WbConnection con, DbObject object)
	{
		if (object == null) return null;
		if (!handlesType(object.getObjectType())) return null;

		DomainIdentifier type = getObjectDefinition(con, object);
		if (type == null) return null;

		String[] columns = new String[] { "TYPE", "DATA_TYPE", "NULLABLE" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN };
		int[] sizes = new int[] { 20, 10, 5};
		DataStore result = new DataStore(columns, types, sizes);
		result.addRow();
		result.setValue(0, 0, type.getObjectName());
		result.setValue(0, 1, type.getDataType());
		result.setValue(0, 2, type.isNullable());

		return result;
	}

	private String getSql(String owner, String typeName)
	{
		String sql = baseSql;
		if (StringUtil.isNonBlank(typeName))
		{
			sql += " AND t.name like '" + typeName + "%' ";
		}
		if (StringUtil.isNonBlank(owner))
		{
			sql += " AND s.name = '" + owner + "' ";
		}

		sql += " ORDER BY 1, 2";
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("SqlServerTypeReader.getSql()", "Using SQL=\n" + sql);
		}
		return sql;
	}

	public DomainIdentifier getObjectDefinition(WbConnection con, DbObject name)
	{
		Statement stmt = null;
		ResultSet rs = null;
		DomainIdentifier result = null;
		try
		{
			stmt = con.createStatementForQuery();
			String typename = con.getMetadata().adjustObjectnameCase(name.getObjectName());
			String schema = con.getMetadata().adjustSchemaNameCase(name.getSchema());

			String sql = getSql(schema, typename);

			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				result = createTypeFromResultSet(rs);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("SqlServerTypeReader.getTypeList()", "Could not read domains", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public String getObjectSource(WbConnection con, DbObject object)
	{
		DomainIdentifier type = getObjectDefinition(con, object);
		StringBuilder result = new StringBuilder(50);
		result.append("CREATE TYPE ");
		result.append(type.getObjectExpression(con));
		result.append("\n  FROM ");
		result.append(type.getDataType());
		if (!type.isNullable())
		{
			result.append(" NOT NULL");
		}
		result.append(";\n");
		return result.toString();
	}
}
