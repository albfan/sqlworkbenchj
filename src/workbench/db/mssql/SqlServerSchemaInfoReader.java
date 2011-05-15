/*
 * SqlServerSchemaInfoReader
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import workbench.db.SchemaInformationReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerSchemaInfoReader
	implements SchemaInformationReader
{
	private String defaultSchema;

	public SqlServerSchemaInfoReader(Connection con)
	{
		// As the default schema is a property of the user definition and nothing that can change at runtime (at least not easily)
		// I assume it's safe to cache the current schema.
		String sql = "SELECT default_schema_name FROM sys.database_principals WHERE type = 'S' AND name = current_user";
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				defaultSchema = rs.getString(1);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlServerSchemaInfoReader", "Could not read database principals to obtain default schema", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Override
	public String getCurrentSchema(WbConnection conn)
	{
		return defaultSchema;
	}

}
