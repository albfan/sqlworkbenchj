/*
 * WbHelp.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.sql.wbcommands;
import java.sql.SQLException;
import java.util.Collection;
import java.util.ResourceBundle;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.ResourcePath;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author Thomas Kellerer
 */
public class WbHelp
	extends SqlCommand
{
	public static final String VERB = "WbHelp";

	public WbHelp()
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
		Collection<String> commands = this.runner.getAllWbCommands();
		StringBuffer msg = new StringBuffer(commands.size() * 25);
		ResourceBundle bundle = ResourceBundle.getBundle("language/cmdhelp", Settings.getInstance().getLanguage());
		commands.remove("DESC");
		commands.add("DESCRIBE"); // only the "long" Verb is needed
		commands.remove("ENABLEOUT");
		commands.remove("DISABLEOUT");
		commands.remove("SHOW");

		for (String verb : commands)
		{
			msg.append(verb);
			try
			{
				String text = bundle.getString(verb);
				msg.append(" - ");
				msg.append(text);
			}
			catch (Exception e)
			{
        String text = ResourceMgr.getString(ResourcePath.EXTENSION, verb, false);

        if (text != null && !text.equals(verb))
        {
          msg.append(" - (extension) ");
          msg.append(text);
        }
        else
        {
          LogMgr.logWarning("WbHelp.execute()", "Error getting command short help from ResourceBundle", e);
        }
      }
			msg.append('\n');
		}
		result.addMessage(msg);
		result.setSuccess();
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
