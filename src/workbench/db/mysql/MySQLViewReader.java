/*
 * PostgresViewReader
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.db.DefaultViewReader;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLViewReader
	extends DefaultViewReader
{

	public MySQLViewReader(WbConnection con)
	{
		super(con);
	}

	@Override
	public CharSequence getExtendedViewSource(TableDefinition view, boolean includeDrop, boolean includeCommit)
		throws SQLException
	{
		if (!this.connection.getDbSettings().getUseMySQLShowCreate("view"))
		{
			return super.getExtendedViewSource(view, includeDrop, includeCommit);
		}

		String source = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			String viewName = view.getTable().getTableExpression(connection);
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery("show create view " + viewName);
			if (rs.next())
			{
				source = rs.getString(2);
			}
			if (includeDrop && source != null)
			{
				source  = "drop view " + viewName + ";\n\n" + source;
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}
}
