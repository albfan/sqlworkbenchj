/*
 * ContainsNotComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import workbench.resource.ResourceMgr;

/**
 * @author Thomas Kellerer
 */
public class ContainsNotComparator
	extends ContainsComparator
{
	@Override
	public String getDescription()
	{
		return getOperator();
	}

	@Override
	public String getOperator()
	{
		return ResourceMgr.getString("TxtOpContainsNot");
	}

	@Override
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		return !super.evaluate(reference, value, ignoreCase);
	}

	@Override
	public boolean equals(Object other)
	{
		return (other instanceof ContainsNotComparator);
	}
}
