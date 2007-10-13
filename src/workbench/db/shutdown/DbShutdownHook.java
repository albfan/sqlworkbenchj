/*
 * DbShutdownHelper.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.shutdown;

import java.sql.SQLException;
import workbench.db.WbConnection;

/**
 *
 * @author support@sql-workbench.net
 */
public interface DbShutdownHook 
{
	void shutdown(WbConnection con)
		throws SQLException;
}
