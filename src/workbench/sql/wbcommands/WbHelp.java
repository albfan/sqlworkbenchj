/*
 * WbHelp.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author  info@sql-workbench.net
 */
public class WbHelp extends SqlCommand
{
	public WbHelp()
	{
	}

	public String getVerb() { return "HELP"; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult("HELP");
		result.setSuccess();
		WbManager.getInstance().getCurrentWindow().showHelp();
		return result;
	}

}
