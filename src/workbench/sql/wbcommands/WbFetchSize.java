/*
 * WbFetchSize.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 * A SQL Statement to change the default fetch size for the current connection.
 * <br/>
 * Setting the default fetch size using this command will overwrite the setting
 * in the connection profile, but will not change the connection profile.
 *
 * @author Thomas Kellerer
 * @see workbench.db.WbConnection#setFetchSize(int)
 */
public class WbFetchSize
	extends SqlCommand
{
	public static final String VERB = "WbFetchSize";

	public WbFetchSize()
	{
		super();
		isUpdatingCommand = false;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String value = getCommandLine(sql);
    if (StringUtil.isNonBlank(value))
    {
      setFetchSize(value, result, currentConnection);
    }
    else
    {
      String currentValue = "(" + ResourceMgr.getString("TxtDefault").toLowerCase() + ")";
      int size = currentConnection.getFetchSize();
      if (size > -1)
      {
        currentValue = Integer.toString(size);
      }
      result.addMessageByKey("MsgFetchSizeCurrent", currentValue);
    }
		return result;
	}

  public static void setFetchSize(String value, StatementRunnerResult result, WbConnection connection)
  {
		int size = -1;

		try
		{
			size = Integer.parseInt(value);
		}
		catch (Exception e)
		{
			result.addErrorMessageByKey("ErrInvalidNumber", value);
			return;
		}

		connection.setFetchSize(size);
		result.addMessageByKey("MsgFetchSizeChanged", Integer.toString(connection.getFetchSize()));
  }

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
