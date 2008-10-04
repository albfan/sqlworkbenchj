/*
 * ValueDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

/**
 *
 * @author support@sql-workbench.net
 */
public class ValueDisplay
{
	private String display;

	public ValueDisplay(Object[] row)
	{
		int count = row.length;
		StringBuilder values = new StringBuilder(count * 20);
		values.append('[');

		for (int i=0; i < count; i++)
		{
			if (i > 0) values.append(',');
			if (row[i] == null)
			{
				values.append("NULL");
			}
			else
			{
				values.append(row[i].toString());
			}
		}
		values.append(']');
		display = values.toString();
	}

	public String toString()
	{
		return display;
	}

}
