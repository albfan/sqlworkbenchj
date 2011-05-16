/*
 * DbDriver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import workbench.db.postgres.PostgresUtil;
import workbench.resource.ResourceMgr;

/**
 *	Represents a JDBC Driver definition.
 *	The definition includes a (logical) name, a driver class
 *	and (optional) a library from which the driver is to
 *	be loaded.
 *
 *	@author  Thomas Kellerer
 */
public class DbDriver
	implements Comparable<DbDriver>
{
	private Driver driverClassInstance;
	private URLClassLoader classLoader;

	protected String name;
	private String driverClass;
	private List<String> libraryList;
	private boolean isInternal;

	private String sampleUrl;

	public DbDriver()
	{
	}

	public DbDriver(Driver aDriverClassInstance)
	{
		this.driverClassInstance = aDriverClassInstance;
		this.driverClass = aDriverClassInstance.getClass().getName();
		this.name = this.driverClass;
	}

	public DbDriver(String aDriverClassname)
	{
		this.setDriverClass(aDriverClassname);
		this.setName(aDriverClassname);
	}

	public DbDriver(String aName, String aClass, String aLibrary)
	{
		this.setName(aName);
		this.setDriverClass(aClass);
		this.setLibrary(aLibrary);
	}

	public boolean isInternal()
	{
		return this.isInternal;
	}

	void setInternal(boolean flag)
	{
		isInternal = flag;
	}

	public String getName()
	{
		return this.name;
	}

	public final void setName(String name)
	{
		this.name = name;
	}

	public String getDriverClass()
	{
		return this.driverClass;
	}

	public final void setDriverClass(String aClass)
	{
		this.driverClass = aClass.trim();
		this.driverClassInstance = null;
		this.classLoader = null;
	}

	public String getDescription()
	{
		StringBuilder b = new StringBuilder(100);
		if (this.name != null)
		{
			b.append(this.name);
			b.append(" (");
			b.append(this.driverClass);
			b.append(')');
		}
		else
		{
			b.append(this.driverClass);
		}
		return b.toString();
	}

	public String getLibraryString()
	{
		return createLibraryString(StringUtil.getPathSeparator());
	}

	private String createLibraryString(String separator)
	{
		if (this.libraryList == null) return null;
		StringBuilder result = new StringBuilder(this.libraryList.size() * 30);
		for (int i=0; i < libraryList.size(); i++)
		{
			if (i > 0) result.append(separator);
			result.append(libraryList.get(i));
		}
		return result.toString();
	}

	public String getLibrary()
	{
		return createLibraryString("|");
	}

	public static List<String> splitLibraryList(String libList)
	{
		if (libList.indexOf('|') > -1)
		{
			return StringUtil.stringToList(libList, "|", true, true, false);
		}
		else if (!StringUtil.isEmptyString(libList))
		{
			return StringUtil.stringToList(libList, StringUtil.getPathSeparator(), true, true, false);
		}
		return null;
	}

	public final void setLibrary(String libList)
	{
		this.libraryList = splitLibraryList(libList);
		this.driverClassInstance = null;
		this.classLoader = null;
	}

	public boolean canReadLibrary()
	{
		// When running in testmode, all necessary libraries are added through
		// the classpath already, so there is no need to check them here.
		if (Settings.getInstance().isTestMode()) return true;

		if (this.isInternal()) return true;

		if ("sun.jdbc.odbc.JdbcOdbcDriver".equals(driverClass)) return true;

		if (libraryList != null)
		{
			for (String lib : libraryList)
			{
				String realLib = Settings.getInstance().replaceLibDirKey(lib);
				File f = new File(realLib);
				if (f.getParentFile() == null)
				{
					f = new File(Settings.getInstance().getLibDir(), realLib);
				}
				if (!f.exists()) return false;
			}
			return true;
		}
		return false;
	}

	public String toString()
	{
		return this.getDescription();
	}

	public void setSampleUrl(String anUrl)
	{
		this.sampleUrl = anUrl;
	}

	public String getSampleUrl()
	{
		return this.sampleUrl;
	}

	public Class loadClassFromDriverLib(String className)
		throws ClassNotFoundException
	{
		if (this.classLoader == null) return null;
		Class clz = this.classLoader.loadClass(className);
		return clz;
	}

	private void loadDriverClass()
		throws ClassNotFoundException, Exception, UnsupportedClassVersionError
	{
		if (this.driverClassInstance != null) return;

		try
		{
			if (this.classLoader == null && this.libraryList != null)
			{
				URL[] url = new URL[libraryList.size()];
				int index = 0;
				for (String fname : libraryList)
				{
					String realFile = Settings.getInstance().replaceLibDirKey(fname);
					File f = new File(realFile);
					if (f.getParentFile() == null)
					{
						f = new File(Settings.getInstance().getLibDir(), realFile);
					}
					url[index] = f.toURI().toURL();
					LogMgr.logInfo("DbDriver.loadDriverClass()", "Adding ClassLoader URL=" + url[index].toString());
					index ++;
				}
				classLoader = new URLClassLoader(url, ClassLoader.getSystemClassLoader());
			}

			Class drvClass = null;
			if (classLoader != null)
			{
				// New Firebird 2.0 driver needs this, and it does not seem to do any harm
				// for other drivers
				Thread.currentThread().setContextClassLoader(classLoader);
				drvClass = this.classLoader.loadClass(driverClass);
			}
			else
			{
				// Assume the driver class is available on the classpath
				//LogMgr.logDebug("DbDriver.loadDriverClass()", "Assuming driver " + this.driverClass + " is in current classpath");
				drvClass = Class.forName(this.driverClass);
			}

			driverClassInstance = (Driver)drvClass.newInstance();
			if (Settings.getInstance().getBoolProperty("workbench.db.registerdriver", false))
			{
				// Some drivers expect to be registered with the DriverManager...
				try
				{
					LogMgr.logDebug("DbDriver.loadDriverClass()", "Registering new driver instance for " + this.driverClass + " with DriverManager");
					DriverManager.registerDriver(this.driverClassInstance);
				}
				catch (Throwable th)
				{
					LogMgr.logError("DbDriver.loadDriverClass()", "Error registering driver instance with DriverManager", th);
				}
			}
		}
		catch (UnsupportedClassVersionError e)
		{
			LogMgr.logError("DbDriver.loadDriverClass()", "Driver class could not be loaded ", e);
			throw e;
		}
		catch (ClassNotFoundException e)
		{
			LogMgr.logError("DbDriver.loadDriverClass()", "Class not found when loading driver", e);
			throw e;
		}
		catch (Throwable e)
		{
			this.classLoader = null;
			LogMgr.logError("DbDriver.loadDriverClass()", "Error loading driver class: " + this.driverClass, e);
			throw new Exception("Could not load driver class " + this.driverClass, e);
		}
	}

	public DbDriver createCopy()
	{
		DbDriver copy = new DbDriver();
		copy.driverClass = this.driverClass;
		copy.libraryList = new ArrayList<String>(libraryList);
		copy.sampleUrl = this.sampleUrl;
		copy.name = this.name;

		// the internal attribute should not be copied!

		return copy;
	}

	public Connection connect(String url, String user, String password, String id, Properties connProps)
		throws ClassNotFoundException, SQLException
	{
		Connection c = null;
		try
		{
			loadDriverClass();

			Properties props = new Properties();
			if (StringUtil.isNonBlank(user)) props.put("user", user);
			if (StringUtil.isNonBlank(password)) props.put("password", password);

			// copy the user defined connection properties into the actually used ones!
			if (connProps != null)
			{
				Enumeration keys = connProps.propertyNames();
				while (keys.hasMoreElements())
				{
					String key = (String)keys.nextElement();
					if (!props.containsKey(key))
					{
						String value = connProps.getProperty(key);
						props.put(key, value);
					}
				}
			}

			setAppInfo(props, url.toLowerCase(), id, user);

			c = this.driverClassInstance.connect(url, props);
			if (c == null)
			{
				LogMgr.logError("DbDriver.connect()", "No connection returned by driver " + this.driverClass + " for URL=" + url, null);
				throw new SQLException("Driver did not return a connection for url=" + url);
			}

			// The system property for the Firebird driver is only needed when the connection is created
			// so after the connect was successful, we can clean up the system properties
			if (doSetAppName() && url.startsWith("jdbc:firebirdsql:"))
			{
				System.clearProperty("org.firebirdsql.jdbc.processName");
			}

			// PostgreSQL 9.0 allows to set an application name, but currently only by executing a SQL statement
			if (doSetAppName() && url.startsWith("jdbc:postgresql"))
			{
				PostgresUtil.setApplicationName(c, getProgramName() + " (" + id + ")");
			}

		}
		catch (ClassNotFoundException e)
		{
			throw e;
		}
		catch (UnsupportedClassVersionError e)
		{
			throw e;
		}
		catch (Throwable th)
		{
			LogMgr.logError("DbDriver.connect()", "Error connecting to driver " + this.driverClass, th);
			throw new SQLException("Error connecting to database. (" + th.getClass().getName() + " - " + th.getMessage() + ")");
		}

		return c;
	}

	private String getProgramName()
	{
		String userPrgName = Settings.getInstance().getProperty("workbench.db.connection.info.programname", null);
		if (userPrgName != null) return userPrgName;

		return ResourceMgr.TXT_PRODUCT_NAME + " " + ResourceMgr.getBuildNumber();
	}

	private boolean doSetAppName()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.connection.set.appname", true);
	}

	/**
	 * Pust the application name and connection information into the passed connection properties.
	 *
	 * @param props the properties to be used when establishing the connection
	 * @param url the JDBC url (needed to identify the DBMS)
	 * @param id the internal connection id
	 * @param user the user for the connection
	 * @throws UnknownHostException
	 */
	private void setAppInfo(Properties props, String url, String id, String user)
		throws UnknownHostException
	{
		if (!doSetAppName()) return;

		// identify the program name when connecting
		// this is different for each DBMS.
		String appNameProperty = null;
		String prgName = getProgramName();

		if (url.startsWith("jdbc:oracle:thin"))
		{
			appNameProperty = "v$session.program";
			if (id != null && !props.containsKey("v$session.terminal")) props.put("v$session.terminal", StringUtil.getMaxSubstring(id, 30));

			// it seems that the Oracle 10 driver does not
			// add this to the properties automatically
			// (as the drivers for 8 and 9 did)
			user = System.getProperty("user.name",null);
			if (user != null && !props.containsKey("v$session.osuser")) props.put("v$session.osuser", user);
		}
		else if (url.startsWith("jdbc:inetdae"))
		{
			appNameProperty = "appname";
		}
		else if (url.startsWith("jdbc:jtds"))
		{
			appNameProperty = "APPNAME";
		}
		else if (url.startsWith("jdbc:microsoft:sqlserver"))
		{
			// Old MS SQL Server driver
			appNameProperty = "ProgramName";
		}
		else if (url.startsWith("jdbc:sqlserver:"))
		{
			// New SQL Server 2005 JDBC driver
			appNameProperty = "applicationName";
			if (!props.containsKey("workstationID"))
			{
				InetAddress localhost = InetAddress.getLocalHost();
				String localName = (localhost != null ? localhost.getHostName() : null);
				if (localName != null)
				{
					props.put("workstationID", localName);
				}
			}
		}
		else if (url.startsWith("jdbc:db2:"))
		{
			appNameProperty = "clientApplicationInformation";
			prgName = ResourceMgr.TXT_PRODUCT_NAME + " (" + id + ")";
			if (!props.containsKey("clientProgramName"))
			{
				props.put("clientProgramName", ResourceMgr.TXT_PRODUCT_NAME);
			}
		}
		else if (url.startsWith("jdbc:firebirdsql:"))
		{
			System.setProperty("org.firebirdsql.jdbc.processName", StringUtil.getMaxSubstring(prgName, 253));
		}
		else if (url.startsWith("jdbc:sybase:tds"))
		{
			appNameProperty = "APPLICATIONNAME";
		}

		if (appNameProperty != null && !props.containsKey(appNameProperty))
		{
			props.put(appNameProperty, prgName);
		}

	}
	/**
	 *	This is a "simplified version of the connect() method
	 *  for issuing a "shutdown command" to Cloudscape
	 */
	public void commandConnect(String url)
		throws SQLException, ClassNotFoundException, Exception
	{
		this.loadDriverClass();
		Properties props = new Properties();
		LogMgr.logDebug("DbDriver.commandConnect()", "Sending command URL=" + url + " to database");
		this.driverClassInstance.connect(url, props);
	}

	public boolean equals(Object other)
	{
		if (other == null) return false;
		if (this.driverClass == null) return false;

		if (other instanceof DbDriver)
		{
			DbDriver o = (DbDriver)other;
			return StringUtil.equalString(o.getId(), getId());
		}
		else if (other instanceof String)
		{
			return StringUtil.equalString(this.driverClass, (String)other);
		}
		else
		{
			return false;
		}
	}

	protected String getId()
	{
		StringBuilder b = new StringBuilder(driverClass == null ? name.length() : driverClass.length() + name.length() + 1);
		b.append(driverClass == null ? "" : driverClass);
		b.append('$');
		b.append(name);
		return b.toString();
	}

	public int hashCode()
	{
		return getId().hashCode();
	}

	public int compareTo(DbDriver o)
	{
		return getId().compareTo(o.getId());
	}

}
