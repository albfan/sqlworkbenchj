/*
 * WbConnection.java
 *
 * Created on 6. Juli 2002, 19:36
 */

package workbench.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import workbench.WbManager;
import workbench.log.LogMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbConnection
{
	private boolean oraOutput = false;
  private String id;
	private Connection sqlConnection;
	private DbMetadata metaData;
	private ConnectionProfile profile;

	private boolean ddlNeedsCommit;
	private boolean ignoreDropErrors = false;
	
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
	}
	
	public ConnectionProfile getProfile()
	{
		return this.profile;
	}
	
	public void reconnect()
	{
		try
		{
			WbManager.getInstance().getConnectionMgr().reconnect(this);
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
				// does not need to be present.
				
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
				// this is equivalent to 
				// OracleConnection con = (OracleConnection)this.sqlConnection;
				// con.db_access.setWarnings(null);
				clearSettings.invoke(dbAccess, new Object[] { null });
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
	 * Any exceptions are ignored. What should we do anyway?
	 */
	public void rollback()
	{
		try
		{
			this.sqlConnection.rollback();
		}
		catch (Throwable th)
		{
			LogMgr.logError("WbConnection.rollback()", "Rollback failed!", th);
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

	public void close()
	{
		//this.rollback();
		
		try
		{
			this.sqlConnection.close();
			this.sqlConnection = null;
		}
		catch (Throwable th)
		{
			LogMgr.logDebug("WbConnection.close()", "Error when closing connection", th);
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

	public boolean cancelNeedsReconnect()
	{
		return this.metaData.cancelNeedsReconnect();
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
		return this.metaData.productName;
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
	
	public boolean getDdlNeedsCommit()
	{
		return this.metaData.getDDLNeedsCommit();
	}
	
}
