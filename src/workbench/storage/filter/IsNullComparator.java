/*
 * IsNullComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import workbench.storage.NullValue;

/**
 * @author support@sql-workbench.net
 */
public class IsNullComparator
	implements ColumnComparator
{
	public IsNullComparator()
	{
	}
	
	public String getValueExpression(Object value) { return (value == null ? "" : value.toString()); }
	public String getOperator() { return "is null"; }
	public boolean needsValue() { return false; }	
	
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		return (value == null || value instanceof NullValue);
	}
	
	public boolean supportsType(Class valueClass)
	{
		return true;
	}

	public boolean supportsIgnoreCase()
	{
		return false;
	}

}