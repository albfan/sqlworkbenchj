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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class to handle encoding related stuff
 *
 * @author  info@sql-workbench.net
 */
public class EncodingUtil
{
	private static String[] charsets;
	
	public static BufferedReader createReader(File f, String encoding)
		throws IOException, UnsupportedEncodingException
	{
		return createReader(f, encoding, 512*1024);
	}
	
	public static BufferedReader createReader(File f, String encoding, int buffSize)
		throws IOException, UnsupportedEncodingException
	{
		InputStream inStream = new FileInputStream(f);
		BufferedReader in = new BufferedReader(new InputStreamReader(inStream, encoding),buffSize);
		return in;
	}
	
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

	public static boolean isEncodingSupported(String encoding)
	{
		String[] available = getEncodings();
		for (int i=0; i < available.length; i++)
		{
			if (available[i].equals(encoding)) return true;
		}
		return false;
	}
	
}
