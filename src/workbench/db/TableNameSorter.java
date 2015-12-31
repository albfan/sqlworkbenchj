/*
 * TableNameSorter.java
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

import java.util.Comparator;

import workbench.util.StringUtil;

/**
 * A comparator to sort table names.
 *
 * Sorting is done case-insensitive and quotes are removed before comparing the tables.
 *
 * @author Thomas Kellerer
 */
public class TableNameSorter
	implements Comparator<TableIdentifier>
{
	private boolean useExpression = false;
  private boolean useNaturalSort;

	public TableNameSorter()
	{
	}

	public TableNameSorter(boolean sortOnExpression)
	{
		useExpression = sortOnExpression;
	}

  public void setUseNaturalSort(boolean flag)
  {
    useNaturalSort = flag;
  }

	@Override
	public int compare(TableIdentifier t1, TableIdentifier t2)
	{
		if (useExpression)
		{
			return StringUtil.compareStrings(buildCleanExpression(t1), buildCleanExpression(t2), true);
		}
    if (useNaturalSort)
    {
      return StringUtil.naturalCompare(t1.getRawTableName(), t2.getRawTableName(), true);
    }
		return StringUtil.compareStrings(t1.getRawTableName(), t2.getRawTableName(), true);
	}

	private String buildCleanExpression(TableIdentifier tbl)
	{
		String catalog = tbl.getRawCatalog();
		String schema = tbl.getRawSchema();
		StringBuilder result = new StringBuilder();
		if (catalog != null)
		{
			result.append(catalog);
			result.append('.');
		}
		if (schema != null)
		{
			result.append(schema);
			result.append('.');
		}
		result.append(tbl.getRawTableName());
		return result.toString();
	}
}
