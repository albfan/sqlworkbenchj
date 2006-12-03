/*
 * WbDesCipher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.StringTokenizer;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import workbench.log.LogMgr;

/**
 * @author  support@sql-workbench.net
 */
public class WbDesCipher
	implements WbCipher
{
	private static final byte[] KEY_DATA = {-108,-50,-5,-75,-98,28,-116,107};
	private static final SecretKeySpec KEY = new SecretKeySpec(KEY_DATA, "DES");
	private Cipher DesCipher; 
	
	/** Creates a new instance of WbCipher */
	public WbDesCipher()
	{
		try
		{
			DesCipher = Cipher.getInstance("DES");
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbDesCipher.init()", "No encryption available!");
			DesCipher = null;
		}
	}

	public String decryptString(String aValue)
	{
		if (aValue == null) return aValue;
		try
		{
			DesCipher.init(Cipher.DECRYPT_MODE, KEY);

			byte[] encrypted = this.makeArray(aValue);
			byte[] decrypted = DesCipher.doFinal(encrypted);
			String result = new String(decrypted);
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return aValue;
		}
	}

	public String encryptString(String aValue)
	{
		if (aValue == null) return null;
		if (DesCipher == null) return aValue;
		try
		{
			DesCipher.init(Cipher.ENCRYPT_MODE, KEY);
			byte[] values = aValue.getBytes();
			byte[] encrypted = DesCipher.doFinal(values);
			return this.makeString(encrypted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return aValue;
		}
	}

	/**
	 *	Creates a String from the given array
	 *	which can be used to store the array
	 *	in a text file (e.g. XML)
	 *	
	 *	@see #makeArray(String)
	 */
	private String makeString(byte[] values)
	{
		StringBuilder buff = new StringBuilder(values.length * 3);
		for (int i=0; i < values.length; i++)
		{
			buff.append('#');
			buff.append(values[i]);
		}
		return buff.toString();
	}

	/**
	 *	Internal method which converts an "Array String" into
	 *	a byte array which can be used for decoding
	 *
	 *	@see #makeString(byte[])
	 */
	private byte[] makeArray(String values)
	{
		StringTokenizer tok = new StringTokenizer(values, "#");
		byte[] result = new byte[tok.countTokens()];
		byte b;
		String c;
		int i=0;
		while (tok.hasMoreTokens())
		{
			c = tok.nextToken();
			try
			{
				b = Byte.parseByte(c);
				result[i] = b;
				i++;
			}
			catch (NumberFormatException e)
			{
				return new byte[1];
			}
		}
		return result;
	}
	
}
