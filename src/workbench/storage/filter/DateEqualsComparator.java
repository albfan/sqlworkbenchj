/*
 * DateEqualsComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import workbench.util.ValueConverter;

/**
 * @author Thomas Kellerer
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

	@Override
	public boolean supportsIgnoreCase()
	{
		return false;
	}

	@Override
	public String getValueExpression(Object value)
	{
		return (value == null ? "" : value.toString());
	}

	@Override
	public String getOperator()
	{
		return "=";
	}

	@Override
	public String getDescription()
	{
		return "equals";
	}

	@Override
	public boolean needsValue()
	{
		return true;
	}

	@Override
	public boolean comparesEquality()
	{
		return true;
	}

	@Override
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null || value == null) return false;
		try
		{
			return reference.equals(value);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public boolean supportsType(Class valueClass)
	{
		return java.util.Date.class.isAssignableFrom(valueClass);
	}

	@Override
	public boolean equals(Object other)
	{
		return (other instanceof DateEqualsComparator);
	}

	@Override
	public boolean validateInput(Object value)
	{
		if (value == null) return false;

		if (value instanceof java.util.Date) return true;

		ValueConverter converter = new ValueConverter();
		try
		{
			converter.convertValue(value, this.sqlType);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

}
