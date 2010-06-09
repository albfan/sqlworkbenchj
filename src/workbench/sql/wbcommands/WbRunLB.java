/*
 * WbInclude.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.liquibase.ChangeSetIdentifier;
import workbench.liquibase.LiquibaseSupport;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;
import workbench.storage.RowActionMonitor;
import workbench.util.CollectionUtil;
import workbench.util.WbFile;

/**
 * A command to run/include the SQL (<sql> or <createProcedure> tag) of a single Liquibase changeset
 *
 * @author  Thomas Kellerer
 */
public class WbRunLB
	extends SqlCommand
{
	public static final String VERB = "WBRUNLB";

	public WbRunLB()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("file");
		cmdLine.addArgument(CommonArgs.ARG_CONTINUE, ArgumentType.BoolArgument);
		cmdLine.addArgument("changeSet", ArgumentType.Repeatable);
		cmdLine.addArgument("author");
		cmdLine.addArgument("verbose", ArgumentType.BoolArgument);
		CommonArgs.addEncodingParameter(cmdLine);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return true;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		result.setSuccess();

		boolean checkParameters = true;

		cmdLine.parse(getCommandLine(aSql));
		WbFile file = null;
		if (cmdLine.hasArguments())
		{
			file = evaluateFileArgument(cmdLine.getValue("file"));
		}
		else
		{
			file = evaluateFileArgument(getCommandLine(aSql));
			checkParameters = false;
		}

		if (file == null)
		{
			String msg = ResourceMgr.getString("ErrLBWrongParameter");
			result.addMessage(msg);
			result.setFailure();
			return result;
		}

		if (!file.exists())
		{
			result.setFailure();
			String msg = ResourceMgr.getString("ErrIncludeFileNotFound");
			msg = StringUtil.replace(msg, "%filename%", file.getFullPath());
			result.addMessage(msg);
			return result;
		}

		boolean continueOnError = checkParameters ? cmdLine.getBoolean(CommonArgs.ARG_CONTINUE, false) : false;
		List<String> idStrings = checkParameters ? cmdLine.getListValue("changeSet") : null;
		List<String> authors = checkParameters ? cmdLine.getListValue("author") : null;
		List<ChangeSetIdentifier> ids = null;

		if (CollectionUtil.isNonEmpty(authors))
		{
			ids = new ArrayList<ChangeSetIdentifier>(authors.size());
			for (String author : authors)
			{
				ChangeSetIdentifier id = new ChangeSetIdentifier(author, "*");
				ids.add(id);
			}
		}
		else if (CollectionUtil.isNonEmpty(idStrings))
		{
			ids = new ArrayList<ChangeSetIdentifier>(idStrings.size());
			for (String param : idStrings)
			{
				ChangeSetIdentifier id = new ChangeSetIdentifier(param);
				ids.add(id);
			}
		}
		
		String encoding = checkParameters ? cmdLine.getValue("encoding", "UTF-8") : "UTF-8";

		if (checkParameters)
		{
			setUnknownMessage(result, cmdLine, null);
		}

		try
		{
			LiquibaseSupport lb = new LiquibaseSupport(file, encoding);

			List<String> statements = lb.getSQLFromChangeSet(ids);
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);

			for (int i=0; i < statements.size(); i++)
			{
				String sql = statements.get(i);
				rowMonitor.setCurrentRow(i, statements.size());
				runner.runStatement(sql);
				StatementRunnerResult stmtResult = runner.getResult();
				result.addMessage(stmtResult.getMessageBuffer());
				result.addMessageNewLine();

				if (!stmtResult.isSuccess() && !continueOnError)
				{
					result.setFailure();
					break;
				}
			}

			if (this.rowMonitor != null)
			{
				this.rowMonitor.jobFinished();
			}
		}
		catch (Exception th)
		{
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(th));
		}
		return result;
	}

	@Override
	public void done()
	{
		// nothing to do
	}

	@Override
	public void cancel()
		throws SQLException
	{
		if (runner != null)
		{
			runner.cancel();
		}
	}
}
