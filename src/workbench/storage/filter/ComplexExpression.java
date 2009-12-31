/*
 * ComplexExpression.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Kellerer
 */
public abstract class ComplexExpression
	implements FilterExpression
{
	protected List<FilterExpression> filter = new LinkedList<FilterExpression>();
	
	public void addExpression(FilterExpression expr)
	{
		filter.add(expr);
	}
	
	public void addColumnExpression(String colname, ColumnComparator comp, Object refValue)
	{
		addColumnExpression(colname, comp, refValue, comp.supportsIgnoreCase());
	}
	
	public void addColumnExpression(String colname, ColumnComparator comp, Object refValue, boolean ignoreCase)
	{
		ColumnExpression def = new ColumnExpression(colname, comp, refValue);
		if (comp.supportsIgnoreCase()) def.setIgnoreCase(ignoreCase);
		addExpression(def);
	}
	
	public boolean hasFilter()
	{
		if (this.filter == null) return false;
		return (this.filter.size() > 0);
	}
	
	public void removeExpression(FilterExpression expr)
	{
		filter.remove(expr);
	}

	/**
	 * Get the list of FilterExpression s that define this ComplexExpression
	 */
	public List getExpressions() { return filter; }
	
	public void setExpressions(List<FilterExpression> l) { this.filter = l;}
	
	public boolean equals(Object other)
	{
		try
		{
			ComplexExpression o = (ComplexExpression)other;
			return this.filter.equals(o.filter);
		}
		catch (Throwable e)
		{
			return false;
		}
	}
	public boolean isColumnSpecific()
	{
		for (FilterExpression expr : filter)
		{	
			if (expr.isColumnSpecific()) return true;
		}
		return false;
	}
	
	public abstract boolean evaluate(Map<String, Object> columnValues);
}
