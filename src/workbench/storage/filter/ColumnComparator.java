/*
 * ColumnComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage.filter;

import java.util.Comparator;

/**
 * @author support@sql-workbench.net
 */
public interface ColumnComparator
{
	String getName();
	String getOperator();
	boolean evaluate(Object reference, Object value, boolean ignoreCase);
	boolean supportsType(Class valueClass);
	boolean supportsIgnoreCase();
	String getValueExpression(Object value);
	
}
