/*
 * StartsWithComparator.java
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

/**
 * @author support@sql-workbench.net
 */
public class StartsWithComparator
	implements ColumnComparator
{
	public StartsWithComparator()
	{
	}
	
	public boolean supportsIgnoreCase() { return true; }
	
	public String getValueExpression(Object value) { return "'" + value + "'";}
	public String getOperator() { return "starts with"; }
	public boolean needsValue() { return true; }
	public boolean validateInput(String value) { return true; }
	
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null && value == null) return true;
		if (reference == null && value != null) return false;
		if (reference != null && value == null) return false;
		try
		{
			String v = (String)value;
			String ref = (String)reference;
			if (ignoreCase)
				return v.toLowerCase().startsWith(ref.toLowerCase());
			else
				return v.startsWith(ref);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public boolean supportsType(Class valueClass)
	{
		return (CharSequence.class.isAssignableFrom(valueClass));
	}
	
	public boolean equals(Object other)
	{
		return (other instanceof StartsWithComparator);
	}
	
}