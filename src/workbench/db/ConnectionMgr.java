/*
 * ConnectionMgr.java
 *
 * Created on November 25, 2001, 4:18 PM
 */
package workbench.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.*;
import workbench.WbManager;

import workbench.exception.NoConnectionException;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.WbCipher;
import workbench.util.WbPersistence;


/**
 * @author  workbench@kellerer.org
 * @version
 */
public class ConnectionMgr
{
	//private WbConnection currentConnection;
	private HashMap activeConnections = new HashMap();
	
	private HashMap profiles;
	private List drivers;
	private boolean profilesChanged;
	
	/** Creates new ConnectionMgr */
	public ConnectionMgr()
	{
	}
	
	/**
	 *	Return a new connection specified by the profile
	 */
	public WbConnection getConnection(ConnectionProfile aProfile, String anId)
		throws ClassNotFoundException, SQLException, NoConnectionException
	{
		this.disconnect(anId);
		
		WbConnection conn = new WbConnection();
		Connection sql = this.connect(aProfile);
		conn.setSqlConnection(sql);
		conn.setProfile(aProfile.createCopy());
		this.activeConnections.put(anId, conn);
		
		return conn;
	}

	Connection connect(ConnectionProfile aProfile)
		throws ClassNotFoundException, SQLException, NoConnectionException
	{
		// The DriverManager refuses to use a driver which was not loaded
		// from the system classloader, so the connection has to be 
		// established directly from the driver.
		String drvName = aProfile.getDriverclass();
		DbDriver drv = this.findDriver(drvName);
		if (drv == null)
		{
			throw new NoConnectionException("Driver class not registered");
		}
		try
		{
			Connection sql = drv.connect(aProfile.getUrl(), aProfile.getUsername(), aProfile.decryptPassword());
		
			try
			{
				sql.setAutoCommit(aProfile.getAutocommit());
			}
			catch (Throwable th)
			{
				// some drivers do not support this, so
				// we just ignore the error :-)
        LogMgr.logInfo("ConnectionMgr.connect()", "Driver (" + drv.getDriverClass() + ") does not support the autocommit property!");
				if (th.getMessage() != null)
				{
					LogMgr.logInfo("ConnectionMgr.connect()", "(" + th.getMessage() + ")");
				}					
			}
			return sql;
		}
		catch (Exception e)
		{
			throw new NoConnectionException(e.getMessage());
		}
	}
	
	public void reconnect(WbConnection aConn)
		throws ClassNotFoundException, SQLException, NoConnectionException
	{
    aConn.close();
    // use the stored profile to reconnect as the SQL connection
    // does not contain information about the username & password
    Connection sql = this.connect(aConn.getProfile());
    aConn.setSqlConnection(sql);
	}
	
	public DbMetadata getMetaDataForConnection(Connection aConn)
	{
		Iterator itr = this.activeConnections.entrySet().iterator();
		while (itr.hasNext())
		{
			java.util.Map.Entry e = (Map.Entry)itr.next();
			WbConnection c = (WbConnection)e.getValue();
			if (c.getSqlConnection().equals(aConn)) 
			{
				return c.getMetadata();
			}
		}
		return null;
	}
	
	public DbDriver findDriver(String drvClassName)
	{
		if (this.drivers == null)
		{
			this.readDrivers();
		}
    DbDriver db = null;
     
    try
    {
      for (int i=0; i < this.drivers.size(); i ++)
      {
        db = (DbDriver)this.drivers.get(i);
        if (db.getDriverClass().equals(drvClassName)) return db;
      }
      
      // not found --> maybe it's present in the normal classpath...
      Class drvcls = Class.forName(drvClassName);
      Driver drv = (Driver)drvcls.newInstance();
      db = new DbDriver(drv);
    }
    catch (Exception cnf)
    {
      LogMgr.logError("ConnectionMgr.findDriver()", "Error when searching for driver " + drvClassName, cnf);
      db = null;
    }
		return db;
	}
	
	/**
	 *	Returns a List of registered drivers.
	 *	This list is read from WbDrivers.xml
	 */
	public List getDrivers()
	{
		if (this.drivers == null)
		{
			this.readDrivers();
		}
		return this.drivers;
	}
	
	/**
	 *	Return a list of driverclasses
	 */
	public List getDriverClasses()
	{
		if (this.drivers == null) this.readDrivers();
		ArrayList result = new ArrayList();
		String drvClass;
		
		for (int i=0; i < this.drivers.size(); i++)
		{
			drvClass = ((DbDriver)this.drivers.get(i)).getDriverClass();
			if (!result.contains(drvClass))
			{
				result.add(drvClass);
			}
		}
		return result;
	}

	public void setDrivers(List aDriverList)
	{
		this.drivers = aDriverList;
	}

	/**
	 *	Returns a Map with the current profiles.
	 *	The key to the map is the profile name, the value is the actual profile
	 */
	public Map getProfiles()
	{
		if (this.profiles == null) this.readProfiles();
		return this.profiles;
	}

