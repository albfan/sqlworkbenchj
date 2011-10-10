/*
 * NotStartsWithComparator.java
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

/**
 * @author Thomas Kellerer
 */
public class NotStartsWithComparator
	implements ColumnComparator
{

	@Override
	public boolean supportsIgnoreCase()
	{
		return true;
	}

	@Override
	public String getValueExpression(Object value)
	{
		return "'" + value + "'";
	}

	@Override
	public String getDescription()
	{
		return getOperator();
	}

	@Override
	public String getOperator()
	{
		return "does not start with";
	}

	@Override
	public boolean needsValue()
	{
		return true;
	}

	@Override
	public boolean validateInput(Object value)
	{
		return value instanceof String;
	}

	@Override
	public boolean comparesEquality()
	{
		return false;
	}

	@Override
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null || value == null)
		{
			return false;
		}
		try
		{
			String v = (String) value;
			String ref = (String) reference;
			if (ignoreCase)
			{
				return !v.toLowerCase().startsWith(ref.toLowerCase());
			}
			else
			{
				return !v.startsWith(ref);
			}
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public boolean supportsType(Class valueClass)
	{
		return (CharSequence.class.isAssignableFrom(valueClass));
	}

	@Override
	public boolean equals(Object other)
	{
		return (other instanceof NotStartsWithComparator);
	}
}
