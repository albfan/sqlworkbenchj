/*
 * HsqlConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import java.sql.Connection;
import java.sql.SQLException;
import workbench.db.*;


/**
 * Constraint reader for HSQLDB
 * @author  support@sql-workbench.net
 */
public class H2ConstraintReader 
	extends AbstractConstraintReader
{
	private final String TABLE_SQL = "select column_name, 'CHECK '||check_constraint \n" + 
           "from information_schema.columns \n" + 
           "where table_name = ? \n" + 
           "and table_schema = ? \n" +
					 "and check_constraint is not null";
	
	public H2ConstraintReader()
	{
	}
	
	public int getIndexForSchemaParameter() { return 2; }
	public int getIndexForTableNameParameter() { return 1; }
	public String getColumnConstraintSql() { return this.TABLE_SQL; }
	public String getTableConstraintSql() { return null; }
	
}
