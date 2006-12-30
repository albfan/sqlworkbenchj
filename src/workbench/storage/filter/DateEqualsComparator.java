/*
 * DateEqualsComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import workbench.util.ValueConverter;

/**
 * @author support@sql-workbench.net
 */
public class DateEqualsComparator
	implements ColumnComparator
{
	private int sqlType;
	
	public DateEqualsComparator()
	{
		this.sqlType = java.sql.Types.TIMESTAMP;
	}
	
	public DateEqualsComparator(int type)
	{
		this.sqlType = type;
	}
	
	public boolean supportsIgnoreCase() { return false; }

	public String getValueExpression(Object value) { return (value == null ? "" : value.toString()); }
	public String getOperator() { return "="; }
	public boolean needsValue() { return true; }
	public boolean comparesEquality() { return true; }
	
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null && value == null) return true;
		if (reference == null && value != null) return false;
		if (reference != null && value == null) return false;
		try
		{
			return reference.equals(value);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public boolean supportsType(Class valueClass)
	{
		return (java.util.Date.class.isAssignableFrom(valueClass));
	}

	public boolean equals(Object other)
	{
		return (other instanceof NumberEqualsComparator);
	}
	
	public boolean validateInput(Object value)
	{
		if (value == null) return false;
		
		if (value instanceof java.util.Date) return true;
		
		ValueConverter converter = new ValueConverter();
		try
		{
			converter.convertValue(value.toString(), this.sqlType);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}	

}
