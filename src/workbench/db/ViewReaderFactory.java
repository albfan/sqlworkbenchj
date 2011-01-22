/*
 * ViewReaderFactory
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

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
		return new DefaultViewReader(con);
	}
}
