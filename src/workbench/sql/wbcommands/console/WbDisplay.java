/*
 * WbDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 * A SQL command to control the output format in console mode.
 *
 * @author  Thomas Kellerer
 */
public class WbDisplay
	extends SqlCommand
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
			ConsoleSettings.getInstance().setRowDisplay(RowDisplay.SingleLine);
			result.addMessageByKey("MsgDispChangeRow");
		}
		else if ("record".equalsIgnoreCase(param) || "form".equalsIgnoreCase(param) || "single".equalsIgnoreCase(param))
		{
			ConsoleSettings.getInstance().setRowDisplay(RowDisplay.Form);
			result.addMessageByKey("MsgDispChangeForm");
		}
		else
		{
			RowDisplay current = ConsoleSettings.getInstance().getRowDisplay();
			String currentDisp = "tab";

			if (current == RowDisplay.Form)
			{
				currentDisp = "record";
			}

			if (StringUtil.isBlank(param)) result.setSuccess();
			else result.setFailure();

			String msg = ResourceMgr.getFormattedString("ErrDispWrongArgument", currentDisp);
			result.addMessage(msg);
		}

		return result;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	public String getVerb()
	{
		return VERB;
	}

}
