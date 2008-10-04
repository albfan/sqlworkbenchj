/*
 * DataRowExpression.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import java.util.Iterator;
import java.util.Map;

/**
 * A class to define the filter criteria for all columns of a data row
 * @author support@sql-workbench.net
 */
public class DataRowExpression
	implements FilterExpression,ExpressionValue
{
	private Object filterValue;
	private ColumnComparator comparator;
	private boolean ignoreCase;
	
	public DataRowExpression()
	{
	}

	/**
	 * Define the filter for a complete row
	 * @param comparator the comparator to be used to compare the reference value against the actual values
	 * @param referenceValue the filter value to compare against the actual values
	 */
	public DataRowExpression(ColumnComparator comp, Object referenceValue)
	{
		setComparator(comp);
		setFilterValue(referenceValue);
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

	public boolean evaluate(Map<String, Object> columnValues)
	{
		Iterator itr = columnValues.values().iterator();
		while (itr.hasNext())
		{
			Object value = itr.next();
			if (value != null)
			{
				boolean result = comparator.evaluate(filterValue, value.toString(), this.ignoreCase);
				if (result) return true;
			}
		}
		return false;
	}

	public String toString()
	{
		return "[any column] " + this.comparator.getOperator() + " " + comparator.getValueExpression(this.filterValue);
	}

	public boolean isIgnoreCase()
	{
		return ignoreCase;
	}

	public void setIgnoreCase(boolean flag)
	{
		this.ignoreCase = flag;
	}
	
	public String getColumnName() { return "*"; }
	
	public boolean isColumnSpecific() 
	{
		return false;
	}
	
}
