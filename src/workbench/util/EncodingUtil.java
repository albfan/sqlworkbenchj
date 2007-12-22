/*
 * EncodingUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
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
		InputStream inStream = new FileInputStream(f);
		return createReader(inStream, encoding);
	}
	
	public static Reader createReader(InputStream in, String encoding)
		throws IOException, UnsupportedEncodingException
	{
		Reader r = null;
		if (encoding != null)
		{
			try
			{
				String enc = cleanupEncoding(encoding);
				
				if (enc.toLowerCase().startsWith("utf"))
				{
					r = new UnicodeReader(in, enc);
				}
				else
				{
					r = new InputStreamReader(in, enc);
				}
			}
			catch (UnsupportedEncodingException e)
			{
				throw e;
			}
		}
		else
		{
			r = new InputStreamReader(in);
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
	public synchronized static String[] getEncodings()
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
	 * @see #cleanupEncoding(String)
	 */
	public static boolean isEncodingSupported(String encoding)
	{
		String enc = cleanupEncoding(encoding);
		try
		{
			Charset.forName(enc);
			return true;
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	public static Writer createWriter(File outfile, String encoding, boolean append)
		throws IOException
	{
		return createWriter(new FileOutputStream(outfile, append), encoding);
	}
	
	public static Writer createWriter(OutputStream stream, String encoding)
		throws IOException
	{
		Writer pw = null;
		final int buffSize = 64*1024;
		if (encoding != null)
		{
			try
			{
				OutputStreamWriter out = new OutputStreamWriter(stream, cleanupEncoding(encoding));
				pw = new BufferedWriter(out, buffSize);
			}
			catch (UnsupportedEncodingException e)
			{
				// Fall back to default encoding
				pw = new BufferedWriter(new OutputStreamWriter(stream), buffSize);
				String msg = ResourceMgr.getString("ErrWrongEncoding");
				msg = StringUtil.replace(msg, "%encoding%", encoding);
				LogMgr.logError("EncodingUtil.createWriter()", msg, e);
			}
		}
		return pw;
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
