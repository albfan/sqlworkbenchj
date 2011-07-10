/*
 * H2TableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.h2database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class H2TableSourceBuilder
	extends TableSourceBuilder
{

	public H2TableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	public String getTableSource(TableIdentifier table, boolean includeDrop, boolean includeFk)
		throws SQLException
	{
		if ("TABLE LINK".equals(table.getType()))
		{
			String sql = getLinkedTableSource(table, includeDrop);
			if (sql != null) return sql;
		}
		return super.getTableSource(table, includeDrop, includeFk);
	}

	private String getLinkedTableSource(TableIdentifier table, boolean includeDrop)
		throws SQLException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String sql =
			"SELECT sql FROM information_schema.tables " +
			" WHERE table_schema = ? " +
			"   AND table_name = ? " +
			"   AND table_type = 'TABLE LINK'";

		StringBuilder createSql = new StringBuilder(100);

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("H2TableSourceBuilder.getLinkedTableSource()", "Using statement: " + sql);
		}

		if (includeDrop)
		{
			createSql.append("DROP TABLE ");
			createSql.append(table.getTableExpression(dbConnection));
			createSql.append(";\n\n");
		}
		try
		{
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getSchema());
			stmt.setString(2, table.getTableName());
			rs = stmt.executeQuery();
			if (rs.next())
			{
				String create = rs.getString(1);
				if (StringUtil.isNonEmpty(create))
				{
					create = create.replace("/*--hide--*/", "");
				}
				createSql.append(create.trim());
				createSql.append(";\n");
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("H2TableSourceBuilder.getLinkedTableSource()", "Error retrieving table source", ex);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return createSql.toString();
	}

}
