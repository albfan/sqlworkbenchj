/*
 * TextlistReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import workbench.log.LogMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class TextlistReader
{
	private List<String> values;
	
	/**
	 * Reads each line of the passed input stream into 
	 * an element of the internal values collection. 
	 * Elements will be trimmed.
	 */
	public TextlistReader(InputStream in)
	{
		try
		{
			values = new LinkedList<String>();
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			String line = r.readLine();
			while (line != null)
			{
				values.add(line.trim());
				line = r.readLine();
			}
			//LogMgr.logDebug("TextlistReader.<init>", values.size() + " element read from InputStream");
		}
		catch (Exception e)
		{
			LogMgr.logError("TestlistReader.<init>", "Error reading input stream", e);
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
	}
	
	public Collection<String> getValues()
	{
		return values;
	}
	
}
