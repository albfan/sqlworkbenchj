/*
 * SystemConsole.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * A class to read input from the console.
 * When run on a Java6 VM it uses the new System.console() class, otherwise
 * it uses the Scanner class to read the input.
 *
 * @author Thomas Kellerer
 */
public class SystemConsole
	implements WbConsole
{

	public SystemConsole()
	{
	}

  @Override
  public void clearScreen()
  {
  }

  @Override
  public char readCharacter()
  {
		if (System.console() == null) return 0;
    try
    {
      int value = System.console().reader().read();
      return (char)value;
    }
    catch (IOException ex)
    {
    }
    return 0;
  }

  @Override
  public void reset()
  {
		if (System.console() == null) return;
    try
    {
      System.console().reader().reset();
    }
    catch (IOException ex)
    {
    }
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
		// Fallback in case console() is not available
		// this might also be the case e.g. inside NetBeans
		System.out.print(prompt);
		Scanner inputScanner = new Scanner(System.in);
		String line = inputScanner.nextLine();
		return line;
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public int getColumns()
	{
		return -1;
	}

	@Override
	public String readLineWithoutHistory(String prompt)
	{
		return readLine(prompt);
	}

	@Override
	public void clearHistory()
	{
	}

	@Override
	public void addToHistory(List<String> lines)
	{
	}

}
