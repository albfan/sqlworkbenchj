/*
 * SqlServerRule.java
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
package workbench.db.mssql;

import java.sql.SQLException;

import workbench.db.DbObject;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerRule
	implements DbObject
{
	private String catalog;
	private String schema;
	private String ruleName;
	private final String objectType = "RULE";
	private String remarks;
	private String source;

	public SqlServerRule(String ruleCatalog, String ruleSchema, String name)
	{
		catalog = ruleCatalog;
		schema = ruleSchema;
		ruleName = name;
	}

	@Override
	public String getCatalog()
	{
		return catalog;
	}

	@Override
	public String getSchema()
	{
		return schema;
	}

	@Override
	public String getObjectType()
	{
		return objectType;
	}

	@Override
	public String getObjectName()
	{
		return ruleName;
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return ruleName;
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.fullyQualifiedName(conn, this);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return ruleName;
	}

	@Override
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

	@Override
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
		return sql.toString();
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return getFullyQualifiedName(con);
	}

	@Override
	public String getComment()
	{
		return remarks;
	}

	@Override
	public void setComment(String cmt)
	{
		remarks = cmt;
	}

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