	/**
	 *	Returns a descriptive String for the given connection.
	 */
	public static String getDisplayString(WbConnection con)
	{
		try
		{
			return getDisplayString(con.getSqlConnection());
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr", "getDisplayString() - No java.sql.Connection!", e);
			return "n/a";
		}
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
			StringBuffer buff = new StringBuffer(100);
			buff.append(ResourceMgr.getString("TxtUser"));
			buff.append('=');
			buff.append(data.getUserName());
			
			String catName = data.getCatalogTerm();
			String catalog = con.getCatalog();
			if (catName == null) catName = "Catalog";
			if (catName != null && catName.length() > 0 &&
			    catalog != null && catalog.length() > 0)
			{
				buff.append(", ");
				buff.append(catName);
				buff.append('=');
				buff.append(catalog);
			}
			
			buff.append(", URL=");
			buff.append(data.getURL());
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
			String key = (String)itr.next();
			this.disconnect(key);
		}
	}
	
	/**
	 *	Disconnect the connection with the given (window) id
	 */
	public void disconnect(String anId)
	{
		try
		{
			WbConnection con = (WbConnection)this.activeConnections.get(anId);
			if (con != null && !con.isClosed()) con.close();
			this.activeConnections.put(anId, null);
		}
		catch (Exception e)
		{
			LogMgr.logError(this, ResourceMgr.getString(ResourceMgr.ERROR_DISCONNECT), e);
		}
	}
	
	public String toString()
	{
		return this.getClass().getName();
	}
	
	public void writeSettings()
	{
		this.saveProfiles();
		this.saveDrivers();
	}
	
	public void saveDrivers()
	{
		WbPersistence.writeObject(this.drivers, WbManager.getSettings().getDriverConfigFileName());
	}
	
	private void readDrivers()
	{
		try
		{
			Object result = WbPersistence.readObject(WbManager.getSettings().getDriverConfigFileName());
			if (result == null) 
			{
				this.drivers = new ArrayList();
			}
			else if (result instanceof ArrayList)
			{
				this.drivers = (ArrayList)result;
			}
			
			// now read the templates and append them to the driver list
			InputStream in = this.getClass().getResourceAsStream("DriverTemplates.xml");
			ArrayList templates = (ArrayList)WbPersistence.readObject(in);
			for (int i=0; i < templates.size(); i++)
			{
				Object drv = templates.get(i);
				if (!this.drivers.contains(drv))
				{
					this.drivers.add(drv);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning(this, "Could not load driver definitions!");
			this.drivers = Collections.EMPTY_LIST;
		}
	}

	public void readProfiles()
	{
		Object result = WbPersistence.readObject(WbManager.getSettings().getProfileFileName());
		if (result instanceof Collection)
		{
			this.setProfiles((Collection)result);
			this.resetProfiles();
		}
		else if (result instanceof Object[])
		{
			Object[] l = (Object[])result;
			this.profiles = new HashMap(20);
			for (int i=0; i < l.length; i++)
			{
				ConnectionProfile prof = (ConnectionProfile)l[i];
				prof.reset();
				this.profiles.put(prof.getIdentifier(), prof);
			}
		}
	}
	
	
	/**
	 *	Reset the changed status on the profiles.
	 *	Called after saving the profiles.
	 */
	private void resetProfiles()
	{
		if (this.profiles != null)
		{
      Iterator values = this.profiles.values().iterator();
      while (values.hasNext())
      {
        ConnectionProfile profile = (ConnectionProfile)values.next();
        profile.reset();
      }
      this.profilesChanged = false;
		}
	}
	
	/**
	 *	Make the profile list persistent.
	 *	This will also reset the changed flag for any modified or new 
	 *	profiles.
	 */
	public void saveProfiles()
	{
		if (this.profiles != null)
		{
			WbPersistence.writeObject(new ArrayList(this.profiles.values()), WbManager.getSettings().getProfileFileName());
			this.resetProfiles();
		}
	}

	/**
	 *	Returns true if any of the profile definitions has changed.
	 *	(Or if a profile has been deleted or added)
	 */
  public boolean profilesChanged()
  {
		if (this.profilesChanged) return true;
    if (this.profiles == null) return false;
    Iterator values = this.profiles.values().iterator();
    while (values.hasNext())
    {
      ConnectionProfile profile = (ConnectionProfile)values.next();
      if (profile.isChanged()) 
			{
				return true;
			}
    }
    return false;
  }

	/**
	 *	This is called from the ProfileListModel when a new profile is added.
	 *	The caller needs to make sure that the status is set to new if that
	 *	profile was just created.
	 */
	public void addProfile(ConnectionProfile aProfile)
	{
    if (this.profiles == null)
    {
      this.readProfiles();
      if (this.profiles == null) this.profiles = new HashMap();
    }
		this.profiles.put(aProfile.getIdentifier(), aProfile);
    this.profilesChanged = true;
	}
	
	/**
	 *	This is called from the ProfileListModel when a profile has been deleted
	 */
	public void removeProfile(ConnectionProfile aProfile)
	{
    if (this.profiles == null) return;
    
		this.profiles.remove(aProfile.getIdentifier());
		// deleting a new profile should not change the status to changed
		if (!aProfile.isNew())
		{
			this.profilesChanged = true;
		}
	}
	
	public void setProfiles(Collection c)
	{
		Iterator itr = ((Collection)c).iterator();
		if (this.profiles == null)
		{
			this.profiles = new HashMap();
		}
		else
		{
			this.profiles.clear();
		}
		while (itr.hasNext())
		{
			ConnectionProfile prof = (ConnectionProfile)itr.next();
			this.profiles.put(prof.getIdentifier(), prof);
		}
	}
		
}
