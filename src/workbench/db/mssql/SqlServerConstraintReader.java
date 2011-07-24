/*
 * SqlServerConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import workbench.db.AbstractConstraintReader;

/**
 * A ConstraintReader for Microsoft SQL Server.
 *
 * @author  Thomas Kellerer
 */
public class SqlServerConstraintReader
	extends AbstractConstraintReader
{
	private final String TABLE_SQL =
		 "select cons.name, c.text \n" +
		 "from sysobjects cons, \n" +
		 "     syscomments c, \n" +
		 "     sysobjects tab \n" +
		 "where cons.xtype = 'C' \n" +
		 "and   cons.id = c.id \n" +
		 "and   cons.parent_obj = tab.id \n" +
		 "and   tab.name = ? \n";

	private final String DEFAULT_CONSTRAINTS_SQL =
		"select col.name, \n" +
		"       case  \n" +
		"          when is_system_named = 1 then 'DEFAULT ' + cons.definition \n" +
		"          else 'CONSTRAINT ' + cons.name + ' DEFAULT ' + cons.definition \n" +
		"       end as value \n" +
		"from sys.default_constraints cons \n" +
		"  join sys.columns col on cons.object_id = col.default_object_id and cons.parent_column_id = col.column_id \n" +
		"  join sysobjects tab on cons.parent_object_id = tab.id  \n" +
		"where cons.type = 'D' \n" +
		"  and tab.name = ? ";

	@Override
	public String getColumnConstraintSql()
	{
		return DEFAULT_CONSTRAINTS_SQL;
	}

	@Override
	public String getTableConstraintSql()
	{
		return TABLE_SQL;
	}


}
