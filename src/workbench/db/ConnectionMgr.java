/*
 * ConnectionMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import workbench.db.shutdown.DbShutdownFactory;
import workbench.db.shutdown.DbShutdownHook;
import workbench.gui.profiles.ProfileKey;

import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.FileUtil;
import workbench.util.PropertiesCopier;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 * A connection factory for the application.
 * 
 * @author  support@sql-workbench.net
 */
public class ConnectionMgr
	implements PropertyChangeListener
{
	private Map<String, WbConnection> activeConnections = new HashMap<String, WbConnection>();
	
	private ArrayList<ConnectionProfile> profiles;
	private List<DbDriver> drivers;
	private boolean profilesChanged;
	private boolean readTemplates = true;
	private boolean templatesImported;
	private List<PropertyChangeListener> groupsChangeListener;
	private static final ConnectionMgr mgrInstance = new ConnectionMgr();
	
	private ConnectionMgr()
	{
		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_PROFILE_STORAGE);
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
			WbConnection old = this.activeConnections.get(anId);
			
			if (old != null)
			{
				LogMgr.logInfo("ConnectionMgr.getConnection()", "Re-using connection ID=" + anId);
				return old;
			}
		}
		
		this.disconnect(anId);
		LogMgr.logInfo("ConnectionMgr.getConnection()", "Creating new connection for [" + aProfile.getKey() + "] with ID=" + anId + " for driver=" + aProfile.getDriverclass());
		WbConnection conn = this.connect(aProfile, anId);
		conn.runPostConnectScript();
		String driverVersion = null;
		
		try
		{
			int minor = conn.getSqlConnection().getMetaData().getDriverMinorVersion();
			int major = conn.getSqlConnection().getMetaData().getDriverMajorVersion();
			driverVersion = major + "." + minor;
		}
		catch (Throwable th)
		{
			driverVersion = "n/a";
		}
		
		LogMgr.logInfo("ConnectionMgr.getConnection()", "Connected to: [" + conn.getMetadata().getProductName() + "], Database version: [" + conn.getMetadata().getDbVersion() + "], Driver version: [" + driverVersion + "]");
		
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
	
	WbConnection connect(ConnectionProfile aProfile, String anId)
		throws ClassNotFoundException, SQLException
	{
		// The DriverManager refuses to use a driver which was not loaded
		// from the system classloader, so the connection has to be
		// established directly from the driver.
		WbConnection conn = new WbConnection(anId);
		
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
		
		copyPropsToSystem(aProfile);
		
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
		conn.setSqlConnection(sql);
		conn.setProfile(aProfile);
		return conn;
	}
	
	private void copyPropsToSystem(ConnectionProfile profile)
	{
		if (profile != null && profile.getCopyExtendedPropsToSystem())
		{
			PropertiesCopier copier = new PropertiesCopier();
			copier.copyToSystem(profile.getConnectionProperties());
		}		
	}
	
	private void removePropsFromSystem(ConnectionProfile profile)
	{
		if (profile != null && profile.getCopyExtendedPropsToSystem())
		{
			PropertiesCopier copier = new PropertiesCopier();
			copier.removeFromSystem(profile.getConnectionProperties());
		}		
	}
					
	public DbDriver findDriverByName(String drvClassName, String aName)
	{
		DbDriver firstMatch = null;
		DbDriver db = null;
		
		if (aName == null || aName.length() == 0) return this.findDriver(drvClassName);
		if (this.drivers == null) this.readDrivers();
		
		for (int i=0; i < this.drivers.size(); i ++)
		{
			db = this.drivers.get(i);
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
			db = this.drivers.get(i);
			if (db.getDriverClass().equals(drvClassName)) return db;
		}
		return null;
	}
	
	public DbDriver findDriver(String drvClassName)
	{
		if (drvClassName == null)
		{
			LogMgr.logError("ConnectionMgr.findDriver()", "Called with a null classname!", new NullPointerException());
			return null;
		}
		
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
	
	/**
	 * Add a new, dynamically defined driver to the list of available
	 * drivers.
	 * This is used if a driver definition is passed on the commandline
	 *
	 * @see workbench.sql.BatchRunner#createCmdLineProfile(workbench.util.ArgumentParser)
	 */
	public DbDriver registerDriver(String drvClassName, String jarFile)
	{
		if (this.drivers == null) this.readDrivers();
		
		DbDriver drv = new DbDriver("JdbcDriver", drvClassName, jarFile);
		
		// this method is called from BatchRunner.createCmdLineProfile() when
		// the user passed all driver information on the command line.
		// as most likely this is the correct driver it has to be put
		// at the beginning of the list, to prevent a different driver
		// with the same driver class in WbDrivers.xml to be used instead
		this.drivers.add(0,drv);
		
		return drv;
	}
	
	/**
	 *	Returns a List of registered drivers.
	 *	This list is read from WbDrivers.xml
	 */
	public List<DbDriver> getDrivers()
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
	public List<String> getDriverClasses()
	{
		if (this.drivers == null) this.readDrivers();
		List<String> result = new ArrayList<String>(this.drivers.size());
		String drvClass;
		
		for (int i=0; i < this.drivers.size(); i++)
		{
			drvClass = this.drivers.get(i).getDriverClass();
			if (!result.contains(drvClass))
			{
				result.add(drvClass);
			}
		}
		return result;
	}
	
	public void setDrivers(List<DbDriver> aDriverList)
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
		ConnectionProfile firstMatch = null;
		for (ConnectionProfile prof : this.profiles)
		{
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
	
	public synchronized Collection<String> getProfileGroups()
	{
		Set<String> result = new TreeSet<String>();
		if (this.profiles == null) this.readProfiles();
		for (ConnectionProfile prof : this.profiles)
		{
			String group = prof.getGroup();
			if (StringUtil.isEmptyString(group)) continue;
			result.add(group);
		}
		return result;
	}
	
	public void addProfileGroupChangeListener(PropertyChangeListener l)
	{
		if (this.groupsChangeListener == null) this.groupsChangeListener = new ArrayList<PropertyChangeListener>();
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
		for (PropertyChangeListener l : this.groupsChangeListener)
		{
			if (l != null) l.propertyChange(evt);
		}
	}
	/**
	 *	Returns a Map with the current profiles.
	 *	The key to the map is the profile name, the value is the actual profile
	 *  (i.e. instances of {@link ConnectionProfile}
	 */
	public synchronized List<ConnectionProfile> getProfiles()
	{
		if (this.profiles == null) this.readProfiles();
		return Collections.unmodifiableList(this.profiles);
	}
	
	/**
	 *	Disconnects all connections
	 */
	public void disconnectAll()
	{
		Iterator<WbConnection> itr = this.activeConnections.values().iterator();
		for (WbConnection con : this.activeConnections.values())
		{
			this.closeConnection(con);
		}
		this.activeConnections.clear();
	}
	
	public synchronized void disconnect(WbConnection con)
	{
		if (con == null) return;
		this.activeConnections.remove(con.getId());
		this.closeConnection(con);
	}
	
	/**
	 *	Disconnect the connection with the given id
	 */
	public synchronized void disconnect(String anId)
	{
		WbConnection con = this.activeConnections.get(anId);
		disconnect(con);
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
			
			removePropsFromSystem(conn.getProfile());
			
			DbShutdownHook hook = DbShutdownFactory.getShutdownHook(conn);
			if (hook != null)
			{
				hook.shutdown(conn);
			}
			else
			{
				conn.close();
			}
			if (conn.getProfile() != null)
			{
				LogMgr.logDebug("ConnectionMgr.disconnect()", "Disconnected [" + conn.getId() + "]");
			}
			
		}
		catch (Exception e)
		{
			LogMgr.logError(this, ResourceMgr.getString("ErrOnDisconnect"), e);
		}
	}
	
	/**
	 * Check if there is another connection active with the same URL.
	 */
	public boolean isActive(WbConnection aConn)
	{
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
			// we found one connection with the same URL 
			if (u.equals(url)) return true;
		}
		
		return false;
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
	
	@SuppressWarnings("unchecked")
	private void readDrivers()
	{
		try
		{
			WbPersistence reader = new WbPersistence(Settings.getInstance().getDriverConfigFilename());
			Object result = reader.readObject();
			if (result == null)
			{
				this.drivers = new ArrayList<DbDriver>();
			}
			else if (result instanceof ArrayList)
			{
				this.drivers = (List<DbDriver>)result;
			}
		}
		catch (FileNotFoundException fne)
		{
			LogMgr.logDebug("ConnectionMgr.readDrivers()", "WbDrivers.xml not found. Using defaults.");
			this.drivers = null;
		}
		catch (Exception e)
		{
			LogMgr.logDebug(this, "Could not load driver definitions. Creating new one...", e);
			this.drivers = null;
		}
		
		if (this.drivers == null)
		{
			this.drivers = new ArrayList<DbDriver>();
		}
		if (this.readTemplates)
		{
			this.importTemplateDrivers();
		}
	}
	
	public void setReadTemplates(boolean aFlag)
	{
//		boolean old = this.readTemplates;
		this.readTemplates = aFlag;
//		if (old != readTemplates && readTemplates)
//		{
//			readDrivers();
//		}
	}
	
	@SuppressWarnings("unchecked")
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
			ArrayList<DbDriver> templates = (ArrayList<DbDriver>)reader.readObject(in);
			
			for (int i=0; i < templates.size(); i++)
			{
				DbDriver drv = templates.get(i);
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
			FileUtil.closeQuitely(in);
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
			result = null;
		}
		catch (Exception e)
		{
			LogMgr.logError("ConnectionMgr.readProfiles()", "Error when reading connection profiles", e);
			result = null;
		}
		
		this.profiles = new ArrayList<ConnectionProfile>();
		
		if (result instanceof Collection)
		{
			Collection c = (Collection)result;
			this.profiles.ensureCapacity(c.size());
			Iterator itr = c.iterator();
			while (itr.hasNext())
			{
				ConnectionProfile prof = (ConnectionProfile)itr.next();
				this.profiles.add(prof);
			}
		}
		else if (result instanceof Object[])
		{
			// This is to support the very first version of the profile storage
			// probably obsolete by know, but you never know...
			Object[] l = (Object[])result;
			this.profiles.ensureCapacity(l.length);
			for (int i=0; i < l.length; i++)
			{
				ConnectionProfile prof = (ConnectionProfile)l[i];
				this.profiles.add(prof);
			}
		}
		this.resetProfiles();
	}
	
	
	/**
	 *	Reset the changed status on the profiles.
	 *	Called after saving the profiles.
	 */
	private void resetProfiles()
	{
		if (this.profiles != null)
		{
			for (ConnectionProfile profile : this.profiles)
			{
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
				this.resetProfiles();
			}
			catch (IOException e)
			{
				LogMgr.logError("ConnectionMgr.saveProfiles()", "Error saving profiles", e);
			}
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
		for (ConnectionProfile profile : this.profiles)
		{
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
	
	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(Settings.PROPERTY_PROFILE_STORAGE))
		{
			this.profiles = null;
		}
	}
	
}
