/*
 * ConnectionMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import workbench.WbManager;
import workbench.gui.profiles.ProfileKey;

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
	implements PropertyChangeListener
{
	//private WbConnection currentConnection;
	private HashMap activeConnections = new HashMap();

	private List profiles;
	private List drivers;
	private boolean profilesChanged;
	private boolean readTemplates = true;
	private boolean templatesImported;
	private List groupsChangeListener;
	private static final ConnectionMgr mgrInstance = new ConnectionMgr();

	/** Creates new ConnectionMgr */
	private ConnectionMgr()
	{
		Settings.getInstance().addPropertyChangeListener(this);
	}

	public static ConnectionMgr getInstance()
	{
		return mgrInstance;
	}

	public WbConnection getConnection(ProfileKey def, String anId)
		throws ClassNotFoundException, SQLException, Exception
	{
		ConnectionProfile prof = this.getProfile(def);
		if (prof == null) return null;

		return this.getConnection(prof, anId);
	}
	
	public WbConnection getConnection(ConnectionProfile aProfile, String anId)
		throws ClassNotFoundException, SQLException
	{
		return getConnection(aProfile, anId, false);
	}
	
	/**
	 *	Return a new connection specified by the profile
	 */
	public WbConnection getConnection(ConnectionProfile aProfile, String anId, boolean reUse)
		throws ClassNotFoundException, SQLException
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
		LogMgr.logInfo("ConnectionMgr.getConnection()", "Creating new connection for [" + aProfile.getKey() + "] with ID=" + anId + " for driver=" + aProfile.getDriverclass());
		Connection sql = this.connect(aProfile, anId);
		conn.setProfile(aProfile);
		conn.setSqlConnection(sql);
		conn.runPostConnectScript();
		
		String driverVersion = null;
		try
		{
			int minor = sql.getMetaData().getDriverMinorVersion();
			int major = sql.getMetaData().getDriverMajorVersion();
			driverVersion = major + "." + minor;
		}
		catch (Throwable th)
		{
			driverVersion = "n/a";
		}

		LogMgr.logInfo("ConnectionMgr.getConnection()", "Connected to: [" + conn.getMetadata().getProductName() + "], Database version: [" + conn.getMetadata().getDbVersion() + "], Driver version: [" + driverVersion + "]");

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

	public Class loadClassFromDriverLib(ConnectionProfile profile, String className)
		throws ClassNotFoundException
	{
		String drvClass = profile.getDriverclass();
		String drvName = profile.getDriverName();
		DbDriver drv = this.findDriverByName(drvClass, drvName);
		if (drv == null) return null;
		return drv.loadClassFromDriverLib(className);
	}

	Connection connect(ConnectionProfile aProfile, String anId)
		throws ClassNotFoundException, SQLException
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

