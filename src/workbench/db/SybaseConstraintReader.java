/*
 * ASAConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;


/**
 * Constraint reader for Adaptive Server Anywhere
 * @author  Thomas Kellerer
 */
public class SybaseConstraintReader
	extends AbstractConstraintReader
{
	private final String TABLE_SQL =
			"select chk.check_defn \n" +
			"from syscheck chk, sysconstraint cons, systable tbl \n" +
			"where chk.check_id = cons.constraint_id \n" +
			"and   cons.constraint_type = 'T' \n" +
			"and   cons.table_id = tbl.table_id \n" +
			"and   tbl.table_name = ? \n";

	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		return TABLE_SQL;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 1;
	}
}
