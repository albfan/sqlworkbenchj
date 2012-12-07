/*
 * ViewReaderFactory
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import workbench.db.mysql.MySQLViewReader;
import workbench.db.oracle.OracleViewReader;
import workbench.db.postgres.PostgresViewReader;

/**
 *
 * @author Thomas Kellerer
 */
public class ViewReaderFactory
{
	public static ViewReader createViewReader(WbConnection con)
	{
		if (con.getMetadata().isPostgres())
		{
			return new PostgresViewReader(con);
		}
		if (con.getMetadata().isMySql())
		{
			return new MySQLViewReader(con);
		}
		if (con.getMetadata().isOracle())
		{
			return new OracleViewReader(con);
		}
		return new DefaultViewReader(con);
	}
}
