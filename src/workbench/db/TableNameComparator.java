/*
 * TableNameComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Comparator;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableNameComparator
	implements Comparator
{
	private Comparator stringComparator = StringUtil.getCaseInsensitiveComparator();
	
	public TableNameComparator()
	{
	}

	public int compare(Object o1, Object o2)
	{
		TableIdentifier t1 = (TableIdentifier)o1;
		TableIdentifier t2 = (TableIdentifier)o2;
		return stringComparator.compare(t1.getTableName(), t2.getTableName());
	}
	
}
