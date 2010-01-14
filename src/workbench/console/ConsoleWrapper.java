/*
 * InputReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.util.Arrays;
import java.util.Scanner;
import workbench.log.LogMgr;

/**
 * A class to read input from the console.
 * When run on a Java6 VM it uses the new System.console() class, otherwise
 * it uses the Scanner class to read the input.
 *
 * This is mainly here to support running the console mode inside an IDE
 * because System.console() returns null when the application is started
 * from within an IDE.
 *
 * @author Thomas Kellerer
 */
public class ConsoleWrapper
{
	private static Scanner inputScanner;

	protected static class LazyInstance
	{
		protected static ConsoleWrapper INSTANCE = new ConsoleWrapper();
	}

	public static ConsoleWrapper getInstance()
	{
		return LazyInstance.INSTANCE;
	}

	private ConsoleWrapper()
	{
	}

	public String readPassword(String prompt)
	{
		if (System.console() != null)
		{
			try
			{
				char[] input = System.console().readPassword(prompt + " ");
				if (input == null) return null;
				String result = new String(input);
				Arrays.fill(input, 'x');
				return result;
			}
			catch (Exception e)
			{
				LogMgr.logError("ConsoleWrapper.readPassword()", "Error accessing console!", e);
			}
		}
		return readLine(prompt);
	}

	public String readLine(String prompt)
	{
		if (System.console() != null)
		{
			try
			{
				return System.console().readLine(prompt);
			}
			catch (Exception e)
			{
				LogMgr.logError("ConsoleWrapper.readLine()", "Error accessing console!", e);
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
