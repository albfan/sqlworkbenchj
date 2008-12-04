/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */
package workbench.console;

import java.sql.SQLException;
import workbench.interfaces.ExecutionController;
import workbench.interfaces.ParameterPrompter;
import workbench.resource.ResourceMgr;
import workbench.sql.VariablePool;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * An implementation of {@link workbench.interfaces.ParameterPrompter} and
 * {@link workbench.interfaces.ExecutionController} for Console and Batch mode
 * of SQL Workbench/J
 *
 * It will interactively prompt the user for variables or the confirmation
 * to continue with a given SQL statement.
 * 
 * @author support@sql-workbench.net
 * @see workbench.interfaces.ParameterPrompter
 * @see workbench.interfaces.ExecutionController
 */
public class ConsolePrompter
	implements ParameterPrompter, ExecutionController
{
	private InputReader input;
	private boolean executeAll = false;

	public ConsolePrompter(InputReader reader)
	{
		this.input = reader;
	}

	public ConsolePrompter()
	{
		this.input = new InputReader();
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

			String newValue = input.readLine(varName + " [" + value + "]: ");
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

	public String getPassword(String prompt)
	{
		return input.readPassword(prompt + " ");
	}

	public boolean confirmExecution(String prompt)
	{
		String yes = ResourceMgr.getString("MsgConfirmConsoleYes");
		String yesNo = yes + "/" + ResourceMgr.getString("MsgConfirmConsoleNo");
		String msg = prompt + " (" + yesNo + ")";
		String choice = input.readLine(msg + " ");

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
		String choice = input.readLine(msg + " ");
		
		if (all.equalsIgnoreCase(choice))
		{
			this.executeAll = true;
			return true;
		}
		
		// allow the localized version and the english yes
		return yes.equalsIgnoreCase(choice) || "yes".equalsIgnoreCase(choice);
	}
}
