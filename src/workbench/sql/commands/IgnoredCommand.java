/*
 * IgnoredCommand.java
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
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * This class simply ignores the command and does not send it to the DBMS.
 *
 * Thus scripts e.g. intended for SQL*Plus (containing WHENEVER or EXIT)
 * can be executed from within the workbench.
 * The commands to be ignored can be configured in workbench.settings
 *
 * @author  Thomas Kellerer
 */
public class IgnoredCommand
	extends SqlCommand
{
	private String verb;

	public IgnoredCommand(String aVerb)
	{
		super();
		this.verb = aVerb.toUpperCase();
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		if (Settings.getInstance().getShowIgnoredWarning())
		{
			String msg = ResourceMgr.getFormattedString("MsgCommandIgnored", this.verb);
			result.addMessage(msg);
		}
		result.setSuccess();
		this.done();
		return result;
	}

	@Override
	public String getVerb()
	{
		return verb;
	}

}
