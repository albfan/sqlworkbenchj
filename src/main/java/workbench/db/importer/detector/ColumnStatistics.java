/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.importer.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnStatistics
{
  private final String name;
  private final SortedMap<ColType, Integer> typeCounts;
  private int maxLength = -1;
  private int maxDigits = -1;

  public ColumnStatistics(String colName)
  {
    name = colName;
    typeCounts = new TreeMap<>();
  }

  public void addValidType(ColType type, int length, int digits)
  {
    int count = getValidCount(type);
    count ++;
    typeCounts.put(type, count);
    if (length > maxLength)
    {
      maxLength = length;
    }
    if (digits > maxDigits)
    {
      maxDigits = digits;
    }
  }

  public ColType getBestType()
  {
    if (typeCounts.isEmpty())
    {
      return ColType.String;
    }

    if (typeCounts.size() == 1)
    {
      return typeCounts.firstKey();
    }
    // The sort order of the enums defines a "top-bottom" order
    // so Decimal is "better" than "Integer", because Decimal
    ColType type = getMostFrequentType();
    ColType firstType = typeCounts.firstKey();

    // if the first type is also the most frequent one
    // then this is OK (e.g.
    if (type == firstType)
    {
      return type;
    }

    if (sameBaseType(type, firstType))
    {
      return firstType;
    }
    return ColType.String;
  }

  public List<ColType> getDetectedTypes()
  {
    return new ArrayList<>(typeCounts.keySet());
  }

  public boolean sameBaseType(ColType one, ColType other)
  {
    return
      (one == ColType.Decimal && other == ColType.Integer) ||
      (one == ColType.Integer && other == ColType.Decimal) ||
      (one == ColType.Date && other == ColType.Timestamp) ||
      (one == ColType.Timestamp && other == ColType.Date);
  }

  public int getValidCount(ColType type)
  {
    Integer count = typeCounts.get(type);
    if (count == null) return 0;
    return count.intValue();
  }

  public ColType getMostFrequentType()
  {
    int maxCount = 0;
    ColType maxType = null;
    for (ColType type : typeCounts.keySet())
    {
      int count = getValidCount(type);
      if (count > maxCount)
      {
        maxCount = count;
        maxType = type;
      }
    }
    return maxType;
  }

  public int getMaxLength()
  {
    return maxLength;
  }

  public int getMaxDigits()
  {
    return maxDigits;
  }

  public String getName()
  {
    return name;
  }

  @Override
  public String toString()
  {
    return name;
  }

}
