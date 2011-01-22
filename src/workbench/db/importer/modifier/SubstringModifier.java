/*
 * SubstringModifier.java
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

import java.util.HashMap;
import java.util.Map;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class SubstringModifier
	implements ImportValueModifier
{
	public Map<ColumnIdentifier, ColumnValueSubstring> limits =  new HashMap<ColumnIdentifier, ColumnValueSubstring>();

	public int getSize()
	{
		return limits.size();
	}

	/**
	 * Define substring limits for a column.
	 * An existing mapping for that column will be overwritten.
	 *
	 * @param col the column for which to apply the substring
	 * @param start the start of the substring
	 * @param end the end of the substring
	 */
	public void addDefinition(ColumnIdentifier col, int start, int end)
	{
		ColumnValueSubstring s = new ColumnValueSubstring(start, end);
		this.limits.put(col.createCopy(), s);
	}

	public String modifyValue(ColumnIdentifier col, String value)
	{
		ColumnValueSubstring s = this.limits.get(col);
		if (s != null)
		{
			return s.getSubstring(value);
		}
		return value;
	}

	/**
	 * For testing purposes to allow access to the actual "modifier"
	 */
	public ColumnValueSubstring getSubstring(ColumnIdentifier col)
	{
		return this.limits.get(col);
	}

}

