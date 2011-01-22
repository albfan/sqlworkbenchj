/*
 * PostgresTableSourceBuilder.java
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.DomainIdentifier;
import workbench.db.EnumIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTableSourceBuilder
	extends TableSourceBuilder
{

	public PostgresTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		if (table == null) return null;

		StringBuilder result = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "select bt.relname as table_name, bns.nspname as table_schema \n" +
             "from pg_class ct \n" +
             "    join pg_namespace cns on ct.relnamespace = cns.oid and cns.nspname = ? \n" +
             "    join pg_inherits i on i.inhrelid = ct.oid and ct.relname = ? \n" +
             "    join pg_class bt on i.inhparent = bt.oid \n" +
             "    join pg_namespace bns on bt.relnamespace = bns.oid";
		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, table.getSchema());
			pstmt.setString(2, table.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresTableSourceBuilder.getAdditionalTableOptioins()", "Using sql: " + pstmt.toString());
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				result = new StringBuilder(50);
				result.append("INHERITS (");

				String tableName = rs.getString(1);
				result.append(tableName);
				while (rs.next())
				{
					tableName = rs.getString(1);
					result.append(',');
					result.append(tableName);
				}
				result.append(")");
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("PostgresTableSourceBuilder.getAdditionalTableOptioins()", "Error retrieving table options", e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}

		// TODO: Append tablespace information (pg_class.reltablespace), storage options (pg_class.reloptions)

		return (result == null ? null : result.toString());
	}


	@Override
	public String getAdditionalColumnSql(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		String schema = table.getSchemaToUse(this.dbConnection);
		CharSequence enums = getEnumInformation(columns, schema);
		CharSequence domains = getDomainInformation(columns, schema);
		CharSequence sequences = getColumnSequenceInformation(table, columns);

		if (enums == null && domains == null && sequences == null) return null;

		int enumLen = (enums != null ? enums.length() : 0);
		int domainLen = (domains != null ? domains.length() : 0);

		StringBuilder result = new StringBuilder(enumLen + domainLen);
		if (enums != null) result.append(enums);
		if (domains != null) result.append(domains);
		if (sequences != null) result.append(sequences);

		return result.toString();
	}

	@Override
	public CharSequence getAdditionalTableSql(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		PostgresRuleReader ruleReader = new PostgresRuleReader();
		return ruleReader.getTableRuleSource(dbConnection, table);
	}

	private CharSequence getColumnSequenceInformation(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		if (!JdbcUtils.hasMinimumServerVersion(this.dbConnection, "8.4")) return null;
		if (table == null) return null;
		if (CollectionUtil.isEmpty(columns)) return null;
		String tblname = table.getTableExpression(dbConnection);
		ResultSet rs = null;
		Statement stmt = null;
		StringBuilder b = new StringBuilder(100);

		Savepoint sp = null;

		try
		{
			sp = dbConnection.setSavepoint();
			stmt = dbConnection.createStatementForQuery();
			for (ColumnIdentifier col : columns)
			{
				// for serial types the sequence is already shown in the default clause
				if (col.getDbmsType().equals("serial")) continue;
				String colname = StringUtil.trimQuotes(col.getColumnName());
				rs = stmt.executeQuery("select pg_get_serial_sequence('" + tblname + "', '" + colname + "')");
				if (rs.next())
				{
					String seq = rs.getString(1);
					if (StringUtil.isNonBlank(seq))
					{
						String msg = ResourceMgr.getFormattedString("TxtSequenceCol", col.getColumnName(), seq);
						b.append("\n-- " + msg);
					}
				}
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logWarning("PostgresTableSourceBuilder.getColumnSequenceInformation()", "Error reading sequence info", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		if (b.length() == 0) return null;
		return b;
	}

	private CharSequence getEnumInformation(List<ColumnIdentifier> columns, String schema)
	{
		PostgresEnumReader reader = new PostgresEnumReader();
		Map<String, EnumIdentifier> enums = reader.getEnumInfo(dbConnection, schema, null);
		if (enums == null || enums.isEmpty()) return null;
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
		if (domains == null || domains.isEmpty()) return null;
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
