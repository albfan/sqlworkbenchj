/*
 * RowDataSearcher
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.search;

import workbench.db.exporter.TextRowDataConverter;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.DataRowExpression;
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
		filterExpression = new ColumnExpression("*", comp, searchValue);
		filterExpression.setIgnoreCase(ignoreCase);
		converter = new TextRowDataConverter();
	}

	public ColumnExpression getExpression()
	{
		ColumnExpression expr = new ColumnExpression("*", filterExpression.getComparator(), filterExpression.getFilterValue());
		expr.setIgnoreCase(true);
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
