/*
 * HsqlMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import java.sql.Connection;
import workbench.db.JdbcUtils;

/**
 *
 * @author support@sql-workbench.net
 */
public class HsqlMetadata
{
	public static boolean supportsInformationSchema(Connection con)
	{
		return JdbcUtils.hasMinimumServerVersion(con, "1.8");
	}	
	
}
