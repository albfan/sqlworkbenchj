/*
 * OracleConstraintReader.java
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
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.List;
import workbench.db.AbstractConstraintReader;
import workbench.db.TableConstraint;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A class to read column and table constraints from Oracle.
 *
 * @author Thomas Kellerer
 */
public class OracleConstraintReader
	extends AbstractConstraintReader
{
	private final String TABLE_SQL =
		 "SELECT constraint_name, search_condition, status, validated \n" +
		 "FROM all_constraints cons   \n" +
		 "WHERE constraint_type = 'C' \n" +
		 " and owner = ? \n" +
		 " and table_name = ?  \n";

	public OracleConstraintReader(String dbId)
	{
		super(dbId);
	}

	@Override
	public int getIndexForSchemaParameter()
	{
		return 1;
	}

	@Override
	public int getIndexForCatalogParameter()
	{
		return -1;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 2;
	}

	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		return TABLE_SQL;
	}

	@Override
	public List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableIdentifier table)
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		List<TableConstraint> result = CollectionUtil.arrayList();

		ResultSet rs = null;
		PreparedStatement stmt = null;

		try
		{
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getSchema());
			stmt.setString(2, table.getTableName());

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String name = rs.getString(1);
				String constraint = rs.getString(2);
				String status = rs.getString(3);
				String valid = rs.getString(4);

				if (constraint != null)
				{
					// NOT NULL constraints do not need to be taken into account
					if (isDefaultNNConstraint(name, constraint)) continue;

					String expression = "(" + constraint + ")";
					if ("DISABLED".equalsIgnoreCase(status))
					{
						expression += " DISABLE";
					}
					if ("NOT VALIDATED".equalsIgnoreCase(valid))
					{
						expression += " NOVALIDATE";
					}
					TableConstraint c = new TableConstraint(name, expression);
					c.setIsSystemName(isSystemConstraintName(name));
					result.add(c);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleConstraintReader", "Error when reading column constraints", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	/**
	 * Checks if the constraint definition is a "default" Not null definition
	 * as created by Oracle. Those constraints will be included in the column
	 * definition already and do not need to be returned.
	 * but a definition like COL_1 IS NOT NULL OR COL_2 IS NOT NULL must
	 * not be treated as a "default" constraint.
	 *
	 * A "default" NN constraint is assumed if an identifier is
	 * immediately followed by the keyword IS NOT NULL and no further
	 * definitions exist.
	 */
	protected boolean isDefaultNNConstraint(String name, String definition)
	{
		// Not null constrainst can be defined as check constraints as well
		// in that case the constraint will have a name that is not system generated
		// and it needs to be included because those columns are not reported as NOT NULL
		if (!isSystemConstraintName(name)) return false;

		try
		{
			SQLLexer lexer = new SQLLexer(definition);
			SQLToken tok = lexer.getNextToken(false, false);
			if (tok == null) return false;

			if (!tok.isIdentifier()) return false;

			// If no further tokens exist, this cannot be a not null constraint
			tok = lexer.getNextToken(false, false);
			if (tok == null) return false;

			SQLToken tok2 = lexer.getNextToken(false, false);
			if (tok2 == null)
			{
				return "IS NOT NULL".equalsIgnoreCase(tok.getContents());
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}
	}

}
