/*
 * SqlServerTableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import workbench.db.DependencyNode;
import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.db.sqltemplates.TemplateHandler;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerTableSourceBuilder
	extends TableSourceBuilder
{
	public static final String CLUSTERED_PLACEHOLDER = "%clustered_attribute%";

	public SqlServerTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	public CharSequence getPkSource(TableIdentifier table, PkDefinition pk, boolean forInlineUse)
	{
		CharSequence pkSource = super.getPkSource(table, pk, forInlineUse);
		if (StringUtil.isEmptyString(pkSource))
		{
			return pkSource;
		}
		String sql = pkSource.toString();

		String type = pk.getIndexType();
		String clustered = "CLUSTERED";
		if ("NORMAL".equals(type))
		{
			clustered = "NONCLUSTERED";
		}

		if (StringUtil.isBlank(clustered))
		{
			sql = TemplateHandler.removePlaceholder(sql, CLUSTERED_PLACEHOLDER, true);
		}
		else
		{
			sql = TemplateHandler.replacePlaceholder(sql, CLUSTERED_PLACEHOLDER, clustered);
		}
		return sql;
	}

	@Override
	protected String getAdditionalFkSql(TableIdentifier table, DependencyNode fk, String template)
	{
		String sql =
			"select is_disabled, is_not_trusted \n" +
			"from sys.foreign_keys \n" +
			"where parent_object_id = object_id(?) \n" +
			"  and name = ? \n" +
			"  and (is_disabled = 'true' or is_not_trusted = 'true')";

		if (table == null || fk == null) return null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getFullyQualifiedName(dbConnection));
			stmt.setString(2, fk.getFkName());

			rs = stmt.executeQuery();
			boolean isDisabled = false;
			boolean isNotTrusted = false;

			if (rs.next())
			{
				isDisabled = rs.getBoolean(1);
				isNotTrusted = rs.getBoolean(2);
			}

			if (isNotTrusted)
			{
				template = template.replace("%nocheck%", "WITH NOCHECK ");
			}
			else
			{
				template = template.replace("%nocheck%", "");
			}
			if (isDisabled)
			{
				template += "\nALTER TABLE " + table.getObjectExpression(dbConnection) + " NOCHECK CONSTRAINT " + dbConnection.getMetadata().quoteObjectname(fk.getFkName());
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("SqlServerTableSourceBuilder.getAdditionalFkSql()", "Could not retrieve FK information", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return template;
	}



}
