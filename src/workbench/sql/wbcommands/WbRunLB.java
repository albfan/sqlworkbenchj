/*
 * WbRunLB.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import workbench.resource.ResourceMgr;
import workbench.sql.ParserType;

import workbench.storage.RowActionMonitor;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.MessageBuffer;
import workbench.util.WbFile;

/**
 * A command to run/include the SQL (<sql> or <createProcedure> tag) of a single Liquibase changeset
 *
 * @author  Thomas Kellerer
 */
public class WbRunLB
	extends SqlCommand
{
	public static final String VERB = "WbRunLB";

	public static final String ARG_CHANGESET = "changeSet";
	public static final String ARG_VERBOSE = "verbose";
	public static final String ARG_FILE = "file";

	public WbRunLB()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_FILE);
		cmdLine.addArgument(CommonArgs.ARG_CONTINUE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_CHANGESET, ArgumentType.Repeatable);
		cmdLine.addArgument(ARG_VERBOSE, ArgumentType.BoolSwitch);
		CommonArgs.addEncodingParameter(cmdLine);
		isUpdatingCommand = true;
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
			file = evaluateFileArgument(cmdLine.getValue(ARG_FILE));
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
			String msg = ResourceMgr.getFormattedString("ErrFileNotFound", file.getFullPath());
			result.addMessage(msg);
			return result;
		}

		boolean continueOnError = checkParameters ? cmdLine.getBoolean(CommonArgs.ARG_CONTINUE, false) : false;
		boolean verbose = checkParameters ? cmdLine.getBoolean(ARG_VERBOSE, false) : false;

		List<String> idStrings = checkParameters ? cmdLine.getListValue(ARG_CHANGESET) : null;
		List<ChangeSetIdentifier> ids = null;

		if (CollectionUtil.isNonEmpty(idStrings))
		{
			ids = new ArrayList<>(idStrings.size());
			for (String param : idStrings)
			{
				ChangeSetIdentifier id = new ChangeSetIdentifier(param);
				ids.add(id);
			}
		}

		String encoding = checkParameters ? cmdLine.getValue(CommonArgs.ARG_ENCODING, "UTF-8") : "UTF-8";

		if (checkParameters)
		{
			setUnknownMessage(result, cmdLine, null);
		}


		boolean oldVerbose = runner.getVerboseLogging();
		try
		{
			runner.setVerboseLogging(verbose);
			LiquibaseSupport lb = new LiquibaseSupport(file, encoding);
			lb.setParserType(ParserType.getTypeFromConnection(currentConnection));

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
			MessageBuffer warnings = lb.getWarnings();
			if (warnings.getLength() > 0)
			{
				result.addMessage(warnings);
				result.setWarning(true);
			}
		}
		catch (Exception th)
		{
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(th));
		}
		finally
		{
			runner.setVerboseLogging(oldVerbose);
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

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
