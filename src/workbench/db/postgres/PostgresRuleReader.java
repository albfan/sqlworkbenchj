/*
 * PostgresRuleReader.java
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
import java.util.ArrayList;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbSettings;
import workbench.db.ObjectListExtender;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read information about rules from Postgres.
 *
 * @author Thomas Kellerer
 */
public class PostgresRuleReader
	implements ObjectListExtender
{

	private final String baseSql = "select current_database() as rule_catalog,\n " +
		         "       n.nspname as rule_schema, \n" +
             "       r.rulename as rule_name, \n" +
             "       c.relname as rule_table, \n" +
             "       case r.ev_type  \n" +
             "         when '1' then 'SELECT' \n" +
             "         when '2' then 'UPDATE' \n" +
             "         when '3' then 'INSERT' \n" +
             "         when '4' then 'DELETE' \n" +
             "         else 'UNKNOWN' \n" +
             "       end as rule_event, \n" +
             "       pg_get_ruledef(r.oid) as definition, \n" +
             "       d.description as remarks \n" +
             "from pg_rewrite r  \n" +
             "  join pg_class c on r.ev_class = c.oid \n" +
             "  left join pg_namespace n on n.oid = c.relnamespace \n" +
             "  left join pg_description d on r.oid = d.objoid ";

	private String getSql(String ruleSchemaPattern, String ruleNamePattern, String ruleTable, boolean excludeSelectView)
	{
		StringBuilder sql = new StringBuilder(150);

		sql.append(baseSql);
		boolean whereAdded = false;
		if (StringUtil.isNonBlank(ruleNamePattern))
		{
			sql.append("\n WHERE r.rulename ");
			if (ruleNamePattern.indexOf('%') > -1)
			{
				sql.append(" LIKE ");
			}
			else
			{
				sql.append(" = ");
			}
			sql.append('\'');
			whereAdded = true;
			sql.append(ruleNamePattern);
			sql.append("' ");
		}

		if (StringUtil.isNonBlank(ruleSchemaPattern))
		{
			if (!whereAdded) 
			{
				sql.append("\n WHERE ");
				whereAdded = true;
			}
			else
			{
				sql.append("\n AND ");
			}
			sql.append(" n.nspname = '");
			sql.append(ruleSchemaPattern);
			sql.append("'");
		}

		if (StringUtil.isNonBlank(ruleTable))
		{
			if (!whereAdded)
			{
				sql.append("\n WHERE ");
				whereAdded = true;
			}
			else
			{
				sql.append("\n AND ");
			}
			sql.append(" c.relname = '");
			sql.append(ruleTable);
			sql.append("'");
		}

		if (excludeSelectView)
		{
			if (!whereAdded)
			{
				sql.append("\n WHERE ");
				whereAdded = true;
			}
			else
			{
				sql.append("\n AND");
			}
			sql.append(" not (c.relkind = 'v' and r.ev_type = '1')");
		}
		sql.append("\n ORDER BY 1, 2 ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresRuleReader.getSql()", "Using SQL=\n" + sql);
		}

		return sql.toString();
	}

	public List<PostgresRule> getRuleList(WbConnection connection, String schemaPattern, String namePattern, String ruleTable)
	{
		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		List<PostgresRule> result = new ArrayList<PostgresRule>();
		try
		{
			sp = connection.setSavepoint();
			stmt = connection.createStatementForQuery();
			String sql = getSql(schemaPattern, namePattern, ruleTable, DbSettings.getExcludePostgresDefaultRules());
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String cat = rs.getString("rule_catalog");
				String schema = rs.getString("rule_schema");
				String name = rs.getString("rule_name");
				PostgresRule rule = new PostgresRule(cat, schema, name);
				rule.setSource(rs.getString("definition"));
				rule.setComment(rs.getString("remarks"));
				rule.setEvent(rs.getString("rule_event"));
				String table = rs.getString("rule_table");
				rule.setTable(new TableIdentifier(cat, schema, table));
				result.add(rule);
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			connection.rollback(sp);
			LogMgr.logError("PostgresRuleReader.getDomainList()", "Could not read domains", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public List<PostgresRule> getTableRules(WbConnection connection, TableIdentifier table)
	{
		return getRuleList(connection, table.getSchema(), null, table.getTableName());
	}

	public CharSequence getTableRuleSource(WbConnection connection, TableIdentifier table)
	{
		List<PostgresRule> rules = getTableRules(connection, table);

		if (CollectionUtil.isNonEmpty(rules))
		{
			StringBuilder result = new StringBuilder(rules.size() * 100);
			for (PostgresRule rule : rules)
			{
				try
				{
					CharSequence src = rule.getSource(connection);
					result.append(src);
					result.append(Settings.getInstance().getInternalEditorLineEnding());
				}
				catch (SQLException e)
				{
					LogMgr.logError("PostgresTableSourceBuilder.getAdditionalTableSql()", "Error retrieving rule source!", e);
				}
			}
			return result.toString();
		}
		return null;
	}
	
	public PostgresRule getObjectDefinition(WbConnection connection, DbObject object)
	{
		List<PostgresRule> rules = getRuleList(connection, object.getSchema(), object.getObjectName(), null);
		if (rules == null || rules.isEmpty()) return null;
		return rules.get(0);
	}

	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objectNamePattern, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("RULE", requestedTypes)) return false;

		List<PostgresRule> rules = getRuleList(con, schema, objectNamePattern, null);
		if (rules.isEmpty()) return false;
		for (PostgresRule rule : rules)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, rule.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, rule.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, rule.getComment());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, rule.getObjectType());
			result.getRow(row).setUserObject(rule);
		}
		return true;
	}

	public boolean handlesType(String type)
	{
		return StringUtil.equalStringIgnoreCase("RULE", type);
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

		PostgresRule rule = getObjectDefinition(con, object);
		if (rule == null) return null;

		String[] columns = new String[] { "RULE", "RULE_TABLE", "EVENT", "REMARKS" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 20, 20, 10, 30 };
		DataStore result = new DataStore(columns, types, sizes);
		result.addRow();
		result.setValue(0, 0, rule.getObjectName());
		result.setValue(0, 1, rule.getTable().getObjectName());
		result.setValue(0, 2, rule.getEvent());
		result.setValue(0, 3, rule.getComment());

		return result;
	}

	public List<String> supportedTypes()
	{
		return CollectionUtil.arrayList("RULE");
	}

	public String getObjectSource(WbConnection con, DbObject object)
	{
		PostgresRule rule = getObjectDefinition(con, object);
		if (rule == null) return null;
		return rule.getSource();
	}

}
