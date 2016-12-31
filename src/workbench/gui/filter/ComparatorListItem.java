/*
 * ComparatorListItem.java
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

	@Override
	public String toString()
	{
		return comparator.getOperator();
	}

	public ColumnComparator getComparator()
	{
		return comparator;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ComparatorListItem)
		{
			ColumnComparator otherComp = ((ComparatorListItem)other).comparator;
			return this.comparator.equals(otherComp);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 11 * hash + (this.comparator != null ? this.comparator.hashCode() : 0);
		return hash;
	}
}
