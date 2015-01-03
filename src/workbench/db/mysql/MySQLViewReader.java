/*
 * MySQLViewReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
			String viewName = view.getTable().getFullyQualifiedName(connection);
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
