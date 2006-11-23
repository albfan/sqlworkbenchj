/*
 * UseCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.sql.formatter.SQLLexer;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLToken;
import workbench.util.StringUtil;

/**
 * MS SQL Server's and MySQL's USE command. 
 * 
 * Actually this will be in effect if the JDBC driver reports
 * that catalog's are supported 
 *
 * This class will notify the connection used that the current database has changed
 * so that the connection display in the main window can be updated.
 * @author  support@sql-workbench.net
 */
public class UseCommand extends SqlCommand
{
	public static final String VERB = "USE";
	public UseCommand()
	{
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		try
		{
			SQLLexer lexer = new SQLLexer(aSql);
			
			// The first token should be the USE verb;
			SQLToken t = lexer.getNextToken(false, false);
			
			// everything after the USE command is the catalog name
			String catName = aSql.substring(t.getCharEnd()).trim();

			// DbMetadata.setCurrentCatalog() will fire the 
			// catalogChanged() event on the connection!
			// no need to do this here
			aConnection.getMetadata().setCurrentCatalog(catName);
			
			String newCatalog = aConnection.getMetadata().getCurrentCatalog();
			
			String msg = ResourceMgr.getString("MsgCatalogChanged");
			String term = aConnection.getMetadata().getCatalogTerm();
			
			msg = StringUtil.replace(msg, "%newcatalog%", newCatalog);
			msg = StringUtil.replace(msg, "%catalogterm%", StringUtil.capitalize(term));
			result.addMessage(msg);
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
		}
		finally
		{
			this.done();
		}

		return result;
	}

	public String getVerb()
	{
		return VERB;
	}

}
