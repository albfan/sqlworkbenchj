/*
 * WbConnection.java
 *
 * Created on 6. Juli 2002, 19:36
 */

package workbench.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.WbManager;
import workbench.db.report.TagWriter;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbConnection
{
  private String id;
	private Connection sqlConnection;
	private DbMetadata metaData;
	private ConnectionProfile profile;

	private boolean ddlNeedsCommit;
	private boolean cancelNeedsReconnect = false;
	
	private List listeners;

	/** Creates a new instance of WbConnection */
	public WbConnection(String anId)
	{
		this.id = anId;
	}

	public String getId()
	{
		return this.id;
	}

	public WbConnection(Connection aConn)
	{
		this.setSqlConnection(aConn);
	}

	public void setProfile(ConnectionProfile aProfile)
	{
		this.profile = aProfile;
		String driverClass = aProfile.getDriverclass();
		List drivers = Settings.getInstance().getCancelWithReconnectDrivers();
		if (drivers.contains(driverClass))
		{
			this.cancelNeedsReconnect = true;
		}
	}

	public ConnectionProfile getProfile()
	{
		return this.profile;
	}

	public void reconnect()
	{
		try
		{
			ConnectionMgr.getInstance().reconnect(this);
		}
		catch (Exception e)
		{
			LogMgr.logError("WbConnection.reconnect()", "Error when reconnecting", e);
		}
	}

	void setSqlConnection(Connection aConn)
	{
		this.sqlConnection = aConn;
		try
		{
			this.metaData = new DbMetadata(this);
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error initializing DB Meta Data", e);
		}
	}

	private Method clearSettings = null;
	private Object dbAccess = null;

	public String getWarnings()
	{
		return this.getWarnings(true);
	}

	public String getWarnings(boolean clearWarnings)
	{
		try
		{
			SQLWarning warn = this.getSqlConnection().getWarnings();
			if (warn == null) return null;

			StringBuffer msg = new StringBuffer(200);
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
		try
		{
			this.sqlConnection.clearWarnings();
			if (this.metaData.isOracle())
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
						clearSettings = dbAccessClass.getDeclaredMethod("setWarnings", new Class[] {SQLWarning.class} );
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

	public void setAutoCommit(boolean aFlag)
		throws SQLException
	{
		this.sqlConnection.setAutoCommit(aFlag);
	}

	public boolean getAutoCommit()
	{
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
		ConnectionMgr.getInstance().disconnect(this.id);
	}

	/**
	 *	This will actually close the connection to the DBMS.
	 *	It will also free an resources from the DbMetadata object.
	 */
	void close()
	{
		if (this.profile != null && this.profile.getRollbackBeforeDisconnect())
		{
			try
			{
				this.rollback();
			}
			catch (Exception e)
			{
				LogMgr.logWarning("WbConnection.close()", "Error reported when doing rollback before disconnect", e);
			}
		}
		
		try
		{
			this.metaData.close();
			this.metaData = null;
			this.sqlConnection.close();
			this.sqlConnection = null;
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("WbConnection.close()", "Error when closing connection", th);
		}
	}

	public boolean isClosed()
		throws SQLException
	{
		return this.sqlConnection.isClosed();
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

	public boolean cancelNeedsReconnect()
	{
		return this.cancelNeedsReconnect;
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

	public String getDisplayString()
	{
		return ConnectionMgr.getDisplayString(this);
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
		tagWriter.appendTag(dbInfo, indent, "jdbc-driver", value);
		
		try { value = db.getDriverVersion(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "jdbc-driver-version", value);
		
		tagWriter.appendTag(dbInfo, indent, "connection", this.getDisplayString());

		try { value = db.getDatabaseProductName(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "database-product-name", value);

		try { value = db.getDatabaseProductVersion(); } catch (Throwable th) { value = "n/a"; }
		tagWriter.appendTag(dbInfo, indent, "database-product-version", value);

		return dbInfo;
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

	public void addChangeListener(ChangeListener l)
	{
		if (this.listeners == null) this.listeners = new ArrayList();
		this.listeners.add(l);
	}

	public void removeChangeListener(ChangeListener l)
	{
		if (this.listeners == null) return;
		this.listeners.remove(l);
	}

	private void fireConnectionStateChanged()
	{
		if (this.listeners != null)
		{
			int count = this.listeners.size();
			ChangeEvent evt = new ChangeEvent(this);
			for (int i=0; i < count; i++)
			{
				ChangeListener l = (ChangeListener)this.listeners.get(i);
				l.stateChanged(evt);
			}
		}
	}

	public void connectionStateChanged()
	{
		this.fireConnectionStateChanged();
	}

}