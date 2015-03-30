/*
 * WbDesCipher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.util;

import java.util.StringTokenizer;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import workbench.log.LogMgr;

/**
 * @author  Thomas Kellerer
 */
public class WbDesCipher
	implements WbCipher
{
	private static final byte[] KEY_DATA = {-108,-50,-5,-75,-98,28,-116,107};
	private static final SecretKeySpec KEY = new SecretKeySpec(KEY_DATA, "DES");
	private Cipher cipher;

	private static WbCipher instance;

	public static WbCipher getInstance()
	{
		synchronized (KEY_DATA)
		{
			if (instance == null)
			{
				WbDesCipher wb = new WbDesCipher();
				if (wb.cipher == null)
				{
					LogMgr.logWarning("WbDesCipher.getInstance()", "Could not create cipher. Using NullCipher!");
					instance = new WbNullCipher();
				}
				else
				{
					LogMgr.logDebug("WbDesCipher.getInstance()", "WbDesCipher created");
					instance = wb;
				}
			}
			return instance;
		}
	}

	private WbDesCipher()
	{
		try
		{
			cipher = Cipher.getInstance("DES");
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbDesCipher.init()", "No encryption available!");
			cipher = null;
		}
	}

	@Override
	public String decryptString(String aValue)
	{
		if (aValue == null) return aValue;
		try
		{
			cipher.init(Cipher.DECRYPT_MODE, KEY);

			byte[] encrypted = this.makeArray(aValue);
			byte[] decrypted = cipher.doFinal(encrypted);
			String result = new String(decrypted);
			return result;
		}
		catch (Exception e)
		{
			LogMgr.logError("WbDesCipher.decryptString()", "Could not decrypt", e);
			return aValue;
		}
	}

	@Override
	public String encryptString(String aValue)
	{
		if (aValue == null) return null;
		if (cipher == null) return aValue;
		try
		{
			cipher.init(Cipher.ENCRYPT_MODE, KEY);
			byte[] values = aValue.getBytes();
			byte[] encrypted = cipher.doFinal(values);
			return this.makeString(encrypted);
		}
		catch (Exception e)
		{
			LogMgr.logError("WbDesCipher.encryptString()", "Could not encrypt", e);
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
