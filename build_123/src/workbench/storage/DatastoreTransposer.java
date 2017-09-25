/*
 * DatastoreTransposer.java
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
package workbench.storage;

import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import workbench.resource.ResourceMgr;

import workbench.util.Alias;
import workbench.util.CollectionUtil;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to turn the columns of a datastore into rows.
 *
 * @author Thomas Kellerer
 */
public class DatastoreTransposer
{
  private DataStore source;
  private boolean useTableNameForResultName;
  private final Set<String> excludeColumns = CollectionUtil.caseInsensitiveSet();

  public DatastoreTransposer(DataStore sourceData)
  {
    this.source = sourceData;
  }

  public void setColumnsToExclude(Collection<String> toExclude)
  {
    excludeColumns.clear();
    for (String colname : toExclude)
    {
      if (source.getColumnIndex(colname) > -1)
      {
        excludeColumns.add(colname);
      }
    }
  }

  public void setUseTableNameForResult(boolean flag)
  {
    useTableNameForResultName = flag;
  }

  private String getSourceResultName()
  {
    if (source == null)
    {
      return "";
    }
    String resultName = source.getResultName();
    if (resultName == null && useTableNameForResultName)
    {
      String sql = source.getGeneratingSql();
      List<Alias> tables = SqlUtil.getTables(sql, false, source.getOriginalConnection());
      if (tables.size() == 1)
      {
        resultName = tables.get(0).getObjectName();
      }
    }
    return resultName;
  }

  public DataStore transposeRows(int[] rows)
  {
    return transposeWithLabel(null, null, rows);
  }

  public DataStore transposeWithLabel(String labelColumn, String addLabel, int[] rows)
  {
    int labelColumnIndex = source.getColumnIndex(labelColumn);

    int rowCount = (rows == null ? source.getRowCount() : rows.length);
    int colCount = rowCount + 1;

    String[] columns = new String[colCount];
    int[] types = new int[colCount];

    types[0] = Types.VARCHAR;
    if (labelColumnIndex > -1)
    {
      columns[0] = "";
    }
    else
    {
      columns[0] = ResourceMgr.getString("TxtColumnName");
    }

    if (addLabel == null) addLabel = "";

    for (int i = 0; i < rowCount ; i++)
    {
      int sourceRow = (rows == null ? i : rows[i]);
      if (labelColumnIndex > -1)
      {
        columns[i+1] = addLabel + StringUtil.coalesce(source.getValueAsString(sourceRow, labelColumnIndex), "");
      }
      else
      {
        columns[i+1] = ResourceMgr.getString("TxtRow") + " " + NumberStringCache.getNumberString(sourceRow + 1);
      }
      types[i+1] = Types.VARCHAR;
    }

    DataStore result = new DataStore(columns, types);

    int targetRows = source.getColumnCount() - excludeColumns.size();
    if (labelColumnIndex > -1) targetRows --;

    for (int i=0; i < targetRows; i ++)
    {
      result.addRow();
    }

    for (int sourceRow=0; sourceRow < rowCount; sourceRow ++)
    {
      int targetRow = 0;
      int targetColumn = sourceRow + 1;

      for (int sourceCol=0; sourceCol < source.getColumnCount(); sourceCol ++)
      {
        if (sourceCol == labelColumnIndex) continue;

        String name = source.getColumnName(sourceCol);
        if (excludeColumns.contains(name)) continue;

        String value = source.getValueAsString(sourceRow, sourceCol);
        result.setValue(targetRow, 0, name);
        result.setValue(targetRow, targetColumn, value);
        targetRow ++;
      }
    }
    result.setResultName(getSourceResultName());
    result.resetStatus();
    return result;
  }

}
