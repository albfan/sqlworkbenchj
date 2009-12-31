/*
 * ComparatorListItem.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;

import workbench.storage.filter.ColumnComparator;

/**
 * A wrapper class to display the operator for a comparator
 * @author Thomas Kellerer
 */
public class ComparatorListItem
{
	private ColumnComparator comparator;

	public ComparatorListItem(ColumnComparator comp)
	{
		comparator = comp;
	}

	public String toString()
	{
		return comparator.getOperator();
	}

	public ColumnComparator getComparator()
	{
		return comparator;
	}

	public boolean equals(Object other)
	{
		if (other instanceof ComparatorListItem)
		{
			return comparator.equals(((ComparatorListItem)other).comparator);
		}
		return false;
	}
}
