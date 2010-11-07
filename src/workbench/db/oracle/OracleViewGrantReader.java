/*
 * OracleViewGrantReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import workbench.db.ViewGrantReader;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleViewGrantReader
	extends ViewGrantReader
{

	@Override
	public String getViewGrantSql()
	{
		return "select grantee, privilege, grantable \n" +
             "from ALL_TAB_PRIVS \n" +
             "where table_name = ? \n" +
             " and table_schema = ? ";
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
