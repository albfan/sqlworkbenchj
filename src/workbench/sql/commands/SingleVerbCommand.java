/*
 * SingleVerbCommand.java
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
import workbench.log.LogMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * Handles COMMIT and ROLLBACK
 *
 * @author Thomas Kellerer
 */
public class SingleVerbCommand extends SqlCommand
{
	public static final SqlCommand COMMIT = new SingleVerbCommand("COMMIT");
	public static final SqlCommand ROLLBACK = new SingleVerbCommand("ROLLBACK");

	private String verb;

	public SingleVerbCommand(String aVerb)
	{
		super();
		this.verb = aVerb;
		this.isUpdatingCommand = "COMMIT".equalsIgnoreCase(this.verb);
	}

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			if (currentConnection.useJdbcCommit())
			{
				if ("COMMIT".equals(this.verb))
				{
					currentConnection.getSqlConnection().commit();
				}
				else if ("ROLLBACK".equals(this.verb))
				{
					currentConnection.getSqlConnection().rollback();
				}
			}
			else
			{
				this.currentStatement = currentConnection.createStatement();
				this.currentStatement.execute(verb);
			}

			appendSuccessMessage(result);
			result.setSuccess();
			processMoreResults(aSql, result, false);
		}
		catch (Exception e)
		{
			addErrorInfo(result, aSql, e);
			LogMgr.logError("SingleVerbCommand.execute()", aSql, e);
		}
		finally
		{
			done();
		}

		return result;
	}

	public String getVerb()
	{
		return verb;
	}

}
