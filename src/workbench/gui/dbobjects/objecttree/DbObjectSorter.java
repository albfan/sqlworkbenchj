/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dbobjects.objecttree;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import workbench.db.DbObject;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectSorter
  implements Comparator<DbObject>
{
  private boolean includeType;
  private boolean useNaturalSort;

  public DbObjectSorter(boolean naturalSort)
  {
    useNaturalSort = naturalSort;
  }

  public void setIncludeType(boolean flag)
  {
    this.includeType = flag;
  }

  public void setUseNaturalSort(boolean flag)
  {
    this.useNaturalSort = flag;
  }

  @Override
  public int compare(DbObject o1, DbObject o2)
  {
    if (o1 == null && o2 != null) return 1;
    if (o1 != null && o2 == null) return -1;
    if (o1 == null && o2 == null) return 0;

    if (includeType)
    {
      String t1 = o1.getObjectType();
      String t2 = o2.getObjectType();
      int compare = StringUtil.compareStrings(t1, t2, true);
      if (compare != 0)
      {
        return compare;
      }
    }
    // types are the same, so sort by name (and only by name
    String name1 = SqlUtil.removeObjectQuotes(o1.getObjectName());
    String name2 = SqlUtil.removeObjectQuotes(o2.getObjectName());
    if (useNaturalSort)
    {
      return StringUtil.naturalCompare(name1, name2, true);
    }
    return StringUtil.compareStrings(name1, name2, true);
  }

  public static void sort(List<? extends DbObject> objects, boolean useNaturalSort)
  {
    sort(objects, useNaturalSort, false);
  }

  public static void sort(List<? extends DbObject> objects, boolean useNaturalSort, boolean includeType)
  {
    if (objects == null) return;
    DbObjectSorter sorter = new DbObjectSorter(useNaturalSort);
    sorter.setIncludeType(includeType);
    Collections.sort(objects, sorter);
  }
}
