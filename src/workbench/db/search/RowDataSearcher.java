/*
 * RowDataSearcher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.search;

import workbench.db.exporter.TextRowDataConverter;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ContainsComparator;

import workbench.util.SqlUtil;

/**
 * A class to search for a string value in all columns of a RowData.
 * <br/>
 * The search is always done case-insesitive and always as a partial
 * match ("contains").
 *
 * @author Thomas Kellerer
 * @see workbench.storage.filter.ContainsComparator
 * @see workbench.storage.filter.DataRowExpression
 */
public class RowDataSearcher
{
	private ColumnExpression filterExpression;
	private TextRowDataConverter converter;

	public RowDataSearcher(String searchValue, ColumnComparator comp, boolean ignoreCase)
	{
		if (comp == null)
		{
			comp = new ContainsComparator();
		}
		filterExpression = new ColumnExpression(comp, searchValue);
		filterExpression.setIgnoreCase(ignoreCase);
		converter = new TextRowDataConverter();
	}

	public ColumnExpression getExpression()
	{
		ColumnExpression expr = new ColumnExpression(filterExpression.getComparator(), filterExpression.getFilterValue());
		expr.setIgnoreCase(filterExpression.isIgnoreCase());
		return expr;
	}

	public boolean isSearchStringContained(RowData row, ResultInfo metaData)
	{
		for (int c = 0; c < row.getColumnCount(); c++)
		{
			if (SqlUtil.isBlobType(metaData.getColumnType(c))) continue;
			String value = converter.getValueAsFormattedString(row, c);
			if (filterExpression.evaluate(value)) return true;
		}
		return false;
	}
}
