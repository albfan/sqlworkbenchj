/*
 * PkDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.objectcache.DbObjectCacheFactory;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PkDefinition
	implements Serializable
{
	private static final long serialVersionUID = DbObjectCacheFactory.CACHE_VERSION_UID;

	private List<IndexColumn> columns = new ArrayList<>();
	private String pkName;
	private String pkIndexName;

  private boolean enabled = true;

  private IndexDefinition pkIndex;

	public PkDefinition (List<String> pkCols)
	{
		for (int i=0; i < pkCols.size(); i++)
		{
			IndexColumn ix = new IndexColumn(pkCols.get(i), i);
			columns.add(ix);
		}
	}

	public PkDefinition(String name, List<IndexColumn> columns)
	{
		this.pkName = StringUtil.trim(name);
		if (columns != null)
		{
			this.columns = new ArrayList<>(columns);
		}
	}

  public IndexDefinition getPkIndexDefinition()
  {
    return pkIndex;
  }

  public void setPkIndexDefinition(IndexDefinition index)
  {
    this.pkIndex = index;
  }

  public void setEnabled(boolean flag)
  {
    enabled = flag;
  }

  public boolean isDisabled()
  {
    return enabled == false;
  }
  
  public boolean isEnabled()
  {
    return enabled;
  }

	public String getPkIndexName()
	{
    if (pkIndex != null) return pkIndex.getName();
		if (pkIndexName == null) return pkName;
		return pkIndexName;
	}

	public void setPkIndexName(String name)
	{
		this.pkIndexName = StringUtil.trim(name);
	}

	public void setPkName(String name)
	{
		this.pkName = name;
	}

	public String getPkName()
	{
		return pkName;
	}

	public List<String> getColumns()
	{
		if (columns == null) return Collections.emptyList();

		if (columns.size() > 1)
		{
			Collections.sort(this.columns, IndexColumn.getSequenceSorter());
		}
		List<String> result = new ArrayList<>(columns.size());
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
		if (pkName == null) return columns.toString();
		return pkName + ": " + columns;
	}
}
