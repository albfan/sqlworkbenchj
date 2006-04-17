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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
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
			String oldCatalog = aConnection.getMetadata().getCurrentCatalog();
			
			Pattern p = Pattern.compile("\\s*USE\\s*", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(aSql);
			String catName = m.replaceAll("").trim();
			aConnection.getSqlConnection().setCatalog(catName);
			
			String newCatalog = aConnection.getMetadata().getCurrentCatalog();
			if (oldCatalog != null && !oldCatalog.equals(newCatalog))
			{
				aConnection.catalogChanged(oldCatalog, newCatalog);
			}
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
