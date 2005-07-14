/*
 * EqualsComparator.java
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
public class ContainsComparator
	implements ColumnComparator
{
	public ContainsComparator()
	{
	}
	
	public String getValueExpression(Object value) { return (value == null ? "" : value.toString()); }
	public String getName() { return "Contains"; }
	public String getOperator() { return "contains"; }
	
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
				return (v.toLowerCase().indexOf(ref.toLowerCase()) > -1);
			else
				return (v.indexOf(ref) > -1);
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

	public boolean supportsIgnoreCase()
	{
		return true;
	}

}