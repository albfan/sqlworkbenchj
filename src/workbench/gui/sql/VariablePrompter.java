/*
 * VariablePrompter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.util.Set;

import workbench.sql.VariablePool;
import workbench.storage.DataStore;

/**
 * Examine SQL statements whether they need parameters to be entered. 
 * If the passed SQL Statement contains Workbench specific variables
 * the user is prompted to enter them.
 * @see workbench.sql.VariablePool
 * 
 * @author  support@sql-workbench.net
 */
public class VariablePrompter
{
	private Set toPrompt;
	private	VariablePool pool = VariablePool.getInstance();
	private String sql;

	public VariablePrompter()
	{
	}

	public void setSql(String input)
	{
		this.sql = input;
		this.toPrompt = null;
	}
	
	public boolean hasPrompt()
	{
		return this.pool.hasPrompt(this.sql);
	}
	
	public boolean needsInput()
	{
		if (!this.hasPrompt()) return false;
		if (this.toPrompt == null)
		{
			this.toPrompt = this.pool.getVariablesNeedingPrompt(this.sql);
		}
		return (this.toPrompt.size() > 0);
	}
	
	public boolean getPromptValues()
	{
		if (this.toPrompt == null)
		{
			this.toPrompt = this.pool.getVariablesNeedingPrompt(this.sql);
		}
		if (this.toPrompt.size() == 0) return true;
		
		DataStore vars = this.pool.getVariablesDataStore(this.toPrompt);
		
		return VariablesEditor.showVariablesDialog(vars);
	}

}
