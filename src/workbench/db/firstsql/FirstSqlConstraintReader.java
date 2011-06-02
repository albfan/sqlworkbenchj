/*
 * FirstSqlConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import java.util.List;
import java.util.Map;
import workbench.db.AbstractConstraintReader;
import workbench.db.TableConstraint;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
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
	private static final String SQL = "select ch.check_clause, ch.constraint_name \n" +
             "from definition_schema.syschecks ch,  \n" +
             "     definition_schema.sysconstraints cons \n" +
             "where cons.constraint_type = 'check' \n" +
             "  and cons.constraint_name = ch.constraint_name" +
             "  and cons.table_schema = ? \n" +
             "  and cons.table_name = ? ";


	@Override
	public Map<String, String> getColumnConstraints(Connection dbConnection, TableIdentifier aTable)
	{
		return Collections.emptyMap();
	}

	@Override
	public List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableIdentifier aTable)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("FirstSqlConstraintReader.getTableConstraints()", "Using query=\n" + SQL);
		}

		List<TableConstraint> result = CollectionUtil.arrayList();

		try
		{
			pstmt = dbConnection.getSqlConnection().prepareStatement(SQL);
			pstmt.setString(1, aTable.getSchema());
			pstmt.setString(2, aTable.getTableName());
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
			LogMgr.logError("FirstSqlMetadata.getTableConstraints()", "Could not retrieve table constraints for " + aTable.getTableExpression(), e);
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
