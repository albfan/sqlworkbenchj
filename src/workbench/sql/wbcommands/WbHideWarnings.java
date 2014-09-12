/*
 * WbHideWarnings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLLexerFactory;
import workbench.sql.formatter.SQLToken;

import workbench.util.ArgumentParser;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbHideWarnings
	extends SqlCommand
{
	public static final String VERB = "WbHideWarnings";

	public WbHideWarnings()
	{
		super();
		this.cmdLine = new ArgumentParser(false);
		this.cmdLine.addArgument("on");
		this.cmdLine.addArgument("off");
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
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

		SQLLexer lexer = SQLLexerFactory.createLexer(currentConnection, sql);
		// Skip the SQL Verb
		SQLToken token = lexer.getNextToken(false, false);

		// get the parameter
		token = lexer.getNextToken(false, false);
		String parm = (token != null ? token.getContents() : null);

		if (parm != null)
		{
			if (!parm.equalsIgnoreCase("on") && !parm.equalsIgnoreCase("off") &&
				  !parm.equalsIgnoreCase("true") && !parm.equalsIgnoreCase("false"))
			{
				result.setFailure();
				result.addMessage(ResourceMgr.getString("ErrShowWarnWrongParameter"));
				return result;
			}
			else
			{
				this.runner.setHideWarnings(StringUtil.stringToBool(parm));
			}
		}

		if (runner.getHideWarnings())
		{
			result.addMessage(ResourceMgr.getString("MsgWarningsDisabled"));
		}
		else
		{
			result.addMessage(ResourceMgr.getString("MsgWarningsEnabled"));
		}
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
