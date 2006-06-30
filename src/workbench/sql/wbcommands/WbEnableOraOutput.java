/*
 * WbEnableOraOutput.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbEnableOraOutput extends SqlCommand
{
	public static final String VERB = "ENABLEOUT";

	public WbEnableOraOutput()
	{
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, Exception
	{
		SQLLexer lexer = new SQLLexer(aSql);
		SQLToken t = lexer.getNextToken(false, false);
		
		// First token is the verb
		if (t != null) t = lexer.getNextToken(false, false);
		
		long limit = -1;
		
		// second token is the buffer size
		if (t != null)
		{
			String value = t.getContents();
			try
			{
				limit = Long.parseLong(value);
			}
			catch (NumberFormatException nfe)
			{
				limit = -1;
			}
		}
		aConnection.getMetadata().enableOutput(limit);
		StatementRunnerResult result = new StatementRunnerResult();
		result.addMessage(ResourceMgr.getString("MsgDbmsOutputEnabled"));
		return result;
	}

}
