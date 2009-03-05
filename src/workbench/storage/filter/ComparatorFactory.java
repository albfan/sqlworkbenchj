/*
 * ComparatorFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;
import java.util.List;
import workbench.util.CollectionBuilder;

/**
 * @author support@sql-workbench.net
 */
public class ComparatorFactory
{
	private final List<ColumnComparator> comparatorList;

	public ComparatorFactory()
	{
		comparatorList = CollectionBuilder.arrayList(
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
			new RegExComparator()
		);
	}

	public List<ColumnComparator> getAvailableComparators()
	{
		return comparatorList;
	}

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
	
	public ColumnComparator findComparatorFor(Class clz)
	{
		for (ColumnComparator comp : comparatorList)
		{
			if (comp.supportsType(clz)) return comp;
		}
		return null;
	}
}
