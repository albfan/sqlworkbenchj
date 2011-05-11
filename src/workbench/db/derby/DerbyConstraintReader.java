/*
 * DerbyConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.derby;

import workbench.db.AbstractConstraintReader;

/**
 * Constraint reader for the Derby database
 * @author  Thomas Kellerer
 */
public class DerbyConstraintReader
	extends AbstractConstraintReader
{
	private static final String TABLE_SQL = "select cons.constraintname, c.checkdefinition \n" +
             "from sys.syschecks c, sys.systables t, sys.sysconstraints cons, sys.sysschemas s \n" +
             "where t.tableid = cons.tableid \n" +
             "and   t.schemaid = s.schemaid \n" +
             "and   cons.constraintid = c.constraintid \n" +
             "and   t.tablename = ? \n" +
             "and   s.schemaname = ?";


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

	@Override
	public int getIndexForSchemaParameter()
	{
		return 2;
	}
}
