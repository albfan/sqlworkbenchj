/*
 * GreaterOrEqualComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class GreaterOrEqualComparator
	implements ColumnComparator
{
	public GreaterOrEqualComparator()
	{
	}
	
	public boolean supportsIgnoreCase() { return false; }

	public String getValueExpression(Object value) { return (value == null ? "" : value.toString()); }
	public String getOperator() { return "\u2265"; }
	public boolean needsValue() { return true; }	
	public boolean comparesEquality() { return false; }
	
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null && value == null) return true;
		if (reference == null && value != null) return false;
		if (reference != null && value == null) return false;
		try
		{
			int result = ((Comparable)reference).compareTo((Comparable)value);
			return result <= 0;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public boolean supportsType(Class valueClass)
	{
		return Comparable.class.isAssignableFrom(valueClass);
	}
	
	public boolean equals(Object other)
	{
		return (other instanceof GreaterOrEqualComparator);
	}
	
	public boolean validateInput(String value)
	{
		return StringUtil.isNumber(value);
	}	

}
