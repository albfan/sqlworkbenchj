/*
 * ComparatorFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;
import java.util.List;
import workbench.util.CollectionUtil;

/**
 * @author Thomas Kellerer
 */
public class ComparatorFactory
{
	private final List<ColumnComparator> comparatorList;

	public ComparatorFactory()
	{
		comparatorList = CollectionUtil.arrayList(
			new ContainsComparator(),
			new StartsWithComparator(),
			new NotStartsWithComparator(),
			new ContainsNotComparator(),
			new StringEqualsComparator(),
			new StringNotEqualsComparator(),
			new NumberEqualsComparator(),
			new DateEqualsComparator(),
			new NumberNotEqualsComparator(),
			new LessThanComparator(),
			new LessOrEqualComparator(),
			new GreaterThanComparator(),
			new GreaterOrEqualComparator(),
			new IsNullComparator(),
			new IsNotNullComparator(),
			new RegExComparator(),
			new BooleanEqualsComparator()
		);
	}

	public List<ColumnComparator> getAvailableComparators()
	{
		return comparatorList;
	}

	/**
	 * Returns the first ColumnComparator that supports an equality comparator for the given class.
	 *
	 * @param clz the class to compare
	 */
	public ColumnComparator findEqualityComparatorFor(Class clz)
	{
		for (ColumnComparator comp : comparatorList)
		{
			if (comp.supportsType(clz) && comp.comparesEquality())
			{
				return comp;
			}
		}
		return null;
	}

	/**
	 * Returns the first ColumnComparator that supports the given class.
	 *
	 * @param clz the class to compare
	 */
	public ColumnComparator findComparatorFor(Class clz)
	{
		for (ColumnComparator comp : comparatorList)
		{
			if (comp.supportsType(clz)) return comp;
		}
		return null;
	}
}
