/*
 * InputReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
	private Object console;
	private Method readLine;
	private Method readPwd;
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
					Class objectArgs = Array.newInstance(Object.class, 0).getClass();

					readLine = console.getClass().getMethod("readLine", String.class, objectArgs);
					readPwd = console.getClass().getMethod("readPassword", String.class, objectArgs);
				}
			}
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("InputReader.<init>", "Console not available, using Java5 input", th);
			readLine = null;
			readPwd = null;
			console = null;
		}
	}

	public String readPassword(String prompt)
	{
		if (readPwd != null)
		{
			try
			{
				char[] input = (char[]) readPwd.invoke(console, prompt, null);
				if (input == null) return null;
				return new String(input);
			}
			catch (Exception e)
			{
				LogMgr.logError("InputReader.readPassword()", "Error accessing console!", e);
				readPwd = null;
			}
		}
		return readLine(prompt);
	}

	public String readLine(String prompt)
	{
		if (readLine != null)
		{
			try
			{
				String result = (String) readLine.invoke(console, prompt, null);
				return result;
			}
			catch (Exception e)
			{
				LogMgr.logError("InputReader.readLine()", "Error accessing console!", e);
				readLine = null;
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

	public static void main(String args[])
	{
		try
		{
			InputReader in = new InputReader();
			String pwd = in.readPassword("Pwd:");
			System.out.println("-->: " + pwd);
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
	}
}
