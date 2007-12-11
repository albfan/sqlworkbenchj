/*
 * DbDriver.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import workbench.WbManager;

/**
 *	Represents a JDBC Driver definition.
 *	The definition includes a (logical) name, a driver class
 *	and (optional) a library from which the driver is to
 *	be loaded.
 * 
 *	@author  support@sql-workbench.net
 */
public class DbDriver
	implements Comparable<DbDriver>
{
	private Driver driverClassInstance;
	private URLClassLoader classLoader;

	protected String name;
	private String driverClass;
	private List<String> libraryList;

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

	public String getName() 
	{
		return this.name; 
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	public String getDriverClass() 
	{
		return this.driverClass; 
	}

	public void setDriverClass(String aClass)
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
	
	public void setLibrary(String libList)
	{
		this.libraryList = null;
		
		if (libList.indexOf("|") > -1)
		{
			this.libraryList = StringUtil.stringToList(libList, "|", true, true, false);
		}
		else if (!StringUtil.isEmptyString(libList))
		{
			this.libraryList = StringUtil.stringToList(libList, StringUtil.getPathSeparator(), true, true, false);
		}
		this.driverClassInstance = null;
		this.classLoader = null;
	}

	public boolean canReadLibrary()
	{
		if (WbManager.getInstance().isTestMode()) return true;
		if (libraryList != null)
		{
			for (String lib : libraryList)
			{
				lib = Settings.getInstance().replaceLibDirKey(lib);
				File f = new File(lib);
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

	public void setSampleUrl(String anUrl) { this.sampleUrl = anUrl; }
	public String getSampleUrl() { return this.sampleUrl; }

	public Class loadClassFromDriverLib(String className)
		throws ClassNotFoundException
	{
		if (this.classLoader == null) return null;
		Class clz = this.classLoader.loadClass(className);
		return clz;
	}
	
	private void loadDriverClass()
		throws ClassNotFoundException, Exception
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
					url[index] = new File(realFile).toURL();
					LogMgr.logInfo("DbDriver.loadDriverClass()", "Adding ClassLoader URL=" + url[index].toString());
					index ++;
				}
				this.classLoader = new URLClassLoader(url, ClassLoader.getSystemClassLoader());
			}

			Class drvClass = null;
			if (this.classLoader != null)
			{
				// New Firebird 2.0 driver needs this, and it does not seem to do any harm
				// for other drivers
				setContextClassLoader();//Thread.currentThread().setContextClassLoader(classLoader);
				drvClass = this.classLoader.loadClass(this.driverClass);
			}
			else
			{
				// Assume the driver class is available on the classpath
				//LogMgr.logDebug("DbDriver.loadDriverClass()", "Assuming driver " + this.driverClass + " is in current classpath");
				drvClass = Class.forName(this.driverClass);
			}
			
			this.driverClassInstance = (Driver)drvClass.newInstance();
			if (Settings.getInstance().getBoolProperty("workbench.db.registerdriver", false))
			{
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
			
			String dbLog = Settings.getInstance().getProperty("workbench.db.driver.log", null);
			if (!StringUtil.isEmptyString(dbLog))
			{
				try
				{
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(dbLog)));
					DriverManager.setLogWriter(pw);
				}
				catch (Exception e)
				{
					LogMgr.logError("DbDriver.loadDriverClass()", "Error setting driverManager logWriter", e);
				}
			}
			
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
			throw new Exception("Could not load driver class " + this.driverClass);
		}
	}

	public DbDriver createCopy()
	{
		DbDriver copy = new DbDriver();
		copy.driverClass = this.driverClass;
		copy.libraryList = new ArrayList<String>();
		copy.libraryList.addAll(this.libraryList);
		copy.sampleUrl = this.sampleUrl;
		copy.name = this.name;
		return copy;
	}

	Connection connect(String url, String user, String password, String id, Properties connProps)
		throws ClassNotFoundException, SQLException
	{
		Connection c = null;
		try
		{
			this.loadDriverClass();
			boolean verify = Settings.getInstance().getVerifyDriverUrl();
			if (!this.driverClassInstance.acceptsURL(url))
			{
				String msg = ResourceMgr.getString("ErrInvalidUrl");
				msg = msg.replaceAll("%driver%", this.driverClass);
				msg = msg + " " + url;
				if (verify)
				{
					throw new SQLException(msg);
				}
				else
				{
					LogMgr.logWarning("DbDriver.connect()", "The driver class " + this.driverClass  + " reports that it does not accept the given URL!");
				}
			}

			// as we are not using the DriverManager, we need to supply username
			// and password in the connection properties!
			Properties props = new Properties();
			if (user != null && user.trim().length() > 0) props.put("user", user);
			if (password != null && password.trim().length() > 0) props.put("password", password);

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

			// identify the program name when connecting
			// this is different for each DBMS.
			String propName = null;
			if (url.startsWith("jdbc:oracle"))
			{
				propName = "v$session.program";
				if (id != null) props.put("v$session.terminal", id);
				
				// it seems that the Oracle 10 driver does not 
				// add this to the properties automatically
				// (as the drivers for 8 and 9 did)
				user = System.getProperty("user.name",null);
				if (user != null) props.put("v$session.osuser", user);
			}
			else if (url.startsWith("jdbc:inetdae"))
			{
				propName = "appname";
			}
			else if (url.startsWith("jdbc:jtds"))
			{
				propName = "APPNAME";
			}
			else if (url.startsWith("jdbc:microsoft:sqlserver"))
			{
				// Old MS SQL Server driver
				propName = "ProgramName";
			}
			else if (url.startsWith("jdbc:sqlserver:"))
			{
				// New SQL Server 2005 JDBC driver
				propName = "applicationName";
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
			if (propName != null && !props.containsKey(propName))
			{
				String appName = ResourceMgr.TXT_PRODUCT_NAME + " (" + ResourceMgr.getBuildNumber() +")";
				props.put(propName, appName);
			}

			c = this.driverClassInstance.connect(url, props);
			if (c == null)
			{
				LogMgr.logError("DbDriver.connect()", "No connection returned by driver " + this.driverClass + " for URL=" + url, null);
				throw new SQLException("Driver did not return a connection for url=" + url);
			}
		}
		catch (ClassNotFoundException e)
		{
			// do not log this error, the caller will log it
			//LogMgr.logError("DbDriver.connect()", "Driver class not found", e);
			throw e;
		}
		catch (SQLException e)
		{
			// do not log this error, the caller will log it
			//LogMgr.logError("DbDriver.connect()", "Error connecting to driver " + this.driverClass, e);
			throw e;
		}
		catch (Throwable th)
		{
			LogMgr.logError("DbDriver.connect()", "Error connecting to driver " + this.driverClass, th);
			throw new SQLException("Error connecting to database. (" + th.getClass().getName() + " - " + th.getMessage() + ")");
		}

		return c;
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
			if (o.driverClass != null && o.driverClass.equals(this.driverClass))
			{
				return (this.name != null && this.name.equalsIgnoreCase(o.name));
			}
			else
			{
				return false;
			}
		}
		else if (other instanceof String)
		{
			return (this.driverClass != null && this.driverClass.equals((String)other));
		}
		else
		{
			return false;
		}
	}

	private void setContextClassLoader()
	{
		if (this.classLoader != null)
		{
			Thread.currentThread().setContextClassLoader(this.classLoader);
		}
	}

	protected String getId()
	{
		StringBuilder b = new StringBuilder(60);
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
		return getDescription().compareTo(o.getDescription());
	}

}
