/*
 * CteDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.completion;

import java.util.List;

import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class CteDefinition
{
	private String cteName;
	private String innerSql;
	private List<ColumnIdentifier> columns;
	private int startAt;
	private int endAt;

	public CteDefinition(String name, List<ColumnIdentifier> columnList)
	{
		this.cteName = name;
		this.columns = columnList;
	}

	public String getInnerSql()
	{
		return innerSql;
	}

	public void setInnerSql(String sql)
	{
		this.innerSql = sql == null ? null : sql;
	}

	public String getName()
	{
		return cteName;
	}

	public List<ColumnIdentifier> getColumns()
	{
		return columns;
	}

	public void setStartInStatement(int startAt)
	{
		this.startAt = startAt;
	}

	public void setEndInStatement(int endAt)
	{
		this.endAt = endAt;
	}

	public int getStartInStatement()
	{
		return startAt;
	}

	public int getEndInStatement()
	{
		return endAt;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder(50);
		result.append(cteName);
		result.append('(');
		boolean first = true;
		for (ColumnIdentifier col : columns)
		{
			if (first) first = false;
			else result.append(',');
			result.append(col.getColumnName());
		}
		result.append(") [from ");
		result.append(startAt);
		result.append(" to ");
		result.append(endAt);
		result.append(']');
		return result.toString();
	}
}
