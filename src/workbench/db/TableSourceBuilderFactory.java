/*
 * TableSourceBuilderFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.db.postgres.PostgresTableSourceBuilder;

/**
 * A factory to create a TableSourceBuilder.
 *
 * Currently this only distinguishes between Postgres and other
 * Databases in order to add enum and domain information to the
 * generated SQL for Postgres
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
		return new TableSourceBuilder(con);
	}
}
