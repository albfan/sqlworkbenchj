/*
 * IgnoredCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * This class simply ignores the command and does not send it to the DBMS
 *
 * Thus scripts e.g. intended for SQL*Plus (containing WHENEVER or EXIT)
 * can be executed from within the workbench.
 * The commands to be ignored can be configured in workbench.settings
 *
 * @author  support@sql-workbench.net
 */
public class IgnoredCommand
	extends SqlCommand
{
	private String verb;

	public IgnoredCommand(String aVerb)
	{
		super();
		this.verb = aVerb;
	}

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String msg = ResourceMgr.getString("MsgCommandIgnored").replace("%verb%", this.verb);
		result.addMessage(msg);
		result.setSuccess();
		this.done();
		return result;
	}

	public String getVerb()
	{
		return verb;
	}

}
