/*
 * WbDisconnect.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
 * @author support@sql-workbench.net
 */
public class WbDisconnect
	extends SqlCommand
{
	public static final String VERB = "WBDISCONNECT";

	public WbDisconnect()
	{
		super();
	}

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
			this.currentConnection.disconnect();
			this.runner.setConnection(null);
			this.runner.fireConnectionChanged();
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
