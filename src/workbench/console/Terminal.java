/*
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 *
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */

package workbench.console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 * A class to read the size of the terminal
 * On unix type systems stty is used, for Windows the mode command is used
 *
 * @author support@sql-workbench.net
 */
public class Terminal
{
	private int lines = -1;
	private int columns = -1;

	public Terminal()
	{
	}

	private synchronized void calculateScreenSize()
	{
		String os = System.getProperty("os.name");
		boolean isWindows = os.toLowerCase().startsWith("windows");

		if (isWindows)
		{
			runModeCommand();
		}
		else
		{
			if (!readFromEnvironment())
			{
				runSttyCommand();
			}
		}
	}

	public int getLines()
	{
		calculateScreenSize();
		return lines;
	}

	public int getColumns()
	{
		calculateScreenSize();
		return columns;
	}

	private boolean readFromEnvironment()
	{
		String l = System.getenv("LINES");
		String c = System.getenv("COLUMNS");
		if (StringUtil.isBlank(l) || StringUtil.isBlank(c)) return false;
		lines = StringUtil.getIntValue(l, -1);
		if (lines == -1)
		{
			lines = 24;
			return false;
		}
		columns = StringUtil.getIntValue(l, -1);
		if (columns == -1)
		{
			columns = 80;
			return false;
		}
		LogMgr.logDebug("Terminal.readFromEnvironment()", "Obtained console size from environment");
		return true;
	}

	private void runSttyCommand()
	{
		// This does not seem to work on Linux although running the same command
		// from the command line gives me the correct values. 
		LogMgr.logDebug("Terminal.runSttyCommand()", "Using stty to obtain console size");

		try
		{
			ProcessBuilder builder = new ProcessBuilder("stty", "-F /dev/tty", "-a");
			Process p = builder.start();
			InputStream out = p.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(out));
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = in.readLine()) != null)
			{
				result.append(line);
			}
			in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = in.readLine()) != null)
			{
				result.append(line);
			}

			System.out.println(result.toString());
			String[] items = result.toString().split(";");
			
			for (String item : items)
			{
				if (item.indexOf("rows") > -1)
				{
					String nr = item.trim().replaceAll("[^0-9]+", "");
					lines = StringUtil.getIntValue(nr, 24);
				}
				if (item.indexOf("columns") > -1)
				{
					String nr = item.trim().replaceAll("[^0-9]+", "");
					columns = StringUtil.getIntValue(nr, 80);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Terminal.runModeCommand()", "Error getting window size", e);
		}

	}
	private void runModeCommand()
	{
		try
		{
			ProcessBuilder builder = new ProcessBuilder("mode.com", "con");
			Process p = builder.start();
			InputStream out = p.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(out));
			String line = null;
			boolean linesRead = false;

			while ((line = in.readLine()) != null)
			{
				if (line.indexOf(":") == -1 || line.trim().endsWith(":")) continue;

				if (!linesRead)
				{
					// Number of lines is reported first
					int pos = line.indexOf(":");
					if (pos > -1)
					{
						String nr = line.substring(pos + 1).trim();
						lines = StringUtil.getIntValue(nr, 24);
					}
					linesRead = true;
				}
				else
				{
					// next line is the information about the lines in the terminal window
					int pos = line.indexOf(":");
					if (pos > -1)
					{
						String nr = line.substring(pos + 1).trim();
						columns = StringUtil.getIntValue(nr, 80);
					}
					break; // nothing more to do
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Terminal.runModeCommand()", "Error getting window size", e);
		}
	}

	public static void main (String[] args)
	{
		Terminal t = new Terminal();
		System.out.println("Lines: " + t.getLines());
		System.out.println("Columns: " + t.getColumns());
	}
}
