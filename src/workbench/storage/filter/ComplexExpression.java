/*
 * ComplexExpression.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Kellerer
 */
public abstract class ComplexExpression
	implements FilterExpression
{
	protected List<FilterExpression> filter = new LinkedList<FilterExpression>();

	public void addExpression(FilterExpression expr)
	{
		filter.add(expr);
	}

	public void addColumnExpression(String colname, ColumnComparator comp, Object refValue)
	{
		addColumnExpression(colname, comp, refValue, comp.supportsIgnoreCase());
	}

	public void addColumnExpression(String colname, ColumnComparator comp, Object refValue, boolean ignoreCase)
	{
		ColumnExpression def = new ColumnExpression(colname, comp, refValue);
		if (comp.supportsIgnoreCase()) def.setIgnoreCase(ignoreCase);
		addExpression(def);
	}

	public boolean hasFilter()
	{
		if (this.filter == null) return false;
		return (this.filter.size() > 0);
	}

	public void removeExpression(FilterExpression expr)
	{
		filter.remove(expr);
	}

	/**
	 * Get the list of FilterExpression s that define this ComplexExpression
	 */
	public List getExpressions() { return filter; }

	public void setExpressions(List<FilterExpression> l) { this.filter = l;}

	@Override
	public boolean equals(Object other)
	{
		try
		{
			ComplexExpression o = (ComplexExpression)other;
			return this.filter.equals(o.filter);
		}
		catch (Throwable e)
		{
			return false;
		}
	}
	
	@Override
	public boolean isColumnSpecific()
	{
		for (FilterExpression expr : filter)
		{
			if (expr.isColumnSpecific()) return true;
		}
		return false;
	}

	@Override
	public abstract boolean evaluate(Map<String, Object> columnValues);
}
