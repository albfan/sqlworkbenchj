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
import java.sql.Connection;
import java.sql.DriverManager;
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
	private HashMap activeConnections;
	private List profiles;
	private List drivers;

	/** Creates new ConnectionMgr */
	public ConnectionMgr()
	{
		this.activeConnections = new HashMap();
		this.readProfiles();
	}
	
	/**
	 *	Return a connection for the given ID.
	 *	A NoConnectException is thrown if no connection
	 *	is found for that ID
	 */
	public WbConnection getConnection(String anId)
		throws NoConnectionException
	{
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
	public WbConnection getConnection(String anId, boolean showSelectWindow)
		throws NoConnectionException
	{
		WbConnection conn = (WbConnection)this.activeConnections.get(anId);
		if (conn == null && showSelectWindow)
		{
			conn = this.selectConnection();
			if (conn != null) this.activeConnections.put(anId, conn);
		}
		if (conn == null)
		{
			throw new NoConnectionException(ResourceMgr.getString(ResourceMgr.ERROR_NO_CONNECTION_AVAIL));
		}
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
	
	private void saveDrivers()
	{
	}
	
	public WbConnection selectConnection()
	{
		WbConnection result = null;
		
		try
		{
			ConnectionInfo info = this.getProfile(WbManager.getSettings().getLastConnection());
			Class.forName(info.getDriverclass());
			Connection conn  = DriverManager.getConnection(info.getUrl(), info.getUsername(), info.getPassword());
			result = new WbConnection(conn);
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error creating connection", e);
		}
		return result;
		
	}
	
	
	/**
	 *	Reads a connection from the applications settings.
	 *	The connection is is identified by the given name and is
	 *	assigned to the given id (=MainWindow)
	 */
	public ConnectionInfo getProfile(int anId)
	{
		try
		{
			return (ConnectionInfo)this.profiles.get(anId);
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
		//throws FileNotFoundException
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
			ConnectionInfo info = new ConnectionInfo(driver, url, user, pwd);
			this.profiles.add(i, info);
		}
		/*
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(aFilename));
		XMLDecoder de = new XMLDecoder(in);
		boolean finished = false;
		while (!finished)
		{
			try
			{
				ConnectionInfo i = (ConnectionInfo)de.readObject();
			}
			catch (ClassCastException ce)
			{
				LogMgr.logError(this, "Wrong class in " + aFilename, ce);
			}
			catch (ArrayIndexOutOfBoundsException ex)
			{
				de.close();
				finished = true;
			}
		}
		*/
	}
	
	public void saveConnectionInfo(String aFilename)
		throws FileNotFoundException
	{
		/*
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(aFilename));
		XMLEncoder en = new XMLEncoder(out);
		Iterator itr = this.connections.values().iterator();
		while (itr.hasNext())
		{
			en.writeObject(itr.next());
		}
		en.close();
		*/
		
	}
	
	
	public static void main(String args[])
	{
		try
		{
//			System.out.println(System.getProperty("user.dir"));
//			ConnectionsHandler handler = new ConnectionsHandlerImpl();
//			org.xml.sax.EntityResolver resolver = new org.xml.sax.helpers.DefaultHandler();
//			
//			ConnectionsParser parser = new ConnectionsParser(handler,resolver);
			BufferedReader in = new BufferedReader(new FileReader("/home/thomas/projects/java/jworkbench/src/workbench/connections.xml"));
//			
//			parser.parse(new InputSource(in));
     javax.xml.parsers.DocumentBuilderFactory builderFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
     javax.xml.parsers.DocumentBuilder builder = builderFactory.newDocumentBuilder();
     org.w3c.dom.Document document = builder.parse (new org.xml.sax.InputSource (in));
//     ConnectionsScanner scanner = new ConnectionsScanner (document);
//     scanner.visitDocument();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
