/*
 * Sortable.java
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

/**
 * An interface for items that can have a sort order.
 * 
 * @author Thomas Kellerer
 */
public interface Sortable
{
	int getSortOrder();
	void setSortOrder(int newIndex);
}
