package workbench.util;

import java.util.StringTokenizer;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import workbench.log.LogMgr;

public class WbCipher
{
	private static final byte[] KEY_DATA = {-108,-50,-5,-75,-98,28,-116,107};
	private static final SecretKeySpec KEY = new SecretKeySpec(KEY_DATA, "DES");
	private Cipher DesCipher; 
	
	/** Creates a new instance of WbCipher */
	public WbCipher()
	{
		try
		{
			DesCipher = Cipher.getInstance("DES");
		}
		catch (Exception e)
		{
			LogMgr.logWarning("ConnectionProfile", "No encryption available!");
			DesCipher = null;
		}
	}

	public String decryptString(String aValue)
	{
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
		StringBuffer buff = new StringBuffer(values.length * 3);
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
	
	/**
	 *	Help method for creating the DES key.
	 */
	private static void createKey()
	{
		try
		{
			KeyGenerator keygen = KeyGenerator.getInstance("DES");
			SecretKey desKey = keygen.generateKey();
			
			byte[] keyvalue = desKey.getEncoded();
			System.out.print("byte[] KEY_DATA = {");
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

}
