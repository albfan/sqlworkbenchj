/*
 * WbListProfiles.java
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
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import java.util.List;

import workbench.WbManager;
import workbench.console.ConsoleReaderFactory;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;
import workbench.sql.macros.MacroManager;
import workbench.sql.macros.MacroStorage;


/**
 * List all defined profiles
 *
 * @author Thomas Kellerer
 */
public class WbListMacros
	extends SqlCommand
{
	public static final String VERB = "WBLISTMACROS";

	public WbListMacros()
	{
		super();
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();

		MacroStorage storage = MacroManager.getInstance().getMacros();
		List<MacroGroup> groups = storage.getGroups();
		for (MacroGroup group : groups)
		{
			result.addMessage(group.getName());
			List<MacroDefinition> macros = group.getMacros();
			for (MacroDefinition def : macros)
			{

				if (def.getExpandWhileTyping())
				{
					result.addMessage("  * " + def.getName());
				}
				else
				{
					result.addMessage("  " + def.getName());
				}
			}
		}
		result.setSuccess();
		return result;
	}

	private int getMaxLength()
	{
		if (WbManager.getInstance().isConsoleMode())
		{
			int columns = ConsoleReaderFactory.getConsoleReader().getColumns();
			if (columns > 0)
			{
				return columns;
			}
		}
		return Integer.MAX_VALUE;
	}
}
