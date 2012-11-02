/*
 * SelectFKValueMarker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import workbench.resource.ResourceMgr;
import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectFKValueMarker
{
	private final String columnName;
	private final TableIdentifier table;

	public SelectFKValueMarker(String column, TableIdentifier baseTable)
	{
		this.columnName = column;
		this.table = baseTable;
	}

	public String getColumnName()
	{
		return columnName;
	}

	public TableIdentifier getTable()
	{
		return table;
	}

	@Override
	public String toString()
	{
		return "(" + ResourceMgr.getString("MnuTxtSelectFkValue") + ")";
	}

}
