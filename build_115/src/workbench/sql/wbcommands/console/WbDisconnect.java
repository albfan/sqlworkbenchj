/*
 * WbDisconnect.java
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
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDisconnect
	extends SqlCommand
{
	public static final String VERB = "WBDISCONNECT";

	public WbDisconnect()
	{
		super();
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		if (this.currentConnection != null)
		{
			//this.currentConnection.disconnect();
			// setConnection will call disconnect() on the "old" connection
			this.runner.setConnection(null);
			result.addMessage(ResourceMgr.getString("MsgDisconnected"));
			result.setSuccess();
		}
		else
		{
			result.addMessage(ResourceMgr.getString("TxtNotConnected"));
			result.setFailure();
		}
		return result;
	}


}
