/*
 * ContainsNotComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

/**
 * @author support@sql-workbench.net
 */
public class ContainsNotComparator
	extends ContainsComparator
{
	public String getName() { return "Contains not"; }
	public String getOperator() { return "contains not"; }
	
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		return !super.evaluate(reference, value, ignoreCase);
	}
	
	public boolean equals(Object other)
	{
		return (other instanceof ContainsNotComparator);
	}	
}

