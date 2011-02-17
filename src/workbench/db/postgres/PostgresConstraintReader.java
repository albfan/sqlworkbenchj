/*
 * PostgresConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import workbench.db.AbstractConstraintReader;


/**
 * Read table level constraints for Postgres
 * (column constraints are stored on table level...)
 * @author  Thomas Kellerer
 */
public class PostgresConstraintReader
	extends AbstractConstraintReader
{
	private static final String TABLE_SQL =
				"select rel.conname,  \n" +
				"       case  \n" +
				"         when rel.consrc is null then pg_get_constraintdef(rel.oid) \n" +
				"         else rel.consrc \n" +
				"       end as src, \n" +
				"       obj_description(t.oid) as remarks  \n" +
				"from pg_class t \n" +
				"  join pg_constraint rel on t.oid = rel.conrelid   \n" +
				"where rel.contype in ('c', 'x') \n" +
				" and t.relname = ? ";

	public String getColumnConstraintSql()
	{
		return null;
	}

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
