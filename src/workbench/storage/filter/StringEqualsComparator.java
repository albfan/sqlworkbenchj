/*
 * StringEqualsComparator.java
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

import workbench.resource.ResourceMgr;

/**
 * @author Thomas Kellerer
 */
public class StringEqualsComparator
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

	public String getDescription()
	{
		return ResourceMgr.getString("TxtOpEquals");
	}

	public String getOperator()
	{
		return "=";
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
		return true;
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
				return ((String) reference).equalsIgnoreCase((String) value);
			}
			else
			{
				return reference.equals(value);
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
		return other instanceof StringEqualsComparator;
	}
}
