/*
 * ConnectionInfo.java
 *
 * Created on December 26, 2001, 3:32 PM
 */
package workbench.db;


/**
 *	Supplies connection information as stored in 
 *	the configuration files. This is used to read & parse
 *	the xml file which stores user defined configuration.
 *
 *	@author  thomas
 */
public class ConnectionInfo
{
	private String name;
	private String url;
	private String driverclass;
	private String driverlib;
	private String username;
	private String password;
	
	public ConnectionInfo(String aName)
	{
		this.name = aName;
	}

	/** Getter for property url.
	 * @return Value of property url.
	 */
	public java.lang.String getUrl()
	{
		return this.url;
	}	
	
	/** Setter for property url.
	 * @param url New value of property url.
	 */
	public void setUrl(java.lang.String aUrl)
	{
		this.url = aUrl;
	}
	
	/** Getter for property driverclass.
	 * @return Value of property driverclass.
	 */
	public java.lang.String getDriverclass()
	{
		return this.driverclass;
	}
	
	/** Setter for property driverclass.
	 * @param driverclass New value of property driverclass.
	 */
	public void setDriverclass(java.lang.String aDriverclass)
	{
		this.driverclass = aDriverclass;
	}
	
	/** Getter for property user.
	 * @return Value of property user.
	 */
	public java.lang.String getUsername()
	{
		return this.username;
	}
	
	/** Setter for property user.
	 * @param user New value of property user.
	 */
	public void setUsername(java.lang.String aUsername)
	{
		this.username = aUsername;
	}
	
	/** Getter for property password.
	 * @return Value of property password.
	 */
	public java.lang.String getPassword()
	{
		return password;
	}

	/** Setter for property password.
	 * @param password New value of property password.
	 */
	public void setPassword(java.lang.String aPassword)
	{
		this.password = aPassword;
	}
	
	public String getEncryptedPassword()
	{
		return this.encryptPassword(this.password);
	}
	
	public void setEncryptedPassword(String anEncryptedPassword)
	{
		this.password = this.decryptPassword(anEncryptedPassword);
	}
	
	private String decryptPassword(String aPwd)
	{
		return aPwd;
	}
	
	private String encryptPassword(String aPwd)
	{
		return aPwd;
	}
	
	
}
