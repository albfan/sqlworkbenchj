/*
 * H2ConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import workbench.db.AbstractConstraintReader;

/**
 * Constraint reader for <a href="http://www.h2database.com">H2 Database</a>
 * 
 * @author  support@sql-workbench.net
 */
public class H2ConstraintReader 
	extends AbstractConstraintReader
{
	private final String TABLE_SQL =
						"select constraint_name, check_expression \n" +
            "from information_schema.constraints \n" +
            "where constraint_type = 'CHECK'  \n" +
            "and table_name = ? \n" +
            "and table_schema = ?";

	public int getIndexForSchemaParameter() { return 2; }
	public int getIndexForTableNameParameter() { return 1; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return this.TABLE_SQL; }

}
