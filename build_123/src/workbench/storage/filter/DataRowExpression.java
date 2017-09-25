/*
 * DataRowExpression.java
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

import java.util.Map;

/**
 * A class to define the filter criteria for all columns of a data row
 * @author Thomas Kellerer
 */
public class DataRowExpression
	implements FilterExpression, ExpressionValue
{
	private Object filterValue;
	private ColumnComparator comparator;
	private boolean ignoreCase;

	public DataRowExpression()
	{
	}

	/**
	 * Define the filter for a complete row
	 * @param comp the comparator to be used to compare the reference value against the actual values
	 * @param referenceValue the filter value to compare against the actual values
	 */
	public DataRowExpression(ColumnComparator comp, Object referenceValue)
	{
		setComparator(comp);
		setFilterValue(referenceValue);
	}

	@Override
	public Object getFilterValue()
	{
		return filterValue;
	}

	public final void setFilterValue(Object value)
	{
		this.filterValue = value;
	}

	@Override
	public ColumnComparator getComparator()
	{
		return comparator;
	}

	public final void setComparator(ColumnComparator comp)
	{
		this.comparator = comp;
	}

	@Override
	public boolean equals(Object other)
	{
		try
		{
			DataRowExpression def = (DataRowExpression)other;
			if (this.filterValue == null || def.filterValue == null) return false;
			boolean result = this.filterValue.equals(def.filterValue);
			if (result)
			{
				result = this.comparator.equals(def.comparator);
			}
			return result;
		}
		catch (Throwable th)
		{
			return false;
		}
	}

	@Override
	public boolean evaluate(Map<String, Object> columnValues)
	{
		for (Object value : columnValues.values())
		{
			if (value != null && !isArray(value))
			{
				boolean result = comparator.evaluate(filterValue, value.toString(), this.ignoreCase);
				if (result) return true;
			}
		}
		return false;
	}

	private boolean isArray(Object value)
	{
		if (value == null) return false;
		String cls = value.getClass().getName();
		return cls.charAt(0) == '[';
	}

	@Override
	public String toString()
	{
		return "[any column] " + this.comparator.getOperator() + " " + comparator.getValueExpression(this.filterValue);
	}

	@Override
	public boolean isIgnoreCase()
	{
		return ignoreCase;
	}

	@Override
	public void setIgnoreCase(boolean flag)
	{
		this.ignoreCase = flag;
	}

	@Override
	public String getColumnName() { return "*"; }

	@Override
	public boolean isColumnSpecific()
	{
		return false;
	}

}
