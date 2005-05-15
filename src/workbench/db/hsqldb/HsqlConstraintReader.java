/*
 * HsqlConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import workbench.db.*;


/**
 * Constraint reader for Adaptive Server Anywhere
 * @author  support@sql-workbench.net
 */
public class HsqlConstraintReader extends AbstractConstraintReader
{
	
	
	private static final String TABLE_SQL = "select chk.check_clause \n" + 
           "from system_check_constraints chk, system_table_constraints cons \n" + 
           "where chk.constraint_name = cons.constraint_name  \n" + 
           "and cons.constraint_type = 'CHECK' \n" + 
           "and cons.table_name = ?; \n";

/** Creates a new instance of FirebirdColumnConstraintReader */
	public HsqlConstraintReader()
	{
	}
	public String getPrefixTableConstraintKeyword() { return "check("; }
	public String getSuffixTableConstraintKeyword() { return ")"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }
}
