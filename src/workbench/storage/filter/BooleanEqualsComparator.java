/*
 * NumberEqualsComparator.java
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

import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class BooleanEqualsComparator
	implements ColumnComparator
{

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
		if (reference == null || value == null)
		{
			return false;
		}

		Boolean refValue = null;
		Boolean compare = null;

		if (reference instanceof Boolean)
		{
			refValue = (Boolean)reference;
		}

		if (value instanceof Boolean)
		{
			compare = (Boolean)value;
		}

		if (reference instanceof String)
		{
			refValue = StringUtil.stringToBool((String)reference);
		}

		if (value instanceof String)
		{
			compare = StringUtil.stringToBool((String)value);
		}
		if (refValue == null || compare == null) return false;

		return refValue.booleanValue() == compare.booleanValue();
	}

	@Override
	public boolean supportsType(Class valueClass)
	{
		return Boolean.class.isAssignableFrom(valueClass);
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof BooleanEqualsComparator;
	}

	@Override
	public boolean validateInput(Object value)
	{
		return value == null ? false : StringUtil.isNumber(value.toString());
	}

}
