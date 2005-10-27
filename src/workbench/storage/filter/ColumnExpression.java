/*
 * ColumnExpression.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage.filter;

import java.util.Comparator;
import java.util.Map;
import workbench.util.StringUtil;

/**
 * A class to define the filter criteria for a single column 
 * @author support@sql-workbench.net
 */
public class ColumnExpression
	implements FilterExpression,ExpressionValue
{
	private String columnName;
	private Object filterValue;
	private ColumnComparator comparator;
	private boolean ignoreCase;
	
	public ColumnExpression()
	{
	}

	/**
	 * Define the filter for a column
	 * @param column the column name
	 * @param comparator the comparator to be used to compare the reference value against the actual values
	 * @param referenceValue the filter value to compare against the actual values
	 */
	public ColumnExpression(String column, ColumnComparator comparator, Object referenceValue)
	{
		setComparator(comparator);
		setFilterValue(referenceValue);
		setColumnName(column);
	}
	
	public Object getFilterValue()
	{
		return filterValue;
	}

	public void setFilterValue(Object value)
	{
		this.filterValue = value;
	}

	public ColumnComparator getComparator()
	{
		return comparator;
	}

	public void setComparator(ColumnComparator comp)
	{
		this.comparator = comp;
	}
	
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

	public boolean evaluate(Map columnValues)
	{
		Object value = columnValues.get(this.columnName);
		if (value == null) return true;
		return comparator.evaluate(filterValue, value, this.ignoreCase);
	}
	
	public String getColumnName()
	{

		return this.columnName;
	}

	public void setColumnName(String column)
	{
		this.columnName = column;
	}
	
	public String toString()
	{
		return "[" + columnName + "] " + this.comparator.getOperator() + " " + comparator.getValueExpression(this.filterValue);
	}

	public boolean isIgnoreCase()
	{
		return ignoreCase;
	}

	public void setIgnoreCase(boolean ignoreCase)
	{
		this.ignoreCase = ignoreCase;
	}
}
