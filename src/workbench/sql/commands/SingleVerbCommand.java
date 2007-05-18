/*
 * SingleVerbCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SingleVerbCommand extends SqlCommand
{
	public static final SqlCommand COMMIT = new SingleVerbCommand("COMMIT");
	public static final SqlCommand ROLLBACK = new SingleVerbCommand("ROLLBACK");

	private String verb;

	public SingleVerbCommand(String aVerb)
	{
		this.verb = aVerb;
		this.isUpdatingCommand = "COMMIT".equalsIgnoreCase(this.verb);
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			if (aConnection.useJdbcCommit())
			{
				if ("COMMIT".equals(this.verb))
				{
					aConnection.getSqlConnection().commit();
				}
				else if ("ROLLBACK".equals(this.verb))
				{
					aConnection.getSqlConnection().rollback();
				}
			}
			else
			{
				this.currentStatement = aConnection.createStatement();
				this.currentStatement.execute(verb);
			}

			result.addMessage(this.verb + " " + ResourceMgr.getString("MsgKnownStatementOK"));
			result.setSuccess();
			processMoreResults(aSql, result, false);
		}
		catch (Exception e)
		{
			addErrorInfo(result, aSql, e);
			LogMgr.logSqlError("SingleVerbCommand.execute()", aSql, e);
		}
		finally
		{
			this.done();
		}

		return result;
	}

	public String getVerb()
	{
		return verb;
	}

}
