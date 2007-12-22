/*
 * WbFeedback.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbFeedback
	extends SqlCommand
{
	public static final String VERB = "WBFEEDBACK";
	private final String command; 
	
	public WbFeedback()
	{
		this(VERB);
	}
	public WbFeedback(String verb)
	{
		this.command = verb;
		this.cmdLine = new ArgumentParser(false);
		this.cmdLine.addArgument("on");
		this.cmdLine.addArgument("off");
	}

	public String getVerb() { return command; }
	
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
		
		if (parm == null)
		{
			if (runner.getVerboseLogging())
			{
				result.addMessage(ResourceMgr.getString("MsgFeedbackEnabled"));
			}
			else
			{
				result.addMessage(ResourceMgr.getString("MsgFeedbackDisabled"));
			}
			result.setSuccess();
		}
		else if ("off".equalsIgnoreCase(parm) || "false".equalsIgnoreCase(parm))
		{
			this.runner.setVerboseLogging(false);
			result.addMessage(ResourceMgr.getString("MsgFeedbackDisabled"));
		}
		else if ("on".equalsIgnoreCase(parm) || "true".equalsIgnoreCase(parm))
		{
			this.runner.setVerboseLogging(true);
			result.addMessage(ResourceMgr.getString("MsgFeedbackEnabled"));
		}
		else
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrFeedbackWrongParameter"));
		}
		return result;
	}
	
}
