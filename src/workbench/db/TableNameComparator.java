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
	public int compare(TableIdentifier t1, TableIdentifier t2)
	{
		return StringUtil.compareStrings(t1.getTableName(), t2.getTableName(), true);
	}

}
