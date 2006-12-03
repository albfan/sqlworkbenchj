/*
 * FirstSqlMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.firstsql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import workbench.db.ConstraintReader;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * @author  support@sql-workbench.net
 */
public class FirstSqlMetadata
	implements ConstraintReader
{
	private static final String SQL = "select ch.check_clause, ch.constraint_name \n" + 
             "from definition_schema.syschecks ch,  \n" + 
             "     definition_schema.sysconstraints cons \n" + 
             "where cons.constraint_type = 'check' \n" + 
             "  and cons.constraint_name = ch.constraint_name" + 
             "  and cons.table_schema = ? \n" + 
             "  and cons.table_name = ? ";
	public FirstSqlMetadata()
	{
	}

	public Map getColumnConstraints(Connection dbConnection, TableIdentifier aTable)
	{
		return Collections.EMPTY_MAP;
	}

	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent) 
		throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuilder result = new StringBuilder(200);
		try
		{
			pstmt = dbConnection.prepareStatement(SQL);
			pstmt.setString(1, aTable.getSchema());
			pstmt.setString(2, aTable.getTableName());
			rs = pstmt.executeQuery();
			int count = 0;
			while (rs.next())
			{
				String constraint = rs.getString(1);
				String name = rs.getString(2);
				
				if (count > 0)
				{
					result.append('\n');
					result.append(indent);
					result.append(',');
				}
				if (name != null)
				{
					result.append("CONSTRAINT ");
					result.append(name);
					result.append(' ');
				}
				result.append("CHECK (");
				result.append(constraint);
				result.append(')');
				count ++;
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("FirstSqlMetadata.getTableConstraints()", "Could not retrieve table constraints for " + aTable.getTableExpression(), e);
			throw e;
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result.toString();
	}
}
