/*
 * WbConnection.java
 *
 * Created on 6. Juli 2002, 19:36
 */

package workbench.db;

import java.sql.Connection;
import java.sql.SQLException;
import voodoosoft.jroots.data.CConnection;
import voodoosoft.jroots.data.CConnectionNotAvailableException;
import workbench.log.LogMgr;

/**
 *
 * @author  thomas.kellerer@web.de
 */
public class WbConnection
{
	
	//private Connection sqlConnection;
	private CConnection cConnection;
	
	/** Creates a new instance of WbConnection */
	public WbConnection(CConnection aConn)
	{
		this.cConnection = aConn;
	}
	
	public CConnection getJrootsConnection()
	{
		return this.cConnection;
	}
	//public void setSqlConnection(Connection aConn) { this.sqlConnection = aConn; }
	public Connection getSqlConnection()
	{
		if (this.cConnection != null)
		{
			try
			{
				return this.cConnection.getConnection();
			}
			catch (CConnectionNotAvailableException e)
			{
				LogMgr.logInfo(this, "getSqlConnection() - could not retrieve connection", e);
			}
		}
		return null;
	}

	public void close()
		throws SQLException
	{
		this.cConnection.close();
	}
	
}
