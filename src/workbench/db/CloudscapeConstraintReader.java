/*
 * CloudscapeConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db;


/**
 * Constraint reader for Cloudscape database
 * @author  info@sql-workbench.net
 */
public class CloudscapeConstraintReader extends AbstractConstraintReader
{
	
	
	private static final String TABLE_SQL = "select 'check '|| c.checkdefinition \n" + 
             "from sys.syschecks c, sys.systables t, sys.sysconstraints cons, sys.sysschemas s \n" + 
             "where t.tableid = cons.tableid \n" + 
             "and   t.schemaid = s.schemaid \n" + 
             "and   cons.constraintid = c.constraintid \n" + 
             "and   t.tablename = ? \n" + 
             "and   s.schemaname = ?";

						 
	public CloudscapeConstraintReader()
	{
	}
	
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }
	
	public int getIndexForTableNameParameter() { return 1; }
	public int getIndexForSchemaParameter() { return 2; }
}
