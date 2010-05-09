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
import java.util.List;
import workbench.liquibase.LiquibaseSupport;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;
import workbench.storage.RowActionMonitor;
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

	private BatchRunner batchRunner;

	public WbRunLB()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("file");
		cmdLine.addArgument(CommonArgs.ARG_CONTINUE, ArgumentType.BoolArgument);
		cmdLine.addArgument("changeSet");
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

		cmdLine.parse(getCommandLine(aSql));
		WbFile file = evaluateFileArgument(cmdLine.getValue("file"));

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

		boolean continueOnError = cmdLine.getBoolean(CommonArgs.ARG_CONTINUE, false);
		List<String> ids = StringUtil.stringToList(cmdLine.getValue("changeSet"));

		String encoding = cmdLine.getValue("encoding");
		if (encoding == null)
		{
			encoding = "UTF-8";
		}
		setUnknownMessage(result, cmdLine, null);

		try
		{
			LiquibaseSupport lb = new LiquibaseSupport(file, encoding);

			List<String> statements =lb.getSQLFromChangeSet(ids);
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
		super.cancel();
		if (batchRunner != null)
		{
			batchRunner.cancel();
		}
	}
}
