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
 * @author  thomas.kellerer@web.de
 */
public class WbConnection
{
	
	//private Connection sqlConnection;
	private Connection sqlConnection;
	
	/** Creates a new instance of WbConnection */
	public WbConnection()
	{
	}
	
	void setSqlConnection(Connection aConn) { this.sqlConnection = aConn; }
	
	public Connection getSqlConnection()
	{
		return this.sqlConnection;
	}

	public void close()
		throws SQLException
	{
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
	
}
