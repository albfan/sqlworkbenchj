/*
 * PostgresDomainReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * A class to read information about defined DOMAINs in Postgres.
 *
 * @author Thomas Kellerer
 */
public class PostgresDomainReader
	implements ObjectListExtender
{
	final String baseSql = "SELECT current_database() as domain_catalog,  \n" +
             "       n.nspname as domain_schema, \n" +
             "       t.typname as domain_name, \n" +
             "       pg_catalog.format_type(t.typbasetype, t.typtypmod) as data_type, \n" +
             "       t.typnotnull as nullable, \n" +
             "       t.typdefault as default_value, \n" +
             "       c.conname as constraint_name, \n" +
             "       pg_catalog.pg_get_constraintdef(c.oid, true) as constraint_definition, \n" +
						 "       obj_description(t.oid) as remarks \n" +
             "FROM pg_catalog.pg_type t \n" +
             "  LEFT JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace \n" +
             "  LEFT JOIN pg_catalog.pg_constraint c ON t.oid = c.contypid \n" +
             "WHERE t.typtype = 'd' \n" +
             "  AND n.nspname <> 'pg_catalog' \n" +
             "  AND n.nspname <> 'information_schema' \n" +
             "  AND pg_catalog.pg_type_is_visible(t.oid)";

	public Map<String, DomainIdentifier> getDomainInfo(WbConnection connection, String schema)
	{
		List<DomainIdentifier> domains = getDomainList(connection, schema, null);
		Map<String, DomainIdentifier> result = new HashMap<String, DomainIdentifier>(domains.size());
		for (DomainIdentifier d : domains)
		{
			result.put(d.getObjectName(), d);
		}
		return result;
	}

	private String getSql(WbConnection connection, String schema, String name)
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 40);

		sql.append("SELECT * FROM ( ");
		sql.append(baseSql);
		sql.append(") di \n");

		boolean whereAdded = false;
		if (StringUtil.isNonBlank(name))
		{
			sql.append(" WHERE domain_name like '");
			sql.append(connection.getMetadata().quoteObjectname(name));
			sql.append("' ");
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(schema))
		{
			sql.append(whereAdded ? " AND " : " WHERE ");

			sql.append(" domain_schema = '");
			sql.append(connection.getMetadata().quoteObjectname(schema));
			sql.append("'");
		}
		sql.append(" ORDER BY 1, 2 ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresDomainReader.getSql()", "Using SQL=\n" + sql);
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
				String cat = rs.getString("domain_catalog");
				String schema = rs.getString("domain_schema");
				String name = rs.getString("domain_name");
				DomainIdentifier domain = new DomainIdentifier(cat, schema, name);
				domain.setCheckConstraint(rs.getString("constraint_definition"));
				domain.setDataType(rs.getString("data_type"));
				domain.setNullable(rs.getBoolean("nullable"));
				domain.setDefaultValue(rs.getString("default_value"));
				domain.setComment(rs.getString("remarks"));
				result.add(domain);
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			connection.rollback(sp);
			LogMgr.logError("PostgresDomainReader.getDomainList()", "Could not read domains", e);
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
			result.append("\n   DEFAULT ");
			result.append(domain.getDefaultValue());
		}
		if (StringUtil.isNonBlank(domain.getCheckConstraint()) || !domain.isNullable())
		{
			result.append("\n   CONSTRAINT ");
			if (StringUtil.isNonBlank(domain.getConstraintName()))
			{
				result.append(domain.getConstraintName() + " ");
			}
			if (!domain.isNullable()) result.append("NOT NULL ");
			if (StringUtil.isNonBlank(domain.getCheckConstraint()))
			{
				result.append(domain.getCheckConstraint());
			}
		}
		result.append(";\n");
		if (StringUtil.isNonBlank(domain.getComment()))
		{
			result.append("\nCOMMENT ON DOMAIN " + domain.getObjectName() + " IS '");
			result.append(SqlUtil.escapeQuotes(domain.getComment()));
			result.append("';\n");
		}
		return result.toString();
	}

	public void extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("DOMAIN", requestedTypes)) return;

		List<DomainIdentifier> domains = getDomainList(con, schema, objects);
		if (domains.size() == 0) return;
		for (DomainIdentifier domain : domains)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, domain.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, domain.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, domain.getComment());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, domain.getObjectType());
		}
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
		result.resetStatus();
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
