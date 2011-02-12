/*
 * HsqlViewGrantReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import workbench.db.JdbcUtils;
import workbench.db.ViewGrantReader;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlViewGrantReader
	extends ViewGrantReader
{
	private String sql;

	public HsqlViewGrantReader(WbConnection con)
	{
		if (JdbcUtils.hasMinimumServerVersion(con, "2.0"))
		{
			sql = "select grantee, privilege_type, is_grantable  \n" +
            "from information_schema.TABLE_PRIVILEGES \n" +
            "where table_name = ? \n" +
            " and table_schema = ? ";
		}
		else
		{
			sql = "select grantee, privilege, is_grantable  \n" +
						"from information_schema.SYSTEM_TABLEPRIVILEGES \n" +
						"where table_name = ? \n" +
						"  and table_schem = ? ";
		}
	}

	@Override
	public String getViewGrantSql()
	{
		return sql;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	@Override
	public int getIndexForSchemaParameter()
	{
		return 2;
	}

}
