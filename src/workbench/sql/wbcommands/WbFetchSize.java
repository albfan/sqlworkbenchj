/*
 * WbFetchSize.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

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
	public static final String VERB = "WBFETCHSIZE";

	public WbFetchSize()
	{
		super();
		isUpdatingCommand = false;
	}

	public String getVerb()
	{
		return VERB;
	}

	protected boolean isConnectionRequired()
	{
		return true;
	}

	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String value = super.getCommandLine(sql);
		int size = -1;

		try
		{
			size = Integer.parseInt(value);
		}
		catch (Exception e)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getFormattedString("ErrInvalidNumber", value));
			return result;
		}

		currentConnection.setFetchSize(size);
		result.addMessage(ResourceMgr.getFormattedString("MsgFetchSizeChanged", currentConnection.getFetchSize()));
		result.setSuccess();

		return result;
	}
}
