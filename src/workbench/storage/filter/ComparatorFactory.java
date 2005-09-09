/*
 * FilterComparatorFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
			new ContainsNotComparator(),
			new StringEqualsComparator(),
			new NumberEqualsComparator(),
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

}
