/*
 * CycleErrorException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class CycleErrorException
	extends Exception
{
	private TableIdentifier root;
	public CycleErrorException(TableIdentifier tbl)
	{
		super();
		root = tbl;
	}

	@Override
	public String getMessage()
	{
		return "A cyclic dependency was detected for root table '" + root.getTableExpression() + "'";
	}

	public TableIdentifier getRootTable()
	{
		return root;
	}
}
