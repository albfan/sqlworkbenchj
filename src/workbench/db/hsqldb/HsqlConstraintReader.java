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

import java.sql.Connection;
import java.sql.SQLException;
import workbench.db.*;


/**
 * Constraint reader for Adaptive Server Anywhere
 * @author  support@sql-workbench.net
 */
public class HsqlConstraintReader 
	extends AbstractConstraintReader
{
	private static final String TABLE_SQL_17 = "select chk.check_clause \n" + 
           "from system_check_constraints chk, system_table_constraints cons \n" + 
           "where chk.constraint_name = cons.constraint_name  \n" + 
           "and cons.constraint_type = 'CHECK' \n" + 
           "and cons.table_name = ?; \n";

	private String TABLE_SQL_18 = "select chk.check_clause \n" + 
           "from information_schema.system_check_constraints chk, information_schema.system_table_constraints cons \n" + 
           "where chk.constraint_name = cons.constraint_name  \n" + 
           "and cons.constraint_type = 'CHECK' \n" + 
           "and cons.table_name = ?; \n";
	
	private String sql;

	public HsqlConstraintReader()
	{
		this.sql = TABLE_SQL_17;
	}
	
	public String getPrefixTableConstraintKeyword() { return "check("; }
	public String getSuffixTableConstraintKeyword() { return ")"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return sql; }
	
	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
		throws SQLException
	{
		String version = null;
		try
		{
			version = dbConnection.getMetaData().getDatabaseProductVersion();
		}
		catch (SQLException e)
		{
			version = "1.7.0";
		}
		
		if (version.startsWith("1.8"))
		{
			this.sql = TABLE_SQL_18;
		}
		else
		{
			this.sql = TABLE_SQL_17;
		}
		
		return super.getTableConstraints(dbConnection, aTable, indent);
	}		
}
