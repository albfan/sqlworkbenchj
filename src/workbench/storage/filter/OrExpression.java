/*
 * OrExpression.java
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

import java.util.Map;

/**
 * @author support@sql-workbench.net
 */
public class OrExpression
	extends ComplexExpression
{
	public boolean evaluate(Map<String, Object> columnValues)
	{
		for (FilterExpression expr : filter)
		{
			if (expr.evaluate(columnValues)) return true;
		}
		return false;
	}

	public boolean equals(Object other)
	{
		if (other instanceof OrExpression)
		{
			return super.equals(other);
		}
		else
		{
			return false;
		}
	}

	public String toString()
	{
		StringBuilder value = new StringBuilder();
		for (FilterExpression expr : filter)
		{
			if (value.length() > 0) value.append(" AND ");
			value.append(expr.toString());
		}
		return value.toString();
	}

}
