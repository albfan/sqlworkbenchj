/*
 * WbDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;

import workbench.console.RowDisplay;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * A SQL command to control the output format in console mode. 
 *
 * @author  support@sql-workbench.net
 */
public class WbDisplay extends SqlCommand
{
	public static final String VERB = "WBDISPLAY";

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String param = getCommandLine(aSql);

		if ("tab".equalsIgnoreCase(param) || "row".equalsIgnoreCase(param))
		{
			result.setSuccess();
			result.setRowDisplay(RowDisplay.SingleLine);
			result.addMessageByKey("MsgDispChangeRow");
		}
		else if ("record".equalsIgnoreCase(param) || "form".equalsIgnoreCase(param) || "single".equalsIgnoreCase(param))
		{
			result.setRowDisplay(RowDisplay.Form);
			result.addMessageByKey("MsgDispChangeForm");
		}
		else
		{
			result.setFailure();
			result.addMessageByKey("ErrDispWrongArgument");
		}

		return result;
	}

	public String getVerb()
	{
		return VERB;
	}

}
