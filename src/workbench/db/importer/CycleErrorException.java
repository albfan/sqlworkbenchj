/*
 * CycleErrorException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.db.TableIdentifier;

/**
 *
 * @author support@sql-workbench.net
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

	public String getMessage()
	{
		return "A cyclic dependency was detected for root table '" + root.getTableExpression() + "'";
	}
	
	public TableIdentifier getRootTable()
	{
		return root;
	}
}
