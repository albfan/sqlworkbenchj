/*
 * VariablePrompter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.util.Set;

import workbench.sql.SqlParameterPool;
import workbench.storage.DataStore;

/**
 *
 * @author  info@sql-workbench.net
 */
public class VariablePrompter
{
	private Set toPrompt = null;
	private	SqlParameterPool pool = SqlParameterPool.getInstance();
	private String sql;

	public VariablePrompter(String input)
	{
		this.sql = input;
	}
	
	public boolean needsInput()
	{
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
