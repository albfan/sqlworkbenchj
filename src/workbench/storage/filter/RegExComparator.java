/*
 * RegExComparator.java
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.resource.ResourceMgr;

/**
 * Implementation of the ColumnComparator using regular expressions.
 *
 * @author Thomas Kellerer
 */
public class RegExComparator
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
		return ResourceMgr.getString("TxtOpMatches");
	}

	public String getOperator()
	{
		return "matches";
	}

	public boolean needsValue()
	{
		return true;
	}

	public boolean comparesEquality()
	{
		return false;
	}

	public boolean supportsType(Class valueClass)
	{
		return (String.class.isAssignableFrom(valueClass));
	}

	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null || value == null)
		{
			return false;
		}

		Pattern p = null;
		if (ignoreCase)
		{
			p = Pattern.compile(reference.toString(), Pattern.CASE_INSENSITIVE);
		}
		else
		{
			p = Pattern.compile(reference.toString());
		}
		Matcher m = p.matcher(value.toString());

		return m.find();
	}

	public boolean equals(Object other)
	{
		return (other instanceof RegExComparator);
	}

	public boolean validateInput(Object value)
	{
		if (!(value instanceof String))
		{
			return false;
		}

		try
		{
			Pattern.compile((String) value);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
