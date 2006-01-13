/*
 * GenericSchemaInfoReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class GenericSchemaInfoReader
	implements SchemaInformationReader
{
	private String schemaQuery = null;

	public GenericSchemaInfoReader(String dbid)
	{
		this.schemaQuery = Settings.getInstance().getProperty("workbench.db." + dbid + ".currentschema.query", null);
	}

	public String getCurrentSchema(WbConnection conn)
	{
		if (this.schemaQuery == null) return null;
		Statement stmt = null;
		ResultSet rs = null;
		String currentSchema = null;
		try
		{
			stmt = conn.createStatementForQuery();
			rs = stmt.executeQuery(schemaQuery);
			if (rs.next())
			{
				currentSchema = rs.getString(1);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("GenericSchemaInfoReader.getCurrentSchema()", "Error reading current schema", e);
			if (e instanceof SQLException)
			{
				// When a SQLException is thrown, we assume an error with the configured
				// query, so it's disabled to avoid subsequent errors
				this.schemaQuery = null;
			}
			currentSchema = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return currentSchema;
		
	}
	
	
}
