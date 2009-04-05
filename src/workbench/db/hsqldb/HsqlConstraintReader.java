/*
 * HsqlConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import java.sql.Connection;
import workbench.db.AbstractConstraintReader;
import workbench.db.JdbcUtils;
import workbench.util.StringUtil;

/**
 * Constraint reader for HSQLDB
 * @author  support@sql-workbench.net
 */
public class HsqlConstraintReader
	extends AbstractConstraintReader
{

	private String TABLE_SQL = "select chk.constraint_name, chk.check_clause \n" +
		"from information_schema.system_check_constraints chk, information_schema.system_table_constraints cons \n" +
		"where chk.constraint_name = cons.constraint_name  \n" +
		"and cons.constraint_type = 'CHECK' \n" +
		"and cons.table_name = ?; \n";
	
	private String sql;

	public HsqlConstraintReader(Connection dbConnection)
	{
		super();
		this.sql = TABLE_SQL;

		if (JdbcUtils.hasMinimumServerVersion(dbConnection, "1.9"))
		{
			this.sql = sql.replace("system_check_constraints", "check_constraints");
			this.sql = sql.replace("system_table_constraints", "table_constraints");
		}
	}

	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		return this.sql;
	}

	@Override
	public boolean isSystemConstraintName(String name)
	{
		if (StringUtil.isBlank(name))	return false;
		return name.trim().startsWith("SYS_");
	}
}
