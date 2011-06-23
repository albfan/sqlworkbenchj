/*
 * UseCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.CatalogChanger;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 * MS SQL Server's and MySQL's USE command.
 * <br/>
 * This command will also be "activated if the JDBC driver reports
 * that catalogs are supported
 *
 * This class will notify the connection used that the current database has changed
 * so that the connection display in the main window can be updated.
 *
 * @see workbench.db.CatalogChanger#setCurrentCatalog(workbench.db.WbConnection, java.lang.String)
 * @see workbench.sql.CommandMapper#getCommandToUse(java.lang.String)
 * @see workbench.sql.CommandMapper#addCommand(workbench.sql.SqlCommand)
 *
 * @author  Thomas Kellerer
 */
public class UseCommand
	extends SqlCommand
{
	public static final String VERB = "USE";

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			SQLLexer lexer = new SQLLexer(aSql);

			// The first token should be the USE verb;
			SQLToken t = lexer.getNextToken(false, false);

			// everything after the USE command is the catalog name
			String catName = aSql.substring(t.getCharEnd()).trim();

			// CatalogChanger.setCurrentCatalog() will fire the
			// catalogChanged() event on the connection!
			CatalogChanger changer = new CatalogChanger();
			changer.setCurrentCatalog(currentConnection, catName);

			String newCatalog = currentConnection.getMetadata().getCurrentCatalog();

			String msg = ResourceMgr.getString("MsgCatalogChanged");
			String term = currentConnection.getMetadata().getCatalogTerm();

			msg = StringUtil.replace(msg, "%newcatalog%", newCatalog);
			msg = StringUtil.replace(msg, "%catalogterm%", StringUtil.capitalize(term));
			result.addMessage(msg);
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getAllExceptions(e));
			result.setFailure();
		}
		finally
		{
			this.done();
		}

		return result;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

}
