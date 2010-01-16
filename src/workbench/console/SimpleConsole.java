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

import java.util.Scanner;

/**
 * A simple WbConsoleReader using a Scanner
 *
 * This is a fallback if neither JLine nor the new Java 6 Console is available
 *
 * @author Thomas Kellerer
 */
public class SimpleConsole
	implements WbConsoleReader
{
	private static Scanner inputScanner;

	public SimpleConsole()
	{
		inputScanner = new Scanner(System.in);
	}

	public String readPassword(String prompt)
	{
		return readLine(prompt);
	}

	public String readLine(String prompt)
	{
		System.out.print(prompt);
		return inputScanner.nextLine();
	}

	@Override
	public void shutdown()
	{
	}

}
