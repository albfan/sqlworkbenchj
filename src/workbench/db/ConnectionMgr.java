/*
 * ConnectionMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
import workbench.util.CaseInsensitiveComparator;
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

	private List<ConnectionProfile> profiles;
	private List<DbDriver> drivers;
	private boolean profilesChanged;
	private boolean readTemplates = true;
	private boolean templatesImported;
	private List<PropertyChangeListener> groupsChangeListener;
	private List<PropertyChangeListener> driverChangeListener;
	private static ConnectionMgr instance = new ConnectionMgr();

	private ConnectionMgr()
	{
		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_PROFILE_STORAGE);
	}

	public static ConnectionMgr getInstance()
	{
		return instance;
	}

	/**
	 * Create a new connection. The profile to be used
	 * is searched by the given profile key
	 * @param def the profile to be used
	 * @param anId the id to be assigned to the connection
	 * @return a new Connection
	 *
	 * @throws java.lang.ClassNotFoundException
	 * @throws java.sql.SQLException
	 * @throws java.lang.Exception
	 */
	public WbConnection getConnection(ProfileKey def, String anId)
		throws ClassNotFoundException, SQLException, Exception
	{
		ConnectionProfile prof = this.getProfile(def);
		if (prof == null) return null;

		return this.getConnection(prof, anId);
	}

	public WbConnection findConnection(String id)
	{
		return this.activeConnections.get(id);
	}

	public int getOpenCount()
	{
		return this.activeConnections.size();
	}

	public WbConnection getConnection(ConnectionProfile aProfile, String anId)
		throws ClassNotFoundException, SQLException
	{
		this.disconnect(anId);
		LogMgr.logInfo("ConnectionMgr.getConnection()", "Creating new connection for [" + aProfile.getKey() + "] for driver=" + aProfile.getDriverclass());
		WbConnection conn = this.connect(aProfile, anId);
		conn.runPostConnectScript();
		String driverVersion = conn.getDriverVersion();
		String dbVersion = conn.getSqlConnection().getMetaData().getDatabaseProductVersion();

		LogMgr.logInfo("ConnectionMgr.getConnection()", "Connected to: [" + conn.getMetadata().getProductName() + "], Database version: [" + dbVersion + "], Driver version: [" + driverVersion + "], ID: ["  + anId + "]");

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
		WbConnection conn = new WbConnection(anId, sql, aProfile);
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
			// Check if there is another connection open which uses
			// the same profile. If that is the case the
			// properties should not be removed from the system properties
			for (WbConnection con : activeConnections.values())
			{
				if (con.getProfile().equals(profile))
				{
					return;
				}
			}
			PropertiesCopier copier = new PropertiesCopier();
			copier.removeFromSystem(profile.getConnectionProperties());
		}
	}

	public DbDriver findDriverByName(String drvClassName, String aName)
	{
		DbDriver firstMatch = null;

		if (this.drivers == null) this.readDrivers();

		if (aName == null || aName.length() == 0) return this.findDriver(drvClassName);

		for (DbDriver db : drivers)
		{
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

		// In batch mode the default drivers (DriverTemplates.xml) are not loaded.
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
		drv.setInternal(true);
		
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
	public synchronized List<DbDriver> getDrivers()
	{
		if (this.drivers == null)
		{
			this.readDrivers();
		}
		return this.drivers;
	}

	public void setDrivers(List<DbDriver> aDriverList)
	{
		this.drivers = aDriverList;
		if (this.driverChangeListener != null)
		{
			PropertyChangeEvent evt = new PropertyChangeEvent(this, "drivers", null, null);
			for (PropertyChangeListener l : this.driverChangeListener)
			{
				l.propertyChange(evt);
			}
		}
	}

	/**
	 * Find a connection profile identified by the given key.
	 *
	 * @param key the key of the profile
	 * @return a connection profile with that name or null if none was found.
	 */
	public ConnectionProfile getProfile(ProfileKey key)
	{
		if (key == null) return null;
		this.getProfiles();
		String name = key.getName();
		String group = key.getGroup();
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

	public void addDriverChangeListener(PropertyChangeListener l)
	{
		if (this.driverChangeListener == null) this.driverChangeListener = new ArrayList<PropertyChangeListener>();
		this.driverChangeListener.add(l);
	}

	public void removeDriverChangeListener(PropertyChangeListener l)
	{
		if (this.driverChangeListener == null) return;
		this.driverChangeListener.remove(l);
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
		PropertyChangeEvent evt = new PropertyChangeEvent(profile, ConnectionProfile.PROPERTY_PROFILE_GROUP, null, profile.getGroup());
		for (PropertyChangeListener l : this.groupsChangeListener)
		{
			if (l != null) l.propertyChange(evt);
		}
	}
	
	/**
	 * Return a list with profile keys that can be displayed to the user.
	 * The returned list is already sorted.
	 * 
	 * @return
	 */
	public synchronized List<String> getProfileKeys()
	{
		List<String> result = new ArrayList(profiles.size());
		for (ConnectionProfile profile : profiles)
		{
			result.add(profile.getKey().toString());
		}
		Collections.sort(result, new CaseInsensitiveComparator());
		return result;
	}
	/**
	 *	Returns a List with the current profiles.
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
	 * Disconnect the given connection.
	 * @param conn the connection to disconnect.
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
				conn.shutdown();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError(this, ResourceMgr.getString("ErrOnDisconnect"), e);
		}
	}

	/**
	 * Check if there is another connection active with the same URL.
	 * This is used when the connection to an embedded database that
	 * needs a {@link workbench.db.shutdown.DbShutdownHook} is called.
	 *
	 * @return true if there is another active connection.
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

	/**
	 * Save profile and driver definitions to external files.
	 * This merely calls {@link #saveProfiles()} and {@link #saveDrivers()}
	 * @see #saveProfiles()
	 * @see #saveDrivers()
	 */
	public void writeSettings()
	{
		this.saveProfiles();
		this.saveDrivers();
	}

	/**
	 * Saves the driver definitions to an external file.
	 *
	 * The name of the file defaults to <tt>WbDrivers.xml</tt>. The exact location
	 * can be set in the configuration file.
	 * @see workbench.resource.Settings#getDriverConfigFilename()
	 * @see WbPersistence#writeObject(Object)
	 */
	public void saveDrivers()
	{
		WbPersistence writer = new WbPersistence(Settings.getInstance().getDriverConfigFilename());

		// As drivers an profiles can be saved in console mode, we need to make
		// sure, the "internal" drivers that are created "on-the-fly" when connecting
		// from the commandline are not stored 
		List<DbDriver> allDrivers = new ArrayList<DbDriver>(this.drivers);
		Iterator<DbDriver> itr = allDrivers.iterator();
		while (itr.hasNext())
		{
			if (itr.next().isInternal())
			{
				itr.remove();
			}
		}
		
		try
		{
			writer.writeObject(allDrivers);
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
		this.readTemplates = aFlag;
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

	/**
	 * Remove all defined connection profiles.
	 * This does not make sure that all connections are closed!
	 * This method is used in Unit tests to setup a new set of profiles.
	 */
	public synchronized void clearProfiles()
	{
		if (this.profiles == null) return;
		this.profiles.clear();
	}

	/**
	 * Retrieves the connection profiles from an XML file.
	 *
	 * @see WbPersistence#readObject()
	 * @see workbench.resource.Settings#getProfileStorage()
	 */
	public synchronized void readProfiles()
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
			this.profiles.addAll(c);
		}
		else if (result instanceof Object[])
		{
			// This is to support the very first version of the profile storage
			// probably obsolete by know, but you never know...
			Object[] l = (Object[])result;
			for (Object prof : l)
			{
				this.profiles.add((ConnectionProfile)prof);
			}
		}
		this.resetProfiles();
	}


	/**
	 * Reset the changed status on the profiles.
	 *
	 * Called after saving the profiles.
	 */
	private synchronized void resetProfiles()
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
	 * Save the connectioin profiles to an external file.
	 *
	 * This will also reset the changed flag for any modified or new
	 * profiles. The name of the file defaults to <tt>WbProfiles.xml</tt>, but
	 * can be defined in the configuration properties.
	 *
	 * @see workbench.resource.Settings#getProfileStorage()
	 * @see WbPersistence#writeObject(Object)
	 */
	public synchronized void saveProfiles()
	{
		if (this.profiles != null)
		{
			WbPersistence.makeTransient(ConnectionProfile.class, "inputPassword");
			WbPersistence.makeTransient(ConnectionProfile.class, "useSeperateConnectionPerTab");
			WbPersistence.makeTransient(ConnectionProfile.class, "disableUpdateTableCheck");
			
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
		
		this.profiles.remove(aProfile);
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

	/**
	 * When the property {@link Settings#PROPERTY_PROFILE_STORAGE} is changed
	 * the current list of profiles is cleared.
	 *
	 * @param evt
	 * @see #clearProfiles()
	 */
	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(Settings.PROPERTY_PROFILE_STORAGE))
		{
			this.clearProfiles();
		}
	}

}
