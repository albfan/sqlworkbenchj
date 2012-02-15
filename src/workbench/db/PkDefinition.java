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
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PkDefinition
{
	private List<IndexColumn> columns = new ArrayList<IndexColumn>();
	private String pkName;
	private String pkIndexName;

	public PkDefinition(String name, List<IndexColumn> columns)
	{
		this.pkName = StringUtil.trim(name);
		if (columns != null)
		{
			this.columns = new ArrayList<IndexColumn>(columns);
		}
	}

	public String getPkIndexName()
	{
		if (pkIndexName == null) return pkName;
		return pkIndexName;
	}

	public void setPkIndexName(String name)
	{
		this.pkIndexName = StringUtil.trim(name);
	}

	public String getPkName()
	{
		return pkName;
	}

	public List<String> getColumns()
	{
		if (columns.size() > 1)
		{
			Collections.sort(this.columns, IndexColumn.getSequenceSorter());
		}
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

	public PkDefinition createCopy()
	{
		PkDefinition copy = new PkDefinition(this.pkName, this.columns);
		copy.pkIndexName = this.pkIndexName;
		return copy;
	}

	@Override
	public String toString()
	{
		return pkName + " " + columns;
	}
}
