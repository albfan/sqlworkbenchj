/*
 * ColumnExpression.java
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

import java.util.Map;

/**
 * A class to define the filter criteria for a single column
 *
 * @author Thomas Kellerer
 */
public class ColumnExpression
	implements FilterExpression, ExpressionValue
{
	private String columnName;
	private Object filterValue;
	private ColumnComparator comparator;
	private boolean ignoreCase;

	/**
	 * Default constructor needed for XML serialisation.
	 */
	public ColumnExpression()
	{
	}

	/**
	 * Define the filter for a column
	 *
	 * @param column the column name
	 * @param comp the comparator to be used to compare the reference value against the actual values
	 * @param value the filter value to compare against the actual values
	 *
	 * @see #setFilterValue(Object)
	 * @see #setComparator(ColumnComparator)
	 */
	public ColumnExpression(String column, ColumnComparator comp, Object value)
	{
		setComparator(comp);
		setFilterValue(value);
		setColumnName(column);
	}

	/**
	 * Define a "generic" column filter
	 *
	 * @param comp the comparator to be used to compare the reference value against the actual values
	 * @param value the filter value to compare against the actual values
	 *
	 * @see #setFilterValue(Object)
	 * @see #setComparator(ColumnComparator)
	 */
	public ColumnExpression(ColumnComparator comp, Object value)
	{
		setComparator(comp);
		setFilterValue(value);
		setColumnName("*");
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
			ColumnExpression def = (ColumnExpression)other;
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

	public boolean evaluate(Object value)
	{
		if (value != null && !comparator.supportsType(value.getClass())) return false;
		return comparator.evaluate(filterValue, value, this.ignoreCase);
	}

	@Override
	public boolean evaluate(Map columnValues)
	{
		Object value = columnValues.get(this.columnName);
		return evaluate(value);
	}

	@Override
	public String getColumnName()
	{
		return this.columnName;
	}

	public final void setColumnName(String column)
	{
		this.columnName = (column == null ? null : column.toLowerCase());
	}

	@Override
	public String toString()
	{
		return "[" + columnName + "] " + this.comparator.getOperator() + " " + comparator.getValueExpression(this.filterValue);
	}

	@Override
	public boolean isIgnoreCase()
	{
		return ignoreCase;
	}

	@Override
	public void setIgnoreCase(boolean ignore)
	{
		this.ignoreCase = ignore;
	}

	@Override
	public boolean isColumnSpecific()
	{
		return true;
	}
}
