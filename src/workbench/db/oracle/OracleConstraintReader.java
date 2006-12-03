/*
 * OracleConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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

	public OracleConstraintReader()
	{
	}

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
          if (constraint.trim().endsWith("NOT NULL")) continue;
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
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		return result.toString();
	}

	public void done()
	{
	}
}
