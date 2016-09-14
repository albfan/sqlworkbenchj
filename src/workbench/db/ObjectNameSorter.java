/*
 * TableNameSorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A comparator to sort table names.
 *
 * Sorting is done case-insensitive and quotes are removed before comparing the tables.
 *
 * @author Thomas Kellerer
 */
public class ObjectNameSorter
  implements Comparator<DbObject>
{
  private boolean useExpression = false;
  private boolean useNaturalSort;

  public ObjectNameSorter()
  {
  }

  public ObjectNameSorter(boolean sortOnExpression)
  {
    useExpression = sortOnExpression;
  }

  public void setUseNaturalSort(boolean flag)
  {
    useNaturalSort = flag;
  }

  @Override
  public int compare(DbObject t1, DbObject t2)
  {
    if (useExpression)
    {
      return StringUtil.compareStrings(buildCleanExpression(t1), buildCleanExpression(t2), true);
    }
    if (useNaturalSort)
    {
      return StringUtil.naturalCompare(SqlUtil.removeObjectQuotes(t1.getObjectName()), SqlUtil.removeObjectQuotes(t2.getObjectName()), true);
    }
    return StringUtil.compareStrings(SqlUtil.removeObjectQuotes(t1.getObjectName()), SqlUtil.removeObjectQuotes(t2.getObjectName()), true);
  }

  private String buildCleanExpression(DbObject tbl)
  {
    String catalog = SqlUtil.removeObjectQuotes(tbl.getCatalog());
    String schema = SqlUtil.removeObjectQuotes(tbl.getSchema());
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
    result.append(SqlUtil.removeObjectQuotes(tbl.getObjectName()));
    return result.toString();
  }
}
