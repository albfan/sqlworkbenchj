/*
 * ConnectionInfo.java
 *
 * Created on December 26, 2001, 3:32 PM
 */
package workbench.db;

import java.util.HashMap;
import java.io.FileOutputStream;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.io.FileInputStream;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *	Supplies connection information as stored in
 *	the configuration files. This is used to read & parse
 *	the xml file which stores user defined configuration.
 *
 *	@author  thomas
 */
public class ConnectionProfile
{
	private static final String CRYPT_PREFIX = "#e#";
	private String name;
	private String url;
	private String driverclass;
	private String driverlib;
	private String username;
	private String password;
	private static final byte[] KEY_DATA = {-108,-50,-5,-75,-98,28,-116,107};
	private static final SecretKeySpec KEY = new SecretKeySpec(KEY_DATA, "DES");
	
	public ConnectionProfile()
	{
	}
	
	public ConnectionProfile(String driverClass, String url, String userName, String pwd)
	{
		this.setUrl(url);
		this.setDriverclass(driverClass);
		this.setUsername(userName);
		this.setPassword(pwd);
	}
	
	/** Getter for property url.
	 * @return Value of property url.
	 */
	public String getUrl()
	{
		return this.url;
	}
	
	/** Setter for property url.
	 * @param url New value of property url.
	 */
	public void setUrl(String aUrl)
	{
		this.url = aUrl;
	}
	
	/** Getter for property driverclass.
	 * @return Value of property driverclass.
	 */
	public String getDriverclass()
	{
		return this.driverclass;
	}
	
	/** Setter for property driverclass.
	 * @param driverclass New value of property driverclass.
	 */
	public void setDriverclass(String aDriverclass)
	{
		this.driverclass = aDriverclass;
	}
	
	/** Getter for property user.
	 * @return Value of property user.
	 */
	public String getUsername()
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
	
	public void setPassword(String aPwd)
	{
		this.password = this.encryptPassword(aPwd);
	}
	
	public String getPassword()
	{
		return this.decryptPassword(this.password);
	}
	
	/** Getter for property password.
	 * @return Value of property password.
	 */
	public String getEncryptedPassword()
	{
		return password;
	}
	
	/** Setter for property password.
	 * @param password New value of property password.
	 */
	public void setEncryptedPassword(String aPassword)
	{
		this.password = aPassword;
	}
	
	public String decryptPassword(String aPwd)
	{
		if (aPwd.startsWith(CRYPT_PREFIX))
		{
			return aPwd.substring(3);
		}
		else
		{
			return aPwd;
		}
	}
	
	public String toString() { return this.name; }
	
	public String encryptPassword(String aPwd)
	{
		try
		{
			Cipher des = Cipher.getInstance("DES");
			des.init(Cipher.ENCRYPT_MODE, KEY);
			byte[] values = aPwd.getBytes();
			byte[] ecnrypted = des.doFinal(values);
			System.out.println("");
			return CRYPT_PREFIX + aPwd;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return aPwd;
		}
	}
	
	/**
	 *	Return the (logical) name
	 *	of this Connection definition
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 *	Set the (logical) name of this connection definition
	 */
	public void setName(String aName)
	{
		this.name = aName;
	}
	
	public static void createKey()
	{
		try
		{
			KeyGenerator keygen = KeyGenerator.getInstance("DES");
			SecretKey desKey = keygen.generateKey();
			
			byte[] keyvalue = desKey.getEncoded();
			System.out.print("byte[] myKey={");
			for (int i=0; i < keyvalue.length; i++)
			{
				System.out.print(keyvalue[i]);
				if (i < keyvalue.length - 1) System.out.print(",");
			}
			System.out.println("};");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void testCipher()
	{
		try
		{
			Cipher des = Cipher.getInstance("DES");
			des.init(Cipher.ENCRYPT_MODE, KEY);
			byte[] values = "secretepassword".getBytes();
			byte[] encrypted = des.doFinal(values);
			System.out.println("enc=" + encrypted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
			
	}
	
	public static void main(String args[])
	{
		//writeObjects();
		//readObjects();
		createKey();
	}
}
