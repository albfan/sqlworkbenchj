/*
 * ConnectionMgr.java
 *
 * Created on November 25, 2001, 4:18 PM
 */

package workbench.db;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.ClassNotFoundException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.xml.sax.InputSource;

import workbench.exception.NoConnectionException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.WbManager;

/**
 *
 * @author  thomas
 * @version
 */
public class ConnectionMgr
{
	private HashMap activeConnections = new HashMap();
	private List profiles;
	private List drivers;

	/** Creates new ConnectionMgr */
	public ConnectionMgr()
	{
	}
	
	/**
	 *	Return a connection for the given ID.
	 *	A NoConnectException is thrown if no connection
	 *	is found for that ID
	 */
	public Connection getConnection(String anId)
		throws NoConnectionException
	{
		if (this.profiles == null) this.readProfiles();
		return this.getConnection(anId, false);
	}
	
	/**
	 *	Return the connection identified by the given id.
	 *	Typically the ID is the ID of the MainWindow requesting
	 *	the connection.
	 *	If no connection is found with that ID and the selectWindow
	 *	parameter is set to true, the connection dialog
	 *	is displayed.
	 *	If still no connection is found a NoConnectionException is thrown
	 *	If a connection is created then it will be stored together
	 *	with the given ID.
	 *
	 *	@param ID the id for the connection
	 *	@param showSelectWindow if true show the connection window
	 *	@throws NoConnectionException
	 *	@see workbench.gui.MainWindow#getWindowId()
	 *	@see #releaseConnection(String)
	 *	@see #disconnectAll()
	 */
	public Connection getConnection(String anId, boolean showSelectWindow)
		throws NoConnectionException
	{
		Connection conn = (Connection)this.activeConnections.get(anId);
		if (conn == null && showSelectWindow)
		{
			//conn = this.selectConnection();
			if (conn != null) this.activeConnections.put(anId, conn);
		}
		if (conn == null)
		{
			throw new NoConnectionException(ResourceMgr.getString(ResourceMgr.ERROR_NO_CONNECTION_AVAIL));
		}
		return conn;
	}

	public Connection getConnection(String anId, ConnectionProfile aProfile)
		throws ClassNotFoundException, SQLException
	{
		Connection conn = (Connection)this.activeConnections.get(anId);
		if (conn != null) return conn;
		
		Class.forName(aProfile.getDriverclass());
		conn  = DriverManager.getConnection(aProfile.getUrl(), aProfile.getUsername(), aProfile.getPassword());
		this.activeConnections.put(anId, aProfile);
		
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
			this.drivers = new ArrayList();
			int i=0;
			String name = null;
			String classname = null;
			String lib = null;
			Settings sett = WbManager.getSettings();
			do
			{
				try
				{
					name = sett.getDriverName(i);
					classname = sett.getDriverClass(i);
					lib = sett.getDriverName(i);
					this.drivers.add(new DbDriver(name, classname, lib));
					i++;
				}
				catch (NoSuchElementException e)
				{
					name = null;
				}
			}
			while (name != null);
		}
		return Collections.unmodifiableList(this.drivers);
	}

	public List getProfiles()
	{
		if (this.profiles == null) this.readProfiles();
		return this.profiles;
	}
	
	private void saveDrivers()
	{
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
			if (db == null)
			{
				db = data.getURL();
			}
			buff.append(db);
			displayString = buff.toString();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr", "Could not retrieve connection information", e);
			displayString = "n/a";
		}
		return displayString;
	}

	public ConnectionProfile selectConnection()
	{
		ConnectionProfile result = null;
		result = this.getProfile(WbManager.getSettings().getLastConnection());
		return result;
	}
	
	
	/**
	 *	Reads a connection profile from the applications settings.
	 *	The connection is is identified by the given name and is
	 *	assigned to the given id (=MainWindow)
	 */
	public ConnectionProfile getProfile(int anId)
	{
		if (this.profiles == null) this.readProfiles();
		try
		{
			return (ConnectionProfile)this.profiles.get(anId);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return null;
		}
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
	
	public void readProfiles()
	{
		this.profiles = new ArrayList();
		Settings set = WbManager.getSettings();
		int count = set.getConnectionCount();
		String driver;
		String url;
		String pwd;
		String user;
		String name;
		for (int i=0; i < count; i++)
		{
			driver = set.getConnectionDriver(i);
			url = set.getConnectionUrl(i);
			user = set.getConnectionUsername(i);
			pwd = set.getConnectionPassword(i);
			name = set.getConnectionName(i);
			ConnectionProfile info = new ConnectionProfile(driver, url, user, pwd);
			info.setName(name);
			this.profiles.add(i, info);
		}
	}
	
}
