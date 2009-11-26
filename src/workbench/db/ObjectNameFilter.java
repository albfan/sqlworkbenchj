/*
 * ObjectNameFilter
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.Set;
import java.util.regex.Pattern;
import workbench.interfaces.PropertyStorage;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectNameFilter
{
	private Set<String> filterExpressions;
	private boolean modified;

	public ObjectNameFilter()
	{
	}

	public void setFilterExpressions(Set<String> expressions)
	{
		filterExpressions = CollectionUtil.caseInsensitiveSet();
		filterExpressions.addAll(expressions);
		modified = false;
	}

	public Set<String> getFilterExpressions()
	{
		return filterExpressions;
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
	
	public void addExpression(String exp)
	{
		if (StringUtil.isBlank(exp)) return;
		
		if (filterExpressions == null)
		{
			filterExpressions = CollectionUtil.caseInsensitiveSet();
		}
		if (filterExpressions.contains(exp)) return;
		filterExpressions.add(exp);
		modified = true;
	}

	public boolean isModified()
	{
		return modified;
	}
	
	public boolean isExcluded(String name)
	{
		if (CollectionUtil.isEmpty(filterExpressions)) return false;
		for (String expr : filterExpressions)
		{
			Pattern p = Pattern.compile(expr, Pattern.CASE_INSENSITIVE);
			return p.matcher(name).matches();
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
			copy.filterExpressions = CollectionUtil.caseInsensitiveSet();
			copy.filterExpressions.addAll(this.filterExpressions);
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

		for (String s : filterExpressions)
		{
			if (!other.filterExpressions.contains(s)) return false;
		}

		for (String s : other.filterExpressions)
		{
			if (!filterExpressions.contains(s)) return false;
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
