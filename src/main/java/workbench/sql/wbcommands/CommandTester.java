/*
 * CommandTester.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql.wbcommands;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import workbench.sql.CommandMapper;
import workbench.sql.CommandRegistry;

import workbench.util.CaseInsensitiveComparator;

/**
 * A class to test whether a given SQL Verb is an internal
 * Workbench command.
 *
 * This is used by the SqlFormatter, because the verbs for WbXXXX commands should be be not formatted in uppercase.
 *
 * This is also used by the code completion to check for WB specific commands.
 *
 * @see workbench.sql.formatter.SqlFormatter
 * @see workbench.gui.completion.StatementContext
 *
 * @author Thomas Kellerer
 */
public class CommandTester
{

	private Map<String, String> commands;

	public CommandTester()
	{
		commands = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
    CommandMapper mapper = new CommandMapper();
    Collection<String> verbs = mapper.getAllWbCommands();
    for (String verb : verbs)
    {
      putVerb(verb);
    }

		verbs = CommandRegistry.getInstance().getVerbs();
		for (String verb : verbs)
		{
			putVerb(verb);
		}
	}

	private void putVerb(String verb)
	{
		commands.put(verb, verb);
	}

	public Collection<String> getCommands()
	{
		return Collections.unmodifiableSet(commands.keySet());
	}

	public boolean isWbCommand(String verb)
	{
		if (verb == null)
		{
			return false;
		}
		return commands.containsKey(verb.trim());
	}

	public String formatVerb(String verb)
	{
		return commands.get(verb);
	}
}
