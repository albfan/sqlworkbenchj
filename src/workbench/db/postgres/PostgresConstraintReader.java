/*
 * PostgresConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.db.*;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.Settings;


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
	
	public PostgresConstraintReader()
	{
	}
	
	public String getPrefixTableConstraintKeyword() { return "check"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }

	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
		throws SQLException
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		StringBuffer result = new StringBuffer(100);

		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
		ResultSet rs = null;
		PreparedStatement stmt = null;
		try
		{
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
		}
		catch (SQLException e)
		{
			LogMgr.logError("AbstractConstraintReader", "Error when reading column constraints " + ExceptionUtil.getDisplay(e), null);
			throw e;
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		return result.toString();
	}
	
}
