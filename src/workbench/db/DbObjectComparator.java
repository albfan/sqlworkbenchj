/*
 * DbObjectComparator.java
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

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectComparator
  implements Comparator<DbObject>
{

  @Override
  public int compare(DbObject o1, DbObject o2)
  {
    if (o1 == null && o2 == null) return 0;
    if (o1 == null) return -1;
    if (o2 == null) return 1;

    String n1 = o1.getFullyQualifiedName(null);
    String n2 = o2.getFullyQualifiedName(null);
    return compareName(n1, n2);
  }

  /**
   * Compare the names of the two objects.
   *
   * Catalog and Schema are only taken into account if both have them.
   *
   * @param one     the first object to compare
   * @param other   the second object to compare
   * @return true if all (defined) name elements of the objects are equal
   */
  public static boolean namesAreEqual(DbObject one, DbObject other)
  {
    return namesAreEqual(one, other, true);
  }

  public static boolean namesAreEqual(DbObject one, DbObject other, boolean checkType)
  {
    if (one == null || other == null) return false;

    if (checkType)
    {
      if (one.getObjectType().equalsIgnoreCase(other.getObjectType()) == false) return false;
    }

    boolean equals = compareName(one.getObjectName(), other.getObjectName()) == 0;

    if (equals && one.getSchema() != null && other.getSchema() != null)
    {
      equals = compareName(one.getSchema(), other.getSchema()) == 0;
    }
    if (equals && one.getCatalog() != null && other.getCatalog() != null)
    {
      equals = compareName(one.getCatalog(), other.getCatalog()) == 0;
    }
    return equals;
  }

  private static int compareName(String n1, String n2)
  {
    if (SqlUtil.isQuotedIdentifier(n1) || SqlUtil.isQuotedIdentifier(n2))
    {
      return n1.compareTo(n2);
    }
    return n1.compareToIgnoreCase(n2);
  }
}
