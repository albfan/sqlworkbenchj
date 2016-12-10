/*
 * WbListVars.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;

import workbench.gui.dbobjects.QuickFilterExpressionBuilder;

import workbench.storage.DataStore;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.RegExComparator;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;

import workbench.util.ArgumentParser;
import workbench.util.StringUtil;

/**
 * Display all variables defined through {@link WbDefineVar}
 *
 * @author Thomas Kellerer
 */
public class WbListVars extends SqlCommand
{
	public static final String VERB = "WbVarList";
	public static final String VERB_ALTERNATE = "WbListVars";
	public static final String ARG_MATCH = "match";

  public WbListVars()
  {
    super();
    this.cmdLine = new ArgumentParser();
    this.cmdLine.addArgument(ARG_MATCH);
    this.isUpdatingCommand = false;
  }

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public String getAlternateVerb()
	{
		return VERB_ALTERNATE;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

    String args = getCommandLine(aSql);

    if (displayHelp(result))
    {
      return result;
    }

    cmdLine.parse(args);

		DataStore ds = VariablePool.getInstance().getVariablesDataStore();
		ds.setResultName(ResourceMgr.getString("TxtVariables"));

    ColumnExpression filter = null;
    if (cmdLine.hasArguments())
    {
      String regex = cmdLine.getValue(ARG_MATCH);
      if (StringUtil.isNonBlank(regex))
      {
        filter = new ColumnExpression(ds.getColumnName(0), new RegExComparator(), regex);
      }
    }
    else if (StringUtil.isNonBlank(args))
    {
      QuickFilterExpressionBuilder builder = new QuickFilterExpressionBuilder();
      filter = builder.buildExpression(args.trim(), ds.getColumnName(0), true);
    }

    if (filter != null)
    {
      ds.applyFilter(filter);
      ds.sortByColumn(1, true);
    }
    ds.resetStatus();

		CommandTester ct = new CommandTester();
		ds.setGeneratingSql(ct.formatVerb(getVerb()) + args);
		result.addDataStore(ds);
		result.setSuccess();

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
