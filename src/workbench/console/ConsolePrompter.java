/*
 * ConsolePrompter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import workbench.sql.VariablePool;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An implementation of {@link workbench.interfaces.ParameterPrompter} and
 * {@link workbench.interfaces.ExecutionController} for Console and Batch mode
 * of SQL Workbench/J
 *
 * It will interactively prompt the user for variables or the confirmation
 * to continue with a given SQL statement.
 *
 * @author Thomas Kellerer
 * @see workbench.interfaces.ParameterPrompter
 * @see workbench.interfaces.ExecutionController
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

	public boolean processParameterPrompts(String sql)
	{
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

	public String getPassword(String prompt)
	{
		return ConsoleReaderFactory.getConsoleReader().readPassword(prompt + " ");
	}

	public boolean confirmExecution(String prompt)
	{
		String yes = ResourceMgr.getString("MsgConfirmConsoleYes");
		String yesNo = yes + "/" + ResourceMgr.getString("MsgConfirmConsoleNo");
		String msg = prompt + " (" + yesNo + ")";
		String choice = readLine(msg + " ");

		// allow the localized version and the english yes
		return yes.equalsIgnoreCase(choice) || "yes".equalsIgnoreCase(choice);
	}

	public boolean confirmStatementExecution(String command)
	{
		if (executeAll) return true;

		String verb = SqlUtil.getSqlVerb(command);
		String yes = ResourceMgr.getString("MsgConfirmConsoleYes");
		String all = ResourceMgr.getString("MsgConfirmConsoleAll");

		String yesNo = yes + "/" + ResourceMgr.getString("MsgConfirmConsoleNo") + "/" + all;

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
