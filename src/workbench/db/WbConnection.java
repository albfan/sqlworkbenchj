/*
 * WbConnection.java
 *
 * Created on 6. Juli 2002, 19:36
 */

package workbench.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.exception.NoConnectionException;
import workbench.log.LogMgr;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class WbConnection
{

	//private Connection sqlConnection;
	private Connection sqlConnection;
	private DbMetadata metaData;

	/** Creates a new instance of WbConnection */
	public WbConnection()
	{
	}

	public WbConnection(Connection aConn)
	{
		this.setSqlConnection(aConn);
	}
	
	void setSqlConnection(Connection aConn) 
	{ 
		this.sqlConnection = aConn;
		try
		{
			this.metaData = new DbMetadata(this);
		}
		catch (SQLException e)
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
		throws SQLException
	{
		this.rollback();
		this.sqlConnection.close();
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

	public DbMetadata getMetadata()
	{
		return this.metaData;
	}

	public String getDisplayString()
	{
		return ConnectionMgr.getDisplayString(this);
	}
}
