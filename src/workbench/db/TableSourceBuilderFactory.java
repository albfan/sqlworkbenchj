/*
 * TableSourceBuilderFactory.java
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

import workbench.db.derby.DerbyTableSourceBuilder;
import workbench.db.postgres.PostgresTableSourceBuilder;

/**
 * A factory to create a TableSourceBuilder.
 *
 * @author Thomas Kellerer
 */
public class TableSourceBuilderFactory
{

	public static TableSourceBuilder getBuilder(WbConnection con)
	{
		if (con.getMetadata().isPostgres())
		{
			return new PostgresTableSourceBuilder(con);
		}
		else if (con.getMetadata().isApacheDerby())
		{
			return new DerbyTableSourceBuilder(con);
		}
		return new TableSourceBuilder(con);
	}

}
