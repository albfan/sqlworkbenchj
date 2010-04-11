/*
 * ObjectNameFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import workbench.log.LogMgr;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectNameFilter
{
	private Set<Pattern> filterExpressions;
	private boolean modified;

	public ObjectNameFilter()
	{
	}

	/**
	 * Define the expressions to be used.
	 * <br/>
	 * This will replace any existing filter definitions and reset the modified flag
	 * Empy expressions (null, "") in the collection will be ignored.
	 * <br/>
	 * If the list is empty the current filter definitions are not changed
	 *
	 * @param expressions
	 * @see #setExpressionList(java.lang.String)
	 */
	public void setFilterExpressions(Collection<String> expressions)
	{
		if (CollectionUtil.isEmpty(expressions)) return;

		filterExpressions = new HashSet<Pattern>(expressions.size());
		for (String exp : expressions)
		{
			if (StringUtil.isNonBlank(exp))
			{
				addExpression(exp);
			}
		}
		modified = false;
	}

	/**
	 * Returns the defined expression values.
	 * <br/>
	 * The values will be sorted alphabetically
	 */
	public Collection<String> getFilterExpressions()
	{
		if (CollectionUtil.isEmpty(filterExpressions)) return null;
		Set<String> result = CollectionUtil.caseInsensitiveSet();
		for (Pattern p : filterExpressions)
		{
			result.add(p.pattern());
		}
		return result;
	}

	public void resetModified()
	{
		modified = false;
	}

	public void clear()
	{
		if (CollectionUtil.isNonEmpty(filterExpressions))
		{
			filterExpressions.clear();
			modified = true;
		}
	}

	/**
	 * Defines a list of expressions for this filter.
	 * <br/>
	 * The expressions can be separated by a semicolon, optionally enclosed with double quotes
	 * This will replace any existing filter definitions.
	 * <br/>
	 * If the list is empty the current filter definitions are not changed
	 *
	 * @param list a semicolon separated list of expressions
	 * @see #setFilterExpressions(java.util.Collection)
	 */
	public void setExpressionList(String list)
	{
		List<String> items = StringUtil.stringToList(list, ";", true, true);
		setFilterExpressions(items);
	}

	public void addExpression(String exp)
	{
		if (StringUtil.isBlank(exp)) return;

		if (filterExpressions == null)
		{
			filterExpressions = new HashSet<Pattern>();
		}

		try
		{
			filterExpressions.add(Pattern.compile(exp.trim(), Pattern.CASE_INSENSITIVE));
		}
		catch (PatternSyntaxException p)
		{
			LogMgr.logError("ObjectNameFilter.addExpression()", "Could not compile expression: " + exp , p);
		}
		modified = true;
	}

	public boolean isModified()
	{
		return modified;
	}

	public boolean isExcluded(String name)
	{
		if (name == null) return true;
		if (CollectionUtil.isEmpty(filterExpressions)) return false;

		for (Pattern p : filterExpressions)
		{
			if (p.matcher(name).matches()) return true;
		}
		return false;
	}

	public int getSize()
	{
		return (filterExpressions == null ? 0 : filterExpressions.size());
	}

	public ObjectNameFilter createCopy()
	{
		ObjectNameFilter copy = new ObjectNameFilter();
		copy.modified = this.modified;
		if (this.filterExpressions != null)
		{
			copy.filterExpressions = new HashSet<Pattern>(this.filterExpressions);
		}
		return copy;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		ObjectNameFilter other = (ObjectNameFilter) obj;
		if (this.filterExpressions == null && other.filterExpressions != null) return false;
		if (this.filterExpressions != null && other.filterExpressions == null) return false;

		Collection<String> myPatterns = getFilterExpressions();
		Collection<String> otherPatterns = other.getFilterExpressions();
		for (String s : myPatterns)
		{
			if (!otherPatterns.contains(s)) return false;
		}

		for (String s : otherPatterns)
		{
			if (!myPatterns.contains(s)) return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 17 * hash + (this.filterExpressions != null ? this.filterExpressions.hashCode() : 0);
		return hash;
	}

}
