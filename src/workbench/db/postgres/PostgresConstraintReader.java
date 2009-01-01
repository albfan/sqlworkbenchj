/*
 * PostgresConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import workbench.db.AbstractConstraintReader;
import workbench.db.TableIdentifier;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;


/**
 * Read table level constraints for Postgres
 * (column constraints are stored on table level...)
 * @author  support@sql-workbench.net
 */
public class PostgresConstraintReader
	extends AbstractConstraintReader
{
	private static final String TABLE_SQL =
					 "select rel.consrc, rel.conname \n" +
           "from pg_class t, pg_constraint rel \n" +
           "where t.relname = ? \n" +
           "and   t.oid = rel.conrelid " +
		       "and   rel.contype = 'c'";

	public String getPrefixTableConstraintKeyword() { return "check"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }

	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
		throws SQLException
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		StringBuilder result = new StringBuilder(100);

		String nl = Settings.getInstance().getInternalEditorLineEnding();

		ResultSet rs = null;
		PreparedStatement stmt = null;
		Savepoint sp = null;
		try
		{
			sp = dbConnection.setSavepoint();
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, aTable.getTableName());
			rs = stmt.executeQuery();
			int count = 0;
			while (rs.next())
			{
				String constraint = rs.getString(1);
				String name = rs.getString(2);
				if (constraint != null)
				{
					if (count > 0)
					{
						result.append(nl);
						result.append(indent);
						result.append(',');
					}
					if (name != null)
					{
						result.append("CONSTRAINT ");
						result.append(name);
						result.append(' ');
					}
					result.append("CHECK ");
					result.append(constraint);
					count++;
				}
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			try { dbConnection.rollback(sp); } catch (Throwable th) {}
			LogMgr.logError("AbstractConstraintReader", "Error when reading column constraints " + ExceptionUtil.getDisplay(e), null);
			throw e;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result.toString();
	}

}
