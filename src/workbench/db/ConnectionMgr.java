/*
 * ConnectionMgr.java
 *
 * Created on November 25, 2001, 4:18 PM
 */
package workbench.db;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import workbench.WbManager;
import workbench.exception.NoConnectionException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
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
	private boolean readTemplates = true;
	private boolean templatesImported;

	/** Creates new ConnectionMgr */
	public ConnectionMgr()
	{
	}

	public WbConnection getConnection(String aProfileName, String anId)
		throws ClassNotFoundException, SQLException, Exception
	{
		ConnectionProfile prof = this.getProfile(aProfileName);
		if (prof == null) return null;

		return this.getConnection(prof, anId);
	}
	/**
	 *	Return a new connection specified by the profile
	 */
	public WbConnection getConnection(ConnectionProfile aProfile, String anId)
		throws ClassNotFoundException, SQLException, Exception
	{
		this.disconnect(anId);

		WbConnection conn = new WbConnection(anId);
		LogMgr.logInfo("ConnectionMgr.getConnection()", "Creating new connection for [" + aProfile.getName() + "] with ID=" + anId + " for driver=" + aProfile.getDriverclass());
		Connection sql = this.connect(aProfile, anId);
		conn.setSqlConnection(sql);
		conn.setProfile(aProfile);
		
		LogMgr.logInfo("ConnectionMgr.connect()", "Connected to: [" + sql.getMetaData().getDatabaseProductName() + "], " + conn.getMetadata().getDbVersion());

		try
		{
			if (WbManager.getSettings().getEnableDbmsOutput())
			{
				int size = WbManager.getSettings().getDbmsOutputDefaultBuffer();
				conn.getMetadata().enableOutput(size);
			}
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("ConnectionMgr.getConnection()", "Could not enable DBMS_OUTPUT package");
		}

		this.activeConnections.put(anId, conn);

		return conn;
	}

	Connection connect(ConnectionProfile aProfile, String anId)
		throws ClassNotFoundException, SQLException, Exception
	{
		// The DriverManager refuses to use a driver which was not loaded
		// from the system classloader, so the connection has to be
		// established directly from the driver.
		String drvClass = aProfile.getDriverclass();
		String drvName = aProfile.getDriverName();
		//long start, end;
		//start = System.currentTimeMillis();
		DbDriver drv = this.findDriverByName(drvClass, drvName);
		//end = System.currentTimeMillis();
		//LogMgr.logDebug("ConnectionMgr.connect()", "FindDriver took " + (end - start) + " ms");
		if (drv == null)
		{
			throw new NoConnectionException("Driver class not registered");
		}

		try
		{
			Connection sql = drv.connect(aProfile.getUrl(), aProfile.getUsername(), aProfile.decryptPassword(), anId, aProfile.getConnectionProperties());

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
			throw e;
		}
	}

	public void reconnect(WbConnection aConn)
		throws ClassNotFoundException, SQLException, Exception
	{
    aConn.close();
    // use the stored profile to reconnect as the SQL connection
    // does not contain information about the username & password
    Connection sql = this.connect(aConn.getProfile(), aConn.getId());
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

	public DbDriver findDriverByName(String drvClassName, String aName)
	{
		DbDriver firstMatch = null;
		DbDriver db = null;

		if (aName == null || aName.length() == 0) return this.findDriver(drvClassName);
		if (this.drivers == null) this.readDrivers();

		LogMgr.logDebug("ConnectionMgr.findDriverByName()", "Searching for DriverClass=" + drvClassName + ",DriverName=" + aName);

		for (int i=0; i < this.drivers.size(); i ++)
		{
			db = (DbDriver)this.drivers.get(i);
			if (db.getDriverClass().equals(drvClassName))
			{
				// if the classname and the driver name are the same return the driver immediately
				// If we don't find a match for the name, we'll use
				// the first match for the classname
				if (db.getName().equals(aName)) return db;
				if (firstMatch == null)
				{
					firstMatch = db;
				}
			}
		}
		LogMgr.logWarning("ConnectionMgr.findDriverByName()", "Did not find driverclass with name="+ aName);

		return firstMatch;
	}

	public DbDriver findRegisteredDriver(String drvClassName)
	{
		if (this.drivers == null)	this.readDrivers();

    DbDriver db = null;

		for (int i=0; i < this.drivers.size(); i ++)
		{
			db = (DbDriver)this.drivers.get(i);
			if (db.getDriverClass().equals(drvClassName)) return db;
		}
		return null;
	}

	public DbDriver findDriver(String drvClassName)
	{
    DbDriver db = this.findRegisteredDriver(drvClassName);

		LogMgr.logDebug("ConnectionMgr.findDriverByName()", "Searching for DriverClass=" + drvClassName);

		if (db == null)
		{
			LogMgr.logWarning("ConnectionMgr.findDriver()", "Did not find a registered driver with classname = ["+drvClassName+"]");
			try
			{
				// not found --> maybe it's present in the normal classpath...
				// eg the ODBC bridge
				Class drvcls = Class.forName(drvClassName);
				Driver drv = (Driver)drvcls.newInstance();
				db = new DbDriver(drv);
			}
			catch (Exception cnf)
			{
				LogMgr.logError("ConnectionMgr.findDriver()", "Error creating instance for driver class [" + drvClassName + "] ", cnf);
				db = null;
			}
		}
		return db;
	}

	public void registerDriver(String drvClassName, String jarFile)
	{
		if (this.drivers == null) this.readDrivers();

		DbDriver drv = new DbDriver("JdbcDriver", drvClassName, jarFile);
		this.drivers.add(drv);
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

	public ConnectionProfile getProfile(String aName)
	{
		this.getProfiles();
		if (this.profiles == null) return null;
		Iterator itr = this.profiles.values().iterator();
		ConnectionProfile prof = null;
		while (itr.hasNext())
		{
			prof = (ConnectionProfile)itr.next();
			if (aName.equalsIgnoreCase(prof.getName())) return prof;
		}
		return null;
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
	 *	Return a readable display of a connection
	 */
	public static String getDisplayString(WbConnection con)
	{
		String displayString = null;
		String model = WbManager.getSettings().getConnectionDisplayModel();
		if (model != null && model.length() > 0) return getDisplayStringFromModel(con);

		try
		{
			DatabaseMetaData data = con.getSqlConnection().getMetaData();
			StringBuffer buff = new StringBuffer(100);
			buff.append(ResourceMgr.getString("TxtUser"));
			buff.append('=');
			buff.append(data.getUserName());

			String catName = data.getCatalogTerm();
			String catalog = con.getMetadata().getCurrentCatalog();
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
			LogMgr.logError("ConnectionMgr.getDisplayString()", "Could not retrieve connection information", e);
			displayString = "n/a";
		}
		return displayString;
	}

	private static String getDisplayStringFromModel(WbConnection con)
	{
		String displayString = WbManager.getSettings().getConnectionDisplayModel();
		try
		{
			DatabaseMetaData data = con.getSqlConnection().getMetaData();
			displayString = displayString.replaceAll("%username%", data.getUserName());

			String catalog = con.getMetadata().getCurrentCatalog();
			displayString = displayString.replaceAll("%catalog%", catalog == null ? "" : catalog);

			displayString = displayString.replaceAll("%url%", data.getURL());
			String prof = con.getProfile().getName();
			displayString = displayString.replaceAll("%profile%", prof == null ? "" : prof);
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr.getDisplayStringFromModel()", "Could not retrieve connection information", e);
			displayString = "n/a";
		}
		return displayString;

	}
	/**
	 *	Disconnects all connections
	 */
	public void disconnectAll()
	{
		Iterator itr = this.activeConnections.values().iterator();
		while (itr.hasNext())
		{
			WbConnection con = (WbConnection)itr.next();
			this.disconnect(con);
		}
		this.activeConnections.clear();
	}

	/**
	 *	Disconnect the connection with the given id
	 */
	public void disconnect(String anId)
	{
		WbConnection con = (WbConnection)this.activeConnections.get(anId);
		this.disconnect(con);
		this.activeConnections.remove(anId);
	}

	/**
	 *	Disconnect the given connection
	 */
	private void disconnect(WbConnection conn)
	{
		try
		{
			if (conn != null && this.canDisconnectHsql(conn))
			{
				LogMgr.logInfo("ConnectionMgr.disconnect()", "Disconnecting: [" + conn.getProfile().getName() + "], ID=" + conn.getId());
				this.disconnectLocalHsql(conn);
				conn.close();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError(this, ResourceMgr.getString(ResourceMgr.ERROR_DISCONNECT), e);
		}
	}

	/**
	 *	Disconnects a local HSQL connection. Beginning with 1.7.2 the local
	 *  (=in process) engine should be closed down with SHUTDOWN when
	 *  disconnecting. It shouldn't hurt for pre-1.7.2 either :-)
	 */
	private void disconnectLocalHsql(WbConnection con)
	{
		String url = con.getUrl();
		if (!url.startsWith("jdbc:hsqldb")) return;

		// this is a HSQL server connection. Do not shut down this!
		if (url.startsWith("jdbc:hsqldb:hsql:")) return;

		try
		{
			Statement stmt = con.createStatement();
			LogMgr.logInfo("ConnectionMgr.disconnect()", "Local HSQL connection detected. Sending SHUTDOWN to the engine before disconnecting()");
			stmt.executeUpdate("SHUTDOWN");
		}
		catch (Exception e)
		{
			LogMgr.logWarning("ConnectionMgr.disconnectLocalHsql()", "Error when executing SHUTDOWN", e);
		}

	}

	/**
	 *	Check if the given (HSQLDB) connection can be safely closed.
	 *	A in-memory HSQLDB engine allows the creation of several connections
	 *	to the same database (from within the same JVM)
	 *	But the connections may not be closed except for the last one, because
	 *	they seem to "share" something in the driver and closing one
	 *	will close the others as well.
	 */
	private boolean canDisconnectHsql(WbConnection aConn)
	{
		if (!aConn.getMetadata().isHsql()) return true;

		// a HSQLDB server connection can always be closed!
		if (aConn.getUrl().startsWith("jdbc:hsqldb:hsql:")) return true;

		String url = aConn.getUrl();
		String id = aConn.getId();

		Iterator itr = this.activeConnections.values().iterator();
		while (itr.hasNext())
		{
			WbConnection c = (WbConnection)itr.next();
			if (c == null) continue;

			if (c.getId().equals(id)) continue;

			String u = c.getUrl();
			// we found one connection with the same URL --> do not connect this one!
			if (u.equals(url)) return false;
		}

		return true;
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
		}
		catch (FileNotFoundException fne)
		{
			LogMgr.logDebug("ConnectionMgr.readDrivers()", "WbDrivers.xml not found. Using defaults.");
			this.drivers = new ArrayList();
		}
		catch (Exception e)
		{
			LogMgr.logDebug(this, "Could not load driver definitions. Creating new one...", e);
			this.drivers = new ArrayList();
		}
		if (this.readTemplates)
		{
			this.importTemplateDrivers();
		}
	}

	public void setReadTemplates(boolean aFlag) { this.readTemplates = aFlag; }

	private void importTemplateDrivers()
	{
		if (this.templatesImported) return;

		if (this.drivers == null) this.readDrivers();

		// now read the templates and append them to the driver list
		InputStream in = null;
		try
		{
			in = this.getClass().getResourceAsStream("DriverTemplates.xml");
			// the additional filename is for logging purposes only
			ArrayList templates = (ArrayList)WbPersistence.readObject(in, "DriverTemplates.xml");

			for (int i=0; i < templates.size(); i++)
			{
				Object drv = templates.get(i);
				if (!this.drivers.contains(drv))
				{
					this.drivers.add(drv);
				}
			}
		}
		catch (Throwable io)
		{
			LogMgr.logWarning("ConectionMgr.readDrivers()", "Could not read driver templates!");
		}
		finally
		{
			try { in.close(); } catch (Throwable ignore) {}
		}
		this.templatesImported = true;
	}

	public void readProfiles()
	{
		Object result = null;
		try
		{
			result = WbPersistence.readObject(WbManager.getSettings().getProfileFileName());
		}
		catch (FileNotFoundException fne)
		{
			LogMgr.logDebug("ConnectionMgr.readProfiles()", "WbProfiles.xml not found. Creating new one.");
			this.profiles = new HashMap();
			return;
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr.readProfiles()", "Error when reading connection profiles", e);
			this.profiles = new HashMap();
			return;
		}
		if (result instanceof Collection)
		{
			this.setProfiles((Collection)result);
			this.resetProfiles();
		}
		else if (result instanceof Object[])
		{
			// This is to support the very first version of the profile storage
			// probably obsolete by know, but you never know...
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