/*
 * DbDriver.java
 *
 * Created on January 25, 2002, 11:41 PM
 */

package workbench.db;

/**
 *	Represents a JDBC Driver definition.
 *	The definition includes a (logical) name, a driver class
 *	and (optional) a library from which the driver is to 
 *	be loaded.
 *	@author  thomas
 */
public class DbDriver
{
	
	/** Holds value of property name. */
	private String name;
	
	/** Holds value of property driverClass. */
	private String driverClass;
	
	/** Holds value of property library. */
	private String library;
	
	public DbDriver()
	{
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
	
}
