/*
 * WbDisableOraOutput.java
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
 *
 * @author  Thomas Kellerer
 */
public class WbDisableOraOutput extends SqlCommand
{
	public static final String VERB = "DISABLEOUT";

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		currentConnection.getMetadata().disableOutput();
		result.addMessage(ResourceMgr.getString("MsgDbmsOutputDisabled"));
		return result;
	}

}
