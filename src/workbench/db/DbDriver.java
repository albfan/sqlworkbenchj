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
	
	/** Creates a new instance of DbDriver */
	public DbDriver(String aName, String aClass, String aLibrary)
	{
		this.setName(aName);
		this.setDriverClass(aClass);
		this.setLibrary(aLibrary);
	}
	
	/** Getter for property name.
	 * @return Value of property name.
	 */
	public String getName()
	{
		return this.name;
	}
	
	/** Setter for property name.
	 * @param name New value of property name.
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/** Getter for property driverClass.
	 * @return Value of property driverClass.
	 */
	public String getDriverClass()
	{
		return this.driverClass;
	}
	
	/** Setter for property driverClass.
	 * @param driverClass New value of property driverClass.
	 */
	public void setDriverClass(String driverClass)
	{
		this.driverClass = driverClass;
	}
	
	/** Getter for property library.
	 * @return Value of property library.
	 */
	public String getLibrary()
	{
		return this.library;
	}
	
	/** Setter for property library.
	 * @param library New value of property library.
	 */
	public void setLibrary(String library)
	{
		this.library = library;
	}
	
	public String toString()
	{
		return this.getName() + " - " + this.getDriverClass();
	}
	
}
