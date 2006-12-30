/*
 * TableAlias.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import workbench.db.TableIdentifier;

/**
 * @author  support@sql-workbench.net
 */
public class TableAlias
{
	private final TableIdentifier table;
	private final String alias;
	private String display;
	
	public TableAlias(String value)
	{
		if (StringUtil.isEmptyString(value)) throw new IllegalArgumentException("Identifier must not be empty");
		
		String tablename = null;
		String[] words = value.split("\\s");
		
		if (words.length > 0)
		{
			tablename = words[0].trim();
		}
		
		if (words.length == 2)
		{
			alias = words[1].trim();
		}
		else if (words.length == 3)
		{
			// Assuming "table AS t1" syntax
			if (words[1].equalsIgnoreCase("as"))
			{
				alias = words[2].trim();
			}
			else
			{
				alias = words[1].trim();
			}
		}
		else
		{
			this.alias = null;
		}

		this.table = new TableIdentifier(tablename);
		
	}
	
	public final String getAlias() { return this.alias; }
	public final TableIdentifier getTable() { return this.table; }
	
	public final String getNameToUse() 
	{
		if (alias == null) return table.getTableName();
		return alias;
	}
	
	public String toString() 
	{
		if (display == null)
		{
			if (alias == null) display = table.getTableName();
			else display = alias + " (" + table + ")";
		}
		return display;
	} 
	
	/**
	 * Compares the given name to this TableAlias checking
	 * if the name either references this table or its alias
	 */
	public boolean isTableOrAlias(String name)
	{
		if (StringUtil.isEmptyString(name)) return false;
		
		TableIdentifier tbl = new TableIdentifier(name);
		return (table.getTableName().equalsIgnoreCase(tbl.getTableName()) || name.equalsIgnoreCase(alias));
	}
}
