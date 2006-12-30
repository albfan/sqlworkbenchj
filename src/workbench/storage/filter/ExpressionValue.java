/*
 * ExpressionValue.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import java.util.Map;

/**
 * @author support@sql-workbench.net
 */
public interface ExpressionValue
{
	boolean isIgnoreCase();
	void setIgnoreCase(boolean flag);
	String getColumnName();
	ColumnComparator getComparator();
	Object getFilterValue();
}
