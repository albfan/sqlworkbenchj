/*
 * ConnectionInfo.java
 *
 * Created on December 26, 2001, 3:32 PM
 */
package workbench.db;

import java.util.HashMap;
import java.util.Comparator;
import java.util.StringTokenizer;
import workbench.WbManager;
import workbench.log.LogMgr;

/**
 *	Supplies connection information as stored in
 *	the configuration files. This is used to read & parse
 *	the xml file which stores user defined configuration.
 *
 *	@author  thomas
 */
public class ConnectionProfile
{
	private static final String CRYPT_PREFIX = "@*@";
	private String name;
	private String url;
	private String driverclass;
	private String driverlib;
	private String username;
	private String password;
	private boolean autocommit;
	private String description;
	private boolean isNew;
	
	public ConnectionProfile()
	{
		this.isNew = true;
	}
	
	public ConnectionProfile(String driverClass, String url, String userName, String pwd)
	{
		this.isNew = true;
		this.setUrl(url);
		this.setDriverclass(driverClass);
		this.setUsername(userName);
		this.setPassword(pwd);
		this.setName(url);
	}
	
	public ConnectionProfile(String aName, String driverClass, String url, String userName, String pwd)
	{
		this.isNew = true;
		this.setUrl(url);
		this.setDriverclass(driverClass);
		this.setUsername(userName);
		this.setPassword(pwd);
		this.setName(aName);
	}
	
	/**
	 *	Sets the current password. If the password
	 *	is not already encrypted, it will be encrypted
	 *
	 *	@see #getPassword()
	 *	@see workbench.util.WbCipher#encryptString(String)
	 */
	public void setPassword(String aPwd)
	{
		if (!aPwd.startsWith(CRYPT_PREFIX))
		{
			this.password = CRYPT_PREFIX + this.encryptPassword(aPwd);
		}
		else
		{
			this.password = aPwd;
		}
	}

	
	/**
	 *	Returns the encrypted version of the password.
	 *	@see #decryptPassword(String)
	 */
	public String getPassword() { return this.password; }
	

	/**
	 *	Returns the plain text version of the
	 *	current password.
	 *
	 *	@see #decryptPassword(String)
	 */
	public String decryptPassword()
	{
		return this.decryptPassword(this.password);
	}

	/**
	 *	Returns the plain text version of the given
	 *	password. This is not put into the getPassword()
	 *	method because the XMLEncode would write the
	 *	password in plain text into the XML file.
	 *	A method beginning with decrypt is not 
	 *	regarded as a property and thus not written
	 *	to the XML file.
	 *
	 *	@parm the encrypted password
	 */
	public String decryptPassword(String aPwd)
	{
		if (!aPwd.startsWith(CRYPT_PREFIX))
		{
			return aPwd;
		}
		else
		{
			return WbManager.getInstance().getCipher().decryptString(aPwd.substring(CRYPT_PREFIX.length()));
		}
	}

	private String encryptPassword(String aPwd)
	{
		return WbManager.getInstance().getCipher().encryptString(aPwd);
	}
	
	/**
	 *	Returns the name of the Profile
	 */
	public String toString() { return this.name; }

	/** Two connection profiles are equal if:
	 *  <ul>
	 * 	<li>the url are equal</li>
	 *  <li>the driver classes are equal</li>
	 *	<li>the usernames are equal</li>
	 *	<li>the (encrypted) passwords are equal</li>
	 *  </ul> 
	 */	
	public boolean equals(Object other)
	{
		try 
		{
			ConnectionProfile prof = (ConnectionProfile)other;
			return this.url.equals(prof.url) && 
						 this.driverclass.equals(prof.driverclass) &&
						 this.username.equals(prof.username) &&
						 this.password.equals(prof.password);
		}
		catch (ClassCastException e)
		{
			return false;
		}
	}
	
	public String getUrl() { return this.url; }
	public void setUrl(String aUrl) { this.url = aUrl; }
	
	public String getDriverclass() { return this.driverclass; }
	public void setDriverclass(String aDriverclass) { this.driverclass = aDriverclass; }
	
	public String getUsername() { return this.username; }
	public void setUsername(java.lang.String aUsername) { this.username = aUsername; }

	public boolean getAutocommit() { return this.autocommit; }
	public void setAutocommit(boolean autocommit) { this.autocommit = autocommit; }
	
	public String getName() { return this.name; }
	public void setName(String aName) { this.name = aName;	}
	
	public String getDescription() { return this.description; }
	public void setDescription(String description) { this.description = description; }

	public static Comparator getNameComparator()
	{
		return new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				if (o1 instanceof ConnectionProfile && o2 instanceof ConnectionProfile)
				{
					String name1 = ((ConnectionProfile)o1).name;
					String name2 = ((ConnectionProfile)o2).name;
					return name1.compareTo(name2);				
				}
				return 0;
			}
		};
	}
	
}
