/*
 * VariablePrompter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.util.Set;

import workbench.sql.VariablePool;
import workbench.storage.DataStore;

/**
 *
 * @author  info@sql-workbench.net
 */
public class VariablePrompter
{
	private Set toPrompt = null;
	private	VariablePool pool = VariablePool.getInstance();
	private String sql;

	public VariablePrompter(String input)
	{
		this.sql = input;
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
	
	public static void main(String[] args)
	{
		try
		{
			String sql = "SELECT * \n" + 
                                 "FROM configurations \n" + 
                                 "where ( key not in (select key from configurations where username = 'tkellerer') \n" + 
                                 "        and key like '%$[?keystart]%' \n" + 
                                 "        and username = '_global_') \n" + 
                                 "or username = 'tkellerer' \n" + 
                                 "and KEY like '%$[&keystart]%' \n" + 
                                 "ORDER BY key";		
			sql += sql;
			sql += sql;
			sql += sql;
			sql += sql;
			sql += sql;
				VariablePrompter p = new VariablePrompter(sql);
				System.out.println(p.hasPrompt());
				
			long start, end;
			start = System.currentTimeMillis();
			for (int i=0; i < 50000; i ++)
			{
				VariablePrompter p2 = new VariablePrompter(sql);
				p2.hasPrompt();
			}
			end = System.currentTimeMillis();
			System.out.println("time=" + (end -start));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("Done.");
	}
}
