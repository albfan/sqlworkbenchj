/*
 * DbDriver.java
 *
 * Created on January 25, 2002, 11:41 PM
 */

package workbench.db;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	Represents a JDBC Driver definition.
 *	The definition includes a (logical) name, a driver class
 *	and (optional) a library from which the driver is to
 *	be loaded.
 *	@author  thomas
 */
public class DbDriver
{
	private Driver driverClassInstance;
	private URLClassLoader classLoader;

	/** Holds value of property name. */
	private String name;

	/** Holds value of property driverClass. */
	private String driverClass;

	private String identifier;

	/** Holds value of property library. */
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
		this.driverClass = aDriverClassname;
		this.name = this.driverClass;
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

	public void setDriverClass(String driverClass)
	{
		this.driverClass = driverClass;
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
	public void setLibrary(String library)
	{
		this.library = library;
		this.driverClassInstance = null;
		this.classLoader = null;
	}

	public boolean canReadLibrary()
	{
		StringTokenizer tok = new StringTokenizer(this.library, StringUtil.PATH_SEPARATOR);
		while(tok.hasMoreTokens())
		{
			File f = new File(tok.nextToken().trim());
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
					url[i] = new File(tok.nextToken().trim()).toURL();
					LogMgr.logInfo("DbDriver.loadDriverClass()", "Adding ClassLoader URL=" + url[i].toString());
				}
				this.classLoader = new URLClassLoader(url);
			}

			Class drvClass = this.classLoader.loadClass(this.driverClass);
			this.driverClassInstance = (Driver)drvClass.newInstance();
		}
		catch (ClassNotFoundException e)
		{
			throw e;
		}
		catch (Throwable e)
		{
			this.classLoader = null;
			LogMgr.logError("DbDriver.loadDriverClass()", "Error loading driver class", e);
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
			boolean verify = WbManager.getSettings().getVerifyDriverUrl();
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
			if (user != null) props.put("user", user);
			if (password != null) props.put("password", password);

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
				/*
				user = System.getProperty("user.name");
				System.out.println("user = " + user);
				props.put("v$session.osuser", user);
				*/
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
				String appName = ResourceMgr.TXT_PRODUCT_NAME;

				if (WbManager.getSettings().getShowBuildInConnectionId())
				{
					String build = ResourceMgr.getString("TxtBuildNumber");
					if (build.startsWith("["))
					{
						appName = appName + " " +build;
					}
					else
					{
						appName = appName + " (B" + build + ")";
					}
				}

				props.put(propName, appName);
			}

			c = this.driverClassInstance.connect(url, props);
			if (c == null)
			{
				throw new Exception("Driver did not return a connection!");
			}
		}
		catch (ClassNotFoundException e)
		{
			throw e;
		}
		catch (SQLException e)
		{
			throw e;
		}
		catch (Throwable th)
		{
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

}