/*
 * TableAlias.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.util;

/**
 * @author  info@sql-workbench.net
 */
public class TableAlias
{
	private final String table;
	private final String alias;
	private String display;
	
	public TableAlias(String value)
	{
		int apos = value.indexOf(' ');
		if (apos > -1)
		{
			this.table = value.substring(0, apos);
			this.alias = value.substring(apos + 1);
		}
		else
		{
			this.table = value;
			this.alias = null;
		}
	}
	
	public final String getAlias() { return this.alias; }
	public final String getTable() { return this.table; }
	public final String getNameToUse() 
	{
		if (alias == null) return table;
		return alias;
	}
	public String toString() 
	{
		if (display == null)
		{
			if (alias == null) display = table;
			else display = alias + " (" + table + ")";
		}
		return display;
	} 
		
	public boolean isTableOrAlias(String name)
	{
		if (this.alias == null)
			return table.equalsIgnoreCase(name);
		else
			return (name.equalsIgnoreCase(table) || name.equalsIgnoreCase(alias));
	}
}
