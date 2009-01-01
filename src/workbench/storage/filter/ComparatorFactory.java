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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author support@sql-workbench.net
 */
public class ComparatorFactory
{
	private final List<ColumnComparator> comparatorList = new ArrayList<ColumnComparator>(17);

	public ComparatorFactory()
	{
		comparatorList.add(new ContainsComparator());
		comparatorList.add(new StartsWithComparator());
		comparatorList.add(new NotStartsWithComparator());
		comparatorList.add(new ContainsNotComparator());
		comparatorList.add(new StringEqualsComparator());
		comparatorList.add(new StringNotEqualsComparator());
		comparatorList.add(new NumberEqualsComparator());
		comparatorList.add(new DateEqualsComparator());
		comparatorList.add(new NumberNotEqualsComparator());
		comparatorList.add(new LessThanComparator());
		comparatorList.add(new LessOrEqualComparator());
		comparatorList.add(new GreaterThanComparator());
		comparatorList.add(new GreaterOrEqualComparator());
		comparatorList.add(new IsNullComparator());
		comparatorList.add(new IsNotNullComparator());
		comparatorList.add(new RegExComparator());
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
