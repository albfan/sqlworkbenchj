/*
 * OracleObjectCompiler.java
 *
 * Created on March 30, 2004, 3:05 PM
 */

package workbench.db.oracle;

import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.WbConnection;

/**
 *
 * @author  workbench@kellerer.org
 */
public class OracleObjectCompiler
{
	private WbConnection dbConnection;
	private Statement stmt;
	private String lastError = null;
	
	public OracleObjectCompiler(WbConnection conn)
		throws SQLException
	{
		this.dbConnection = conn;
		this.stmt = this.dbConnection.createStatement();
	}

	public void close()
	{
		if (this.stmt != null)
		{
			try { stmt.close(); } catch (Exception ignore) {}
		}
	}
	
	public String getLastError()
	{
		return this.lastError;
	}
	
	public boolean compileObject(String name, String type)
	{
		String sql = "ALTER " + type + " " + name + " COMPILE";
		this.lastError = null;
		try
		{
			this.stmt.executeUpdate(sql);
			return true;
		}
		catch (SQLException e)
		{
			this.lastError = e.getMessage();
			return false;
		}
	}
	
}
