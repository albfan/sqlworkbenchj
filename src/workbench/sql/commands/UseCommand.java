/*
 * UseCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * MS SQL Server's USE command. 
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
			this.currentStatement = aConnection.createStatement();
			this.currentStatement.execute(aSql);

			StringBuffer warnings = new StringBuffer();
			if (this.appendWarnings(aConnection, this.currentStatement , warnings))
			{
				result.addMessage(warnings.toString());
			}
			this.currentStatement.close();
			
			String newCatalog = aConnection.getMetadata().getCurrentCatalog();
			if (oldCatalog != null && !oldCatalog.equals(newCatalog))
			{
				aConnection.catalogChanged(oldCatalog, newCatalog);
			}
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
