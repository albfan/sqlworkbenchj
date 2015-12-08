/*
 * DbDriver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.postgres.PostgresUtil;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

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
  public static final String LIB_SEPARATOR = "|";

	private Driver driverClassInstance;
	private URLClassLoader classLoader;

	protected String name;
	private String driverClass;
	private List<String> libraryList;
	private boolean isTemporary;
	private String sampleUrl;
  private static final String MS_AUTHDLL = "sqljdbc_auth.dll";

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

  public boolean isTemporaryDriver()
  {
    return isTemporary;
  }

  /**
   * Marks this driver as a temporary driver that should not be saved.
   *
   */
	public void setTemporary()
	{
		isTemporary = true;
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

	private boolean doAddLibraryPath()
	{
    // alway adjust the library path for SQL Server driver to make enabling Windows authentication easier.
    if (driverClass.equals("com.microsoft.sqlserver.jdbc.SQLServerDriver"))
    {
      // if the DLL is already available on the library.path, there is no need to change it
      return FileUtil.isDLLAvailable(MS_AUTHDLL) == false;
    }

		return Settings.getInstance().getBoolProperty("workbench.dbdriver.fixlibrarypath", false);
	}

  private String findAuthDLLDir()
  {
    boolean is64Bit = System.getProperty("os.arch").equals("amd64");
    if (CollectionUtil.isEmpty(libraryList)) return null;

    // the Microsoft Driver is only a single jar file, no need to search more than one entry
    WbFile f = buildFile(libraryList.get(0));
    String jarDir = f.getAbsoluteFile().getParent();

    WbFile authDir = null;
    if (is64Bit)
    {
      authDir = new WbFile(jarDir, "auth\\x64");
    }
    else
    {
      authDir = new WbFile(jarDir, "auth\\x86");
    }

    File authDLL = new File(authDir, MS_AUTHDLL);
    if (authDLL.exists())
    {
      return authDir.getFullPath();
    }
    return null;
  }

	private void addToLibraryPath()
	{
    if (CollectionUtil.isEmpty(libraryList)) return;

    String addPath = null;

    if (driverClass.equals("com.microsoft.sqlserver.jdbc.SQLServerDriver"))
    {
      addPath = findAuthDLLDir();
    }

    if (addPath == null)
    {
      // By putting the directories into a Set, we make sure each directory is only added once
      Set<String> paths = new TreeSet<>();
      
      for (String file : libraryList)
      {
        WbFile f = buildFile(file);
        File dir = f.getParentFile().getAbsoluteFile();

        // only add the directory if it isn't already on the path
        if (FileUtil.isDirectoryOnLibraryPath(dir) == false)
        {
          paths.add(dir.getAbsolutePath());
        }
      }

      if (CollectionUtil.isNonEmpty(paths))
      {
        addPath = "";
        for (String path : paths)
        {
          addPath += path + File.pathSeparator;
        }
      }
    }

    if (StringUtil.isBlank(addPath)) return;

		String current = System.getProperty("java.library.path");

		String newPath = current + File.pathSeparator + addPath;

    LogMgr.logInfo("DbDriver.addToLibraryPath()", "Adding " + addPath + " to java.library.path");
		LogMgr.logDebug("DbDriver.addToLibraryPath()", "Setting java.library.path=" + newPath);

		System.setProperty("java.library.path", newPath);

		// the following hack is taken from: http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
		// the explanation for this hack is:
		// The Classloader has a static field (sys_paths) that contains the paths.
		// If that field is set to null, it is initialized automatically. Therefore forcing that field to null will result
		// into the reevaluation of the library path as soon as loadLibrary() is called and as we have changed
		// java.library.path before nulling the field, this will result in an updated java.library.path
		try
		{
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		}
		catch (Throwable nf)
		{
			LogMgr.logError("DbDriver.addToLibraryPath()", "Could not modify system path!", nf);
		}
	}

	public void setLibraryList(List<String> files)
	{
		if (CollectionUtil.isEmpty(files))
		{
			this.libraryList = null;
		}
		else
		{
			this.libraryList = new ArrayList<>(files);
		}
	}

	public List<String> getLibraryList()
	{
		if (CollectionUtil.isEmpty(this.libraryList)) return Collections.emptyList();
		return Collections.unmodifiableList(libraryList);
	}

	public static List<String> splitLibraryList(String libList)
	{
		if (libList == null) return null;

		if (libList.indexOf(LIB_SEPARATOR) > -1)
		{
			return StringUtil.stringToList(libList, LIB_SEPARATOR, true, true, false);
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

		if (isTemporary) return true;

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

	@Override
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
		Thread.currentThread().setContextClassLoader(classLoader);
		Class clz = this.classLoader.loadClass(className);
		return clz;
	}

	private synchronized void loadDriverClass()
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
					File f = buildFile(fname);
					url[index] = f.toURI().toURL();
					LogMgr.logInfo("DbDriver.loadDriverClass()", "Adding ClassLoader URL=" + url[index].toString());
					index ++;
				}
				classLoader = new URLClassLoader(url, ClassLoader.getSystemClassLoader());
			}

			if (doAddLibraryPath())
			{
				addToLibraryPath();
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

	private WbFile buildFile(String fname)
	{
		String realFile = Settings.getInstance().replaceLibDirKey(fname);
		WbFile f = new WbFile(realFile);
		if (f.getParentFile() == null)
		{
			f = new WbFile(Settings.getInstance().getLibDir(), realFile);
		}
		return f;
	}

	public DbDriver createCopy()
	{
		DbDriver copy = new DbDriver();
		copy.driverClass = this.driverClass;
		copy.libraryList = new ArrayList<>(libraryList);
		copy.sampleUrl = this.sampleUrl;
		copy.name = this.name;

		// the internal attribute should not be copied!

		return copy;
	}

	private boolean useEmptyStringForEmptyPassword()
	{
		return Settings.getInstance().getBoolProperty(this.driverClass + ".use.emptypassword", false);
	}

	private boolean useEmptyStringForEmptyUser()
	{
		return Settings.getInstance().getBoolProperty(this.driverClass + ".use.emptyuser", false);
	}

	public Connection connect(String url, String user, String password, String id, Properties connProps)
		throws ClassNotFoundException, NoConnectionException, SQLException
	{
		Connection conn = null;
		try
		{
			loadDriverClass();

			Properties props = new Properties();
			if (StringUtil.isNonBlank(user))
			{
				props.put("user", user);
			}
			else if (useEmptyStringForEmptyUser())
			{
				props.put("user", "");
			}

			if (StringUtil.isNonBlank(password))
			{
				props.put("password", password);
			}
			else if (useEmptyStringForEmptyPassword())
			{
				props.put("password", "");
			}

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

			// this replaces the deleted MySQLTableCommentReader
			if (url.startsWith("jdbc:mysql")
					&& Settings.getInstance().getBoolProperty("workbench.db.mysql.tablecomments.retrieve", false)
				  && !props.containsKey("useInformationSchema"))
			{
				// see: http://bugs.mysql.com/bug.php?id=65213
				props.setProperty("useInformationSchema", "true");
			}

			setAppInfo(props, url.toLowerCase(), id, user);

			conn = this.driverClassInstance.connect(url, props);

			// The system property for the Firebird driver is only needed when the connection is created
			// so after the connect was successful, we can clean up the system properties
			if (doSetAppName() && url.startsWith("jdbc:firebirdsql:"))
			{
				System.clearProperty("org.firebirdsql.jdbc.processName");
			}

			// PostgreSQL 9.0 allows to set an application name, but currently only by executing a SQL statement
			if (doSetAppName() && url.startsWith("jdbc:postgresql") && !props.containsKey(PostgresUtil.APP_NAME_PROPERTY))
			{
				PostgresUtil.setApplicationName(conn, getProgramName() + " (" + id + ")");
			}
		}
		catch (ClassNotFoundException | UnsupportedClassVersionError e)
		{
			throw e;
		}
		catch (Throwable th)
		{
			LogMgr.logError("DbDriver.connect()", "Error connecting to the database using URL=" + url + ", username=" + user, th);
			throw new SQLException(th.getMessage(), th);
		}

		if (conn == null)
		{
			LogMgr.logError("DbDriver.connect()", "No connection returned by driver " + this.driverClass + " for URL=" + url, null);
			throw new NoConnectionException("Driver did not return a connection for url=" + url);
		}

		return conn;
	}

	public void releaseDriverInstance()
	{
		LogMgr.logDebug("DbDriver.releaseDriverInstance()", "Releasing classloader and driver");
		if (this.driverClassInstance != null)
		{
			try
			{
				DriverManager.deregisterDriver(this.driverClassInstance);
			}
			catch (SQLException sql)
			{
				LogMgr.logWarning("DbDriver.releaseDriverInstance()", "Could not de-register driver", sql);
			}
			this.driverClassInstance = null;
		}
		this.classLoader = null;
		System.gc();
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

		if (url.startsWith("jdbc:postgresql") && PostgresUtil.supportsAppInfoProperty(this.driverClassInstance.getClass()))
		{
			appNameProperty = PostgresUtil.APP_NAME_PROPERTY;
			prgName += " (" + id + ")";
		}

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

	@Override
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

	@Override
	public int hashCode()
	{
		return getId().hashCode();
	}

	@Override
	public int compareTo(DbDriver o)
	{
		return getId().compareTo(o.getId());
	}

}
