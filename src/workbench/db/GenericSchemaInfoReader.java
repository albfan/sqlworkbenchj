/*
 * GenericSchemaInfoReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
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
	implements SchemaInformationReader, PropertyChangeListener
{
	private String schemaQuery;
	private boolean useSavepoint;
	private boolean isCacheable;
	private boolean reuseStmt;

	private PreparedStatement query;
	private String cachedSchema;

	private String reuseProp;
	private String queryProp;
	private String cacheProp;
	private int callCount;

	public GenericSchemaInfoReader(String dbid)
	{
		useSavepoint = Settings.getInstance().getBoolProperty("workbench.db." + dbid + ".currentschema.query.usesavepoint", false);

		queryProp = "workbench.db." + dbid + ".currentschema.query";
		cacheProp = "workbench.db." + dbid + ".currentschema.cacheable";
		reuseProp = "workbench.db." + dbid + ".currentschema.reuse.stmt";

		schemaQuery = Settings.getInstance().getProperty(queryProp, null);
		isCacheable = Settings.getInstance().getBoolProperty(cacheProp, false);
		reuseStmt = Settings.getInstance().getBoolProperty(reuseProp, false);
		Settings.getInstance().addPropertyChangeListener(this, cacheProp, queryProp, reuseProp);
		logSettings();
	}

	private void logSettings()
	{
		LogMgr.logDebug("GenericSchemaInfoReader.logSettings()", "Keep statement: " + reuseStmt + ", cache value: "+ isCacheable + ", SQL: " + schemaQuery);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(queryProp))
		{
			SqlUtil.closeStatement(query);
			this.query = null;
			this.schemaQuery = Settings.getInstance().getProperty(queryProp, null);
		}
		if (evt.getPropertyName().equals(cacheProp))
		{
			isCacheable = Settings.getInstance().getBoolProperty(cacheProp, false);
			if (!isCacheable)
			{
				cachedSchema = null;
			}
		}
		if (evt.getPropertyName().equals(reuseProp))
		{
			reuseStmt = Settings.getInstance().getBoolProperty(reuseProp, false);
			if (!reuseStmt)
			{
				SqlUtil.closeStatement(query);
				query = null;
			}
		}
		logSettings();
	}



	/**
	 * Retrieves the currently active schema from the server.
	 *
	 * This is done by running the query configured for the passed dbid.
	 * If no query is configured or an error is thrown, this method returns null
	 *
	 * If a configured query throws an error, the query will be ignored for all subsequent calls. 
	 *
	 * @see #GenericSchemaInfoReader(String)
	 * @see workbench.db.DbMetadata#getDbId()
	 */
	@Override
	public String getCurrentSchema(WbConnection conn)
	{
		if (conn == null) return null;
		if (StringUtil.isEmptyString(this.schemaQuery)) return null;
		if (isCacheable && cachedSchema != null) return cachedSchema;

		callCount ++;
		String currentSchema = null;

		Savepoint sp = null;
		ResultSet rs = null;
		Statement stmt = null;

		try
		{
			if (useSavepoint)
			{
				sp = conn.setSavepoint();
			}

			if (reuseStmt)
			{
				if (query == null)
				{
					query = conn.getSqlConnection().prepareStatement(schemaQuery);
				}
				rs = query.executeQuery();
			}
			else
			{
				stmt = conn.createStatement();
				rs = stmt.executeQuery(schemaQuery);
			}

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
				SqlUtil.closeStatement(query);
				this.query = null;
			}
			currentSchema = null;
		}
		finally
		{
			SqlUtil.closeResult(rs);
			SqlUtil.closeStatement(stmt);
		}

		if (isCacheable)
		{
			cachedSchema = currentSchema;
		}
		return currentSchema;
	}

	@Override
	public void dispose()
	{
		if (query != null)
		{
			LogMgr.logDebug("GenericSchemaInformationReader.dispose()", "Freeing statement.");
			SqlUtil.closeStatement(query);
			query = null;
		}
		cachedSchema = null;
		Settings.getInstance().removePropertyChangeListener(this);
		LogMgr.logDebug("GenericSchemaInformationReader.dispose()", "Called: " + callCount + " times");
	}
}
