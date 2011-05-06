/*
 * DummyInsert.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
public class DummyUpdate
	extends DummyDML
	implements DbObject
{
	public DummyUpdate(TableIdentifier tbl)
	{
		super(tbl, true);
	}

	public DummyUpdate(TableIdentifier tbl, List<ColumnIdentifier> cols)
	{
		super(tbl, cols, true);
	}

	@Override
	public String getObjectType()
	{
		return "UPDATE";
	}

}
