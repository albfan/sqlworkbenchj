/*
 * StringNotEqualsComparator.java
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

/**
 * @author Thomas Kellerer
 */
public class StringNotEqualsComparator
	implements ColumnComparator
{

	public boolean supportsIgnoreCase()
	{
		return true;
	}

	public String getValueExpression(Object value)
	{
		return "'" + value + "'";
	}

	public String getOperator()
	{
		return "<>";
	}

	public String getDescription()
	{
		return "not equal";
	}

	public boolean needsValue()
	{
		return true;
	}

	public boolean validateInput(Object value)
	{
		return value instanceof String;
	}

	public boolean comparesEquality()
	{
		return false;
	}

	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null || value == null)
		{
			return false;
		}
		try
		{
			if (ignoreCase)
			{
				return !((String) reference).equalsIgnoreCase((String) value);
			}
			else
			{
				return !reference.equals(value);
			}
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public boolean supportsType(Class valueClass)
	{
		return (String.class.isAssignableFrom(valueClass));
	}

	public boolean equals(Object other)
	{
		return (other instanceof StringNotEqualsComparator);
	}
}
