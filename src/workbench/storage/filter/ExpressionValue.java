/*
 * ExpressionValue.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

/**
 * @author Thomas Kellerer
 */
public interface ExpressionValue
{
	boolean isIgnoreCase();
	void setIgnoreCase(boolean flag);
	String getColumnName();
	ColumnComparator getComparator();
	Object getFilterValue();
}
