/*
 * TriggerReaderFactory
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import workbench.db.postgres.PostgresTriggerReader;

/**
 * A factory to create instances of TriggerReader.
 *
 * Currently only one specialized TriggerReader is used for Postgres, for all
 * other DBMS, the DefaultTriggerReader is used.
 * 
 * @author Thomas Kellerer
 */
public class TriggerReaderFactory
{
	public static TriggerReader createReader(WbConnection con)
	{
		if (con == null) return null;
		if (con.getMetadata() == null) return null;

		if (con.getMetadata().isPostgres())
		{
			return new PostgresTriggerReader(con);
		}
		return new DefaultTriggerReader(con);
	}
}
