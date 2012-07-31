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
	private WbConnection connection;
	private String schemaQuery;
	private boolean useSavepoint;
	private boolean isCacheable;
	private boolean reuseStmt;

	private PreparedStatement query;
	private String cachedSchema;

	private String reuseProp;
	private String queryProp;
	private String cacheProp;
	private String timeoutProp;

	public GenericSchemaInfoReader(WbConnection conn, String dbid)
	{
		connection = conn;
		useSavepoint = Settings.getInstance().getBoolProperty("workbench.db." + dbid + ".currentschema.query.usesavepoint", false);

		queryProp = "workbench.db." + dbid + ".currentschema.query";
		cacheProp = "workbench.db." + dbid + ".currentschema.cacheable";
		reuseProp = "workbench.db." + dbid + ".currentschema.reuse.stmt";
		reuseProp = "workbench.db." + dbid + ".currentschema.timeout";

		schemaQuery = Settings.getInstance().getProperty(queryProp, null);
		isCacheable = Settings.getInstance().getBoolProperty(cacheProp, false);
		reuseStmt = Settings.getInstance().getBoolProperty(reuseProp, false);
		Settings.getInstance().addPropertyChangeListener(this, cacheProp, queryProp, reuseProp, timeoutProp);

		connection.addChangeListener(this);
		logSettings();
	}

	private void logSettings()
	{
		LogMgr.logDebug("GenericSchemaInfoReader.logSettings()", "Re-Use statement: " + reuseStmt + ", cache current schema: "+ isCacheable + ", SQL: " + schemaQuery);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == this.connection && evt.getPropertyName().equals(WbConnection.PROP_SCHEMA))
		{
			this.cachedSchema = null;
			return;
		}

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

	private int getQueryTimeout()
	{
		int timeout = Settings.getInstance().getIntProperty(timeoutProp, 0);
		if (timeout < 0) return 0;
		return timeout;
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
	public String getCurrentSchema()
	{
		if (this.connection == null) return null;
		if (StringUtil.isEmptyString(this.schemaQuery)) return null;
		if (isCacheable && cachedSchema != null)
		{
			LogMgr.logDebug("GenericSchemaInfoReader.getCurrenSchema()", "Using cached schema: " + cachedSchema);
			return cachedSchema;
		}

		String currentSchema = null;

		Savepoint sp = null;
		ResultSet rs = null;
		Statement stmt = null;

		try
		{
			if (useSavepoint)
			{
				sp = connection.setSavepoint();
			}

			if (reuseStmt)
			{
				if (query == null)
				{
					query = connection.getSqlConnection().prepareStatement(schemaQuery);
				}
				setQueryTimeout(query);
				rs = query.executeQuery();
			}
			else
			{
				stmt = connection.createStatement();
				setQueryTimeout(stmt);
				rs = stmt.executeQuery(schemaQuery);
			}

			if (rs != null && rs.next())
			{
				currentSchema = rs.getString(1);
			}
			if (currentSchema != null) currentSchema = currentSchema.trim();
			connection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			connection.rollback(sp);
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

	private void setQueryTimeout(Statement stmt)
	{
		int timeout = getQueryTimeout();
		try
		{
			if (timeout > 0)
			{
				stmt.setQueryTimeout(timeout);
			}
		}
		catch (SQLException sql)
		{
			LogMgr.logWarning("GenericSchemaInformationReader.setQueryTimeout()", "Could not set query timeout to " + timeout +
				" Please adjust the value of the property: " + queryProp, sql);
		}
	}

	@Override
	public String getCachedSchema()
	{
		return cachedSchema;
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
		connection = null;
		Settings.getInstance().removePropertyChangeListener(this);
	}
}
