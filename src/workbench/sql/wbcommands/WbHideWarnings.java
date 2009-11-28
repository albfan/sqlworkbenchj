/*
 * WbHideWarnings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbHideWarnings
	extends SqlCommand
{
	public static final String VERB = "WBHIDEWARNINGS";

	public WbHideWarnings()
	{
		super();
		this.cmdLine = new ArgumentParser(false);
		this.cmdLine.addArgument("on");
		this.cmdLine.addArgument("off");
	}

	public String getVerb() { return VERB; }

	protected boolean isConnectionRequired() { return false; }

	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

		SQLLexer lexer = new SQLLexer(sql);
		// Skip the SQL Verb
		SQLToken token = lexer.getNextToken(false, false);

		// get the parameter
		token = lexer.getNextToken(false, false);
		String parm = (token != null ? token.getContents() : null);

		if (parm != null)
		{
			if (!parm.equalsIgnoreCase("on") && !parm.equalsIgnoreCase("off") &&
				  !parm.equalsIgnoreCase("true") && !parm.equalsIgnoreCase("false"))
			{
				result.setFailure();
				result.addMessage(ResourceMgr.getString("ErrShowWarnWrongParameter"));
				return result;
			}
			else
			{
				this.runner.setHideWarnings(StringUtil.stringToBool(parm));
			}
		}

		if (runner.getHideWarnings())
		{
			result.addMessage(ResourceMgr.getString("MsgWarningsDisabled"));
		}
		else
		{
			result.addMessage(ResourceMgr.getString("MsgWarningsEnabled"));
		}
		return result;
	}

}
