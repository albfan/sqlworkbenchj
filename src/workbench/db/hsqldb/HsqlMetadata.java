/*
 * HsqlMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author info@sql-workbench.net
 */
public class HsqlMetadata
{
	public static boolean supportsInformationSchema(Connection con)
	{
		int major = 0;
		int minor = 0;
		try
		{
			major = con.getMetaData().getDatabaseMajorVersion();
			minor = con.getMetaData().getDriverMinorVersion();
		}
		catch (SQLException e)
		{
			major = 1;
			minor = 7;
		}
		
		if (major >= 1 && minor >= 8)
		{
			return true;
		}
		else
		{
			return false;
		}
	}	
	
}
