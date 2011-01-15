/*
 * PostgresViewGrantReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import workbench.db.ViewGrantReader;

/**
 * A class to read view grants from the standard ANSI information_schema.
 * This will work for the following DBMS
 *
 * <ul>
 *	<li>PostgreSQL</li>
 *  <li>H2 Database</li>
 *  <li>MySQL</li>
 *  <li>Microsoft SQL Server 2000</li>
 * </ul>
 * @see workbench.db.ViewGrantReader#createViewGrantReader(workbench.db.WbConnection)
 * 
 * @author Thomas Kellerer
 */
public class PostgresViewGrantReader
	extends ViewGrantReader
{

	@Override
	public String getViewGrantSql()
	{
		String sql = "select grantee, privilege_type, is_grantable  \n" +
             "from information_schema.table_privileges \n" +
             "where table_name = ? \n" +
             " and table_schema = ? ";
		return sql;
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
