/*
 * NuoDBDomainReader.java
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
package workbench.db.nuodb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

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
 * A class to read information about defined DOMAINs in NuoDB.
 *
 * @author Thomas Kellerer
 */
public class NuoDBDomainReader
	implements ObjectListExtender
{
	final String baseSql =
			"SELECT dom.schema, \n" +
			"       dom.domainname, \n" +
			"       dom.collationsequence, \n" +
			"       dt.name as datatype, \n" +
			"       dt.jdbctype as jdbctype, \n" +
			"       dom.scale, \n" +
			"       dom.precision, \n" +
			"       dom.length, \n" +
			"       case when FLAGS = 1 then false else true end as nullable, \n" +
			"       dom.defaultvalue, \n" +
			"       dom.remarks \n" +
			"from system.domains dom \n" +
			" join system.datatypes dt on dt.id = dom.datatype";

	private String getSql(WbConnection connection, String schema, String name)
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 40);

		sql.append(baseSql);

		boolean whereAdded = false;
		if (StringUtil.isNonBlank(name))
		{
			sql.append(" WHERE dom.domainname like '");
			sql.append(connection.getMetadata().quoteObjectname(name));
			sql.append("%' ");
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(schema))
		{
			sql.append(whereAdded ? " AND " : " WHERE ");

			sql.append(" dom.schema = '");
			sql.append(connection.getMetadata().quoteObjectname(schema));
			sql.append("'");
		}
		sql.append(" ORDER BY dom.schema, dom.domainname ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("NuoDbDomainReader.getSql()", "Query to retrieve domain:\n" + sql);
		}

		return sql.toString();
	}

	public List<DomainIdentifier> getDomainList(WbConnection connection, String schemaPattern, String namePattern)
	{
		Statement stmt = null;
		ResultSet rs = null;
		List<DomainIdentifier> result = new ArrayList<DomainIdentifier>();
		try
		{
			stmt = connection.createStatementForQuery();
			String sql = getSql(connection, schemaPattern, namePattern);
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String schema = rs.getString("schema");
				String name = rs.getString("domainname");
				DomainIdentifier domain = new DomainIdentifier(null, schema, name);
				String typeName = rs.getString("datatype");
				int type = rs.getInt("jdbctype");
				int precision = rs.getInt("precision");
				int scale = 0;
				if (SqlUtil.isCharacterType(type))
				{
					scale = rs.getInt("length");
				}
				else
				{
					scale = rs.getInt("scale");
				}
				String dataType = SqlUtil.getSqlTypeDisplay(typeName, type, scale, precision);
				domain.setDataType(dataType);
				boolean nullable = rs.getBoolean("nullable");
				domain.setNullable(nullable);
				domain.setDefaultValue(rs.getString("defaultvalue"));
				domain.setComment(rs.getString("remarks"));
				result.add(domain);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("NuoDbDomainReader.getDomainList()", "Could not read domains", e);
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

		StringBuilder result = new StringBuilder(50);
		result.append("CREATE DOMAIN ");
		result.append(domain.getObjectName());
		result.append(" AS ");
		result.append(domain.getDataType());
		if (StringUtil.isNonBlank(domain.getDefaultValue()))
		{
			result.append(" DEFAULT ");
			result.append(domain.getDefaultValue());
		}
		if (!domain.isNullable())
		{
			result.append(" NOT NULL");
		}
		result.append(";\n");
		return result.toString();
	}

	@Override
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
	public boolean isDerivedType()
	{
		return false;
	}
}
