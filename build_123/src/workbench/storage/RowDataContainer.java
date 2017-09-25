/*
 * RowDataContainer.java
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

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public interface RowDataContainer
{
  int getRowCount();
  RowData getRow(int rowIndex);
  ResultInfo getResultInfo();
  TableIdentifier getUpdateTable();
  WbConnection getOriginalConnection();

  class Factory
  {
    public static RowDataContainer createContainer(DataStore data)
    {
      return data;
    }

    public static RowDataContainer createContainer(WbConnection conn, RowData row, ResultInfo info)
    {
      return new SingleRowDataContainer(conn, row, info);
    }

    public static RowDataContainer createContainer(DataStore data, int row)
    {
      return new SingleRowDataContainer(data.getOriginalConnection(), data.getRow(row), data.getResultInfo());
    }

    public static RowDataContainer createContainer(DataStore data, int[] selectedRows)
    {
      return new SelectionRowDataContainer(data, selectedRows);
    }
  }
}


class SingleRowDataContainer
  implements RowDataContainer
{
  private RowData row;
  private ResultInfo info;
  private WbConnection connection;

  SingleRowDataContainer(WbConnection conn, RowData row, ResultInfo info)
  {
    this.row = row;
    this.info = info;
    this.connection = conn;
  }

  @Override
  public WbConnection getOriginalConnection()
  {
    return connection;
  }

  @Override
  public int getRowCount()
  {
    return 1;
  }

  @Override
  public RowData getRow(int rowIndex)
  {
    if (rowIndex != 0) throw new ArrayIndexOutOfBoundsException(rowIndex);
    return row;
  }

  @Override
  public ResultInfo getResultInfo()
  {
    return info;
  }

  @Override
  public TableIdentifier getUpdateTable()
  {
    return info.getUpdateTable();
  }
}

class SelectionRowDataContainer
  implements RowDataContainer
{
  private DataStore data;
  private int[] selection;

  SelectionRowDataContainer(DataStore data, int[] rows)
  {
    this.data = data;
    this.selection = rows;
  }

  @Override
  public WbConnection getOriginalConnection()
  {
    return data.getOriginalConnection();
  }

  @Override
  public int getRowCount()
  {
    return selection.length;
  }

  @Override
  public RowData getRow(int rowIndex)
  {
    return data.getRow(selection[rowIndex]);
  }

  @Override
  public ResultInfo getResultInfo()
  {
    return data.getResultInfo();
  }

  @Override
  public TableIdentifier getUpdateTable()
  {
    return data.getUpdateTable();
  }
}
