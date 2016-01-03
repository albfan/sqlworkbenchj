/*
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

import javax.swing.JOptionPane;

import workbench.WbManager;
import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.StringUtil;



/**
 * A SQL Statement to simply display a message dialog in GUI mode.
 *
 * <br>
 * When running in batch or console mode, this command has no effect.
 *
 * @author Thomas Kellerer
 */
public class WbMessage
	extends SqlCommand
{
	public static final String VERB = "WbMessage";
  public static final String PARAM_TITLE = "title";

	public WbMessage()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(WbConfirm.PARAM_MSG);
    cmdLine.addArgument(PARAM_TITLE);
		isUpdatingCommand = false;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		String args = getCommandLine(sql);
		cmdLine.parse(args);

    String msg = null;
		String title = ResourceMgr.TXT_PRODUCT_NAME;

		if (cmdLine.hasArguments())
		{
			msg = cmdLine.getValue(WbConfirm.PARAM_MSG);
      title = cmdLine.getValue(PARAM_TITLE, ResourceMgr.TXT_PRODUCT_NAME);
		}
		else
		{
			msg = StringUtil.trimQuotes(args);
		}

		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

    if (StringUtil.isNonBlank(msg) && WbManager.getInstance().isGUIMode())
    {
      JOptionPane.showMessageDialog(WbManager.getInstance().getCurrentWindow(), msg, title, JOptionPane.INFORMATION_MESSAGE);
    }
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
