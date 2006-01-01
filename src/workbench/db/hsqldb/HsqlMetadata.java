/*
 * HsqlMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import workbench.db.SchemaInformationReader;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author info@sql-workbench.net
 */
public class HsqlMetadata
	implements SchemaInformationReader
{
	private boolean is18;
	
	public HsqlMetadata(WbConnection conn)
	{
		String version = null;
		try
		{
			version = conn.getSqlConnection().getMetaData().getDatabaseProductVersion();
		}
		catch (Exception e)
		{
			version = "1.7";
		}
		is18 = (version != null && version.startsWith("1.8"));
	}

	private String CURRENT_SCHEMA_SQL = "SELECT value FROM information_schema.system_sessioninfo WHERE key = 'SCHEMA'";
	
	public String getCurrentSchema(WbConnection conn)
	{
		if (!is18) return null;
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String schema = null;
		try
		{
			stmt = conn.getSqlConnection().prepareStatement(CURRENT_SCHEMA_SQL);
			rs = stmt.executeQuery();
			if (rs.next()) schema = rs.getString(1);;
		}
		catch (Exception e)
		{
			schema = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return schema;
	}
	
}
