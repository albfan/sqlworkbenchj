/*
 * OracleConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import workbench.db.AbstractConstraintReader;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class OracleConstraintReader
	extends AbstractConstraintReader
{
	private static final String TABLE_SQL =
	         "SELECT constraint_name, search_condition \n" +
           "FROM all_constraints cons   \n" +
           "WHERE constraint_type = 'C' \n" +
           " and owner = ? \n" +
           " and table_name = ?  \n";

	public int getIndexForSchemaParameter()
	{
		return 1;
	}
	public int getIndexForCatalogParameter()
	{
		return -1;
	}
	public int getIndexForTableNameParameter()
	{
		return 2;
	}
	public String getPrefixTableConstraintKeyword() { return "CHECK ("; }
	public String getSuffixTableConstraintKeyword() { return ")"; }

	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }

	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		StringBuilder result = new StringBuilder(100);
		ResultSet rs = null;
		PreparedStatement stmt = null;
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		try
		{
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, aTable.getSchema());
			stmt.setString(2, aTable.getTableName());

			rs = stmt.executeQuery();
			int count = 0;
			while (rs.next())
			{
				String name = rs.getString(1);
				String constraint = rs.getString(2);
				if (constraint != null)
				{
					// NOT NULL constraints do not need to be taken into account
          if (isDefaultNNConstraint(constraint)) continue;
					if (count > 0)
					{
						result.append(nl);
						result.append(indent);
						result.append(',');
					}
					if (!name.startsWith("SYS_"))
					{
						result.append("CONSTRAINT ");
						result.append(name);
						result.append(' ');
					}
					result.append("CHECK (");
					result.append(constraint);
					result.append(')');
					count++;
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
		return result.toString();
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
	protected boolean isDefaultNNConstraint(String definition)
	{
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
