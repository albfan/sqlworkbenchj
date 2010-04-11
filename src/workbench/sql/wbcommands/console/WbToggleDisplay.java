/*
 * WbDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * A SQL command to control the output format in console mode.
 *
 * @author  Thomas Kellerer
 */
public class WbToggleDisplay extends SqlCommand
{
	public static final String VERB = "WBTOGGLEDISPLAY";

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		RowDisplay current = ConsoleSettings.getInstance().getRowDisplay();
		RowDisplay newDisplay = null;
		if (current == RowDisplay.Form)
		{
			newDisplay = RowDisplay.SingleLine;
			result.addMessageByKey("MsgDispChangeRow");
		}
		else
		{
			newDisplay = RowDisplay.Form;
			result.addMessageByKey("MsgDispChangeForm");
		}
		ConsoleSettings.getInstance().setRowDisplay(newDisplay);
		result.setSuccess();
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
