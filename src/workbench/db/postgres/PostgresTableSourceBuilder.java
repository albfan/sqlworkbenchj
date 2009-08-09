/*
 * PostgresTableSourceBuilder.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.DomainIdentifier;
import workbench.db.EnumIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class PostgresTableSourceBuilder
	extends TableSourceBuilder
{

	public PostgresTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	public String getAdditionalColumnInformation(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		String schema = table.getSchemaToUse(this.dbConnection);
		CharSequence enums = getEnumInformation(columns, schema);
		CharSequence domains = getDomainInformation(columns, schema);
		if (enums == null && domains == null) return null;

		int enumLen = (enums != null ? enums.length() : 0);
		int domainLen = (domains != null ? domains.length() : 0);

		StringBuilder result = new StringBuilder(enumLen + domainLen);
		if (enums != null) result.append(enums);
		if (domains != null) result.append(domains);
		return result.toString();
	}

	private CharSequence getEnumInformation(List<ColumnIdentifier> columns, String schema)
	{
		PostgresEnumReader reader = new PostgresEnumReader();
		Map<String, EnumIdentifier> enums = reader.getEnumInfo(dbConnection, schema, null);
		if (enums == null || enums.size() == 0) return null;
		StringBuilder result = new StringBuilder(50);
		
		for (ColumnIdentifier col : columns)
		{
			String dbType = col.getDbmsType();
			EnumIdentifier enumDef = enums.get(dbType);
			if (enumDef != null)
			{
				result.append("\n-- enum '" + dbType + "': ");
				result.append(StringUtil.listToString(enumDef.getValues(), ",", true, '\''));
			}
		}
		
		return result;
	}

	public CharSequence getDomainInformation(List<ColumnIdentifier> columns, String schema)
	{
		PostgresDomainReader reader = new PostgresDomainReader();
		Map<String, DomainIdentifier> domains = reader.getDomainInfo(dbConnection, schema);
		if (domains == null || domains.size() == 0) return null;
		StringBuilder result = new StringBuilder(50);

		for (ColumnIdentifier col : columns)
		{
			String dbType = col.getDbmsType();
			DomainIdentifier domain = domains.get(dbType);
			if (domain != null)
			{
				result.append("\n-- domain '" + dbType + "': ");
				result.append(domain.getSummary());
			}
		}
		
		return result;
	}
}
