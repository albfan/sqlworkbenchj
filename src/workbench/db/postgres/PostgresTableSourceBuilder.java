/*
 * PostgresTableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import workbench.db.*;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
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
	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList)
	{
		if (table == null) return null;

		if ("FOREIGN TABLE".equals(table.getType()))
		{
			return getForeignTableOptions(table);
		}

		StringBuilder result = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "select bt.relname as table_name, bns.nspname as table_schema \n" +
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
			LogMgr.logError("PostgresTableSourceBuilder.getAdditionalTableOptioins()", "Error retrieving table options", e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}

		String options = table.getTableConfigOptions();
		if (StringUtil.isNonEmpty(options))
		{
			if (result == null)
			{
				result = new StringBuilder(options.length() + 10);
			}
			else
			{
				result.append('\n');
			}
			result.append("WITH (");
			result.append(options);
			result.append(")");
		}

		String tblSpace = table.getTablespace();
		if (StringUtil.isNonEmpty(tblSpace))
		{
			if (result == null)
			{
				result = new StringBuilder(tblSpace.length() + 10);
			}
			else
			{
				result.append('\n');
			}
			result.append("TABLESPACE ");
			result.append(tblSpace);
		}
		return (result == null ? null : result.toString());
	}

	@Override
	public void readTableConfigOptions(TableIdentifier tbl)
	{
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
				LogMgr.logDebug("PostgresTableSourceBuilder.readTablePersistence()", "Using sql: " + pstmt.toString());
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
							tbl.setTableTypeOption("UNLOGGED");
							break;
						case 't':
							tbl.setTableTypeOption("TEMPORARY");
							break;
					}
				}
				if ("f".equalsIgnoreCase(type))
				{
					tbl.setTableTypeOption("FOREIGN");
				}
				tbl.setTableConfigOptions(options);
				tbl.setTablespace(tableSpace);
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("PostgresTableSourceBuilder.getAdditionalTableOptions()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
	}

	public String getForeignTableOptions(TableIdentifier table)
	{
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
				return result.toString();
			}
			return null;
		}
		catch (SQLException ex)
		{
			dbConnection.rollback(sp);
			sp = null;
			LogMgr.logError("PostgresTableSourceBuilder.getForeignTableOptions()", "Could not retrieve table options", ex);
			return null;
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
		String sql = "select bt.relname as table_name, bns.nspname as table_schema \n" +
             "from pg_class ct \n" +
             "    join pg_namespace cns on ct.relnamespace = cns.oid and cns.nspname = ? \n" +
             "    join pg_inherits i on i.inhparent = ct.oid and ct.relname = ? \n" +
             "    join pg_class bt on i.inhrelid = bt.oid \n" +
             "    join pg_namespace bns on bt.relnamespace = bns.oid";

		Savepoint sp = null;
		try
		{
			// Retrieve child table(s) for this table
			sp = dbConnection.setSavepoint();
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
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
					result.append("\n-- Child tables:");
				}
				String tableName = rs.getString(1);
				String schemaName = rs.getString(2);
				result.append("\n--    ");
				result.append(schemaName);
				result.append('.');
				result.append(tableName);
				count ++;
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
