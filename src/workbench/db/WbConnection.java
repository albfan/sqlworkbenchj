/*
 * WbConnection.java
 *
 * Created on November 25, 2001, 6:14 PM
 */

package workbench.db;

import java.sql.DatabaseMetaData;
import workbench.log.LogMgr;

/**
 *	Wrapper class for a java.sql.Connection which provides
 *	an enhanced toString() method and is used instead of
 *	java.sql.Connection throughout the JWorkbench
 *
 * @author  thomas
 * @version $Revision: 1.2 $  
 */
public class WbConnection
	implements java.sql.Connection
{
	private java.sql.Connection sqlConnection;
	private String displayString = null;
	
	/** Creates new WbConnection */
	public WbConnection(java.sql.Connection aConn)
	{
		if (aConn == null) throw new NullPointerException("Connection may not be null!");
		this.sqlConnection = aConn;
	}

	public String getDisplayString()
	{
		if (this.displayString == null)
		{
			try
			{
				DatabaseMetaData data = this.sqlConnection.getMetaData();
				StringBuffer buff = new StringBuffer(data.getDatabaseProductName());
				buff.append(" - ");
				String db = this.sqlConnection.getCatalog();
				buff.append(data.getUserName());
				buff.append('@');
				if (db == null)
				{
					db = data.getURL();
				}
				buff.append(db);
				this.displayString = buff.toString();
			}
			catch (Exception e)
			{
				LogMgr.logError(this, "Could not retrieve connection information", e);
				this.displayString = "n/a";
			}
		}
		return this.displayString;
	}
	
	public String toString() 
	{
		return this.getDisplayString();
	}
	
	public void close() throws java.sql.SQLException
	{
		this.sqlConnection.close();
	}
	
	public void setAutoCommit(boolean param) throws java.sql.SQLException
	{
		this.sqlConnection.setAutoCommit(param);
	}
	
	public java.sql.SQLWarning getWarnings() throws java.sql.SQLException
	{
		return this.sqlConnection.getWarnings();
	}
	
	public java.lang.String getCatalog() throws java.sql.SQLException
	{
		return this.sqlConnection.getCatalog();
	}
	
	public void setTypeMap(java.util.Map map) throws java.sql.SQLException
	{
		this.sqlConnection.setTypeMap(map);
	}
	
	public java.util.Map getTypeMap() throws java.sql.SQLException
	{
		return this.sqlConnection.getTypeMap();
	}
	
	public int getTransactionIsolation() throws java.sql.SQLException
	{
		return this.sqlConnection.getTransactionIsolation();
	}
	
	public boolean isReadOnly() throws java.sql.SQLException
	{
		return this.sqlConnection.isReadOnly();
	}
	
	public java.sql.DatabaseMetaData getMetaData() throws java.sql.SQLException
	{
		return this.sqlConnection.getMetaData();
	}
	
	public void clearWarnings() throws java.sql.SQLException
	{
		this.sqlConnection.clearWarnings();
	}
	
	public java.lang.String nativeSQL(java.lang.String str) throws java.sql.SQLException
	{
		return this.sqlConnection.nativeSQL(str);
	}
	
	public java.sql.PreparedStatement prepareStatement(java.lang.String str, int param, int param2) throws java.sql.SQLException
	{
		return this.sqlConnection.prepareStatement(str, param, param2);
	}
	
	public void setTransactionIsolation(int param) throws java.sql.SQLException
	{
		this.sqlConnection.setTransactionIsolation(param);
	}
	
	public void setReadOnly(boolean param) throws java.sql.SQLException
	{
		this.sqlConnection.setReadOnly(param);
	}
	
	public void setCatalog(java.lang.String str) throws java.sql.SQLException
	{
		this.sqlConnection.setCatalog(str);
		this.getDisplayString();
	}
	
	public boolean isClosed() throws java.sql.SQLException
	{
		return this.sqlConnection.isClosed();
	}
	
	public java.sql.Statement createStatement() throws java.sql.SQLException
	{
		return this.sqlConnection.createStatement();
	}
	
	public java.sql.Statement createStatement(int param, int param1) throws java.sql.SQLException
	{
		return this.sqlConnection.createStatement(param, param1);
	}
	
	public java.sql.PreparedStatement prepareStatement(java.lang.String str) throws java.sql.SQLException
	{
		return this.sqlConnection.prepareStatement(str);
	}
	
	public boolean getAutoCommit() throws java.sql.SQLException
	{
		return this.sqlConnection.getAutoCommit();
	}
	
	public java.sql.CallableStatement prepareCall(java.lang.String str) throws java.sql.SQLException
	{
		return this.sqlConnection.prepareCall(str);
	}
	
	public void commit() throws java.sql.SQLException
	{
		this.sqlConnection.commit();
	}
	
	public java.sql.CallableStatement prepareCall(java.lang.String str, int param, int param2) throws java.sql.SQLException
	{
		return this.sqlConnection.prepareCall(str, param, param2);
	}
	
	public void rollback() throws java.sql.SQLException
	{
		this.sqlConnection.rollback();
	}
	
}
