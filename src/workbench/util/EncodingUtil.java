/*
 * EncodingUtil.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JComponent;
import workbench.resource.Settings;

/**
 * Utility class to handle encoding related stuff
 *
 * @author  support@sql-workbench.net
 */
public class EncodingUtil
{
	private static String[] charsets;
	
	/**
	 *	Create a BufferedReader for the given file and encoding
	 *  The buffer size is set to 64K
	 */
	public static Reader createReader(File f, String encoding)
		throws IOException, UnsupportedEncodingException
	{
		Reader r = null;
		if (encoding != null)
		{
			try
			{
				InputStream inStream = new FileInputStream(f);
				String enc = cleanupEncoding(encoding);
				
				if (enc.toLowerCase().startsWith("utf"))
				{
					r = new UnicodeReader(inStream, enc);
				}
				else
				{
					r = new InputStreamReader(inStream, enc);
				}
			}
			catch (UnsupportedEncodingException e)
			{
				throw e;
			}
		}
		else
		{
			r = new FileReader(f);
		}
		return r;
	}
	
	/**
	 * Create a BufferedReader for the given file and encoding.
	 * If no encoding is given, then a regular FileReader without 
	 * a specific encoding is used.
	 * The default buffer size is 16kb
	 */
	public static BufferedReader createBufferedReader(File f, String encoding)
		throws IOException
	{
		return createBufferedReader(f, encoding, 16*1024);
	}
	
	/**
	 * Create a BufferedReader for the given file, encoding and buffer size.
	 * If no encoding is given, then a regular FileReader without 
	 * a specific encoding is used.
	 */
	public static BufferedReader createBufferedReader(File f, String encoding, int buffSize)
		throws IOException
	{
		Reader r = createReader(f, encoding);
		return new BufferedReader(r, buffSize);
	}
	
	/**
	 * Allow some common other names for encodings (e.g. UTF for UTF-8) 
	 */
	public static String cleanupEncoding(String input)
	{
		if (input == null) return null;
		if ("utf".equalsIgnoreCase(input)) return "UTF-8";
		if ("utf8".equalsIgnoreCase(input)) return "UTF-8";
		return input;
	}

	/**
	 * Returns the system's default encoding. 
	 */
	public static String getDefaultEncoding()
	{
		String enc = Settings.getInstance().getDefaultFileEncoding();
		return enc;
	}
	/**
	 * Return all available encodings.
	 */
	public static String[] getEncodings()
	{
		if (charsets == null)
		{
			Map sets = java.nio.charset.Charset.availableCharsets();
			Iterator names = sets.keySet().iterator();
			charsets = new String[sets.size()];
			int i=0;
			while (names.hasNext())
			{
				charsets[i] = (String)names.next();
				i++;
			}
		}
		return charsets;
	}

	/**
	 * Test if the given encoding is supported. Before this is 
	 * tested, cleanupEncoding() is called to allow for some
	 * common "abbreviations"
	 * @see cleanupEncoding(String)
	 */
	public static boolean isEncodingSupported(String encoding)
	{
		String enc = cleanupEncoding(encoding);
		try
		{
			Charset.forName(enc);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public static JComponent createEncodingPanel(String encoding)
	{
		try
		{
			Class cls = Class.forName("workbench.gui.components.EncodingPanel");
			Class[] types = new Class[] { String.class };
			Constructor cons = cls.getConstructor(types);
			Object[] args =  new Object[] { encoding };
			JComponent p = (JComponent)cons.newInstance(args);
			return p;
		} 
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static JComponent createEncodingPanel()
	{
		try
		{
			return (JComponent)Class.forName("workbench.gui.components.EncodingPanel").newInstance();
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
