/*
 * PostgresRule.java
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

import java.sql.SQLException;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresRule
	implements DbObject
{
	private String catalog;
	private String schema;
	private String ruleName;
	private final String objectType = "RULE";
	private String remarks;
	private String source;
	private TableIdentifier ruleTable;
	private String event;

	public PostgresRule(String dcatalog, String dschema, String name)
	{
		catalog = dcatalog;
		schema = dschema;
		ruleName = name;
	}

	public void setEvent(String evt)
	{
		event = evt;
	}

	public String getEvent()
	{
		return event;
	}

	/**
	 * Set the table for which this rule is defined
	 * @param tbl
	 */
	public void setTable(TableIdentifier tbl)
	{
		ruleTable = tbl;
	}

	public TableIdentifier getTable()
	{
		return ruleTable;
	}

	public String getCatalog()
	{
		return catalog;
	}

	public String getSchema()
	{
		return schema;
	}

	public String getObjectType()
	{
		return objectType;
	}

	public String getObjectName()
	{
		return ruleName;
	}

	public String getObjectName(WbConnection conn)
	{
		return ruleName;
	}

	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.buildExpression(null, catalog, schema, ruleName);
	}

	public String getObjectExpression(WbConnection conn)
	{
		return ruleName;
	}

	public String toString()
	{
		return getObjectName();
	}

	public void setSource(String sql)
	{
		this.source = sql;
	}

	public String getSource()
	{
		return source;
	}

	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (this.source == null)
		{
			return con.getMetadata().getObjectSource(this);
		}
		return source;
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		StringBuilder sql = new StringBuilder(50);
		sql.append("DROP RULE ");
		sql.append(con.getMetadata().quoteObjectname(ruleName));
		sql.append(" ON ");
		sql.append(ruleTable.getObjectName(con));
		if (cascade)
		{
			sql.append(" CASCADE");
		}
		return sql.toString();
	}

	public String getObjectNameForDrop(WbConnection con)
	{
		return getFullyQualifiedName(con);
	}

	public String getComment()
	{
		return remarks;
	}

	public void setComment(String cmt)
	{
		remarks = cmt;
	}

}
