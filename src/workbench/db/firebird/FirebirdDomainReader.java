/*
 * FirebirdDomainReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.firebird;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read information about defined DOMAINs in Firebird.
 *
 * @author Thomas Kellerer
 */
public class FirebirdDomainReader
	implements ObjectListExtender
{
	final String baseSql =
		         "SELECT trim(rdb$field_name) AS domain_name, \n" +
             "       rdb$validation_source as constraint_definition, \n" +
             "       case rdb$field_type  \n" +
             "         when 261 then 'BLOB' \n" +
             "         when 14 then 'CHAR' \n" +
             "         when 40 then 'CSTRING' \n" +
             "         when 11 then 'D_FLOAT' \n" +
             "         when 27 then 'DOUBLE' \n" +
             "         when 10 then 'FLOAT' \n" +
             "         when 16 then 'BIGINT' \n" +
             "         when 8 then 'INTEGER' \n" +
             "         when 9 then 'QUAD' \n" +
             "         when 7 then 'SMALLINT' \n" +
             "         when 12 then 'DATE' \n" +
             "         when 35 then 'TIMESTAMP' \n" +
             "         when 3 then 'DATE' \n" +
             "         when 37 then 'VARCHAR' \n" +
             "         else 'UNKNOWN' \n" +
             "       end as data_type, \n" +
             "       rdb$default_source as default_value, \n" +
             "       case rdb$null_flag \n" +
             "         when 1 then 1 \n" +
             "         else 0 \n" +
             "       end as nullable \n" +
             "FROM rdb$fields \n" +
             "WHERE rdb$field_name NOT LIKE 'RDB$%'";

	private String getSql(WbConnection connection, String schema, String name)
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 40);

		sql.append(baseSql);

		if (StringUtil.isNonBlank(name))
		{
			sql.append(" AND trim(rdb$field_name) ");
			if (name.indexOf('%') == -1)
			{
				sql.append('=');
			}
			else
			{
				sql.append("LIKE");
			}
			sql.append(" '");
			sql.append(connection.getMetadata().quoteObjectname(name));
			sql.append("' ");
		}

		sql.append(" ORDER BY 1 ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("FirebirdDomainReader.getSql()", "Using SQL=\n" + sql);
		}

		return sql.toString();
	}

	public List<DomainIdentifier> getDomainList(WbConnection connection, String schemaPattern, String namePattern)
	{
		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		List<DomainIdentifier> result = new ArrayList<DomainIdentifier>();
		try
		{
			sp = connection.setSavepoint();
			stmt = connection.createStatementForQuery();
			String sql = getSql(connection, schemaPattern, namePattern);
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String name = rs.getString("domain_name");
				DomainIdentifier domain = new DomainIdentifier(null, null, name);
				domain.setCheckConstraint(rs.getString("constraint_definition"));
				domain.setDataType(rs.getString("data_type"));
				int nullFlag = rs.getInt("nullable");
				domain.setNullable(nullFlag == 0);
				domain.setDefaultValue(rs.getString("default_value"));
				result.add(domain);
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			connection.rollback(sp);
			LogMgr.logError("FirebirdDomainReader.getDomainList()", "Could not read domains", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public DomainIdentifier getObjectDefinition(WbConnection connection, DbObject object)
	{
		List<DomainIdentifier> domains = getDomainList(connection, object.getSchema(), object.getObjectName());
		if (CollectionUtil.isEmpty(domains)) return null;
		return domains.get(0);
	}

	public String getDomainSource(DomainIdentifier domain)
	{
		if (domain == null) return null;
		StringBuilder result = new StringBuilder(50);
		result.append("CREATE DOMAIN ");
		result.append(domain.getObjectName());
		result.append(" AS ");
		result.append(domain.getDataType());
		if (domain.getDefaultValue() != null)
		{
			result.append("\n   ");
			result.append(domain.getDefaultValue());
		}
		if (StringUtil.isNonBlank(domain.getCheckConstraint()) || !domain.isNullable())
		{
			result.append("\n   ");
			if (!domain.isNullable()) result.append("NOT NULL ");
			if (StringUtil.isNonBlank(domain.getCheckConstraint()))
			{
				result.append(domain.getCheckConstraint());
			}
		}
		result.append(";\n");
		return result.toString();
	}

	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("DOMAIN", requestedTypes)) return false;

		List<DomainIdentifier> domains = getDomainList(con, schema, objects);
		if (domains.isEmpty()) return false;
		
		for (DomainIdentifier domain : domains)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, domain.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, domain.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, domain.getComment());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, domain.getObjectType());
			result.getRow(row).setUserObject(domain);
		}
		return true;
	}

	public boolean handlesType(String type)
	{
		return StringUtil.equalStringIgnoreCase("DOMAIN", type) || "*".equals(type);
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
		if (object == null) return null;
		if (!handlesType(object.getObjectType())) return null;

		DomainIdentifier domain = getObjectDefinition(con, object);
		if (domain == null) return null;

		String[] columns = new String[] { "DOMAIN", "DATA_TYPE", "NULLABLE", "CONSTRAINT", "REMARKS" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN, Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 20, 10, 5, 30, 30 };
		DataStore result = new DataStore(columns, types, sizes);
		result.addRow();
		result.setValue(0, 0, domain.getObjectName());
		result.setValue(0, 1, domain.getDataType());
		result.setValue(0, 2, domain.isNullable());
		result.setValue(0, 3, domain.getCheckConstraint());
		result.setValue(0, 4, domain.getComment());

		return result;
	}

	public List<String> supportedTypes()
	{
		return CollectionUtil.arrayList("DOMAIN");
	}

	public String getObjectSource(WbConnection con, DbObject object)
	{
		return getDomainSource(getObjectDefinition(con, object));
	}

}
