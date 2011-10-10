/*
 * DummyInsert.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.List;

/**
 * @author Thomas Kellerer
 */
public class DummyInsert
	extends DummyDML
	implements DbObject
{
	public DummyInsert(TableIdentifier tbl)
	{
		super(tbl, false);
	}

	public DummyInsert(TableIdentifier tbl, List<ColumnIdentifier> cols)
	{
		super(tbl, cols, false);
	}

	@Override
	public String getObjectType()
	{
		return "INSERT";
	}

}
