/*
 * WbConnection.java
 *
 * Created on 6. Juli 2002, 19:36
 */

package workbench.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.WbManager;
import workbench.exception.NoConnectionException;
import workbench.log.LogMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbConnection
{
	private boolean oraOutput = false;
  
	private Connection sqlConnection;
	private DbMetadata metaData;
	private ConnectionProfile profile;
	
	/** Creates a new instance of WbConnection */
	public WbConnection()
	{
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
			LogMgr.logError(this, "Rollback failed!", th);
		}
	}

	public void setAutoCommit(boolean aFlag)
		throws SQLException
	{
		this.sqlConnection.setAutoCommit(aFlag);
	}

	public boolean getAutoCommit()
		throws SQLException
	{
		return this.sqlConnection.getAutoCommit();
	}

	public void close()
	{
		try
		{
			this.rollback();
		}
		catch (Throwable th)
		{
			// we will ignore this. What can we do :-)
		}
		try
		{
			this.sqlConnection.close();
		}
		catch (Throwable th)
		{
			// see above...
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

}
