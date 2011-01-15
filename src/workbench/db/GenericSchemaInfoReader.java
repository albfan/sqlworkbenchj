/*
 * GenericSchemaInfoReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class GenericSchemaInfoReader
	implements SchemaInformationReader
{
	private String schemaQuery = null;
	private boolean useSavepoint = false;
	
	public GenericSchemaInfoReader(String dbid)
	{
		this.schemaQuery = Settings.getInstance().getProperty("workbench.db." + dbid + ".currentschema.query", null);
		useSavepoint = Settings.getInstance().getBoolProperty("workbench.db." + dbid + ".currentschema.query.usesavepoint", false);
	}
	
	/**
	 * Retrieves the currently active schema from the server.
	 * This is done by running the query configured for the passed dbid.
	 * If no query is configured or an error is thrown, this method
	 * returns null
	 * @see #GenericSchemaInfoReader(String)
	 * @see workbench.db.DbMetadata#getDbId()
	 */
	public String getCurrentSchema(WbConnection conn)
	{
		if (conn == null) return null;
		if (StringUtil.isEmptyString(this.schemaQuery)) return null;
		Statement stmt = null;
		String currentSchema = null;

		Savepoint sp = null;
		ResultSet rs = null;

		try
		{
			if (useSavepoint)
			{
				sp = conn.setSavepoint();
			}
			stmt = conn.createStatementForQuery();
			stmt.execute(schemaQuery);
			rs = stmt.getResultSet();
			if (rs != null && rs.next())
			{
				currentSchema = rs.getString(1);
			}
			if (currentSchema != null) currentSchema = currentSchema.trim();
			conn.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			conn.rollback(sp);
			LogMgr.logWarning("GenericSchemaInfoReader.getCurrentSchema()", "Error reading current schema using query: " + schemaQuery, e);
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
