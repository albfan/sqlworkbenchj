/*
 * SqlServerConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
	private static final String TABLE_SQL =
					 "select cons.name, c.text \n" +
           "from sysobjects cons, \n" +
           "     syscomments c, \n" +
           "     sysobjects tab \n" +
           "where cons.xtype = 'C' \n" +
           "and   cons.id = c.id \n" +
           "and   cons.parent_obj = tab.id \n" +
           "and   tab.name = ? \n";

	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }


}
