/*
 * JLineWrapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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


import workbench.util.FileUtil;

import jline.ConsoleReader;
import jline.History;
import jline.Terminal;

/**
 *
 * @author Thomas Kellerer
 */
public class JLineWrapper
	implements WbConsoleReader
{
	private ConsoleReader reader;

	public JLineWrapper()
		throws IOException
	{
		reader = new ConsoleReader();
		reader.setUseHistory(true);
		reader.setUsePagination(false);
		reader.setBellEnabled(false);
	}

	@Override
	public int getColumns()
	{
		Terminal t = Terminal.getTerminal();
		if (t != null)
		{
			return t.getTerminalWidth();
		}
		return -1;
	}

	@Override
	public String readPassword(String prompt)
	{
		try
		{
			return reader.readLine(prompt, Character.valueOf('*'));
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	public String readLine(String prompt)
	{
		try
		{
			return reader.readLine(prompt);
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	public void shutdown()
	{
		History h = reader.getHistory();
		if (h != null)
		{
			FileUtil.closeQuietely(h.getOutput());
		}
	}
}
