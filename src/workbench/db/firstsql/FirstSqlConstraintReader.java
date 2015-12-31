/*
 * FirstSqlConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.firstsql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.AbstractConstraintReader;
import workbench.db.TableConstraint;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * Constraint reader for <a href="http://www.firstsql.com/">FirstSQL</a>
 *
 * @author Thomas Kellerer
 */
public class FirstSqlConstraintReader
	extends AbstractConstraintReader
{
	private final String SQL =
		"select ch.check_clause, ch.constraint_name \n" +
		"from definition_schema.syschecks ch,  \n" +
		"     definition_schema.sysconstraints cons \n" +
		"where cons.constraint_type = 'check' \n" +
		"  and cons.constraint_name = ch.constraint_name" +
		"  and cons.table_schema = ? \n" +
		"  and cons.table_name = ? ";

	public FirstSqlConstraintReader()
	{
		super("firstsqlj");
	}

	@Override
	public List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableDefinition def)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("FirstSqlConstraintReader.getTableConstraints()", "Using query=\n" + SQL);
		}

		List<TableConstraint> result = CollectionUtil.arrayList();
		TableIdentifier table = def.getTable();
		try
		{
			pstmt = dbConnection.getSqlConnection().prepareStatement(SQL);
			pstmt.setString(1, table.getSchema());
			pstmt.setString(2, table.getTableName());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String constraint = rs.getString(1);
				String name = rs.getString(2);
				result.add(new TableConstraint(name, "(" + constraint + ")"));
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("FirstSqlMetadata.getTableConstraints()", "Could not retrieve table constraints for " + table.getTableExpression(), e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}

	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		return SQL;
	}
}
