/*
 * OracleConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.util.CollectionBuilder;
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

	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }

	public List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableIdentifier aTable)
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		List<TableConstraint> result = CollectionBuilder.arrayList();

		ResultSet rs = null;
		PreparedStatement stmt = null;
		try
		{
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, aTable.getSchema());
			stmt.setString(2, aTable.getTableName());

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String name = rs.getString(1);
				String constraint = rs.getString(2);
				if (constraint != null)
				{
					// NOT NULL constraints do not need to be taken into account
          if (isDefaultNNConstraint(constraint)) continue;
					TableConstraint c = new TableConstraint(name, "(" + constraint + ")");
					c.setIsSystemName(name.startsWith("SYS_"));
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