//		try
//		{
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
//		}
//		catch (SQLException e)
//		{
//			LogMgr.logError("ConnectionMgr.connect()", "Error when creating connection", e);
//			throw e;
//		}
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
		
		// In datch mode the default drivers (DriverTemplates.xml) are not loaded. 
		
		if (firstMatch == null && WbManager.getInstance().isBatchMode())
		{
			// We simple pretend there is one available, this will e.g. make
			// the ODBC Bridge work without a WbDrivers.xml 
			return new DbDriver(aName, drvClassName, null);
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

		//LogMgr.logDebug("ConnectionMgr.findDriverByName()", "Searching for DriverClass=" + drvClassName);

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

	public DbDriver registerDriver(String drvClassName, String jarFile)
	{
		if (this.drivers == null) this.readDrivers();

		DbDriver drv = new DbDriver("JdbcDriver", drvClassName, jarFile);
		this.drivers.add(drv);
		return drv;
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

	public ConnectionProfile getProfile(ProfileKey def)
	{
		this.getProfiles();
		if (def == null) return null;
		String name = def.getName();
		String group = def.getGroup();
		if (this.profiles == null) return null;
		Iterator itr = this.profiles.iterator();
		ConnectionProfile prof = null;
		ConnectionProfile firstMatch = null;
		while (itr.hasNext())
		{
			prof = (ConnectionProfile)itr.next();
			if (name.equalsIgnoreCase(prof.getName()))
			{
				if (firstMatch == null) firstMatch = prof;
				if (group == null) 
				{
					return prof;
				}
				else if (group.equalsIgnoreCase(prof.getGroup())) 
				{
					return prof;
				}
			}
		}
		return firstMatch;
	}

	public synchronized Collection getProfileGroups()
	{
		Set result = new TreeSet();
		if (this.profiles == null) this.readProfiles();
		Iterator itr = this.profiles.iterator();
		while (itr.hasNext())
		{
			String group = ((ConnectionProfile)itr.next()).getGroup();
			if (StringUtil.isEmptyString(group)) continue;
			result.add(group);
		}
		return result;
	}

	public void addProfileGroupChangeListener(PropertyChangeListener l)
	{
		if (this.groupsChangeListener == null) this.groupsChangeListener = new ArrayList();
		this.groupsChangeListener.add(l);
	}
	
	public void removeProfileGroupChangeListener(PropertyChangeListener l)
	{
		if (groupsChangeListener == null) return;
		groupsChangeListener.remove(l);
	}
	
	public void profileGroupChanged(ConnectionProfile profile)
	{
		if (this.groupsChangeListener == null) return;
		Iterator itr = this.groupsChangeListener.iterator();
		PropertyChangeEvent evt = new PropertyChangeEvent(profile, ConnectionProfile.PROPERTY_PROFILE_GROUP, null, profile.getGroup());
		while (itr.hasNext())
		{
			PropertyChangeListener l = (PropertyChangeListener)itr.next();
			if (l != null) l.propertyChange(evt);
		}
	}
	/**
	 *	Returns a Map with the current profiles.
	 *	The key to the map is the profile name, the value is the actual profile
	 *  (i.e. instances of {@link ConnectionProfile}
	 */
	public synchronized List getProfiles()
	{
		if (this.profiles == null) this.readProfiles();
		return Collections.unmodifiableList(this.profiles);
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
			this.closeConnection(con);
		}
		this.activeConnections.clear();
	}

	public synchronized void disconnect(WbConnection con)
	{
		if (this.activeConnections.containsKey(con.getId()))
		{
			this.activeConnections.remove(con.getId());
		}
		this.closeConnection(con);
	}
	
	/**
	 *	Disconnect the connection with the given id
	 */
	public synchronized void disconnect(String anId)
	{
		WbConnection con = (WbConnection)this.activeConnections.get(anId);
		this.closeConnection(con);
		this.activeConnections.remove(anId);
	}

	/**
	 *	Disconnect the given connection
	 */
	private synchronized void closeConnection(WbConnection conn)
	{
		if (conn == null) return;
		if (conn.isClosed()) return;
		
		try
		{
			if (conn.getProfile() != null)
			{
				LogMgr.logInfo("ConnectionMgr.disconnect()", "Disconnecting: [" + conn.getProfile().getName() + "], ID=" + conn.getId());
			}
			
			conn.runPreDisconnectScript();
			
			if (conn.getMetadata() == null)
			{
				conn.close();
			}
			else if (conn.getMetadata().isCloudscape() || conn.getMetadata().isApacheDerby())
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
				conn.close();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError(this, ResourceMgr.getString("ErrOnDisconnect"), e);
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
		if (url == null) return;
		if (!url.startsWith("jdbc:hsqldb")) return;

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
	 *	Shut down the connection to an internal Cloudscape/Derby database
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
			// Cloudscape/Derby reports the shutdown success through an exception
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
		try
		{
			writer.writeObject(this.drivers);
		} 
		catch (IOException e)
		{
			LogMgr.logError("ConnectionMgr.saveDrivers()", "Could not save drivers", e);
		}
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
			
			WbPersistence reader = new WbPersistence();
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
			WbPersistence reader = new WbPersistence(Settings.getInstance().getProfileStorage());
			result = reader.readObject();
		}
		catch (FileNotFoundException fne)
		{
			LogMgr.logDebug("ConnectionMgr.readProfiles()", "WbProfiles.xml not found. Creating new one.");
			this.profiles = new ArrayList();
			return;
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr.readProfiles()", "Error when reading connection profiles", e);
			this.profiles = new ArrayList();
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
			this.profiles = new ArrayList(l.length);
			for (int i=0; i < l.length; i++)
			{
				ConnectionProfile prof = (ConnectionProfile)l[i];
				prof.reset();
				this.profiles.add(prof);
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
      Iterator values = this.profiles.iterator();
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
			WbPersistence writer = new WbPersistence(Settings.getInstance().getProfileStorage());
			try
			{
				writer.writeObject(this.profiles);
			}
			catch (IOException e)
			{
				LogMgr.logError("ConnectionMgr.saveProfiles()", "Error saving profiles", e);
			}
			this.resetProfiles();
		}
	}

	/**
	 *	Returns true if any of the profile definitions has changed.
	 *	(Or if a profile has been deleted or added)
	 */
  public boolean profilesAreModified()
  {
		if (this.profilesChanged) return true;
    if (this.profiles == null) return false;
    Iterator values = this.profiles.iterator();
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
      if (this.profiles == null) this.profiles = new ArrayList();
    }
		this.profiles.add(aProfile);
    this.profilesChanged = true;
	}

	/**
	 *	This is called from the ProfileListModel when a profile has been deleted
	 */
	public void removeProfile(ConnectionProfile aProfile)
	{
    if (this.profiles == null) return;

		this.profiles.remove(aProfile);
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
			this.profiles = new ArrayList();
		}
		else
		{
			this.profiles.clear();
		}
		while (itr.hasNext())
		{
			ConnectionProfile prof = (ConnectionProfile)itr.next();
			this.profiles.add(prof);
		}
	}

	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(Settings.PROPERTY_PROFILE_STORAGE))
		{
			this.profiles = null;
		}
	}

}
