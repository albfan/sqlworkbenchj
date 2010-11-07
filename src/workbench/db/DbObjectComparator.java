/*
 * DbObjectComparator
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.Comparator;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectComparator
	implements Comparator<DbObject>
{

	@Override
	public int compare(DbObject o1, DbObject o2)
	{
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;

		String n1 = o1.getFullyQualifiedName(null);
		String n2 = o2.getFullyQualifiedName(null);
		if (n1.startsWith("\"") || n2.startsWith("\""))
		{
			return n1.compareTo(n2);
		}
		return n1.compareToIgnoreCase(n2);
	}

}
