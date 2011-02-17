/*
 * TableConstraint.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.util.StringUtil;

/**
 * Represents a single table (check) constraint
 *
 * @author Thomas Kellerer
 */
public class TableConstraint
	implements Comparable<TableConstraint>
{
	private String name;
	private String expression;
	private boolean isSystemName;
	private String comment;

	public TableConstraint(String cName, String expr)
	{
		name = cName;
		expression = StringUtil.isNonBlank(expr) ? expr.trim() : null;
	}

	public void setIsSystemName(boolean flag)
	{
		this.isSystemName = flag;
	}

	public boolean isSystemName()
	{
		return this.isSystemName;
	}

	public String getConstraintName()
	{
		return this.name;
	}

	public void setComment(String remark)
	{
		comment = remark;
	}

	public String getComment()
	{
		return comment;
	}

	public String getExpression()
	{
		return expression;
	}

	public int compareTo(TableConstraint other)
	{
		if (other == null) return -1;
		if (isSystemName && other.isSystemName)
		{
			return StringUtil.compareStrings(this.expression, other.expression, false);
		}
		int c = StringUtil.compareStrings(this.name, other.name, true);
		if (c == 0)
		{
			c = StringUtil.compareStrings(this.expression, other.expression, false);
		}
		return c;
	}

	@Override
	public int hashCode()
	{
		int hash = 5;
		hash = 19 * hash + (this.expression != null ? this.expression.hashCode() : 0);
		return hash;
	}

	public boolean expressionIsEqual(TableConstraint other)
	{
		if (other == null) return false;
		return StringUtil.equalString(expression, other.expression);
	}

	public boolean equals(Object other)
	{
		if (other instanceof TableConstraint)
		{
			return compareTo((TableConstraint)other) == 0;
		}
		return false;
	}

	/**
	 * Returns the type of this table constraint.
	 *
	 * @return "check"
	 */
	public String getType()
	{
		if (expression.toLowerCase().startsWith("exclude"))
		{
			return "exclusion";
		}
		return "check";
	}

	public String toString()
	{
		return getSql();
	}

	public String getSql()
	{
		StringBuilder result = new StringBuilder(50);

		if (StringUtil.isNonBlank(name) && !isSystemName())
		{
			result.append("CONSTRAINT ");
			result.append(name);
			result.append(' ');
		}

		// Check if the returned expression already includes the CHECK keyword
		// PostgreSQL 9.0 supports "exclusion constraint which start with EXCLUDE
		// in that case the keyword CHECK may not be added as well.
		if (!expression.toLowerCase().startsWith("check") && !expression.toLowerCase().startsWith("exclude"))
		{
			result.append("CHECK ");
		}
		result.append(expression);
		return result.toString();
	}
}
