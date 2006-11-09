/*
 * ComparatorFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author support@sql-workbench.net
 */
public class ComparatorFactory
{
	private ColumnComparator[] comparatorList;

	public ComparatorFactory()
	{
		comparatorList = new ColumnComparator[]
		{
			new ContainsComparator(),
			new StartsWithComparator(),
			new NotStartsWithComparator(),
			new ContainsNotComparator(),
			new StringEqualsComparator(),
			new StringNotEqualsComparator(),
			new NumberEqualsComparator(),
			new DateEqualsComparator(0),
			new NumberNotEqualsComparator(),
			new LessThanComparator(),
			new LessOrEqualComparator(),
			new GreaterThanComparator(),
			new GreaterOrEqualComparator(),
			new IsNullComparator(),
			new IsNotNullComparator(),
			new RegExComparator()
		};
	}

	public ColumnComparator[] getAvailableComparators()
	{
		return comparatorList;
	}

	public ColumnComparator findEqualityComparatorFor(Class clz)
	{
		for (int i=0; i < comparatorList.length; i++)
		{
			if (comparatorList[i].supportsType(clz) && comparatorList[i].comparesEquality()) 
			{
				return comparatorList[i];
			}
		}
		return null;
	}
	
	public ColumnComparator findComparatorFor(Class clz)
	{
		for (int i=0; i < comparatorList.length; i++)
		{
			if (comparatorList[i].supportsType(clz)) return comparatorList[i];
		}
		return null;
	}
}
