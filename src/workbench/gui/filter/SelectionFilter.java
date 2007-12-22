/*
 * SelectionFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;

import workbench.gui.components.WbTable;
import workbench.storage.filter.AndExpression;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ComparatorFactory;
import workbench.storage.filter.ComplexExpression;
import workbench.storage.filter.DateEqualsComparator;
import workbench.storage.filter.IsNullComparator;
import workbench.storage.filter.NumberEqualsComparator;
import workbench.storage.filter.OrExpression;
import workbench.storage.filter.StringEqualsComparator;
import workbench.util.SqlUtil;

/**
 * @author support@sql-workbench.net
 */
public class SelectionFilter
{
	private WbTable client;

	public SelectionFilter(WbTable tbl)
	{
		this.client = tbl;
	}

	public void applyFilter()
	{
		if (client == null) return;
		int rowCount = client.getSelectedRowCount();
		int colCount = client.getSelectedColumnCount();

		if (rowCount < 1 || (rowCount > 1 && colCount != 1)) return;
		int[] columns = null;
		// if whole rows are selected, use the currently
		// focused column for the filter
		if (colCount == client.getColumnCount())
		{
			columns = client.getSelectedColumns();
		}
		else
		{
			columns = new int[]{client.getSelectedColumn()};
		}
		if (columns == null || columns.length == 0) return;
		ComplexExpression expr = null;
		if (rowCount == 1)
		{
			expr = new AndExpression();
		}
		else
		{
			expr = new OrExpression();
		}

		int[] rows = client.getSelectedRows();
		for (int ri = 0; ri < rows.length; ri++)
		{
			int row = rows[ri];

			for (int i = 0; i < columns.length; i++)
			{
				String name = client.getColumnName(columns[i]);
				Object value = client.getValueAt(row, columns[i]);
				int type = client.getDataStore().getColumnType(columns[i]);
				ColumnComparator comparator = null;

				if (value == null)
				{
					comparator = new IsNullComparator();
				}
				else if (SqlUtil.isCharacterType(type))
				{
					comparator = new StringEqualsComparator();
				}
				else if (SqlUtil.isNumberType(type) && value instanceof Number)
				{
					comparator = new NumberEqualsComparator();
				}
				else if (SqlUtil.isDateType(type) && value instanceof java.util.Date)
				{
					comparator = new DateEqualsComparator(type);
				}
				else
				{
					ComparatorFactory factory = new ComparatorFactory();
					comparator = factory.findEqualityComparatorFor(value.getClass());
				}

				if (comparator != null)
				{
					expr.addColumnExpression(name, comparator, value);
				}
			}
		}
		if (expr.hasFilter()) client.applyFilter(expr);
	}
}
