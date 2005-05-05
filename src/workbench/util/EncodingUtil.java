/*
 * EncodingUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import workbench.resource.Settings;

/**
 * Utility class to handle encoding related stuff
 *
 * @author  info@sql-workbench.net
 */
public class EncodingUtil
{
	private static String[] charsets;
	
	/**
	 *	Create a BufferedReader for the given file and encoding
	 */
	public static BufferedReader createReader(File f, String encoding)
		throws IOException, Exception
	{
		return createReader(f, encoding, 512*1024);
	}
	
	/**
	 * Create a BufferedReader for the given file and encoding.
	 * If no encoding is given, then a regular FileReader without 
	 * a specific encoding is used.
	 */
	public static BufferedReader createReader(File f, String encoding, int buffSize)
		throws IOException, Exception
	{
		BufferedReader in = null;
		if (encoding != null)
		{
			try
			{
				InputStream inStream = new FileInputStream(f);
				in = new BufferedReader(new InputStreamReader(inStream, cleanupEncoding(encoding)),buffSize);
			}
			catch (UnsupportedEncodingException e)
			{
				throw new Exception("Encoding " + encoding + " not supported");
			}
		}
		else
		{
			in = new BufferedReader(new FileReader(f), buffSize);
		}
		return in;
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
			Charset charset = Charset.forName(enc);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
}
