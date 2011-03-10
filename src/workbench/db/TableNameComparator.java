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
 *
 * @author Thomas Kellerer
 */
public class TableNameComparator
	implements Comparator<TableIdentifier>
{
	private boolean useExpression = false;

	public TableNameComparator()
	{
	}

	public TableNameComparator(boolean sortOnExpression)
	{
		useExpression = sortOnExpression;
	}

	@Override
	public int compare(TableIdentifier t1, TableIdentifier t2)
	{
		if (useExpression)
		{
			return StringUtil.compareStrings(t1.getTableExpression(), t2.getTableExpression(), true);
		}
		return StringUtil.compareStrings(t1.getTableName(), t2.getTableName(), true);
	}

}
