/*
 * ConnectionMgr.java
 *
 * Created on November 25, 2001, 4:18 PM
 */

package workbench.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import workbench.exception.NoConnectionException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.WbManager;
import workbench.util.WbPersistence;

/**
 *
 * @author  thomas
 * @version
 */
public class ConnectionMgr
{
	private HashMap activeConnections = new HashMap();
	private HashMap profiles;
	private List drivers;

	/** Creates new ConnectionMgr */
	public ConnectionMgr()
	{
	}
	

	/**
	 *	Return a connection for the given windowId
	 *	@param ID the windowId for window which is requesting the connection
	 *	@throws NoConnectionException
	 *	@see workbench.gui.MainWindow#getWindowId()
	 *	@see #releaseConnection(String)
	 *	@see #disconnectAll()
	 */
	public Connection getConnection(String aWindowId)
		throws NoConnectionException
	{
		Connection conn = (Connection)this.activeConnections.get(aWindowId);
		if (conn == null)
		{
			throw new NoConnectionException(ResourceMgr.getString(ResourceMgr.ERROR_NO_CONNECTION_AVAIL));
		}
		return conn;
	}

	/**
	 *	Return a new connection specified by the profile, for the
	 *	given window id
	 */
	public Connection getConnection(String aWindowId, ConnectionProfile aProfile)
		throws ClassNotFoundException, SQLException
	{
		this.disconnect(aWindowId);
		
		Class.forName(aProfile.getDriverclass());
		Connection conn  = DriverManager.getConnection(aProfile.getUrl(), aProfile.getUsername(), aProfile.decryptPassword());
		conn.setAutoCommit(aProfile.getAutocommit());
		this.activeConnections.put(aWindowId, conn);
		
		return conn;
	}
	
	/**
	 *	Returns a List of registered drivers.
	 *	This list is read from the workbench.settings file
	 */
	public List getDrivers()
	{
		if (this.drivers == null)
		{
			this.readDrivers();
		}
		return Collections.unmodifiableList(this.drivers);
	}

	public Map getProfiles()
	{
		if (this.profiles == null) this.readProfiles();
		return this.profiles;
	}
	
	/**
	 *	Return a readable display of a connection
	 */
	public static String getDisplayString(Connection con)
	{
		String displayString = null;
		
		try
		{
			DatabaseMetaData data = con.getMetaData();
			StringBuffer buff = new StringBuffer(data.getDatabaseProductName());
			buff.append(" - ");
			String db = con.getCatalog();
			buff.append(data.getUserName());
			buff.append('@');
			String url = data.getURL().substring(5);
			buff.append(url);
			displayString = buff.toString();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr", "Could not retrieve connection information", e);
			displayString = "n/a";
		}
		return displayString;
	}

	/**
	 *	Disconnects all connections
	 */
	public void disconnectAll()
	{
		Iterator itr = this.activeConnections.keySet().iterator();
		while (itr.hasNext())
		{
			String key = itr.next().toString();
			this.disconnect(key);
			this.activeConnections.put(key, null);
		}
	}
	
	/**
	 *	Disconnect the connection with the given key
	 */
	public void disconnect(String anId)
	{
		try
		{
			Connection con = (Connection)this.activeConnections.get(anId);
			if (con != null) con.close();
			this.activeConnections.put(anId, null);
		}
		catch (Exception e)
		{
			LogMgr.logError(this, ResourceMgr.getString(ResourceMgr.ERROR_DISCONNECT) + " " + anId, e);
		}
	}
	
	public String toString()
	{
		return this.getClass().getName();
	}
	
	public void writeSettings()
	{
		this.saveXmlProfiles();
		this.saveDrivers();
	}
	
	private void saveDrivers()
	{
		WbPersistence.writeObject(this.drivers, "WbDrivers.xml");
	}
	
	private void readDrivers()
	{
		Object result = WbPersistence.readObject("WbDrivers.xml");
		if (result instanceof Collection)
		{
			Iterator itr = ((Collection)result).iterator();
			this.drivers = new ArrayList();
			while (itr.hasNext())
			{
				DbDriver driv = (DbDriver)itr.next();
				this.drivers.add(driv);
			}
		}
	}
	
	public void readProfiles()
	{
		this.readXmlProfiles();
	}

	public void saveXmlProfiles()
	{
		if (this.profiles != null)
		{
			WbPersistence.writeObject(new ArrayList(this.profiles.values()), "WbProfiles.xml");
		}
	}
	
	public void readXmlProfiles()
	{
		Object result = WbPersistence.readObject("WbProfiles.xml");
		if (result instanceof Collection)
		{
			Iterator itr = ((Collection)result).iterator();
			this.profiles = new HashMap();
			while (itr.hasNext())
			{
				ConnectionProfile prof = (ConnectionProfile)itr.next();
				this.profiles.put(prof.getName(), prof);
			}
		}
		else if (result instanceof Object[])
		{
			Object[] l = (Object[])result;
			this.profiles = new HashMap(20);
			for (int i=0; i < l.length; i++)
			{
				ConnectionProfile prof = (ConnectionProfile)l[i];
				this.profiles.put(prof.getName(), prof);
			}
		}
	}

}
