/*
 * WbConnection.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.db.objectcache.DbObjectCache;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.db.objectcache.DbObjectCacheFactory;

import workbench.db.report.TagWriter;
import workbench.interfaces.DbExecutionListener;
import workbench.resource.ResourceMgr;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.ScriptParser;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.preparedstatement.PreparedStatementPool;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbConnection
	implements DbExecutionListener
{
	public static final String PROP_CATALOG = "catalog";
	public static final String PROP_SCHEMA = "schema";
	public static final String PROP_AUTOCOMMIT = "autocommit";
	public static final String PROP_CONNECTION_STATE = "state";
	public static final String CONNECTION_CLOSED = "closed";
	public static final String CONNECTION_OPEN = "open";

  private String id;
	private StringBuilder scriptError;
	private Connection sqlConnection;
	private DbMetadata metaData;
	private ConnectionProfile profile;
	private PreparedStatementPool preparedStatementPool;
	private List<PropertyChangeListener> listeners;

	private Method clearSettings;
	private Object dbAccess;
	private boolean doOracleClear = true;

	private boolean busy;
	private KeepAliveDaemon keepAlive;
	private String currentCatalog;
	private String currentSchema;

	private boolean removeComments;
	private boolean removeNewLines;
	private Integer fetchSize;

	private boolean supportsGetWarnings = true;

	/**
	 * Create a new wrapper connection around the original SQL connection.
	 * This will also initialize a {@link DbMetadata} instance.
	 */
	public WbConnection(String anId, Connection aConn, ConnectionProfile aProfile)
		throws SQLException
	{
		this.id = anId;
		setSqlConnection(aConn);
		setProfile(aProfile);

		// removeComments and removeNewLines are properties
		// that are needed each time a SQL statement is executed
		// To speed up SQL parsing, the value for those properties are
		// "cached" here
		if (profile != null)
		{
			removeComments = profile.getRemoveComments();
		}
		if (metaData != null)
		{
			DbSettings db = metaData.getDbSettings();
			if (!removeComments)
			{
				removeComments = !db.supportsCommentInSql();
			}
			removeNewLines = db.removeNewLinesInSQL();
		}
	}

	public TransactionChecker getTransactionChecker()
	{
		return TransactionChecker.Factory.createChecker(this);
	}
	
	public ObjectNameFilter getCatalogFilter()
	{
		return profile == null ? null : profile.getCatalogFilter();
	}

	public ObjectNameFilter getSchemaFilter()
	{
		return profile == null ? null : profile.getSchemaFilter();
	}

	public boolean getRemoveComments()
	{
		return removeComments;
	}

	public boolean getRemoveNewLines()
	{
		return removeNewLines;
	}
	/**
	 * Returns the internal ID of this connection.
	 *
	 * @return the internal id of this connection.
	 */
	public String getId()
	{
		return this.id;
	}

	public String getDbId()
	{
		if (getDbSettings() == null) return null;
		return getDbSettings().getDbId();
	}

	private void setProfile(ConnectionProfile aProfile)
	{
		this.profile = aProfile;
		initKeepAlive();
	}

	/**
	 * Returns the current isolation level as a readable string
	 * @return
	 */
	public String getIsolationLevel()
	{
		if (this.sqlConnection == null) return "";
		try
		{
			return SqlUtil.getIsolationLevelName(sqlConnection.getTransactionIsolation());
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbConnection.getIsolationLevel()", "Error retrieving isolation level", e);
		}
		return "n/a";
	}

	public PreparedStatementPool getPreparedStatementPool()
	{
		if (this.preparedStatementPool == null)
		{
			this.preparedStatementPool = new PreparedStatementPool(this);
		}
		return this.preparedStatementPool;
	}

	public DbObjectCache getObjectCache()
	{
		return DbObjectCacheFactory.getInstance().getCache(this);
	}

	/**
	 * Returns a "cached" version of the current schema. It is safe
	 * to call this method any time as it does not send any
	 * statement to the database, but might not necessarily
	 * return the correct schema
	 */
	public String getDisplaySchema()
	{
		return currentSchema;
	}

	/**
	 * Return the current schema of the connection.
	 * This will send a query to the database, so this might
	 * not be usable if a statement or a transaction is currently
	 * in progress.
	 */
	public String getCurrentSchema()
	{
		if (metaData == null) return null;
		return this.metaData.getCurrentSchema();
	}

	/**
	 * Return the name of the current user.
	 * Wrapper for DatabaseMetaData.getUserName() that throws no Exception
	 */
	public String getCurrentUser()
	{
		try
		{
			return this.sqlConnection.getMetaData().getUserName();
		}
		catch (Throwable e)
		{
			return StringUtil.EMPTY_STRING;
		}
	}

	public boolean supportsQueryTimeout()
	{
		if (this.metaData == null) return false;
		return this.metaData.getDbSettings().supportsQueryTimeout();
	}

	/**
	 * @return The profile associated with this connection
	 */
	public ConnectionProfile getProfile()
	{
		return this.profile;
	}

	void runPreDisconnectScript()
	{
		if (this.profile == null) return;
		if (this.sqlConnection == null) return;
		if (this.keepAlive != null)
		{
			this.keepAlive.shutdown();
		}
		String sql = profile.getPreDisconnectScript();
		runConnectScript(sql, "disconnect");
	}

	void runPostConnectScript()
	{
		if (this.profile == null) return;
		if (this.sqlConnection == null) return;
		String sql = profile.getPostConnectScript();
		runConnectScript(sql, "connect");
	}

	private void runConnectScript(String sql, String type)
	{
		if (StringUtil.isBlank(sql)) return;
		LogMgr.logInfo("WbConnection.runConnectScript()", "Executing " + type + " script...");

		StatementRunner runner = new StatementRunner();
		runner.setConnection(this);
		runner.setReturnOnlyErrorMessages(true);

		ScriptParser p = new ScriptParser(sql);
		p.setAlternateLineComment(this.getDbSettings().getLineComment());


		// The statemenRunner will call clearMessages() when statementDone()
		// is called which in turn will call clearWarnings() on this instance.
		// This will also clear the scriptError and thus all messages
		// that are collected here. So I have to store the messages locally
		// and cannot use the scriptError variable directly
		StringBuilder messages = new StringBuilder(150);
		String resKey = "MsgConnScript" + type;

		String command = null;
		try
		{
			int count = p.getSize();
			for (int i=0; i < count; i++)
			{
				command = p.getCommand(i);
				String stmtSql = StringUtil.getMaxSubstring(SqlUtil.makeCleanSql(command, false),250);

				try
				{
					runner.runStatement(command);
					StatementRunnerResult result = runner.getResult();
					String msg = ResourceMgr.getString(resKey) + " " + stmtSql;
					messages.append(msg);
					LogMgr.logDebug("WbConnection.runConnectScript()", "  Executed statement: " + stmtSql);
					if (!result.isSuccess())
					{
						messages.append("\n  ");
						messages.append(ResourceMgr.getString("TxtError"));
						messages.append(": ");
						messages.append(result.getMessageBuffer().toString());
					}
					messages.append("\n");
				}
				finally
				{
					runner.statementDone();
				}
			}
			messages.append("\n");
		}
		catch (Throwable e)
		{
			LogMgr.logError("WbConnection.runConnectScript()", "Error executing " + type + " script", e);
			messages = new StringBuilder(50);
			messages.append(ResourceMgr.getString("MsgBatchStatementError"));
			messages.append(": ");
			messages.append(command);
			messages.append('\n');
			messages.append(e.getMessage());
		}
		finally
		{
			if (runner != null) runner.done();
		}
		this.scriptError = messages;
	}

	private void setSqlConnection(Connection aConn)
		throws SQLException
	{
		this.sqlConnection = aConn;
		this.metaData = new DbMetadata(this);
		this.doOracleClear = this.metaData.isOracle();
		this.currentCatalog = metaData.getCurrentCatalog();
	}

	/**
	 * Return any warnings that are stored in the underlying SQL Connection.
	 * The warnings are then cleared from the connection object.
	 *
	 * @see #clearWarnings()
	 * @return any warnings reported from the server, null if no warnings are available.
	 */
	public String getWarnings()
	{
		if (!supportsGetWarnings) return null;

		try
		{
			SQLWarning warn = this.getSqlConnection().getWarnings();
			if (warn == null)
			{
				if (this.scriptError != null)
				{
					String error = this.scriptError.toString();
					this.scriptError = null;
					return error;
				}
				return null;
			}

			StringBuilder msg = new StringBuilder(200);
			if (!StringUtil.isEmptyString(this.scriptError)) msg.append(this.scriptError);

			String s = null;
			while (warn != null)
			{
				s = warn.getMessage();
				msg.append('\n');
				msg.append(s);
				warn = warn.getNextWarning();
			}
			this.clearWarnings();
			return msg.toString();
		}
		catch (UnsupportedOperationException e)
		{
			supportsGetWarnings = false;
			LogMgr.logWarning("WbConnection.getWarnings()", "getWarnings() not supported by the driver");
			return null;
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbConnection.getWarnings()", "Error when retrieving SQL Warnings", e);
			return null;
		}
	}

	/**
	 *	This will clear the warnings from the connection object.
	 *	Some drivers will not replace existing warnings until clearWarnings()
	 *	is called, thus SQL Workbench would show the same error message over and
	 *  over again.
	 *  This method also works around a bug in the Oracle JDBC driver, because
	 *	that does not properly clear the warnings list.
	 */
	public void clearWarnings()
	{
		this.scriptError = null;
		if (this.sqlConnection == null) return;
		try
		{
			this.sqlConnection.clearWarnings();

			if (doOracleClear)
			{
				// obviously the Oracle driver does NOT clear the warnings
				// (as discovered when looking at the source code)
				// this is not true for newer drivers (10.x)

				// luckily the instance variable on the driver which holds the
				// warnings is defined as public and thus we can
				// reset the warnings "manually"
				// This is done via reflection so that the Oracle driver
				// does not need to be present when compiling

				if (this.clearSettings == null || dbAccess == null)
				{
					Class ora = this.sqlConnection.getClass();

					if (ora.getName().equals("oracle.jdbc.driver.OracleConnection"))
					{
						Field dbAccessField = ora.getField("db_access");
						Class dbAccessClass = dbAccessField.getType();
						dbAccess = dbAccessField.get(this.sqlConnection);
						try
						{
							clearSettings = dbAccessClass.getMethod("setWarnings", new Class[] {SQLWarning.class} );
						}
						catch (Throwable e)
						{
							// newer drivers do not seem to support this any more,
							// so after the first error, we'll skip this for the rest of the session
							doOracleClear = false;
							clearSettings = null;
						}
					}
				}
				// the following line is equivalent to:
				// OracleConnection con = (OracleConnection)this.sqlConnection;
				// con.db_access.setWarnings(null);
				if (clearSettings != null) clearSettings.invoke(dbAccess, new Object[] { null });
			}
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("WbConnection.clearWarnings()", "Error resetting warnings!", th);
		}
	}

	public Connection getSqlConnection()
	{
		return this.sqlConnection;
	}

	public void commit()
		throws SQLException
	{
		if (!getDbSettings().supportsTransactions()) return;
		this.sqlConnection.commit();
	}

	public Savepoint setSavepoint()
		throws SQLException
	{
		if (this.getAutoCommit()) return null;
		return this.sqlConnection.setSavepoint();
	}

	/**
	 * A non-exception throwing wrapper around Connection.rollback(Savepoint)
	 */
	public void rollback(Savepoint sp)
	{
		if (sp == null) return;
		if (this.sqlConnection == null) return;

		try
		{
			this.sqlConnection.rollback(sp);
		}
		catch (Throwable e)
		{
			LogMgr.logError("WbConnection.rollback(Savepoint)", "Error releasing savepoint", e);
		}
	}

	/**
	 * A non-exception throwing wrapper around Connection.releaseSavepoint(Savepoint)
	 */
	public void releaseSavepoint(Savepoint sp)
	{
		if (sp == null) return;
		try
		{
			if (!this.getAutoCommit())
			{
				this.sqlConnection.releaseSavepoint(sp);
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("WbConnection.releaseSavepoint", "Error releasing savepoint", e);
		}
	}

	/**
	 * Execute a rollback on the connection.
	 */
	public void rollback()
		throws SQLException
	{
		if (sqlConnection == null) return;
		if (!getDbSettings().supportsTransactions()) return;

		this.sqlConnection.rollback();
	}

	public void rollbackSilently()
	{
		try
		{
			rollback();
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbConnection.rollbackSilently()", "Could not rollback!", e);
		}
	}

	public boolean getIgnoreDropErrors()
	{
		if (this.profile != null)
		{
			return this.profile.getIgnoreDropErrors();
		}
		else
		{
			return false;
		}
	}

	public void toggleAutoCommit()
	{
		boolean flag = this.getAutoCommit();
		try
		{
			this.setAutoCommit(!flag);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbConnection.toggleAutoCommit()", "Error when switching autocommit to " + !flag, e);
		}
	}

	public void setAutoCommit(boolean flag)
		throws SQLException
	{
		if (!getDbSettings().supportsTransactions()) return;

		boolean old = this.getAutoCommit();
		if (old != flag)
		{
			this.sqlConnection.setAutoCommit(flag);
			fireConnectionStateChanged(PROP_AUTOCOMMIT, Boolean.toString(old), Boolean.toString(flag));
		}
	}

	/**
	 * Some DBMS (e.g. MySQL) seem to start a new transaction in default
	 * isolation mode. Which means that if the SELECT is not committed,
	 * no changes will be visible until a commit is issued.
	 * In the DbExplorer this is a problem, as the user has no way
	 * of sending a commit to end the transation if the DbExplorer
	 * uses a separate connection.
	 * The {@link workbench.gui.dbobjects.TableDataPanel} will issue
	 * a commit after retrieving the data if this method returns true.
	 *
	 * @see workbench.gui.dbobjects.TableDataPanel#doRetrieve(boolean)
	 * @see workbench.gui.dbobjects.TableDataPanel#showRowCount()
	 * @see DbSettings#selectStartsTransaction()
	 */
	public boolean selectStartsTransaction()
	{
		return getDbSettings().selectStartsTransaction();
	}

	public boolean getAutoCommit()
	{
		if (this.sqlConnection == null) return false;

		if (!getDbSettings().supportsTransactions())
		{
			return true;
		}

		try
		{
			return this.sqlConnection.getAutoCommit();
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("WbConnection.getAutoCommit()", "Error when retrieving autoCommit attribute", e);
			return false;
		}
	}

	/**
	 * Disconnect this connection.
	 * This is delegated to the Connection Manager because for certain DBMS some cleanup works needs to be done.
	 * The ConnectionMgr is the only one who knows if there are more connections
	 * around, which might influence what needs to be cleaned up
	 * <br/>
	 * (Currently this is only HSQLDB, but who knows...)
	 * This will also fire a connectionStateChanged event.
	 */
	public void disconnect()
	{
		ConnectionMgr.getInstance().disconnect(this);
		fireConnectionStateChanged(PROP_CONNECTION_STATE, CONNECTION_OPEN, CONNECTION_CLOSED);
	}

	/**
	 * This will physically close the connection to the DBMS.
	 * <br/>
	 * It will also free an resources from the DbMetadata object and
	 * shutdown the keep alive thread.
	 * <br/>
	 * Normally {@link #disconnect()} should be used.
	 * <br/>
	 * This is <b>only</b> public to allow cross-package calls in the workbench.db
	 * package (basically for the shutdown hooks)
	 *
	 * This will <b>not</b> notify the ConnectionMgr that this connection has been closed.
	 * a connectionStateChanged event will <b>not</b> be fired.
	 * @see #disconnect()
	 */
	public void shutdown()
	{
		shutdown(true);
	}

	public void shutdown(boolean withRollback)
	{
		if (this.keepAlive != null)
		{
			this.keepAlive.shutdown();
			this.keepAlive = null;
		}

		if (this.preparedStatementPool != null)
		{
			this.preparedStatementPool.done();
		}

		if (withRollback && this.profile != null && this.profile.getRollbackBeforeDisconnect() && this.sqlConnection != null)
		{
			try
			{
				this.rollback();
			}
			catch (Exception e)
			{
				LogMgr.logWarning("WbConnection.close()", "Error when calling rollback before disconnect", e);
			}
		}

		try
		{
			if (this.metaData != null) this.metaData.close();
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("WbConnection.close()", "Error when releasing metadata", th);
		}
		finally
		{
			this.metaData = null;
		}

		try
		{
			if (this.sqlConnection != null) this.sqlConnection.close();
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("WbConnection.close()", "Error when closing connection", th);
		}
		finally
		{
			this.sqlConnection = null;
		}

		LogMgr.logDebug("WbConnection.close()", "Connection " + this.getId() + " closed.");
	}

	public boolean isClosed()
	{
		return (this.sqlConnection == null);
	}

	/**
	 * Overwrite the fetch size defined in the connection profile
	 * @param size
	 */
	public void setFetchSize(int size)
	{
		if (size <= 0)
		{
			fetchSize = null;
		}
		else
		{
			fetchSize = Integer.valueOf(size);
		}
	}

	/**
	 * Return the fetch size to be used.
	 * <br/>
	 * If a fetch size has been defined using {@link #setFetchSize(int)) that size
	 * is used, otherwise the fetch size defined on the connection profile is used.
	 *
	 * @return the defined fetch size, or -1 if no fetch size was defined
	 */
	public int getFetchSize()
	{
		if (fetchSize != null)
		{
			return fetchSize.intValue();
		}
		if (getProfile() != null)
		{
			return getProfile().getFetchSize();
		}
		return -1;
	}


	/**
	 * Create a statement that produces ResultSets that
	 * are read only and forward only (for performance reasons)
	 * <br/>
	 * If the profile defined a default fetch size, this
	 * will be set as well.
	 *
	 * @throws java.sql.SQLException
	 * @see #getFetchSize()
	 */
	public Statement createStatementForQuery()
		throws SQLException
	{
		Statement stmt = null;
		if (getDbSettings().allowsExtendedCreateStatement())
		{
			stmt = this.sqlConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		}
		else
		{
			stmt = this.sqlConnection.createStatement();
		}

		try
		{
			int size = getFetchSize();
			if (size > -1) stmt.setFetchSize(size);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbConnection.createStatementForQuery()", "Error when setting the fetchSize: " + ExceptionUtil.getDisplay(e));
		}
		return stmt;
	}

	/**
	 * Create a new statement object.
	 * <br/>
	 * This is just a wrapper for java.sql.Connection.createStatement().
	 *
	 * If a default fetch size was defined in the connection profile, this is applied to
	 * the created statement.
	 *
	 * @return a Statement object
	 * @see #getFetchSize()
	 */
	public Statement createStatement()
		throws SQLException
	{
		Statement stmt = this.sqlConnection.createStatement();
		if (Settings.getInstance().getBoolProperty("workbench.sql.fetchsize.always", true))
		{
			try
			{
				int size = getFetchSize();
				if (size > -1) stmt.setFetchSize(size);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("WbConnection.createStatement()", "Error when setting the fetchSize: " + ExceptionUtil.getDisplay(e));
			}
		}
		return stmt;
	}

	public boolean supportsSavepoints()
	{
		if (this.sqlConnection == null) return false;

		try
		{
			return sqlConnection.getMetaData().supportsSavepoints();
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	public boolean useJdbcCommit()
	{
		return this.metaData.getDbSettings().useJdbcCommit();
	}

	public DbSettings getDbSettings()
	{
		return this.metaData.getDbSettings();
	}

	public DbMetadata getMetadata()
	{
		return this.metaData;
	}

	/**
	 * Wrapper around DatabaseMetadata.getSearchStringEscape() that does not throw an exception.
	 *
	 * @return the escape characters to mask wildcards in a string literal
	 */
	public String getSearchStringEscape()
	{
		// using a config property to override the driver's behaviour
		// is necessary because some Oracle drivers return the wrong escape character
		String escape = getDbSettings().getSearchStringEscape();
		if (escape != null)
		{
			return escape;
		}

		try
		{
			return this.sqlConnection.getMetaData().getSearchStringEscape();
		}
		catch (Throwable e)
		{
			// Should not happen
			return null;
		}
	}

	public String getUrl()
	{
		if (profile != null)
		{
			return profile.getUrl();
		}

		try
		{
			return this.sqlConnection.getMetaData().getURL();
		}
		catch (Throwable e)
		{
			return null;
		}
	}

	@Override
	public String toString()
	{
		return getId() + ", " + getDisplayUser() + "@" + getUrl();
	}

	private String getDisplayUser()
	{
		if (profile == null)
		{
			return getCurrentUser();
		}
		return profile.getUsername();
	}

	/**
	 * Return a readable display of a connection.
	 *
	 * This might actually send a SELECT to the database to
	 * retrieve the current user or schema.
	 *
	 * @see #getCurrentUser()
	 * @see DbMetadata#getSchemaToUse()
	 * @see DbMetadata#getCurrentCatalog()
	 */
	public String getDisplayString()
	{
		String displayString = null;
		try
		{
			DbMetadata meta = getMetadata();
			StringBuilder buff = new StringBuilder(100);
			String user = getDisplayUser();
			buff.append(ResourceMgr.getString("TxtUser"));
			buff.append('=');
			buff.append(user);

			String catalog = meta.getCurrentCatalog();
			if (catalog != null && catalog.length() > 0)
			{
				String catName = meta.getCatalogTerm();
				buff.append(", ");
				buff.append(catName == null ? "Catalog" : StringUtil.capitalize(catName));
				buff.append('=');
				buff.append(catalog);
			}

			String schema = meta.getSchemaToUse();
			if (schema != null && !schema.equalsIgnoreCase(user))
			{
				String schemaName = meta.getSchemaTerm();
				buff.append(", ");
				buff.append(schemaName == null ? "Schema" : StringUtil.capitalize(schemaName));
				buff.append('=');
				buff.append(schema);
			}

			buff.append(", URL=");
			buff.append(getUrl());
			displayString = buff.toString();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr.getDisplayString()", "Could not retrieve connection information", e);
			displayString = toString();
		}
		return displayString;
	}

	public String getJDBCVersion()
	{
		try
		{
			DatabaseMetaData jdbcmeta = getSqlConnection().getMetaData();
			int major = jdbcmeta.getJDBCMajorVersion();
			int minor = jdbcmeta.getJDBCMinorVersion();

			return major + "." + minor;
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("WbConnection.getJDBCVersion()", "Error retrieving DB version (" + ExceptionUtil.getDisplay(e) + ")");
			return "n/a";
		}
	}

	/**
	 * An exception safe version of getDatabaseProductVersion().
	 *
	 * @return the result of getDatabaseProductVersion() or an empty string if an exception occurred
	 */
	public String getDatabaseProductVersion()
	{
		try
		{
			DatabaseMetaData jdbcmeta = getSqlConnection().getMetaData();
			return jdbcmeta.getDatabaseProductVersion();
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("WbConnection.getDatabaseProductVersion()", "Error retrieving DB product ersion (" + ExceptionUtil.getDisplay(e) + ")");
			return "";
		}
	}

	/**
	 * Return the database version as reported by DatabaseMetaData.getDatabaseMajorVersion() and getDatabaseMinorVersion()
	 *
	 * @return a string with the major and minor version concatenated with a dot.
	 */
	public String getDatabaseVersion()
	{
		try
		{
			DatabaseMetaData jdbcmeta = getSqlConnection().getMetaData();
			int major = jdbcmeta.getDatabaseMajorVersion();
			int minor = jdbcmeta.getDatabaseMinorVersion();

			return major + "." + minor;
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("WbConnection.getDatabaseVersion()", "Error retrieving DB version (" + ExceptionUtil.getDisplay(e) + ")");
			return "n/a";
		}
	}

	public String getDatabaseProductName()
	{
		if (metaData == null) return "";
		return this.metaData.getProductName();
	}

	public String getOutputMessages()
	{
		if (metaData == null) return "";
		return this.metaData.getOutputMessages();
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof WbConnection)
		{
			return this.id.equals(((WbConnection)o).id);
		}
		return false;
	}

	public String getDriverVersion()
	{
		DatabaseMetaData db = null;
		try
		{
			db = this.sqlConnection.getMetaData();
			return db.getDriverVersion();
		}
		catch (Throwable e)
		{
			LogMgr.logError("WbConnection.getDriverVersion()", "Error retrieving driver version", e);
			return "n/a";
		}
	}

	/**
	 *	Returns information about the DBMS and the JDBC driver
	 *	in the XML format used for the XML export
	 */
	public StrBuffer getDatabaseInfoAsXml(StrBuffer indent)
	{
		StrBuffer dbInfo = new StrBuffer(200);
		DatabaseMetaData db = null;
		try
		{
			db = this.sqlConnection.getMetaData();
		}
		catch (Exception e)
		{
			return new StrBuffer("");
		}

		TagWriter tagWriter = new TagWriter();
		String value = null;


		tagWriter.appendTag(dbInfo, indent, "created", StringUtil.getCurrentTimestampWithTZString());

		try { value = db.getDriverName(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "jdbc-driver", cleanValue(value));

		try { value = db.getDriverVersion(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "jdbc-driver-version", cleanValue(value));

		tagWriter.appendTag(dbInfo, indent, "connection", this.getDisplayString());
		tagWriter.appendTag(dbInfo, indent, "schema", getCurrentSchema());
		tagWriter.appendTag(dbInfo, indent, "catalog", metaData.getCurrentCatalog());

		try { value = db.getDatabaseProductName(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "database-product-name", cleanValue(value));

		try { value = db.getDatabaseProductVersion(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "database-product-version", cleanValue(value));

		return dbInfo;
	}

	/**
	 *	Some DBMS have strange characters when reporting their name
	 *  This method ensures that an XML "compatible" value is returned in
	 *  getDatabaseInfoAsXml
	 */
	private String cleanValue(String value)
	{
		if (value == null) return null;
		int len = value.length();
		StringBuilder result = new StringBuilder(len);
		for (int i=0; i < len; i++)
		{
			char c = value.charAt(i);
			if ( (c > 32 && c != 127) || c == 9 || c == 10 || c == 13)
			{
				result.append(c);
			}
			else
			{
				result.append(' ');
			}
		}
		return result.toString();
	}

	/**
	 *	Some DBMS need to commit DDL (CREATE, DROP, ...) statements.
	 *	If the current connection needs a commit for a DDL, this will return true.
	 *	The metadata class reads the names of those DBMS from the Settings object!
	 */
	protected boolean getDDLNeedsCommit()
	{
		return this.metaData.getDbSettings().ddlNeedsCommit();
	}

	/**
	 * Checks if DDL statement need a commit for this connection.
	 *
	 * @return false if autocommit is on or the DBMS does not support DDL transactions
	 * @see #getDDLNeedsCommit()
	 */
	public boolean shouldCommitDDL()
	{
		if (this.getAutoCommit()) return false;
		return this.getDDLNeedsCommit();
	}


	public synchronized void addChangeListener(PropertyChangeListener l)
	{
		if (this.listeners == null) this.listeners = new ArrayList<PropertyChangeListener>();
		if (!this.listeners.contains(l)) this.listeners.add(l);
	}

	public synchronized void removeChangeListener(PropertyChangeListener l)
	{
		if (this.listeners == null) return;
		this.listeners.remove(l);
	}

	private void fireConnectionStateChanged(String property, String oldValue, String newValue)
	{
		if (this.listeners != null)
		{
			int count = this.listeners.size();
			PropertyChangeEvent evt = new PropertyChangeEvent(this, property, oldValue, newValue);
			for (int i=0; i < count; i++)
			{
				PropertyChangeListener l = this.listeners.get(i);
				l.propertyChange(evt);
			}
		}
	}

	/**
	 * Return the current catalog as returned by the JDBC driver.
	 */
	public String getCurrentCatalog()
	{
		try
		{
			return this.sqlConnection.getCatalog();
		}
		catch (SQLException e)
		{
			return null;
		}
	}

	public String getDisplayCatalog()
	{
		return currentCatalog;
	}

	/**
	 * This is called whenever the current catalog was changed.
	 *
	 * It will fire a connectionStateChanged event and will clear the object cache
	 * as the cache is schema based. Changing the catalog means changing the
	 * database in MySQL or SQL Server. If the new database has the same schemas
	 * as the old, the object cache would show invalid data.
	 *
	 * @param oldCatalog
	 * @param newCatalog
	 */
	public void catalogChanged(String oldCatalog, String newCatalog)
	{
		boolean changed = currentCatalog != null && !currentCatalog.equals(newCatalog);
		this.currentCatalog = newCatalog;
		this.getObjectCache().clear();
		if (changed)
		{
			this.fireConnectionStateChanged(PROP_CATALOG, oldCatalog, newCatalog);
		}
	}

	public void schemaChanged(String oldSchema, String newSchema)
	{
		boolean changed = currentSchema != null && !currentSchema.equals(newSchema);
		this.currentSchema = newSchema;
		if (changed)
		{
			this.fireConnectionStateChanged(PROP_SCHEMA, oldSchema, newSchema);
		}
	}

	private void initKeepAlive()
	{
		if (this.keepAlive != null)
		{
			this.keepAlive.shutdown();
			this.keepAlive = null;
		}

		if (this.profile == null) return;
		String sql = this.profile.getIdleScript();
		if (StringUtil.isBlank(sql)) return;
		long idleTime = this.profile.getIdleTime();
		if (idleTime <= 0) return;
		this.keepAlive = new KeepAliveDaemon(idleTime, this, sql);
		this.keepAlive.startThread();
	}

	public boolean isBusy()
	{
		return this.busy;
	}

	public void setBusy(boolean flag)
	{
		this.busy = flag;
		if (flag && this.keepAlive != null)
		{
			this.keepAlive.setLastDbAction(System.currentTimeMillis());
		}
	}

	@Override
	public void executionStart(WbConnection conn, Object source)
	{
		if (conn == this)
		{
			setBusy(true);
		}
	}

	/*
	 *	Fired by the SqlPanel if DB access finished
	 */
	@Override
	public void executionEnd(WbConnection conn, Object source)
	{
		if (conn == this)
		{
			setBusy(false);
		}
	}

}
