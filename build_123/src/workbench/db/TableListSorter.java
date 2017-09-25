/*
 * TableListSorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import workbench.db.postgres.PgRangeType;

import workbench.storage.RowData;
import workbench.storage.RowDataListSorter;
import workbench.storage.SortDefinition;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableListSorter
  extends RowDataListSorter
{
  private static final String TABLE_TYPE = "TABLE";
  private boolean mviewAsTable = false;

  public TableListSorter(SortDefinition sortDef)
  {
    super(sortDef);
  }

  public TableListSorter(int column, boolean ascending)
  {
    super(column, ascending);
  }

  public TableListSorter(int[] columns, boolean[] order)
  {
    super(columns, order);
  }

  public void setSortMViewAsTable(boolean flag)
  {
    this.mviewAsTable = flag;
  }

  @Override
  protected int compareColumn(int column, RowData row1, RowData row2)
  {
    if (column == DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE)
    {
      String value1 = getType(row1);
      String value2 = getType(row2);
      return value1.compareTo(value2);
    }
    return super.compareColumn(column, row1, row2);
  }

  private String getType(RowData row)
  {
    String value = (String)row.getValue(DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
    if (value == null) return StringUtil.EMPTY_STRING;
    if (mviewAsTable)
    {
      if (DbMetadata.MVIEW_NAME.equals(value)) return TABLE_TYPE;
    }

    // Always sort "RANGE TYPE" as "TYPE"
    if (PgRangeType.RANGE_TYPE_NAME.equals(value)) return "TYPE";

    return value;
  }

}
