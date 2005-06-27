/*
 * ConnectionMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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

import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 * A connection factory for the application.
 * @author  support@sql-workbench.net
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
	private static final ConnectionMgr mgrInstance = new ConnectionMgr();

	/** Creates new ConnectionMgr */
	private ConnectionMgr()
	{
	}

	public static ConnectionMgr getInstance()
	{
		return mgrInstance;
	}

	public WbConnection getConnection(String aProfileName, String anId)
		throws ClassNotFoundException, SQLException, Exception
	{
		ConnectionProfile prof = this.getProfile(aProfileName);
		if (prof == null) return null;

		return this.getConnection(prof, anId);
	}
	
	public WbConnection getConnection(ConnectionProfile aProfile, String anId)
		throws ClassNotFoundException, SQLException, Exception
	{
		return getConnection(aProfile, anId, false);
	}
	
	/**
	 *	Return a new connection specified by the profile
	 */
	public WbConnection getConnection(ConnectionProfile aProfile, String anId, boolean reUse)
		throws ClassNotFoundException, SQLException, Exception
	{
		
		if (reUse)
		{
			WbConnection old = (WbConnection)this.activeConnections.get(anId);
			
			if (old != null) 
			{
				LogMgr.logInfo("ConnectionMgr.getConnection()", "Re-using connection ID=" + anId);
				return old;
			}
		}
		
		this.disconnect(anId);
		WbConnection conn = new WbConnection(anId);
		LogMgr.logInfo("ConnectionMgr.getConnection()", "Creating new connection for [" + aProfile.getName() + "] with ID=" + anId + " for driver=" + aProfile.getDriverclass());
		Connection sql = this.connect(aProfile, anId);
		conn.setSqlConnection(sql);
		conn.setProfile(aProfile);
		
		String version = null;
		try
		{
			int minor = sql.getMetaData().getDriverMinorVersion();
			int major = sql.getMetaData().getDriverMajorVersion();
			version = minor + "." + major;
		}
		catch (Throwable th)
		{
			version = "n/a";
		}

		LogMgr.logInfo("ConnectionMgr.getConnection()", "Connected to: [" + sql.getMetaData().getDatabaseProductName() + "], Database version: [" + conn.getMetadata().getDbVersion() + "], Driver version: [" + version + "]");

		try
		{
			if (Settings.getInstance().getEnableDbmsOutput())
			{
				int size = Settings.getInstance().getDbmsOutputDefaultBuffer();
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
		throws ClassNotFoundException, Exception
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
			throw new SQLException("Driver class not registered");
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
        LogMgr.logInfo("ConnectionMgr.connect()", "Driver (" + drv.getDriverClass() + ") does not support the autocommit property: " + ExceptionUtil.getDisplay(th));
			}
			return sql;
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr.connect()", "Error when creating connection", e);
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
		LogMgr.logDebug("ConnectionMgr.findDriverByName()", "Did not find driver with name="+ aName + ", using " + (firstMatch == null ? "(n/a)" : firstMatch.getName()));

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
		if (conn == null) return;

		try
		{
			if (conn.getMetadata().isCloudscape() || conn.getMetadata().isApacheDerby())
			{
				ConnectionProfile prof = conn.getProfile();
				boolean shutdown = this.canShutdownCloudscape(conn);
				conn.close();
				if (shutdown)
				{
					this.shutdownCloudscape(prof);
				}
			}
			else if (conn.getMetadata().isHsql() && this.canShutdownHsql(conn))
			{
				this.shutdownHsql(conn);
				conn.close();
			}
			else
			{
				LogMgr.logInfo("ConnectionMgr.disconnect()", "Disconnecting: [" + conn.getProfile().getName() + "], ID=" + conn.getId());
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
	private void shutdownHsql(WbConnection con)
	{
		if (con == null) return;
		String url = con.getUrl();
		if (url != null && !url.startsWith("jdbc:hsqldb")) return;

		// this is a HSQL server connection. Do not shut down this!
		if (url.startsWith("jdbc:hsqldb:hsql:")) return;

		try
		{
			Statement stmt = con.createStatement();
			LogMgr.logInfo("ConnectionMgr.disconnect()", "Local HSQL connection detected. Sending SHUTDOWN to the engine before disconnecting");
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
	private boolean canShutdownHsql(WbConnection aConn)
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
			if (u == null) continue;
			// we found one connection with the same URL --> do not shutdown this one!
			if (u.equals(url)) return false;
		}

		return true;
	}

	/**
	 *	Shut down the connection to an internal Cloudscape database
	 */
	private void shutdownCloudscape(ConnectionProfile prof)
	{
		String drvClass = prof.getDriverclass();
		String drvName = prof.getDriverName();

		String url = prof.getUrl();
		String command = null;
		String shutdown = ";shutdown=true";

		int pos = url.indexOf(";");
		if (pos > -1)
		{
			command = url.substring(0, pos) + shutdown;
		}
		else
		{
			command = url + shutdown;
		}
		try
		{
			DbDriver drv = this.findDriverByName(drvClass, drvName);
			LogMgr.logInfo("ConnectionMgr.shutdownCloudscape()", "Local Cloudscape connection detected. Shutting down engine...");
			drv.commandConnect(command);
		}
		catch (SQLException e)
		{
			// This exception is expected!
			// Cloudscape reports the shutdown success through an exception
			LogMgr.logInfo("ConnectionMgr.shutdownCloudscape()", ExceptionUtil.getDisplay(e));
		}
		catch (Throwable th)
		{
			LogMgr.logError("ConnectionMgr.shutdownCloudscape()", "Error when shutting down Cloudscape/Derby", th);
		}
	}

	private boolean canShutdownCloudscape(WbConnection aConn)
	{
		if (!aConn.getMetadata().isCloudscape() && !aConn.getMetadata().isApacheDerby()) return true;

		String url = aConn.getUrl();
		String prefix;
		if (aConn.getMetadata().isCloudscape())
			prefix = "jdbc:cloudscape:";
		else
			prefix = "jdbc:derby:";


		// check for cloudscape connection
		if (!url.startsWith(prefix)) return true;

		// do not shutdown server connections!
		if (url.startsWith(prefix + "net:")) return false;

		String id = aConn.getId();

		Iterator itr = this.activeConnections.values().iterator();
		while (itr.hasNext())
		{
			WbConnection c = (WbConnection)itr.next();
			if (c == null) continue;

			if (c.getId().equals(id)) continue;

			String u = c.getUrl();
			// we found one connection with the same URL --> do not shutdown this one!
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
		WbPersistence writer = new WbPersistence(Settings.getInstance().getDriverConfigFilename());
		writer.writeObject(this.drivers);
	}

	private void readDrivers()
	{
		try
		{
			WbPersistence reader = new WbPersistence(Settings.getInstance().getDriverConfigFilename());
			Object result = reader.readObject();
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
			WbPersistence reader = new WbPersistence("DriverTemplates.xml");
			ArrayList templates = (ArrayList)reader.readObject(in);

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
			WbPersistence reader = new WbPersistence(Settings.getInstance().getProfileFilename());
			result = reader.readObject();
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
			WbPersistence writer = new WbPersistence(Settings.getInstance().getProfileFilename());
			writer.writeObject(new ArrayList(this.profiles.values()));
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
