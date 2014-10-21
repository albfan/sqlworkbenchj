/*
 * ConsolePrompter.java
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
package workbench.console;

import java.sql.SQLException;

import workbench.interfaces.ExecutionController;
import workbench.interfaces.ParameterPrompter;
import workbench.interfaces.StatementParameterPrompter;
import workbench.resource.ResourceMgr;

import workbench.storage.DataStore;

import workbench.sql.VariablePool;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.sql.wbcommands.WbDefineVar;
import workbench.util.HtmlUtil;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An implementation of {@link workbench.interfaces.ParameterPrompter} and
 * {@link workbench.interfaces.ExecutionController} for Console and Batch mode
 * of SQL Workbench/J.
 *
 * It will interactively prompt the user for variables or the confirmation
 * to continue with a given SQL statement.
 *
 * @author Thomas Kellerer
 * @see workbench.interfaces.ParameterPrompter
 * @see workbench.interfaces.ExecutionController
 * @see ConsoleReaderFactory
 */
public class ConsolePrompter
	implements ParameterPrompter, ExecutionController, StatementParameterPrompter
{
	private boolean executeAll = false;

	public ConsolePrompter()
	{
	}

	public void resetExecuteAll()
	{
		this.executeAll = false;
	}

	@Override
	public boolean processParameterPrompts(String sql)
	{
		String verb = SqlUtil.getSqlVerb(sql);

		// Don't prompt for variables when defining a macro
		if (verb.equalsIgnoreCase(WbDefineVar.VERB)) return true;

		VariablePool pool = VariablePool.getInstance();

		DataStore ds = pool.getParametersToBePrompted(sql);
		if (ds == null || ds.getRowCount() == 0) return true;

		System.out.println(ResourceMgr.getString("TxtVariableInputText"));
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			String varName = ds.getValueAsString(row, 0);
			String value = ds.getValueAsString(row, 1);

			String msg = StringUtil.isBlank(value) ? varName + ": " : varName + " [" + value + "]: ";
			String newValue = readLine(msg);
			if (StringUtil.isEmptyString(newValue) && StringUtil.isNonEmpty(value))
			{
				newValue = value;
			}
			ds.setValue(row, 1, newValue);
		}

		try
		{
			ds.updateDb(null, null);
		}
		catch (SQLException ignore)
		{
			// Cannot happen
		}
		return true;
	}

	protected String readLine(String prompt)
	{
		return ConsoleReaderFactory.getConsoleReader().readLine(prompt);
	}

	@Override
	public String getPassword(String prompt)
	{
		return ConsoleReaderFactory.getConsoleReader().readPassword(prompt + " ");
	}

	@Override
	public boolean confirmExecution(String prompt, String yesText, String noText)
	{
		String yes = yesText == null ? ResourceMgr.getString("MsgConfirmYes") : yesText;
		String no = noText == null ? ResourceMgr.getString("MsgConfirmNo") : noText;
		String yesNo = yes + "/" + no;
		String msg = HtmlUtil.cleanHTML(prompt) + " (" + yesNo + ")";
		String choice = readLine(msg + " ");

		if (StringUtil.isBlank(choice))
		{
			return false;
		}
		choice = choice.trim().toLowerCase();

		return yes.toLowerCase().startsWith(choice) || "yes".equalsIgnoreCase(choice);
	}

	@Override
	public boolean confirmStatementExecution(String command)
	{
		if (executeAll) return true;

		String verb = SqlUtil.getSqlVerb(command);
		String yes = ResourceMgr.getString("MsgConfirmYes");
		String all = ResourceMgr.getString("MsgConfirmConsoleAll");

		String yesNo = yes + "/" + ResourceMgr.getString("MsgConfirmNo") + "/" + all;

		String msg = ResourceMgr.getFormattedString("MsgConfirmConsoleExec", verb, yesNo);
		String choice = readLine(msg + " ");

		if (all.equalsIgnoreCase(choice))
		{
			this.executeAll = true;
			return true;
		}

		// allow the localized version and the english yes
		return yes.equalsIgnoreCase(choice) || "yes".equalsIgnoreCase(choice);
	}

	@Override
	public boolean showParameterDialog(StatementParameters parms, boolean showNames)
	{
		System.out.println(ResourceMgr.getString("TxtVariableInputText"));
		for (int param = 0; param < parms.getParameterCount(); param ++)
		{
			String varName = parms.getParameterName(param);
			Object value = parms.getParameterValue(param);
			String stringValue = (value == null ? "" : value.toString());

			String msg = StringUtil.isBlank(stringValue) ? varName : varName + " [" + stringValue + "]";

			String newValue = readLine(msg + ": ");
			parms.setParameterValue(param, newValue);
		}
		return true;
	}
}
