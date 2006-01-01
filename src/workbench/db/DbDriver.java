/*
 * DbDriver.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import java.sql.DriverManager;

/**
 *	Represents a JDBC Driver definition.
 *	The definition includes a (logical) name, a driver class
 *	and (optional) a library from which the driver is to
 *	be loaded.
 *	@author  support@sql-workbench.net
 */
public class DbDriver
{
	private static final String LIB_DIR_KEY = "%LibDir%";
	private Driver driverClassInstance;
	private URLClassLoader classLoader;

	private String name;
	private String driverClass;

	private String identifier;
	private String library;

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

	/** Creates a new instance of DbDriver */
	public DbDriver(String aName, String aClass, String aLibrary)
	{
		this.setName(aName);
		this.setDriverClass(aClass);
		this.setLibrary(aLibrary);
	}

	public String getName() { return this.name; }
	public void setName(String name)
	{
		this.name = name;
		this.identifier = null;
	}

	public String getDriverClass() {  return this.driverClass; }

	public void setDriverClass(String aClass)
	{
		this.driverClass = aClass.trim();
		this.identifier = null;
		this.driverClassInstance = null;
		this.classLoader = null;
	}

	public String getIdentifier()
	{
		if (this.identifier == null)
		{
			StringBuffer b = new StringBuffer(100);
			if (this.name != null)
			{
				b.append(this.name);
				b.append(" (");
				b.append(this.driverClass);
				b.append(")");
			}
			else
			{
				b.append(this.driverClass);
			}
			this.identifier = b.toString();
		}
		return this.identifier;
	}

	public String getLibrary() { return this.library; }
	public void setLibrary(String libList)
	{
		this.library = libList; 
		this.driverClassInstance = null;
		this.classLoader = null;
	}

	public boolean canReadLibrary()
	{
		StringTokenizer tok = new StringTokenizer(this.library, StringUtil.PATH_SEPARATOR);
		while(tok.hasMoreTokens())
		{
			String lib = tok.nextToken().trim();
			lib = replaceLibDirKey(lib);
			File f = new File(lib);
			if (!f.exists()) return false;
		}
		return true;
	}

	public String toString()
	{
		return this.getIdentifier();
	}

	public void setSampleUrl(String anUrl) { this.sampleUrl = anUrl; }
	public String getSampleUrl() { return this.sampleUrl; }

	private void loadDriverClass()
		throws ClassNotFoundException, Exception
	{
		if (this.driverClassInstance != null) return;
		
		try
		{
			if (this.classLoader == null)
			{
				StringTokenizer tok = new StringTokenizer(this.library, StringUtil.PATH_SEPARATOR);
				URL[] url = new URL[tok.countTokens()];
				for (int i=0; tok.hasMoreTokens(); i++)
				{
					String fname = tok.nextToken().trim();
					String realFile = replaceLibDirKey(fname);
					url[i] = new File(realFile).toURL();
					LogMgr.logInfo("DbDriver.loadDriverClass()", "Adding ClassLoader URL=" + url[i].toString());
				}
				this.classLoader = new URLClassLoader(url);
			}

			// New Firebird 2.0 driver needs this, and it does not seem to do any harm
			// for other drivers
			Thread.currentThread().setContextClassLoader(classLoader);
			
			Class drvClass = this.classLoader.loadClass(this.driverClass);
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
		copy.library = this.library;
		copy.sampleUrl = this.sampleUrl;
		copy.name = this.name;
		return copy;
	}

	public Connection connect(String url, String user, String password)
		throws ClassNotFoundException, SQLException, Exception
	{
		return this.connect(url, user, password, null);
	}

	public Connection connect(String url, String user, String password, String id)
		throws ClassNotFoundException, SQLException, Exception
	{
		return this.connect(url, user, password, id, null);
	}

	public Connection connect(String url, String user, String password, String id, Properties connProps)
		throws ClassNotFoundException, SQLException, Exception
	{
		Connection c = null;
		try
		{
			this.loadDriverClass();
			boolean verify = Settings.getInstance().getVerifyDriverUrl();
			if (!this.driverClassInstance.acceptsURL(url))
			{
				String msg = ResourceMgr.getString("ErrorInvalidUrl");
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
			// If nothing is specified, Oracle would only list "JDBC Thin Driver"
			// as the client program, which isn't very nice.
			String propName = null;
			if (url.startsWith("jdbc:oracle"))
			{
				propName = "v$session.program";
				if (id != null) props.put("v$session.terminal", id);
				
				// it seems that the Oracle 10 driver does not 
				// add this to the properties automatically
				// (as the drivers for 8 and 9 did)
				user = System.getProperty("user.name");
				props.put("v$session.osuser", user);
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
				propName = "ProgramName";
			}

			if (propName != null && !props.containsKey(propName))
			{
				String appName = ResourceMgr.TXT_PRODUCT_NAME + " (" + ResourceMgr.getBuildId() +")";
				props.put(propName, appName);
			}

			c = this.driverClassInstance.connect(url, props);
			if (c == null)
			{
				LogMgr.logError("DbDriver.connect()", "No connection returned by driver " + this.driverClass + " for URL=" + url, null);
				throw new Exception("Driver did not return a connection for url=" + url);
			}
		}
		catch (ClassNotFoundException e)
		{
			LogMgr.logError("DbDriver.connect()", "Driver class not found", e);
			throw e;
		}
		catch (SQLException e)
		{
			LogMgr.logError("DbDriver.connect()", "Error connecting to driver " + this.driverClass, e);
			throw e;
		}
		catch (Throwable th)
		{
			LogMgr.logError("DbDriver.connect()", "Error connecting to driver " + this.driverClass, th);
			throw new Exception("Error connecting to database. (" + th.getClass().getName() + " - " + th.getMessage() + ")");
		}

		return c;
	}

	/**
	 *	This is a "simplified version of the connect() method
	 *  for issuing a "shutdown command" to Cloudscape
	 */
	void commandConnect(String url)
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

	public static Comparator getNameComparator()
	{
		return new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				if (o1 instanceof DbDriver && o2 instanceof DbDriver)
				{
					String name1 = ((DbDriver)o1).name;
					String name2 = ((DbDriver)o2).name;
					return name1.compareTo(name2);
				}
				return 0;
			}
		};
	}

	public static Comparator getDriverClassComparator()
	{
		return new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				if (o1 instanceof DbDriver && o2 instanceof DbDriver)
				{
					String drv1 = ((DbDriver)o1).getIdentifier(); // returns driver class & name
					String drv2 = ((DbDriver)o2).getIdentifier();
					return drv1.compareTo(drv2);
				}
				return 0;
			}
		};
	}

	private String replaceLibDirKey(String aPathname)
	{
		if (aPathname == null) return null;
		String libDir = Settings.getInstance().getLibDir();
		if (libDir == null) return aPathname;
		return StringUtil.replace(aPathname, LIB_DIR_KEY, libDir);
	}	
}
