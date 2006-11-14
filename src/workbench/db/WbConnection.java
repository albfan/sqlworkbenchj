/*
 * WbConnection.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import workbench.db.report.TagWriter;
import workbench.interfaces.DbExecutionListener;
import workbench.resource.ResourceMgr;
import workbench.sql.ScriptCommandDefinition;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.ScriptParser;
import workbench.sql.preparedstatement.PreparedStatementPool;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
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
	private StringBuffer scriptError = null;
	private Connection sqlConnection;
	private DbMetadata metaData;
	private ConnectionProfile profile;
	private PreparedStatementPool preparedStatementPool;

	private List listeners;
	private DbObjectCache objectCache;

	private Method clearSettings = null;
	private Object dbAccess = null;
	private boolean doOracleClear = true;

	private boolean busy; 
	private Object busyLock = new Object();
	
	public WbConnection(String anId)
	{
		this.id = anId;
	}

	public String getId()
	{
		return this.id;
	}

	/**
	 * Create a new connection
	 */
	public WbConnection(Connection aConn)
	{
		this.setSqlConnection(aConn);
	}

	public void setProfile(ConnectionProfile aProfile)
	{
		this.profile = aProfile;
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
		if (this.objectCache == null)
		{
			this.objectCache = new DbObjectCache(this);
		}
		return this.objectCache;
	}

	public String getCurrentUser()
	{
		if (this.metaData == null) return null;
		return this.metaData.getUserName();
	}

	public boolean supportsQueryTimeout()
	{
		if (this.metaData == null) return true;
		return this.metaData.supportsQueryTimeout();
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
		String sql = profile.getPreDisconnectScript();
		runConnectScript(sql);
	}
	
	void runPostConnectScript()
	{
		if (this.profile == null) return;
		if (this.sqlConnection == null) return;
		String sql = profile.getPostConnectScript();
		runConnectScript(sql);
	}	
	
	private void runConnectScript(String sql)
	{
		if (StringUtil.isEmptyString(sql)) return;
		ScriptParser p = new ScriptParser(sql);
		Iterator itr = p.getIterator();
		Statement stmt = null;
		String command = null;
		try
		{
			stmt = this.sqlConnection.createStatement();
			this.scriptError = new StringBuffer();
			while (itr.hasNext())
			{
				command = p.getNextCommand();
				LogMgr.logDebug("WbConnection.runConnectScript()", "Executing statement: " + sql);
				stmt.execute(command);
				this.scriptError.append(ResourceMgr.getString("MsgBatchExecutingStatement"));
				this.scriptError.append(": ");
				this.scriptError.append(StringUtil.getMaxSubstring(command,250));
				this.scriptError.append("\n\n");
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbConnection.runConnectScript()", "Error executing post-connect script", e);
			this.scriptError = new StringBuffer();
			this.scriptError.append(ResourceMgr.getString("MsgBatchStatementError"));
			this.scriptError.append(": ");
			this.scriptError.append(command + "\n");
			this.scriptError.append(e.getMessage());
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}
	
	void setSqlConnection(Connection aConn)
	{
		this.sqlConnection = aConn;
		try
		{
			this.metaData = new DbMetadata(this);
			this.doOracleClear = this.metaData.isOracle();
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error initializing DB Meta Data", e);
		}
	}

	public String getWarnings()
	{
		return this.getWarnings(true);
	}

	public String getWarnings(boolean clearWarnings)
	{
		try
		{
			
			SQLWarning warn = this.getSqlConnection().getWarnings();
			if (warn == null) 
			{
				if (this.scriptError != null)
				{
					String error = this.scriptError.toString();
					if (clearWarnings) this.scriptError = null;
					return error;
				}
				return null;
			}
			
			StringBuffer msg = new StringBuffer(200);
			if (this.scriptError != null) msg.append(this.scriptError);

			String s = null;
			while (warn != null)
			{
				s = warn.getMessage();
				msg.append('\n');
				msg.append(s);
				warn = warn.getNextWarning();
			}
			if (clearWarnings) this.clearWarnings();
			return msg.toString();
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
		if (this.metaData == null) return;
		try
		{
			this.sqlConnection.clearWarnings();

			if (doOracleClear && this.metaData.isOracle())
			{
				// obviously the Oracle driver does NOT clear the warnings
				// (as discovered when looking at the source code)
				// luckily the instance on the driver which holds the
				// warnings is defined as public and thus we can
				// reset the warnings there
				// this is done via reflection so that the Oracle driver
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
							clearSettings = dbAccessClass.getDeclaredMethod("setWarnings", new Class[] {SQLWarning.class} );
						}
						catch (NoSuchMethodException e)
						{
							// newer drivers do not seem to support this any more,
							// so after the first error, we'll skip this for the rest of the session
							doOracleClear = false;
							clearSettings = null;
						}
						
					}
				}
				// the following line is equivalent to:
				//   OracleConnection con = (OracleConnection)this.sqlConnection;
				//   con.db_access.setWarnings(null);
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
		this.sqlConnection.commit();
	}

	/**
	 * Execute a rollback on the connection.
	 */
	public void rollback()
		throws SQLException
	{
		this.sqlConnection.rollback();
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
			// ignore it
		}
	}
	
	public void setAutoCommit(boolean flag)
		throws SQLException
	{
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
	 */
	public boolean selectStartsTransaction()
	{
		String key = "workbench.db." + this.metaData.getDbId() + ".select.startstransaction";
		boolean flag = Settings.getInstance().getBoolProperty(key, false);
		return flag;
	}
	
	public boolean getAutoCommit()
	{
		if (this.sqlConnection == null) return false;
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
	 *	Disconnect this connection. This is delegated to the Connection Manager
	 *	because for certain DBMS some cleanup works needs to be done.
	 *  And the ConnectionMgr is the only one who knows if there are more connections
	 *  around, which might influence what needs to be cleaned up
	 *  (Currently this is only HSQLDB, but who knows...)
	 */
	public void disconnect()
	{
		ConnectionMgr.getInstance().disconnect(this);
		if (this.preparedStatementPool != null)
		{
			this.preparedStatementPool.done();
		}
		fireConnectionStateChanged(PROP_CONNECTION_STATE, CONNECTION_OPEN, CONNECTION_CLOSED);
	}

	/**
	 *	This will actually close the connection to the DBMS.
	 *	It will also free an resources from the DbMetadata object.
	 */
	void close()
	{
		if (this.profile != null && this.profile.getRollbackBeforeDisconnect() && this.sqlConnection != null)
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
			this.metaData = null;
			if (this.sqlConnection != null) this.sqlConnection.close();
			this.sqlConnection = null;
			
			if (Settings.getInstance().getProperty("workbench.db.driver.log", null) != null)
			{
				PrintWriter pw = DriverManager.getLogWriter();
				if (pw != null) 
				{
					try { pw.close(); } catch (Throwable th) {}
				}
			}
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("WbConnection.close()", "Error when closing connection", th);
		}
	}

	public boolean isClosed()
	{
		if (this.sqlConnection == null) return true;
		try
		{
			return this.sqlConnection.isClosed();
		}
		catch (Exception e)
		{
			return true;
		}
	}

	/**
	 * Create a statement that produces ResultSets that
	 * are read only and forward only (for performance)
	 * If the profile defined a default fetch size, this
	 * will be set as well.
	 */
	public Statement createStatementForQuery()
		throws SQLException
	{
		Statement stmt = this.sqlConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		try
		{
			if (this.getProfile() != null)
			{
				int fetchSize = this.getProfile().getFetchSize();
				if (fetchSize > -1) stmt.setFetchSize(fetchSize);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbConnection.createStatementForQuery()", "Error when setting the fetchSize: " + ExceptionUtil.getDisplay(e));
		}
		return stmt;
	}

	public Statement createStatement()
		throws SQLException
	{
		return this.sqlConnection.createStatement();
	}

	public boolean useJdbcCommit()
	{
		return this.metaData.getUseJdbcCommit();
	}

	public DbMetadata getMetadata()
	{
		return this.metaData;
	}

	public String getUrl()
	{
		try
		{
			return this.sqlConnection.getMetaData().getURL();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Return a readable display of a connection. 
	 * This might actually send a SELECT to the database to 
	 * retrieve the current user or schema.
	 * @see DbMetadata#getUserName()
	 * @see DbMetadata#getSchemaToUse()
	 * @see DbMetadata#getCurrentCatalog()
	 */
	public String getDisplayString()
	{
		String displayString = null;
		try
		{
			DbMetadata meta = getMetadata();
			StringBuffer buff = new StringBuffer(100);
			String user = meta.getUserName();
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
			if (getProfile() != null)
			{
				buff.append(getProfile().getUrl());
			}
			else
			{
				buff.append(getSqlConnection().getMetaData().getURL());
			}
			displayString = buff.toString();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr.getDisplayString()", "Could not retrieve connection information", e);
			displayString = "n/a";
		}
		return displayString;
	}

	public String getDatabaseProductName()
	{
		return this.metaData.getProductName();
	}

	public String getOutputMessages()
	{
		return this.metaData.getOutputMessages();
	}

	public boolean equals(Object o)
	{
		if (o != null && o instanceof WbConnection)
		{
			return (this.id.equals(((WbConnection)o).id));
		}
		return false;
	}

	public StrBuffer getDatabaseInfoAsXml(StrBuffer indent)
	{
		return this.getDatabaseInfoAsXml(indent, null);
	}

	/**
	 *	Returns information about the DBMS and the JDBC driver
	 *	in the XML format used for the XML export
	 */
	public StrBuffer getDatabaseInfoAsXml(StrBuffer indent, String namespace)
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

		TagWriter tagWriter = new TagWriter(namespace);
		String value = null;

		tagWriter.appendTag(dbInfo, indent, "created", StringUtil.getCurrentTimestampWithTZString());

		try { value = db.getDriverName(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "jdbc-driver", cleanValue(value));

		try { value = db.getDriverVersion(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "jdbc-driver-version", cleanValue(value));

		tagWriter.appendTag(dbInfo, indent, "connection", this.getDisplayString());

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
		StringBuffer result = new StringBuffer(len);
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
	public boolean getDdlNeedsCommit()
	{
		return this.metaData.getDDLNeedsCommit();
	}

	public synchronized void addChangeListener(PropertyChangeListener l)
	{
		if (this.listeners == null) this.listeners = new ArrayList();
		this.listeners.add(l);
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
				PropertyChangeListener l = (PropertyChangeListener)this.listeners.get(i);
				l.propertyChange(evt);
			}
		}
	}

	public void catalogChanged(String oldCatalog, String newCatalog)
	{
		this.fireConnectionStateChanged(PROP_CATALOG, oldCatalog, newCatalog);
	}

	public void schemaChanged(String oldSchema, String newSchema)
	{
		this.fireConnectionStateChanged(PROP_SCHEMA, oldSchema, newSchema);
	}

	public boolean isBusy()
	{
			synchronized (busyLock)
			{
				return this.busy;
			}
	}
	
	public void setBusy(boolean flag)
	{
		synchronized (busyLock)
		{
			this.busy = flag;
		}
	}
	
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
	public void executionEnd(WbConnection conn, Object source)
	{
		if (conn == this)
		{
			setBusy(false);
		}
	}	
}
