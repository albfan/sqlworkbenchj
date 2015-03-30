/*
 * SqlServerConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.mssql;

import workbench.db.AbstractConstraintReader;
import workbench.db.WbConnection;

/**
 * A ConstraintReader for Microsoft SQL Server.
 *
 * @author  Thomas Kellerer
 */
public class SqlServerConstraintReader
	extends AbstractConstraintReader
{

	private boolean is2000;

	public SqlServerConstraintReader(WbConnection con)
	{
		super(con.getDbId());
		if (!SqlServerUtil.isSqlServer2005(con))
		{
			is2000 = true;
		}
	}

	/**
	 * The SQL to retrieve check constraints for SQL Server 2000 and earlier
	 */
	private final String OLD_TABLE_SQL =
		 "select cons.name, c.text \n" +
		 "from sysobjects cons, \n" +
		 "     syscomments c, \n" +
		 "     sysobjects tab \n" +
		 "where cons.xtype = 'C' \n" +
		 "and   cons.id = c.id \n" +
		 "and   cons.parent_obj = tab.id \n" +
		 "and   tab.name = ? \n";

	private final String TABLE_SQL =
		"select cons.name, cons.definition \n" +
		"from sys.check_constraints cons with (nolock) \n" +
		"  join sys.tables tab with (nolock) on cons.parent_object_id = tab.object_id \n" +
		"  join sys.schemas s with (nolock) on s.schema_id = tab.schema_id \n" +
		"where tab.name = ?  \n" +
		"  and s.name = ?";

	private final String DEFAULT_CONSTRAINTS_SQL =
		"select col.name, \n" +
		"       case  \n" +
		"          when is_system_named = 1 then 'DEFAULT ' + cons.definition \n" +
		"          else 'CONSTRAINT ' + cons.name + ' DEFAULT ' + cons.definition \n" +
		"       end as value \n" +
		"from sys.default_constraints cons with (nolock) \n" +
		"  join sys.columns col with (nolock) on cons.object_id = col.default_object_id and cons.parent_column_id = col.column_id \n" +
		"  join sys.tables tab with (nolock) on cons.parent_object_id = tab.object_id \n" +
		"  join sys.schemas s with (nolock) on s.schema_id = tab.schema_id \n" +
		"where cons.type = 'D' \n" +
		"  and tab.name = ? \n" +
		"  and s.name = ? ";

	@Override
	public String getColumnConstraintSql()
	{
		if (is2000)
		{
			return null;
		}
		return DEFAULT_CONSTRAINTS_SQL;
	}

	@Override
	public String getTableConstraintSql()
	{
		if (is2000)
		{
			return OLD_TABLE_SQL;
		}
		return TABLE_SQL;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	@Override
	public int getIndexForSchemaParameter()
	{
		if (is2000)
		{
			return -1;
		}
		return 2;
	}

}
