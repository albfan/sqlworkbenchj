/*
 * ComparatorFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
