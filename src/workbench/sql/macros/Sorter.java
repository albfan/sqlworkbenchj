/*
 * Sorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.macros;

import java.util.Comparator;

/**
 * A comparator to sort <tt>Sortable> objects.
 *
 * @author Thomas Kellerer
 * @see Sortable
 */
public class Sorter
	implements Comparator<Sortable>
{

	public int compare(Sortable o1, Sortable o2)
	{
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		return o1.getSortOrder() - o2.getSortOrder();
	}

}
