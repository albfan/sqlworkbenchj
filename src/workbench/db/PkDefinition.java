/*
 * PkDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public class PkDefinition
{

	private List<IndexColumn> columns = new ArrayList<IndexColumn>();
	private String pkName;
	private TableIdentifier table;

	public PkDefinition(TableIdentifier baseTable, String name, List<IndexColumn> columns)
	{
		this.table = baseTable;
		this.pkName = name;
		if (columns != null)
		{
			this.columns = new ArrayList<IndexColumn>(columns);
		}
	}

	public TableIdentifier getTable()
	{
		return table;
	}

	public String getPkName()
	{
		return pkName;
	}

	public List<String> getColumns()
	{
		Collections.sort(this.columns, IndexColumn.getSequenceSorter());
		List<String> result = new ArrayList<String>(columns.size());
		for (IndexColumn col : columns)
		{
			result.add(col.getColumn());
		}
		return result;
	}

	public void addColumn(IndexColumn col)
	{
		this.columns.add(col);
	}

}
