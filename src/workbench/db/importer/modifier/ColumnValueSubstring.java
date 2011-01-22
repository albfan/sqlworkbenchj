/*
 * ColumnValueSubstring.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer.modifier;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnValueSubstring
{
	private int start;
	private int end;

	public ColumnValueSubstring(int s, int e)
	{
		start = s;
		end = e;
	}
	
	public String getSubstring(String input)
	{
		if (input == null) return null;
		if (start > input.length()) return input;
		if (end > input.length()) return input.substring(start);
		return input.substring(start, end);
	}
	
	public int getStart() { return start; }
	public int getEnd() { return end; }
}
