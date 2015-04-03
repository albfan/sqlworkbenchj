/*
 * PostgresDomainReader.java
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
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

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
	final String baseSql = "SELECT null::text as domain_catalog,  \n" +
             "       n.nspname as domain_schema, \n" +
             "       t.typname as domain_name, \n" +
             "       pg_catalog.format_type(t.typbasetype, t.typtypmod) as data_type, \n" +
             "       not t.typnotnull as nullable, \n" +
             "       t.typdefault as default_value, \n" +
             "       c.conname as constraint_name, \n" +
             "       pg_catalog.pg_get_constraintdef(c.oid, true) as constraint_definition, \n" +
						 "       obj_description(t.oid) as remarks \n" +
             "FROM pg_catalog.pg_type t \n" +
             "  LEFT JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace \n" +
             "  LEFT JOIN pg_catalog.pg_constraint c ON t.oid = c.contypid \n" +
             "WHERE t.typtype = 'd' \n" +
             "  AND n.nspname <> 'pg_catalog' \n" +
             "  AND n.nspname <> 'information_schema' \n";


	public Map<String, DomainIdentifier> getDomainInfo(WbConnection connection, String schema)
	{
		List<DomainIdentifier> domains = getDomainList(connection, schema, null);
		Map<String, DomainIdentifier> result = new HashMap<>(domains.size());
		for (DomainIdentifier d : domains)
		{
			result.put(d.getObjectName(), d);
		}
		return result;
	}

	private String getSql(WbConnection con, String schema, String name)
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 40);

		sql.append("SELECT * FROM ( ");
		sql.append(baseSql);
		sql.append(") di \n");

		boolean whereAdded = false;
		if (StringUtil.isNonBlank(name))
		{
			sql.append("\n WHERE ");
			SqlUtil.appendExpression(sql, "domain_name", name, con);
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(schema))
		{
			sql.append(whereAdded ? " AND " : " WHERE ");
			SqlUtil.appendExpression(sql, "domain_schema", schema, con);
		}

		sql.append("\n ORDER BY 1, 2 ");

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
		List<DomainIdentifier> result = new ArrayList<>();
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

	@Override
	public DomainIdentifier getObjectDefinition(WbConnection connection, DbObject object)
	{
		List<DomainIdentifier> domains = getDomainList(connection, object.getSchema(), object.getObjectName());
		if (CollectionUtil.isEmpty(domains)) return null;
		return domains.get(0);
	}

	public String getDomainSource(DomainIdentifier domain)
	{
		if (domain == null) return null;
		String name = SqlUtil.buildExpression(null, domain);
		StringBuilder result = new StringBuilder(50);
		result.append("CREATE DOMAIN ");
		result.append(name);
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
				result.append(domain.getConstraintName()).append(" ");
			}
			if (!domain.isNullable()) result.append("NOT NULL");
			if (StringUtil.isNonBlank(domain.getCheckConstraint()))
			{
				if (!domain.isNullable()) result.append(' ');
				result.append(domain.getCheckConstraint());
			}
		}
		result.append(";\n");
		if (StringUtil.isNonBlank(domain.getComment()))
		{
			result.append("\nCOMMENT ON DOMAIN ").append(name).append(" IS '");
			result.append(SqlUtil.escapeQuotes(domain.getComment()));
			result.append("';\n");
		}
		return result.toString();
	}

	@Override
	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (!handlesType(requestedTypes)) return false;
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

	@Override
	public boolean isDerivedType()
	{
		return false;
	}

	@Override
	public boolean handlesType(String type)
	{
		return StringUtil.equalStringIgnoreCase("DOMAIN", type);
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

	@Override
	public List<String> supportedTypes()
	{
		return Collections.singletonList("DOMAIN");
	}

	@Override
	public String getObjectSource(WbConnection con, DbObject object)
	{
		return getDomainSource(getObjectDefinition(con, object));
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
