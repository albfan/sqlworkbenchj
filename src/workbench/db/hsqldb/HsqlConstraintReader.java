/*
 * HsqlConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import java.sql.Connection;
import workbench.db.AbstractConstraintReader;


/**
 * Constraint reader for HSQLDB
 * @author  support@sql-workbench.net
 */
public class HsqlConstraintReader 
	extends AbstractConstraintReader
{
	private String TABLE_SQL = "select chk.check_clause \n" + 
           "from information_schema.system_check_constraints chk, information_schema.system_table_constraints cons \n" + 
           "where chk.constraint_name = cons.constraint_name  \n" + 
           "and cons.constraint_type = 'CHECK' \n" + 
           "and cons.table_name = ?; \n";
	
	private String sql;

	public HsqlConstraintReader(Connection dbConnection)
	{
		if (HsqlMetadata.supportsInformationSchema(dbConnection))
		{
			this.sql = TABLE_SQL;
		}
		else
		{
			this.sql = TABLE_SQL.replaceAll("information_schema\\.","");
		}
	}
	
	public String getPrefixTableConstraintKeyword() { return "check("; }
	public String getSuffixTableConstraintKeyword() { return ")"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return this.sql; }
	
}
