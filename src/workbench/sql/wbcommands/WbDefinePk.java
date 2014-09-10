/*
 * WbDefinePk.java
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

import workbench.storage.PkMapping;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 * Defines a primary key for a table or view. This is stored in a
 * workbench specific file in order allow updates on tables (or views) that
 * have no defined primary key in the database.
 *
 * @author Thomas Kellerer
 */
public class WbDefinePk
	extends SqlCommand
{
	public static final String VERB = "WbDefinePK";

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
	public StatementRunnerResult execute(String sqlCommand)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String sql = getCommandLine(sqlCommand);

		WbStringTokenizer tok = new WbStringTokenizer("=", true, "\"'", false);
		tok.setSourceString(sql);
		String columns = null;
		String table = null;

		if (tok.hasMoreTokens()) table = tok.nextToken();

		if (table == null)
		{
			result.addMessage(ResourceMgr.getString("ErrPkDefWrongParameter"));
			result.setFailure();
			return result;
		}

		if (tok.hasMoreTokens()) columns = tok.nextToken();
		String msg = null;
		if (columns == null)
		{
			PkMapping.getInstance().removeMapping(currentConnection, table);
			msg = ResourceMgr.getString("MsgPkDefinitionRemoved");
		}
		else
		{
			PkMapping.getInstance().addMapping(table, columns);
			msg = ResourceMgr.getString("MsgPkDefinitionAdded");
		}
		msg = StringUtil.replace(msg, "%table%", table);
		result.setSuccess();
		result.addMessage(msg);
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
