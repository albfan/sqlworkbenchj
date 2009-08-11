/*
 * IsNullComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

/**
 * @author Thomas Kellerer
 */
public class IsNullComparator
	implements ColumnComparator
{

	public String getValueExpression(Object value)
	{
		return (value == null ? "" : value.toString());
	}

	public String getOperator()
	{
		return "is null";
	}

	public String getDescription()
	{
		return getOperator();
	}

	public boolean needsValue()
	{
		return false;
	}

	public boolean validateInput(Object value)
	{
		return true;
	}

	public boolean comparesEquality()
	{
		return false;
	}

	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		return (value == null);
	}

	public boolean supportsType(Class valueClass)
	{
		return true;
	}

	public boolean supportsIgnoreCase()
	{
		return false;
	}

	public boolean equals(Object other)
	{
		return (other instanceof IsNullComparator);
	}
}
