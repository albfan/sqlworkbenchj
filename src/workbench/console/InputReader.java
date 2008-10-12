/*
 * InputReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Scanner;
import workbench.log.LogMgr;

/**
 * A class to read input from the console. When run on a Java6 VM it uses
 * the new System.console() class
 * 
 * @author support@sql-workbench.net
 */
public class InputReader
{
	private boolean useConsole = true;
	private Method readLine;
	private Object console;
	private Scanner inputScanner;

	public InputReader()
	{
		try
		{
			Method getConsole = System.class.getMethod("console", new Class[]	{});
			
			if (getConsole != null)
			{
				console = getConsole.invoke(null, new Object[] {});
				
				if (console != null)
				{
					readLine = console.getClass().getMethod("readLine", Array.newInstance(String.class, 0).getClass());
				}
			}
			useConsole = (readLine != null);
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("InputReader.<init>", "Console not available, using Java5 input");
			useConsole = false;
		}
	}

	public String readLine(String prompt)
	{
		if (useConsole)
		{
			try
			{
				String result = (String) readLine.invoke(console, prompt);
				return result;
			}
			catch (Exception e)
			{
				LogMgr.logError("InputReader.readLine()", "Error accessing console!", e);
				useConsole = false;
			}
		}
		System.out.print(prompt);
		if (inputScanner == null)
		{
			inputScanner = new Scanner(System.in);
		}
		String line = inputScanner.nextLine();
		return line;
	}
}
