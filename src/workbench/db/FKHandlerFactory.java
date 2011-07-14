/*
 * FKHandlerFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.db.oracle.OracleFKHandler;

/**
 *
 * @author Thomas Kellerer
 */
public class FKHandlerFactory
{
	public static FKHandler createInstance(WbConnection conn)
	{
		if (conn.getMetadata().isOracle() && conn.getDbSettings().fixFKRetrieval())
		{
			return new OracleFKHandler(conn);
		}
		return new FKHandler(conn);
	}
}
