/*
 * WbRemoveVar.java
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

import java.sql.SQLException;

import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;

/**
 * Delete a variable defined through {@link WbDefineVar}.
 *
 * @author Thomas Kellerer
 */
public class WbRemoveVar extends SqlCommand
{
	public static final String VERB = "WbVarDelete";

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
		String var = getCommandLine(sqlCommand);

		String msg = null;

		if (var == null || var.length() == 0)
		{
			result.addMessageByKey("ErrVarRemoveWrongParameter");
			result.setFailure();
			return result;
		}
		else
		{
			int removed = VariablePool.getInstance().removeVariable(var);
      if (var.indexOf('*') > -1 || var.indexOf('%') > -1)
      {
        msg = ResourceMgr.getFormattedString("MsgVarPatternRemoved", removed);
      }
      else if (removed > 0)
			{
				msg = ResourceMgr.getFormattedString("MsgVarRemoved", var);
			}
			else
			{
				msg = ResourceMgr.getFormattedString("MsgVarNotRemoved", var);
			}
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
