/*
 * SqlServerConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import workbench.db.AbstractConstraintReader;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SqlServerConstraintReader extends AbstractConstraintReader
{
	private static final String TABLE_SQL = 
					 "select c.text \n" + 
           "from sysobjects cons, \n" + 
           "     syscomments c, \n" + 
           "     sysobjects tab \n" + 
           "where cons.xtype = 'C' \n" + 
           "and   cons.id = c.id \n" + 
           "and   cons.parent_obj = tab.id \n" + 
           "and   tab.name = ? \n";	
	public SqlServerConstraintReader()
	{
	}
	
	public String getPrefixTableConstraintKeyword() { return "check"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }


}
