/*
 * PostgresTableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.*;

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
	public void readTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList)
	{
		TableSourceOptions option = table.getSourceOptions();

		PostgresRuleReader ruleReader = new PostgresRuleReader();
		CharSequence rule = ruleReader.getTableRuleSource(dbConnection, table);
		if (rule != null)
		{
			option.setAdditionalSql(rule.toString());
		}

		if ("FOREIGN TABLE".equals(table.getType()))
		{
			readForeignTableOptions(table);
		}
		else
		{
			readTableOptions(table);
		}
	}

	private void readTableOptions(TableIdentifier tbl)
	{
		TableSourceOptions option = tbl.getSourceOptions();
		StringBuilder inherit = readInherits(tbl);

		StringBuilder tableSql = new StringBuilder();
		if (option.getAdditionalSql() != null)
		{
			tableSql.append(option.getAdditionalSql());
		}

		if (inherit != null)
		{
			if (tableSql.length() > 0) tableSql.append('\n');
			tableSql.append(inherit);
		}

		String optionsCol = null;
		if (JdbcUtils.hasMinimumServerVersion(dbConnection, "8.1"))
		{
			optionsCol = "array_to_string(ct.reloptions, ', ')";
		}
		else
		{
			optionsCol = "null as reloptions";
		}

		String tempCol = null;
		if (JdbcUtils.hasMinimumServerVersion(dbConnection, "9.1"))
		{
			tempCol = "ct.relpersistence";
		}
		else if (JdbcUtils.hasMinimumServerVersion(dbConnection, "8.4"))
		{
			tempCol = "case when ct.relistemp then 't' else null::char end as relpersitence";
		}
		else
		{
			tempCol = "null::char as relpersistence";
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select " + tempCol + ", ct.relkind, " + optionsCol + ", spc.spcname \n" +
			"from pg_class ct \n" +
			"    join pg_namespace cns on ct.relnamespace = cns.oid \n " +
			"    left join pg_tablespace spc on spc.oid = ct.reltablespace \n" +
			" where cns.nspname = ? \n" +
			"   and ct.relname = ?";

		Savepoint sp = null;
		try
		{
			sp = dbConnection.setSavepoint();
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getSchema());
			pstmt.setString(2, tbl.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresTableSourceBuilder.readTableConfigOptions()", "Using sql: " + pstmt.toString());
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String persistence = rs.getString(1);
				String type = rs.getString(2);
				String options = rs.getString(3);
				String tableSpace = rs.getString(4);
				if (StringUtil.isNonEmpty(persistence))
				{
					switch (persistence.charAt(0))
					{
						case 'u':
							option.setTypeModifier("UNLOGGED");
							break;
						case 't':
							option.setTypeModifier("TEMPORARY");
							break;
					}
				}
				if ("f".equalsIgnoreCase(type))
				{
					option.setTypeModifier("FOREIGN");
				}
				tbl.setTablespace(tableSpace);
				if (StringUtil.isNonEmpty(options))
				{
					option.setConfigOption(options);
					tableSql.append("\nWITH (");
					tableSql.append(options);
					tableSql.append(")");
				}
				if (StringUtil.isNonBlank(tableSpace))
				{
					tableSql.append("\nTABLESPACE ");
					tableSql.append(tableSpace);
				}
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("PostgresTableSourceBuilder.readTableConfigOptions()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		option.setTableOption(tableSql.toString());
	}

	private StringBuilder readInherits(TableIdentifier table)
	{
		if (table == null) return null;

		StringBuilder result = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		String sql =
			"select bt.relname as table_name, bns.nspname as table_schema \n" +
			"from pg_class ct \n" +
			"    join pg_namespace cns on ct.relnamespace = cns.oid and cns.nspname = ? \n" +
			"    join pg_inherits i on i.inhrelid = ct.oid and ct.relname = ? \n" +
			"    join pg_class bt on i.inhparent = bt.oid \n" +
			"    join pg_namespace bns on bt.relnamespace = bns.oid";

		Savepoint sp = null;
		try
		{
			// Retrieve parent table(s) for this table
			sp = dbConnection.setSavepoint();
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, table.getSchema());
			pstmt.setString(2, table.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresTableSourceBuilder.getAdditionalTableOptions()", "Using sql: " + pstmt.toString());
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
				result.append(')');
			}

			// retrieve child tables for this table

			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("PostgresTableSourceBuilder.getAdditionalTableOptions()", "Error retrieving table options", e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}

	public void readForeignTableOptions(TableIdentifier table)
	{
		TableSourceOptions option = table.getSourceOptions();

		String sql =
			"select ft.ftoptions, fs.srvname \n" +
			"from pg_foreign_table ft \n" +
			"  join pg_class tbl on tbl.oid = ft.ftrelid  \n" +
			"  join pg_namespace ns on tbl.relnamespace = ns.oid  \n" +
			"  join pg_foreign_server fs on ft.ftserver = fs.oid \n " +
			" WHERE tbl.relname = ? \n" +
			"   and ns.nspname = ? ";

		PreparedStatement stmt = null;
		ResultSet rs = null;
		StringBuilder result = new StringBuilder(100);
		Savepoint sp = null;
		try
		{
			sp = dbConnection.setSavepoint();
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getTableName());
			stmt.setString(2, table.getSchema());
			rs = stmt.executeQuery();
			if (rs.next())
			{
				Array array = rs.getArray(1);
				String[] options = array == null ? null : (String[])array.getArray();
				String serverName = rs.getString(2);
				result.append("SERVER ");
				result.append(serverName);
				if (options != null && options.length > 0)
				{
					result.append("\nOPTIONS (");
					for (int i = 0; i < options.length; i++)
					{
						if (i > 0)
						{
							result.append(", ");
						}
						result.append(options[i]);
					}
					result.append(')');
				}
				option.setTableOption(result.toString());
			}
		}
		catch (SQLException ex)
		{
			dbConnection.rollback(sp);
			sp = null;
			LogMgr.logError("PostgresTableSourceBuilder.getForeignTableOptions()", "Could not retrieve table options", ex);
		}
		finally
		{
			dbConnection.releaseSavepoint(sp);
			SqlUtil.closeAll(rs, stmt);
		}
	}

	/**
	 * Return domain information for columns in the specified table.
	 *
	 * @param table
	 * @param columns
	 * @param indexList
	 * @return
	 */
	@Override
	public String getAdditionalTableInfo(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList)
	{
		String schema = table.getSchemaToUse(this.dbConnection);
		CharSequence enums = getEnumInformation(columns, schema);
		CharSequence domains = getDomainInformation(columns, schema);
		CharSequence sequences = getColumnSequenceInformation(table, columns);
		CharSequence children = getChildTables(table);

		if (enums == null && domains == null && sequences == null && children == null) return null;

		int enumLen = (enums != null ? enums.length() : 0);
		int domainLen = (domains != null ? domains.length() : 0);
		int childLen = (children != null ? children.length() : 0);

		StringBuilder result = new StringBuilder(enumLen + domainLen + childLen);
		if (enums != null) result.append(enums);
		if (domains != null) result.append(domains);
		if (sequences != null) result.append(sequences);
		if (children != null) result.append(children);

		return result.toString();
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
				String defaultValue = col.getDefaultValue();
				// if the default value is shown as nextval, the sequence name is already visible
				if (defaultValue != null && defaultValue.toLowerCase().startsWith("nextval")) continue;
				String colname = StringUtil.trimQuotes(col.getColumnName());
				rs = stmt.executeQuery("select pg_get_serial_sequence('" + tblname + "', '" + colname + "')");
				if (rs.next())
				{
					String seq = rs.getString(1);
					if (StringUtil.isNonBlank(seq))
					{
						String msg = ResourceMgr.getFormattedString("TxtSequenceCol", col.getColumnName(), seq);
						b.append("\n-- ");
						b.append(msg);
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
				result.append("\n-- enum '");
				result.append(dbType);
				result.append("': ");
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
				result.append("\n-- domain '");
				result.append(dbType);
				result.append("': ");
				result.append(domain.getSummary());
			}
		}

		return result;
	}

	protected CharSequence getChildTables(TableIdentifier table)
	{
		if (table == null) return null;

		StringBuilder result = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		final String sql83 =
			"select bt.relname as table_name, bns.nspname as table_schema, 0 as level \n" +
			"from pg_class ct \n" +
			"    join pg_namespace cns on ct.relnamespace = cns.oid and cns.nspname = ? \n" +
			"    join pg_inherits i on i.inhparent = ct.oid and ct.relname = ? \n" +
			"    join pg_class bt on i.inhrelid = bt.oid \n" +
			"    join pg_namespace bns on bt.relnamespace = bns.oid";

		// Recursive version for 8.4+ based Craig Rigner's statement from here: http://stackoverflow.com/a/12139506/330315
		final String sql84 =
			"with recursive inh as ( \n" +
			" \n" +
			"				select i.inhrelid, 1 as level, array[inhrelid] as path \n" +
			"				from pg_catalog.pg_inherits i  \n" +
			"				  join pg_catalog.pg_class cl on i.inhparent = cl.oid \n" +
			"				  join pg_catalog.pg_namespace nsp on cl.relnamespace = nsp.oid \n" +
			"				where nsp.nspname = ? \n" +
			"				and cl.relname = ? \n" +
			"				 \n" +
			"				union all \n" +
			"				 \n" +
			"				select i.inhrelid, inh.level + 1, inh.path||i.inhrelid \n" +
			"				from inh  \n" +
			"				  inner join pg_catalog.pg_inherits i on (inh.inhrelid = i.inhparent) \n" +
			") \n" +
			"select pg_class.relname as table_name, pg_namespace.nspname as table_schema, inh.level \n" +
			"		from inh \n" +
			"			inner join pg_catalog.pg_class on (inh.inhrelid = pg_class.oid) \n" +
			"			inner join pg_catalog.pg_namespace on (pg_class.relnamespace = pg_namespace.oid) \n" +
			"order by path";

		final boolean isRecursive;
		if (JdbcUtils.hasMinimumServerVersion(dbConnection, "8.4"))
		{
			isRecursive = true;
		}
		else
		{
			isRecursive = false;
		}

		Savepoint sp = null;
		try
		{
			// Retrieve direct child table(s) for this table
			// this does not handle multiple inheritance
			sp = dbConnection.setSavepoint();
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(isRecursive ? sql84 : sql83);
			pstmt.setString(1, table.getSchema());
			pstmt.setString(2, table.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresTableSourceBuilder.getChildTables()", "Using sql: " + pstmt.toString());
			}
			rs = pstmt.executeQuery();
			int count = 0;
			while (rs.next())
			{
				if (count == 0)
				{
					result = new StringBuilder(50);
					if (isRecursive)
					{
						result.append("\n/* Inheritance tree:\n\n");
						result.append(table.getSchema());
						result.append('.');
						result.append(table.getTableName());
					}
					else
					{
						result.append("\n-- Child tables:");
					}
				}
				String tableName = rs.getString(1);
				String schemaName = rs.getString(2);
				int level = rs.getInt(3);
				if (isRecursive)
				{
					result.append('\n');
					result.append(StringUtil.padRight(" ", level * 2));
				}
				else
				{
					result.append("\n--  ");
				}
				result.append(schemaName);
				result.append('.');
				result.append(tableName);
				count ++;
			}
			if (isRecursive && result != null)
			{
				result.append("\n*/");
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("PostgresTableSourceBuilder.getChildTables()", "Error retrieving table options", e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}

}
