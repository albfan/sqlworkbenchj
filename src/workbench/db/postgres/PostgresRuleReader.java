/*
 * PostgresRuleReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
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
             "  left join pg_description d on r.oid = d.objoid\n" +
						 "WHERE r.rulename <> '_RETURN'::name ";
	
	private String getSql(WbConnection connection, String schema, String name)
	{
		StringBuilder sql = new StringBuilder(150);

		sql.append(baseSql);

		if (StringUtil.isNonBlank(name))
		{
			sql.append(" AND r.rulename like '");
			sql.append(connection.getMetadata().quoteObjectname(name));
			sql.append("%' ");
		}

		if (StringUtil.isNonBlank(schema))
		{
			sql.append(" AND ");

			sql.append(" n.nspname = '");
			sql.append(connection.getMetadata().quoteObjectname(schema));
			sql.append("'");
		}
		sql.append(" ORDER BY 1, 2 ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresRuleReader.getSql()", "Using SQL=\n" + sql);
		}

		return sql.toString();
	}

	public List<PostgresRule> getRuleList(WbConnection connection, String schemaPattern, String namePattern)
	{
		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		List<PostgresRule> result = new ArrayList<PostgresRule>();
		try
		{
			sp = connection.setSavepoint();
			stmt = connection.createStatementForQuery();
			String sql = getSql(connection, schemaPattern, namePattern);
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

	public PostgresRule getObjectDefinition(WbConnection connection, DbObject object)
	{
		List<PostgresRule> rules = getRuleList(connection, object.getSchema(), object.getObjectName());
		if (rules == null || rules.size() == 0) return null;
		return rules.get(0);
	}

	public void extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("RULE", requestedTypes)) return;

		List<PostgresRule> rules = getRuleList(con, schema, objects);
		if (rules.size() == 0) return;
		for (PostgresRule rule : rules)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, rule.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, rule.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, rule.getComment());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, rule.getObjectType());
		}
	}

	public boolean handlesType(String type)
	{
		return StringUtil.equalStringIgnoreCase("RULE", type) || "*".equals(type);
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
