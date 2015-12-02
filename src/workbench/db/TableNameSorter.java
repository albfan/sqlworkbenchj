/*
 * TableNameSorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
      return naturalCompare(t1.getRawTableName(), t2.getRawTableName(), true);
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

  // taken from http://stackoverflow.com/a/26884326/330315
  public static int naturalCompare(String a, String b, boolean ignoreCase)
  {
    if (ignoreCase)
    {
      a = a.toLowerCase();
      b = b.toLowerCase();
    }
    int aLength = a.length();
    int bLength = b.length();
    int minSize = Math.min(aLength, bLength);
    char aChar, bChar;
    boolean aNumber, bNumber;
    boolean asNumeric = false;
    int lastNumericCompare = 0;
    for (int i = 0; i < minSize; i++)
    {
      aChar = a.charAt(i);
      bChar = b.charAt(i);
      aNumber = aChar >= '0' && aChar <= '9';
      bNumber = bChar >= '0' && bChar <= '9';
      if (asNumeric)
      {
        if (aNumber && bNumber)
        {
          if (lastNumericCompare == 0)
          {
            lastNumericCompare = aChar - bChar;
          }
        }
        else if (aNumber)
        {
          return 1;
        }
        else if (bNumber)
        {
          return -1;
        }
        else if (lastNumericCompare == 0)
        {
          if (aChar != bChar)
          {
            return aChar - bChar;
          }
          asNumeric = false;
        }
        else
        {
          return lastNumericCompare;
        }
      }
      else if (aNumber && bNumber)
      {
        asNumeric = true;
        if (lastNumericCompare == 0)
        {
          lastNumericCompare = aChar - bChar;
        }
      }
      else if (aChar != bChar)
      {
        return aChar - bChar;
      }
    }

    if (asNumeric)
    {
      if (aLength > bLength && a.charAt(bLength) >= '0' && a.charAt(bLength) <= '9') // as number
      {
        return 1; // a has bigger size, thus b is smaller
      }
      else if (bLength > aLength && b.charAt(aLength) >= '0' && b.charAt(aLength) <= '9') // as number
      {
        return -1; // b has bigger size, thus a is smaller
      }
      else
      {
        return lastNumericCompare;
      }
    }
    else
    {
      return aLength - bLength;
    }
  }
}
