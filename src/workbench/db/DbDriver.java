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
import java.util.Properties;
import java.util.StringTokenizer;
import workbench.exception.WbException;
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
	public void setName(String name) { 	this.name = name; }
	
	public String getDriverClass() {  return this.driverClass; }
	public void setDriverClass(String driverClass) { this.driverClass = driverClass;	}
	
	public String getLibrary() { return this.library; }
	public void setLibrary(String library) { this.library = library; }
	
	public String toString() { return this.getDriverClass(); }

	public void setSampleUrl(String anUrl) { this.sampleUrl = anUrl; }
	public String getSampleUrl() { return this.sampleUrl; }
	
	private void loadDriverClass()
		throws WbException
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
				}
				this.classLoader = new URLClassLoader(url);
			}
			
			Class drvClass = this.classLoader.loadClass(this.driverClass);
			this.driverClassInstance = (Driver)drvClass.newInstance();
		}
		catch (Exception e)
		{
			this.classLoader = null;
			throw new WbException("Could not load driver class " + this.driverClass);
		}
	}
	
	public DbDriver createCopy()
	{
		DbDriver copy = new DbDriver();
		copy.driverClass = this.driverClass;
		copy.library = this.library;
		copy.sampleUrl = this.sampleUrl;
		return copy;
	}
	
	public Connection connect(String url, String user, String password)
		throws WbException, SQLException
	{
		Connection c = null;
		try
		{
			this.loadDriverClass();
			Properties props = new Properties();
			if (user != null) props.put("user", user);
			if (password != null) props.put("password", password);
			c = this.driverClassInstance.connect(url, props);
		}
		catch (WbException e)
		{
			throw e;
		}
		catch (SQLException e)
		{
			throw e;
		}
		catch (Throwable th)
		{
			throw new WbException("Error connecting to database. (" + th.getClass().getName() + " - " + th.getMessage() + ")");
		}
		
		return c;
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

}
