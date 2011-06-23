/*
 * WbConnInfo
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.WbManager;
import workbench.db.ConnectionInfoBuilder;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author Thomas Kellerer
 */
public class WbConnInfo
	extends SqlCommand
{
	public static final String VERB = "WBCONNINFO";

	public WbConnInfo()
	{
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
		StatementRunnerResult result = new StatementRunnerResult(sql);
		result.setSuccess();

		if (this.currentConnection == null)
		{
			result.addMessage(ResourceMgr.getString("TxtNotConnected"));
		}
		else
		{
			int indent = 0;
			ConnectionInfoBuilder info = new ConnectionInfoBuilder();
			if (WbManager.getInstance().isConsoleMode())
			{
				result.addMessage(" ");
				indent = 2;
			}
			result.addMessage(info.getPlainTextDisplay(currentConnection, indent));
			if (WbManager.getInstance().isConsoleMode())
			{
				result.addMessage("");
			}
		}
		return result;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

}
