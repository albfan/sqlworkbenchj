/*
 * DbShutdownFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.shutdown;

import workbench.db.WbConnection;

/**
 * A factory to create instances of the DbShutdownHook interface.
 * 
 * @author Thomas Kellerer
 */
public class DbShutdownFactory 
{
	/**
	 * Create a DbShutdownHook for the given connection.
	 * @param con the connection for which to create the shutdown hook
	 * @return null if not shutdown processing is necessary, an approriate instance otherwise
	 */
	public static DbShutdownHook getShutdownHook(WbConnection con)
	{
		if (con == null) return null;
		if (con.getMetadata() == null) return null;
		
		if (con.getMetadata().isHsql())
		{
			return new HsqlShutdownHook();
		}
		else if (con.getMetadata().isApacheDerby())
		{
			return new DerbyShutdownHook();
		}
		return null;
	}
}
