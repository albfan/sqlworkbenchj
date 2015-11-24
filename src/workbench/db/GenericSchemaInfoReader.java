/*
 * GenericSchemaInfoReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
	private final boolean useSavepoint;
	private boolean reuseStmt;

	private PreparedStatement query;
	private String cachedSchema;

	private final String reuseProp = "currentschema.reuse.stmt";
	private final String queryProp = "currentschema.query";
	private final String cacheProp = "currentschema.cacheable";
	private final String timeoutProp = "currentschema.timeout";

	public GenericSchemaInfoReader(WbConnection conn, DbSettings settings)
	{
		connection = conn;
		useSavepoint = settings.getBoolProperty("currentschema.query.usesavepoint", false);
		schemaQuery = settings.getProperty(queryProp, null);
		reuseStmt = settings.getBoolProperty(reuseProp, false);
		connection.addChangeListener(this);
		logSettings();
	}

	@Override
	public boolean isSupported()
	{
		return StringUtil.isNonEmpty(schemaQuery);
	}

	private void logSettings()
	{
		LogMgr.logDebug("GenericSchemaInfoReader.logSettings()", "Re-Use statement: " + reuseStmt + ", cache current schema: "+ isCacheable() + ", SQL: " + schemaQuery);
	}

	private boolean isCacheable()
	{
		return Settings.getInstance().getBoolProperty(cacheProp, false);
	}

	@Override
	public void clearCache()
	{
		this.cachedSchema = null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == this.connection && evt.getPropertyName().equals(WbConnection.PROP_SCHEMA))
		{
			Object value = evt.getNewValue();
			if (value instanceof String)
			{
				this.cachedSchema = (String)value;
			}
			else
			{
				this.cachedSchema = null;
			}
		}
	}

	private int getQueryTimeout()
	{
		int timeout = connection.getDbSettings().getIntProperty(timeoutProp, 0);
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
	 * @see workbench.db.DbMetadata#getDbId()
	 */
	@Override
	public String getCurrentSchema()
	{
		if (this.connection == null) return null;
		if (StringUtil.isEmptyString(this.schemaQuery)) return null;

		boolean isCacheable = isCacheable();
		if (isCacheable && cachedSchema != null)
		{
//			LogMgr.logTrace("GenericSchemaInfoReader.getCurrenSchema()", "Using cached schema: " + cachedSchema);
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
			currentSchema = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
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
		catch (Throwable sql)
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
		SqlUtil.closeStatement(query);
		cachedSchema = null;
		connection = null;
		Settings.getInstance().removePropertyChangeListener(this);
	}
}
