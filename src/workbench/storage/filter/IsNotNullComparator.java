/*
 * IsNotNullComparator.java
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
public class IsNotNullComparator
	implements ColumnComparator
{

	@Override
	public String getValueExpression(Object value)
	{
		return (value == null ? "" : value.toString());
	}

	@Override
	public String getOperator()
	{
		return "not null";
	}

	@Override
	public String getDescription()
	{
		return getOperator();
	}

	@Override
	public boolean needsValue()
	{
		return false;
	}

	@Override
	public boolean validateInput(Object value)
	{
		return true;
	}

	@Override
	public boolean comparesEquality()
	{
		return false;
	}

	@Override
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		return (value != null);
	}

	@Override
	public boolean supportsType(Class valueClass)
	{
		return true;
	}

	@Override
	public boolean supportsIgnoreCase()
	{
		return false;
	}

	@Override
	public boolean equals(Object other)
	{
		return (other instanceof IsNotNullComparator);
	}
}
