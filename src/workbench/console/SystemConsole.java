/*
 * InputReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.util.Arrays;
import java.util.Scanner;

/**
 * A class to read input from the console.
 * When run on a Java6 VM it uses the new System.console() class, otherwise
 * it uses the Scanner class to read the input.
 *
 * @author Thomas Kellerer
 */
public class SystemConsole
	implements WbConsoleReader
{

	public SystemConsole()
	{
	}

	@Override
	public String readPassword(String prompt)
	{
		if (System.console() != null)
		{
			char[] input = System.console().readPassword(prompt + " ");
			if (input == null) return null;
			String result = new String(input);
			Arrays.fill(input, 'x');
			return result;
		}
		return readLine(prompt);
	}

	@Override
	public String readLine(String prompt)
	{
		if (System.console() != null)
		{
			return System.console().readLine(prompt);
		}
		// Fallback in case console() is not available;
		System.out.print(prompt);
		Scanner inputScanner = new Scanner(System.in);
		String line = inputScanner.nextLine();
		return line;
	}

	@Override
	public void shutdown()
	{
	}

}
