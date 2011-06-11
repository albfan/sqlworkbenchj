/*
 * TableNameComparator.java
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

import java.util.Comparator;
import workbench.util.StringUtil;

/**
 * A comparator to sort table names.
 *
 * Sorting is done case-insensitive and quotes are removed before comparing the tables.
 *
 * @author Thomas Kellerer
 */
public class TableNameSorter
	implements Comparator<TableIdentifier>
{
	private boolean useExpression = false;

	public TableNameSorter()
	{
	}

	public TableNameSorter(boolean sortOnExpression)
	{
		useExpression = sortOnExpression;
	}

	@Override
	public int compare(TableIdentifier t1, TableIdentifier t2)
	{
		if (useExpression)
		{
			return StringUtil.compareStrings(buildCleanExpression(t1), buildCleanExpression(t2), true);
		}
		return StringUtil.compareStrings(t1.getRawTableName(), t2.getRawTableName(), true);
	}

	private String buildCleanExpression(TableIdentifier tbl)
	{
		String catalog = tbl.getRawCatalog();
		String schema = tbl.getRawSchema();
		StringBuilder result = new StringBuilder();
		if (catalog != null)
		{
			result.append(catalog);
			result.append('.');
		}
		if (schema != null)
		{
			result.append(schema);
			result.append('.');
		}
		result.append(tbl.getRawTableName());
		return result.toString();
	}
}
