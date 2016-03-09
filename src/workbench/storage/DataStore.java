/*
 * DataStore.java
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
package workbench.storage;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionProfile;
import workbench.db.DeleteScriptGenerator;
import workbench.db.QuoteHandler;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;

import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.FilterExpression;

import workbench.sql.ResultNameAnnotation;

import workbench.util.Alias;
import workbench.util.CollectionUtil;
import workbench.util.ConverterException;
import workbench.util.ExceptionUtil;
import workbench.util.LowMemoryException;
import workbench.util.MemoryWatcher;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;

/**
 * A class to cache the result of a database query.
 * If the underlying SELECT used only one table, then the
 * DataStore can be updated and the changes can be saved back
 * to the database.
 *
 * For updating or deleting rows, key columns are required
 * For inserting new rows, no keys are required.
 *
 * @see workbench.storage.ResultInfo
 *
 * @author Thomas Kellerer
 */
public class DataStore
  implements RowDataContainer
{
  private RowActionMonitor rowActionMonitor;

  private boolean modified;

  private RowDataList data;
  private RowDataList deletedRows;
  private RowDataList filteredRows;

  // The SQL statement that was used to generate this DataStore
  private String sql;

  // A ColumnExpression that was used while populating this datastore
  private ColumnExpression generatingFilter;

  private ResultInfo resultInfo;
  private TableIdentifier updateTable;
  private TableIdentifier updateTableToBeUsed;

  private WbConnection originalConnection;

  private boolean allowUpdates;
  private boolean updateHadErrors;

  private boolean cancelRetrieve;
  private boolean cancelUpdate;

  private List<ColumnIdentifier> missingPkcolumns;

  private int currentUpdateRow;
  private int currentInsertRow;
  private int currentDeleteRow;

  private boolean trimCharData;
  private boolean hasGeneratedKeys;
  private long loadedAt;

  protected boolean useNaturalSort;
  private SortDefinition lastSort;

  /**
   *	Create a DataStore which is not based on a result set
   *	and contains the columns defined in the given array
   *	The column types need to match the values from from java.sql.Types
   *  @param colNames the column names
   *  @param colTypes data types for each column (matching java.sql.Types.XXXX)
   */
  public DataStore(String[] colNames, int[] colTypes)
  {
    this(colNames, colTypes, null);
  }

  /**
   *	Create a DataStore which is not based on a result set
   *	and contains the columns defined in the given array
   *	The column types need to match the values from from java.sql.Types
   *  @param colNames the column names
   *  @param colTypes data types for each column (matching java.sql.Types.XXXX)
   *  @param colSizes display size for each column
   *
   * @see #getColumnDisplaySize(int)
   * @see workbench.gui.components.DataStoreTableModel#getColumnWidth(int)
   */
  public DataStore(String[] colNames, int[] colTypes, int[] colSizes)
  {
    this.data = createData();
    this.resultInfo = new ResultInfo(colNames, colTypes, colSizes);
    checkForGeneratedKeys();
    setLoadTimeToNow();
  }

  /**
   * Create a DataStore based on the contents of the given ResultSet.
   *
   * The ResultSet has to be closed by the caller.
   *
   * @see #initData(ResultSet, int)
   */
  public DataStore(ResultSet result, WbConnection connection)
    throws SQLException
  {
    if (result == null) return;
    setOriginalConnection(connection);
    this.initData(result);
  }

  public DataStore(ResultInfo metaData)
  {
    this.resultInfo = metaData;
    this.data = createData();
    checkForGeneratedKeys();
  }

  private void checkForGeneratedKeys()
  {
    this.hasGeneratedKeys = false;
    if (this.resultInfo == null) return;
    for (int i=0; i < resultInfo.getColumnCount(); i++)
    {
      if (resultInfo.getColumn(i).isAutoGenerated())
      {
        this.hasGeneratedKeys = true;
        break;
      }
    }
  }

  /**
   * Initialize this DataStore based on the given ResultSet but without reading the data.
   * This is equivalent to calling new DataStore(result, false)
   *
   * The ResultSet has to be closed by the caller.
   *
   * @param result the ResultSet to process
   * @throws java.sql.SQLException
   *
   * @see #DataStore(ResultSet, boolean)
   */
  public DataStore(ResultSet result)
    throws SQLException
  {
    this(result, false);
  }

  /**
   * Initialize this DataStore based on the given ResultSet.
   *
   * The ResultSet has to be closed by the caller.
   *
   * @param aResult the ResultSet to process
   * @param readData if true, the ResultSet will be processed, otherwise only the MetaData will be read
   * @throws java.sql.SQLException
   *
   * @see #initData(ResultSet, int)
   */
  public DataStore(ResultSet aResult, boolean readData)
    throws SQLException
  {
    this(aResult, readData, null, -1, null);
  }

  public DataStore(ResultSet aResult, WbConnection conn, boolean readData)
    throws SQLException
  {
    this(aResult, readData, null, -1, conn);
  }

  /**
   *	Create a DataStore based on the given ResultSet.
   *	@param aResult the result set to use
   *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
   *  @param maxRows limit number of rows to maxRows if the JDBC driver does not already limit them
   */
  public DataStore(ResultSet aResult, boolean readData, int maxRows)
    throws SQLException
  {
    this(aResult, readData, null, maxRows, null);
  }

  /**
   *	Create a DataStore based on the given ResultSet.
   *	@param aResult the result set to use
   *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
   *  @param aMonitor if not null, the loading process is displayed through this monitor
   */
  public DataStore(ResultSet aResult, boolean readData, RowActionMonitor aMonitor)
    throws SQLException
  {
    this(aResult, readData, aMonitor, -1, null);
  }


  /**
   *	Create a DataStore based on the given ResultSet.
   *	@param aResult the result set to use
   *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
   *  @param aMonitor if not null, the loading process is displayed through this monitor
   *  @param maxRows limit number of rows to maxRows if the JDBC driver does not already limit them
   *  @param conn the connection that was used to retrieve the result set
   */
  public DataStore(ResultSet aResult, boolean readData, RowActionMonitor aMonitor, int maxRows, WbConnection conn)
    throws SQLException
  {
    this.rowActionMonitor = aMonitor;
    setOriginalConnection(conn);
    if (readData)
    {
      this.initData(aResult, maxRows);
      this.cancelRetrieve = false;
    }
    else
    {
      ResultSetMetaData metaData = aResult.getMetaData();
      this.initMetaData(metaData);
      this.data = createData();
    }
  }

  /**
   * Create an empty DataStore based on the information given in the MetaData
   * object.
   * <br/>
   * The DataStore can be populated with the {@link #initData(java.sql.ResultSet) }  method.
   */
  public DataStore(ResultSetMetaData metaData, WbConnection aConn)
    throws SQLException
  {
    setOriginalConnection(aConn);
    this.initMetaData(metaData);
    this.data = createData();
  }

  public void setTrimCharData(boolean flag)
  {
    this.trimCharData = flag;
  }

  /**
   * Return the connection that was used to retrieve the result.
   * Can be null if the DataStore was not populated using a ResultSet
   */
  @Override
  public WbConnection getOriginalConnection()
  {
    return this.originalConnection;
  }

  public void setOriginalConnection(WbConnection aConn)
  {
    this.originalConnection = aConn;
    if (this.originalConnection != null)
    {
      ConnectionProfile prof = this.originalConnection.getProfile();
      if (prof != null)
      {
        trimCharData = prof.getTrimCharData();
      }
    }
  }

  public void setUseNaturalSort(boolean flag)
  {
    useNaturalSort = flag;
  }

  public void setColumnSizes(int[] sizes)
  {
    if (this.resultInfo == null) return;
    this.resultInfo.setColumnSizes(sizes);
  }

  public void setAllowUpdates(boolean aFlag)
  {
    this.allowUpdates = aFlag;
  }

  private RowDataList createData(int size)
  {
    return new RowDataList(size);
  }

  private RowDataList createData()
  {
    return new RowDataList();
  }

  private String resultName;

  /**
   * Returns a descriptive name for this result DataStore
   *
   * @return the value of resultName
   */
  public String getResultName()
  {
    return resultName;
  }

  /**
   * Set a descriptive name for this result
   *
   * This is used in the GUI when displaying result tabs.
   *
   * @param name new value of resultName
   */
  public void setResultName(String name)
  {
    this.resultName = name;
  }

  public void copyFrom(DataStore source)
  {
    if (source == null) return;
    if (source.getRowCount() == 0) return;
    if (source.getColumnCount() != this.getColumnCount()) return;

    int rows = source.getRowCount();
    for (int i=0; i < rows; i++)
    {
      RowData row = source.getRow(i);
      this.addRow(row);
    }
  }

  public DataStore createCopy(boolean includeRows)
  {
    DataStore result = new DataStore(this.resultInfo.createCopy());
    if (includeRows)
    {
      result.copyFrom(this);
    }
    return result;
  }

  public int duplicateRow(int aRow)
  {
    if (aRow < 0 || aRow >= this.getRowCount()) return -1;
    RowData oldRow = this.getRow(aRow);
    RowData newRow = oldRow.createCopy();
    int newIndex = aRow + 1;
    if (newIndex >= this.getRowCount()) newIndex = this.getRowCount();
    this.data.add(newIndex, newRow);
    this.modified = true;
    return newIndex;
  }

  public int getFilteredCount()
  {
    if (filteredRows == null) return 0;
    return filteredRows.size();
  }

  @Override
  public int getRowCount()
  {
    return this.data.size();
  }

  public int getColumnCount()
  {
    return this.resultInfo.getColumnCount();
  }

  /**
   *	Returns the total number of modified, new or deleted rows
   */
  public int getModifiedCount()
  {
    if (!this.isModified()) return 0;
    int count = this.getRowCount();
    int modifiedCount = 0;
    for (int i=0; i < count; i++)
    {
      if (this.isRowModified(i)) modifiedCount++;
    }
    if (this.deletedRows != null)
    {
      count = this.deletedRows.size();
      for (int i=0; i < count; i++)
      {
        RowData rowData = this.deletedRows.get(i);
        if (!rowData.isNew()) modifiedCount++;
      }
    }
    return modifiedCount;
  }

  public int getColumnType(int column)
    throws IndexOutOfBoundsException
  {
    return this.resultInfo.getColumnType(column);
  }

  public String getColumnClassName(int column)
  {
    DataConverter conv = RowDataReader.getConverterInstance(originalConnection);
    if (conv != null)
    {
      int type = resultInfo.getColumnType(column);
      String dbmsType = resultInfo.getDbmsTypeName(column);
      if (conv.convertsType(type, dbmsType))
      {
        Class clz = conv.getConvertedClass(type, dbmsType);
        if (clz != null) return clz.getName();
      }
    }
    return this.resultInfo.getColumnClassName(column);
  }

  public Class getColumnClass(int column)
  {
    DataConverter conv = RowDataReader.getConverterInstance(originalConnection);
    if (conv != null)
    {
      int type = resultInfo.getColumnType(column);
      String dbmsType = resultInfo.getDbmsTypeName(column);
      if (conv.convertsType(type, dbmsType))
      {
        return conv.getConvertedClass(type, dbmsType);
      }
    }
    return this.resultInfo.getColumnClass(column);
  }

  /**
   * Applies a filter based on the given {@link workbench.storage.filter.FilterExpression}
   * to this datastore. Each row that does not satisfy the {@link workbench.storage.filter.FilterExpression#evaluate(Map)}
   * criteria is removed from the active data
   *
   * @param filterExpression the expression identifying the rows to be kept
   *
   * @see workbench.storage.filter.FilterExpression
   * @see #clearFilter()
   */
  public void applyFilter(FilterExpression filterExpression)
  {
    this.clearFilter();
    int cols = getColumnCount();
    Map<String, Object> valueMap = new HashMap<>(cols);
    this.filteredRows = createData();
    int count = this.getRowCount();
    for (int i= (count - 1); i >= 0; i--)
    {
      RowData rowData = this.getRow(i);

      // Build the value map required for the FilterExpression
      for (int c=0; c < cols; c++)
      {
        String col = getColumnName(c);
        Object value = rowData.getValue(c);
        valueMap.put(col.toLowerCase(), value);
      }

      if (!filterExpression.evaluate(valueMap))
      {
        this.data.remove(i);
        this.filteredRows.add(rowData);
      }
    }

    if (this.filteredRows.size() == 0) this.filteredRows = null;
  }

  /**
   * Restores all rows that were filtered.
   */
  public void clearFilter()
  {
    if (this.filteredRows == null) return;
    int count = this.filteredRows.size();
    for (int i=0; i < count; i++)
    {
      RowData row = this.filteredRows.get(i);
      this.data.add(row);
    }
    this.filteredRows.clear();
    this.filteredRows = null;
  }

  /**
   *	Deletes the given row and saves it in the delete buffer
   *	in order to be able to generate a DELETE statement if
   *	this DataStore needs updating
   */
  public void deleteRow(int aRow)
    throws IndexOutOfBoundsException
  {
    RowData row = this.data.get(aRow);
    // new rows (not read from the database)
    // do not need to be put into the deleted buffer
    if (row.isNew())
    {
      this.data.remove(aRow);
    }
    else
    {
      if (this.deletedRows == null) this.deletedRows = createData();
      this.deletedRows.add(row);
      this.data.remove(aRow);
      this.modified = true;
    }
  }

  public void deleteRowWithDependencies(int aRow)
    throws IndexOutOfBoundsException, SQLException
  {
    if (this.updateTable == null) this.checkUpdateTable(originalConnection);

    RowData row = this.data.get(aRow);
    if (row == null) return;

    if (!row.isNew())
    {
      List<ColumnData> pk = getPkValues(aRow, true);

      DeleteScriptGenerator generator = new DeleteScriptGenerator(originalConnection);
      generator.setTable(updateTable);
      List<String> statements = generator.getStatementsForValues(pk, false);
      row.setDependencyDeletes(statements);
    }
    this.deleteRow(aRow);
  }

  /**
   *	Adds a new empty row to the DataStore.
   *	The new row will be marked as Modified
   *	@return int - the new row number
   */
  public int addRow()
  {
    RowData row = new RowData(this.resultInfo);
    this.data.add(row);
    this.modified = true;
    return this.getRowCount() - 1;
  }

  public int addRow(RowData row)
  {
    this.data.add(row);
    this.modified = true;
    return this.getRowCount() - 1;
  }

  /**
   *	Inserts a row after the given row number.
   *	If the new Index is greater then the current
   *	row count or the new index is &lt; 0 the new
   *	row will be added at the end.
   *	@return int - the new row number
   */
  public int insertRowAfter(int anIndex)
  {
    RowData row = new RowData(this.resultInfo);
    anIndex ++;
    int newIndex;

    if (anIndex > this.data.size() || anIndex < 0)
    {
      this.data.add(row);
      newIndex = this.getRowCount();
    }
    else
    {
      this.data.add(anIndex, row);
      newIndex = anIndex;
    }
    this.modified = true;
    return newIndex;
  }

  /**
   * Prepare the information which table should be updated. This
   * will not trigger the retrieval of the columns.
   *
   * This table will be used the next time checkUpdateTable() will
   * be called. checkUpdateTable() will not retrieve the
   * table name from the original SQL then.
   * @see #setUpdateTable(TableIdentifier)
   */
  public void setUpdateTableToBeUsed(TableIdentifier tbl)
  {
    this.updateTableToBeUsed = (tbl == null ? null : tbl.createCopy());
  }

  public void setUpdateTable(String aTablename, WbConnection aConn)
  {
    if (StringUtil.isEmptyString(aTablename))
    {
      setUpdateTable((TableIdentifier)null, aConn);
    }
    else
    {
      TableIdentifier tbl = new TableIdentifier(aTablename, aConn);
      tbl.setPreserveQuotes(true);
      setUpdateTable(tbl, aConn);
    }
  }

  public List<ColumnIdentifier> getMissingPkColumns()
  {
    return this.missingPkcolumns;
  }

  public boolean pkColumnsComplete()
  {
    return (CollectionUtil.isEmpty(this.missingPkcolumns));
  }

  public void setUpdateTable(TableIdentifier tbl)
  {
    setUpdateTable(tbl, this.originalConnection);
  }

  /**
   * Only for test purposes!
   * @param tbl the update table to be used without checking
   */
  public void forceUpdateTable(TableIdentifier tbl)
  {
    this.updateTable = (tbl == null ? null : tbl.createCopy());
  }

  /**
   * Sets the table to be updated for this DataStore.
   * Upon setting the table, the column definition for the table
   * will be retrieved using {@link workbench.db.DbMetadata}
   *
   * To define the table that should be used for updates, but without
   * retrieving its definition (for performance reasons) use
   * {@link #setUpdateTableToBeUsed(TableIdentifier)}
   *
   * any PK column that is not found in the current ResultInfo
   * will be stored and can be retrieved using getMissingPkColumns()
   *
   * @param tbl the table to be used as the update table
   * @param conn the connection where this table exists
   *
   * @see #setUpdateTableToBeUsed(TableIdentifier)
   * @see #getMissingPkColumns()
   */
  public void setUpdateTable(TableIdentifier tbl, WbConnection conn)
  {
    if (conn == null || (tbl != null && TableIdentifier.tablesAreEqual(tbl, this.updateTable, conn)) ) return;

    // Reset everything
    this.updateTable = null;
    this.resultInfo.setUpdateTable(null);
    this.missingPkcolumns = null;

    if (tbl == null) return;

    SourceTableDetector checkTable = new SourceTableDetector();
    checkTable.checkColumnTables(this.sql, resultInfo, conn);

    UpdateTableDetector detector = new UpdateTableDetector(conn);
    detector.setCheckPKOnly(conn.getDbSettings().getUpdateTableCheckPkOnly());

    detector.checkUpdateTable(tbl, resultInfo);
    updateTable = detector.getUpdateTable();
    missingPkcolumns = detector.getMissingPkColumns();

    checkForGeneratedKeys();
    restoreModifiedNotUpdateableColumns();
  }

  public void setUpdateTable(TableDefinition tableDef)
  {
    this.missingPkcolumns = new ArrayList<>(0);

    if (tableDef == null)
    {
      this.updateTable = null;
      this.resultInfo.setUpdateTable(null);
      return;
    }
    else
    {
      this.updateTable = tableDef.getTable();
      this.resultInfo.setUpdateTable(updateTable);
    }

    UpdateTableDetector detector = new UpdateTableDetector(originalConnection);

    List<ColumnIdentifier> columns = tableDef.getColumns();
    for (ColumnIdentifier column : columns)
    {
      int index = this.findColumn(column.getColumnName());
      if (index > -1)
      {
        detector.syncResultColumn(index, column, resultInfo);
      }
      else
      {
        LogMgr.logError("DataStore.setUpdateTable()", "Could not find column " + column + " from table definition in ResultInfo!", null);
      }
    }
    checkForGeneratedKeys();
  }

  private boolean columnBelongsToUpdateTable(int col)
  {
    // conservative approach: if no table was detected assume the column belongs to the update table
    if (this.updateTable == null) return true;
    if (!this.resultInfo.isColumnTableDetected()) return true;

    String sourceTable = resultInfo.getColumn(col).getSourceTableName();

    // conservative approach: if no table was detected assume the column belongs to the update table
    if (StringUtil.isEmptyString(sourceTable)) return true;

    TableIdentifier tbl = new TableIdentifier(sourceTable);
    return tablesAreEqual(tbl, updateTable);
  }

  private boolean tablesAreEqual(TableIdentifier t1, TableIdentifier t2)
  {
    if (t1 == null && t2 == null) return true;
    if (t1 == null || t2 == null) return false;

    if (!StringUtil.equalStringIgnoreCase(t1.getRawTableName(), t2.getRawTableName())) return false;
    String s1 = t1.getRawSchema();
    String s2 = t2.getRawSchema();
    if (s1 != null && s2 != null && !StringUtil.equalStringIgnoreCase(s1, s2)) return false;

    String c1 = t1.getRawCatalog();
    String c2 = t2.getRawCatalog();

    if (c1 != null && c2 != null && !StringUtil.equalStringIgnoreCase(c1, c2)) return false;
    return true;
  }


  /**
   * Restore the original column values for all columns that are marked as not modifieable and have been modified.
   *
   * Because checking the update table might have been initiated by actually editing
 a column value, we need to resetChangedFlags that value if the column(s) in question
 isn't actually updateable.
   *
   */
  private void restoreModifiedNotUpdateableColumns()
  {
    if (!Settings.getInstance().getCheckEditableColumns()) return;

    if (resultInfo.getUpdateTable() == null) return;

    String tname = resultInfo.getUpdateTable().getTableExpression();

    for (int row=0; row < getRowCount(); row ++)
    {
      if (!isRowModified(row)) continue;

      for (int col=0; col < getColumnCount(); col++)
      {
        String colName = resultInfo.getColumnName(col);
        boolean canUpdate = resultInfo.isUpdateable(col) && !resultInfo.getColumn(col).isReadonly();

        if (!canUpdate && isColumnModified(row, col))
        {
          boolean isComputed = resultInfo.getColumn(col).getComputedColumnExpression() != null;
          LogMgr.logWarning("DataStore.restoreModifiedNotUpdateableColumns()",
            "Restoring original value for column " + tname + "." + colName + " because column is marked as not updateable. " +
            "(isUpdateable: " + resultInfo.isUpdateable(col) +
            ", isReadonly: " + resultInfo.getColumn(col).isReadonly() +
            ", isComputed: " + isComputed + ")");
          getRow(row).restoreOriginalValue(col);
        }
        else if (!columnBelongsToUpdateTable(col))
        {
          String colTable = resultInfo.getColumn(col).getSourceTableName();
          LogMgr.logWarning("DataStore.restoreModifiedNotUpdateableColumns()",
            "Restoring original value for column " + colTable + "." + colName + " because column does not belong to the detected update table: " + tname);
          getRow(row).restoreOriginalValue(col);
        }
      }
    }
  }

  /**
   * Returns the current table to be updated if this DataStore is based on a SELECT query
   *
   * @return The current update table
   *
   * @see #setGeneratingSql(String)
   */
  @Override
  public TableIdentifier getUpdateTable()
  {
    if (this.updateTable == null) return null;
    return this.updateTable.createCopy();
  }

  /**
   * Return the name of the given column
   * @param aColumn The index of the column in this DataStore. The first column index is 0
   * @return The name of the column
   * @see ResultInfo#getColumnName(int)
   */
  public String getColumnName(int aColumn)
    throws IndexOutOfBoundsException
  {
    return this.resultInfo.getColumnName(aColumn);
  }

  /**
   * Return the display name of the given column.
   * In some cases (and for some JDBC drivers) this might be different
   * than the column name, e.g. if a column alias has been specified with <tt>AS</tt>
   *
   * @param col The index of the column in this DataStore. The first column index is 0
   * @return The display label of the column
   * @see ResultInfo#getColumnDisplayName(int)
   */
  public String getColumnDisplayName(int col)
  {
    return this.resultInfo.getColumnDisplayName(col);
  }

  /**
   * Return the size in characters of the column. This is
   * delegated to the instance of the {@link workbench.storage.ResultInfo} class
   * that is used to store the column meta data
   *
   * @param aColumn the column index
   * @return the suggested display size
   *
   * @see workbench.storage.ResultInfo#getColumnSize(int)
   * @see workbench.gui.components.DataStoreTableModel#getColumnWidth(int)
   */
  public int getColumnSize(int aColumn)
    throws IndexOutOfBoundsException
  {
    return this.resultInfo.getColumn(aColumn).getColumnSize();
  }

  public int getColumnDisplaySize(int col)
    throws IndexOutOfBoundsException
  {
    return this.resultInfo.getColumn(col).getDisplaySize();
  }

  public Object getOriginalValue(int aRow, int aColumn)
  {
    RowData row = this.getRow(aRow);
    return row.getOriginalValue(aColumn);
  }

  /**
   * Returns the current value of the specified column in the specified row.
   * @param aRow the row to get the data from (starts at 0)
   * @param aColumn the column to get the data for (starts at 0)
   *
   * @return the current value of the column might be different to the value
   * retrieved from the database!
   *
   * @see workbench.storage.RowData#getValue(int)
   * @see #getRow(int)
   */
  public Object getValue(int aRow, int aColumn)
    throws IndexOutOfBoundsException
  {
    RowData row = this.getRow(aRow);
    return row.getValue(aColumn);
  }

  /**
   * Returns the current value of the named column in the specified row.
   * This is equivalent to calling <tt>getRow(row, findColumn(columnName))</tt>
   * @param aRow the row to get the data from (starts at 0)
   * @param columnName the column to get the data for
   *
   * @return the current value of the column might be different to the value
   * retrieved from the database!
   *
   * @see workbench.storage.RowData#getValue(int)
   * @see #getRow(int)
   * @see #getColumnIndex(String)
   * @see #getValue(int, int)
   */
  public Object getValue(int aRow, String columnName)
    throws IndexOutOfBoundsException
  {
    int index = findColumn(columnName);
    RowData row = this.getRow(aRow);
    return row.getValue(index);
  }


  /**
   * Returns the value of the given row/column as a String.
   * The value's toString() method is used to convert the value to a String value.
   * @return Null if the column is null, or the column's value as a String
   */
  public String getValueAsString(int aRow, String colName)
    throws IndexOutOfBoundsException
  {
    return getValueAsString(aRow, findColumn(colName));
  }

  /**
   * Returns the value of the given row/column as a String.
   * The value's toString() method is used to convert the value to a String value.
   * @return Null if the column is null, or the column's value as a String
   */
  public String getValueAsString(int aRow, int aColumn)
    throws IndexOutOfBoundsException
  {
    Object value = getValue(aRow, aColumn);
    if (value == null) return null;

    if (value instanceof Clob)
    {
      try
      {
        Clob lob = (Clob)value;
        long len = lob.length();
        return lob.getSubString(1, (int)len);
      }
      catch (Exception e)
      {
        LogMgr.logError("DataStore.getValueAsString()", "Error converting BLOB to String", e);
        return null;
      }
    }
    else
    {
      return value.toString();
    }
  }

  /**
   * Return the value of a column as an int value.
   * If the object stored in the DataStore is an instance of Number
   * the intValue() of that object will be returned, otherwise the String value
   * of the column will be converted to an integer.
   * If it cannot be converted to an int, the default value will be returned
   * @param aRow The row
   * @param aColumn The column to be returned
   * @param aDefault The default value that will be returned if the the column's value cannot be converted to an int
   */
  public int getValueAsInt(int aRow, int aColumn, int aDefault)
  {
    Object value = getValue(aRow, aColumn);
    if (value == null)
    {
      return aDefault;
    }
    else if (value instanceof Number)
    {
      return ((Number)value).intValue();
    }
    else
    {
      return StringUtil.getIntValue(value.toString(), aDefault);
    }
  }

  /**
   * Return the value of a column as an long value.
   *
   * If the object stored in the DataStore is an instance of Number
   * the longValue() of that object will be returned, otherwise the String value
   * of the column will be converted to a long.
   *
   * If it cannot be converted to an long, the default value will be returned
   *
   * If the column contains null, the default value will be returned.
   *
   * @param aRow The row
   * @param aColumn The column to be returned
   * @param aDefault The default value that will be returned if the the column's value cannot be converted to a long
   *
   * @see StringUtil#getLongValue(java.lang.String, long)
   */
  public long getValueAsLong(int aRow, int aColumn, long aDefault)
  {
    Object value = getValue(aRow, aColumn);
    if (value == null)
    {
      return aDefault;
    }
    else if (value instanceof Number)
    {
      return ((Number)value).longValue();
    }
    else
    {
      return StringUtil.getLongValue(value.toString(), aDefault);
    }
  }

  /**
   * Set a value received from a user input.
   *
   * This will convert the given value to an object of the correct class.
   * @see #convertCellValue(java.lang.Object, int)
   */
  public void setInputValue(int row, int col, Object value)
    throws ConverterException
  {
    Object realValue = this.convertCellValue(value, col);
    this.setValue(row, col, realValue);
  }

  /**
   * Set the value for the given column.
   *
   * It is expected that the value is an instance of the correct class for the specified column.
   *
   * This will change the internal state of the DataStore to modified.
   *
   * @param rowNumber  the row to update
   * @param colIndex   the column to update
   * @param value      the (new) value to be set
   */
  public void setValue(int rowNumber, int colIndex, Object value)
    throws IndexOutOfBoundsException
  {
    // do not allow setting the value for columns
    // which do not have a name for. Those columns cannot
    // be saved to the database (because most likely they
    // are computed columns like count(*) etc)
    if (this.resultInfo.getColumnName(colIndex) == null) return;

    boolean checkEditable = Settings.getInstance().getCheckEditableColumns() && resultInfo.getUpdateTable() != null;
    if (checkEditable)
    {
      String tname = resultInfo.getUpdateTable().getTableName();
      String col = resultInfo.getColumnName(colIndex);
      if (!resultInfo.isUpdateable(colIndex) || resultInfo.getColumn(colIndex).isReadonly())
      {
        boolean isComputed = resultInfo.getColumn(colIndex).getComputedColumnExpression() != null;
        LogMgr.logWarning("DataStore.setValue()",
              "Discarding new value for column " + tname + "." + col + " because column is marked as not updateable. " +
              "(isUpdateable: " + resultInfo.isUpdateable(colIndex) +
              ", isReadonly: " + resultInfo.getColumn(colIndex).isReadonly() +
              ", isComputed: " + isComputed + ")");
        return;
      }
      else if (!columnBelongsToUpdateTable(colIndex))
      {
        LogMgr.logWarning("DataStore.restoreModifiedNotUpdateableColumns()",
          "Restoring original value for column " + tname + "." + col + " because column does not belong to the detected update table: " + updateTable.getTableExpression());
        return;
      }
    }

    RowData row = this.getRow(rowNumber);
    if (row == null)
    {
      LogMgr.logError("DataStore.setValue()", "Could not find specified row!", new Exception("Invalid row specified"));
      return;
    }

    row.setValue(colIndex,value);
    this.modified = row.isModified();
  }

  /**
   * Returns the index of the column with the given name.
   * @param aName The column's name to search for
   * @return The column's index (first column starts at 0)
   */
  public int getColumnIndex(String aName)
  {
    return this.findColumn(aName);
  }

  /**
   * Returns true if the given row has been modified.
   * A new row is considered modified only if setValue() has been called at least once.
   *
   * @param aRow The row to check
   */
  public boolean isRowModified(int aRow)
  {
    RowData row = this.getRow(aRow);
    return row.isModified();
  }

  /**
   * Returns true if the given column in the given row has been modified.
   *
   * @param row The row to check
   * @param column the column to check
   */
  public boolean isColumnModified(int row, int column)
  {
    RowData rowData = this.getRow(row);
    return rowData.isColumnModified(column);
  }

  /**
   * Restores the original value for the given row and column.
   *
   * @param row
   * @param column
   * @return true if the value could be restored
   */
  public Object restoreColumnValue(int row, int column)
  {
    RowData dataRow = getRow(row);
    if (dataRow != null)
    {
      return dataRow.restoreOriginalValue(column);
    }
    return null;
  }

  /**
   * Restore the original values as retrieved from the database.
   * This will have no effect if {@link #isModified()} returns <code>false</code>
   * @see #setValue(int, int, Object)
   */
  public boolean restoreOriginalValues()
  {
    RowData row;
    int rows = 0;
    if (this.deletedRows != null)
    {
      rows = deletedRows.size();
      for (int i=0; i < this.deletedRows.size(); i++)
      {
        row = this.deletedRows.get(i);
        this.data.add(row);
      }
      this.deletedRows = null;
    }
    for (int i=0; i < this.data.size(); i++)
    {
      row = this.getRow(i);
      boolean restored = row.restoreOriginalValues();
      if (restored)
      {
        rows ++;
      }
    }
    this.resetStatus();
    return rows > 0;
  }

  /**
   *	Remove all data from the DataStore
   */
  public void reset()
  {
    this.data.reset();
    if (this.deletedRows != null)
    {
      this.deletedRows.clear();
      this.deletedRows = null;
    }
    if (this.filteredRows != null)
    {
      this.filteredRows.clear();
      this.filteredRows = null;
    }
    this.modified = false;
  }

  public boolean hasUpdateableColumns()
  {
    return this.resultInfo.hasUpdateableColumns();
  }

  /**
   *	Returns true if at least one row has been updated.
   */
  public boolean hasUpdatedRows()
  {
    if (!this.isModified()) return false;
    int count = this.getRowCount();
    for (int i=0; i < count; i++)
    {
      RowData row = this.getRow(i);
      if (row.isModified() && !row.isNew()) return true;
    }
    return false;
  }

  /**
   *	Returns true if at least one row has been deleted
   */
  public boolean hasDeletedRows()
  {
    return (this.deletedRows != null && this.deletedRows.size() > 0);
  }

  /**
   * Returns true if key columns are needed to save the changes
   * to the database. If only inserted rows are present, then no
   * key is needed. For updated or deleted rows a key is needed
   */
  public boolean needPkForUpdate()
  {
    if (!this.isModified()) return false;
    return (this.hasDeletedRows() || this.hasUpdatedRows());
  }

  public boolean isFiltered()
  {
    return this.filteredRows != null;
  }

  public boolean isModified()
  {
    return this.modified;
  }

  public boolean isUpdateable()
  {
    if (this.allowUpdates) return true;
    return (this.updateTable != null && this.hasUpdateableColumns());
  }

  private int findColumn(String name)
  {
    return this.resultInfo.findColumn(name, this.originalConnection != null ? originalConnection.getMetadata() : QuoteHandler.STANDARD_HANDLER);
  }

  public Map<String, Object> getRowData(int row)
  {
    Map<String, Object> result = new HashMap<>(this.getColumnCount());
    for (int i=0; i < this.resultInfo.getColumnCount(); i++)
    {
      result.put(getColumnName(i), getValue(row, i));
    }
    return result;
  }

  @Override
  public RowData getRow(int aRow)
    throws IndexOutOfBoundsException
  {
    return this.data.get(aRow);
  }

  private void initMetaData(ResultSetMetaData metaData)
    throws SQLException
  {
    this.resultInfo = new ResultInfo(metaData, this.originalConnection);
    checkForGeneratedKeys();
  }

  /**
   * Read the column definitions from the result set's meta data
   * and store the data from the ResultSet in this DataStore with no maximum
   *
   * The ResultSet must be closed by the caller.
   *
   * @param aResultSet the ResultSet to read
   * @see #initData(ResultSet,int)
   */
  public final void initData(ResultSet aResultSet)
    throws SQLException
  {
    this.initData(aResultSet, -1);
  }

  public void setLoadTimeToNow()
  {
    this.loadedAt = System.currentTimeMillis();
  }

  public int fetchOnly(ResultSet rs)
    throws SQLException
  {
    return initData(rs, 0, false);
  }

  /**
   * Read the column definitions from the result set's meta data
   * and store the data from the ResultSet in this DataStore (up to maxRows)
   *
   * The ResultSet must be closed by the caller.
   *
   * @param aResultSet the ResultSet to read
   * @param maxRows max. number of rows to read. Zero or lower to read all rows
   * @see #initData(ResultSet)
   */
  public void initData(ResultSet aResultSet, int maxRows)
    throws SQLException
  {
    initData(aResultSet, maxRows, true);
  }

  protected int initData(ResultSet rs, int maxRows, boolean bufferData)
    throws SQLException
  {
    if (this.resultInfo == null)
    {
      try
      {
        ResultSetMetaData metaData = rs.getMetaData();
        this.initMetaData(metaData);
      }
      catch (SQLException e)
      {
        LogMgr.logError("DataStore.initData()", "Error while retrieving ResultSetMetaData", e);
        throw e;
      }
    }

    if (this.rowActionMonitor != null)
    {
      this.rowActionMonitor.setMonitorType(RowActionMonitor.MONITOR_LOAD);
    }

    setLoadTimeToNow();

    cancelRetrieve = false;
    boolean lowMemory = false;

    int rowCount = 0;
    final int reportInterval = Settings.getInstance().getIntProperty("workbench.gui.data.reportinterval", 10);
    final int checkInterval = Settings.getInstance().getIntProperty("workbench.gui.data.memcheckinterval", 100);

    if (bufferData && this.data == null)
    {
      this.data = createData();
    }

    try
    {
      RowDataReader reader = RowDataReaderFactory.createReader(resultInfo, originalConnection);

      if (bufferData)
      {
        // we cannot use streams because they would never be properly closed
        reader.setUseStreamsForBlobs(false);
        reader.setUseStreamsForClobs(false);
      }

      while (!this.cancelRetrieve && rs.next())
      {
        rowCount ++;

        if (this.rowActionMonitor != null && rowCount % reportInterval == 0)
        {
          this.rowActionMonitor.setCurrentRow(rowCount, -1);
        }

        if (bufferData)
        {
          RowData row = reader.read(rs, trimCharData);
          data.add(row);
        }

        if (maxRows > 0 && rowCount > maxRows) break;

        if (bufferData && rowCount % checkInterval == 0 && MemoryWatcher.isMemoryLow(false))
        {
          LogMgr.logError("DataStore.initData()", "Memory is running low. Aborting reading...", null);
          lowMemory = true;
          break;
        }
      }
      this.cancelRetrieve = false;
    }
    catch (SQLException e)
    {
      if (this.cancelRetrieve)
      {
        // some JDBC drivers will throw an exception when cancel() is called
        // as we silently want to use the data that has been retrieved so far
        // the Exception should not be passed to the caller
        LogMgr.logInfo("DataStore.initData()", "Retrieve cancelled");
        // do not resetChangedFlags the cancelRetrieve flag, because this is checked
        // by the caller!
      }
      else
      {
        LogMgr.logError("DataStore.initData()", "SQL Error during retrieve", e);
        throw e;
      }
    }
    catch (Exception e)
    {
      this.cancelRetrieve = false;
      LogMgr.logError("DataStore.initData()", "Error during retrieve", e);
      throw new SQLException(ExceptionUtil.getDisplay(e));
    }
    finally
    {
      this.modified = false;
    }

    if (lowMemory)
    {
      throw new LowMemoryException();
    }
    return rowCount;
  }

  /**
   * Return the system time this DataStore was retrieved.
   */
  public long getLoadedAt()
  {
    return loadedAt;
  }

  /**
   * Define the (SELECT) statement that was used to produce this
   * DataStore's result set.
   *
   * This is used to find the update table later. The passed SQL is also checked
   * for a definition of the result name (using @wbresult), but only if no
   * result name was already defined
   *
   */
  public void setGeneratingSql(String aSql)
  {
    this.sql = aSql;
    ResultNameAnnotation parser = new ResultNameAnnotation();
    if (resultName == null)
    {
      String name = parser.getResultName(sql);
      if (name == null && GuiSettings.getUseTablenameAsResultName())
      {
        List<Alias> tables = SqlUtil.getTables(aSql, false, this.originalConnection);
        if (tables.size() > 0)
        {
          name = tables.get(0).getObjectName();
        }
      }
      setResultName(name);
    }
  }

  /**
   * Define a column expression that was used to filter rows for this datastore.
   * This can be used to highlight the filter condition in the front end.
   *
   * @param filter
   */
  public void setGeneratingFilter(ColumnExpression filter)
  {
    this.generatingFilter = filter;
  }

  public ColumnExpression getGeneratingFilter()
  {
    return generatingFilter;
  }

  public String getGeneratingSql()
  {
    return this.sql;
  }

  public boolean checkUpdateTable()
  {
    return this.checkUpdateTable(this.originalConnection);
  }

  public boolean checkUpdateTable(WbConnection aConn)
  {
    if (aConn == null) return false;

    if (this.updateTableToBeUsed != null)
    {
      TableIdentifier ut = this.updateTableToBeUsed;
      this.updateTableToBeUsed = null;
      this.setUpdateTable(ut, aConn);
    }
    else if (this.updateTable == null)
    {
      if (this.sql == null) return false;
      List<Alias> tables = SqlUtil.getTables(this.sql, false, aConn);
      if (tables.size() != 1)
      {
        LogMgr.logWarning("DataStore.checkUpdateTable()", "More than one table found in the original query. No update table will be set.");
        return false;
      }
      String table = tables.get(0).getObjectName();
      LogMgr.logDebug("DataStore.checkUpdateTable()", "Using table name: " + table);
      this.setUpdateTable(table, aConn);
    }
    return this.updateTable != null;
  }

  /**
   *	Return the table that should be used when generating INSERTs
   *  ("copy as INSERT").
   *
   *	Normally this is the update table. If no update table
   *	is defined, the table from the SQL statement will be used
   *	but no checking for key columns takes place (which might take long)
   */
  public String getInsertTable()
  {
    if (this.updateTable != null) return this.updateTable.getTableExpression();
    if (this.updateTableToBeUsed != null) return this.updateTableToBeUsed.getTableExpression();
    if (this.sql == null) return null;
    if (!this.sqlHasUpdateTable()) return null;
    List<Alias> tables = SqlUtil.getTables(this.sql, false, originalConnection);
    if (tables.size() != 1) return null;
    String table = tables.get(0).getObjectName();
    return table;
  }

  /**
   *	Returns true if the current data can be converted to SQL INSERT statements.
   *	The data can be saved as SQL INSERTs if an update table is defined.
   *	If no update table is defined, then this method will call {@link #checkUpdateTable()}
   *  and try to determine the table from the used SQL statement.
   *
   *  @return true if an update table is defined
   */
  public boolean canSaveAsSqlInsert()
  {
    return (this.getInsertTable() != null);
  }

  private SqlLiteralFormatter createLiteralFormatter()
  {
    return new SqlLiteralFormatter(this.originalConnection);
  }

  /**
   * Checks if the underlying SQL statement references only one table.
   * @return true if only one table is found in the SELECT statement
   *
   * @see workbench.util.SqlUtil#getTables(java.lang.String)
   */
  public boolean sqlHasUpdateTable()
  {
    if (this.updateTable != null) return true;
    if (this.sql == null) return false;
    List<Alias> tables = SqlUtil.getTables(this.sql, false, this.originalConnection);
    return (tables.size() == 1);
  }

  /**
   * Set all values in the given row to NULL
   */
  public void setRowNull(int aRow)
  {
    for (int i=0; i < this.resultInfo.getColumnCount(); i++)
    {
      this.setValue(aRow, i, null);
    }
  }

  /**
   * Cancels a currently running update. This has to be called
   * from a different thread than the one from which updatedb() was
   * called
   *
   * @see #updateDb(workbench.db.WbConnection, workbench.interfaces.JobErrorHandler)
   */
  public void cancelUpdate()
  {
    this.cancelUpdate = true;
  }

  /**
   * If the DataStore is beeing initialized with a ResultSet, this
   * cancels the processing of the ResultSet.
   *
   * @see #DataStore(java.sql.ResultSet)
   * @see #initData(java.sql.ResultSet)
   */
  public void cancelRetrieve()
  {
    this.cancelRetrieve = true;
  }

  /**
   * Checks if the last ResultSet processing was cancelled.
   * This will only be correct if initData() was called previously
   *
   * @return true if retrieval was cancelled.
   *
   * @see #DataStore(java.sql.ResultSet)
   * @see #initData(ResultSet)
   */
  public boolean isCancelled()
  {
    return this.cancelRetrieve;
  }

  public void resetCancelStatus()
  {
    this.cancelRetrieve = false;
  }

  private void updateProgressMonitor(int currentRow, int totalRows)
  {
    if (this.rowActionMonitor != null)
    {
      this.rowActionMonitor.setCurrentRow(currentRow, totalRows);
    }
  }

  /**
   * Returns a List of {@link workbench.storage.DmlStatement}s which
   * would be executed in order to store the current content
   * of the DataStore.
   * The returned list will be empty if no changes were made to the datastore
   *
   * @return a List of {@link workbench.storage.DmlStatement}s to be sent to the database
   *
   * @see workbench.storage.StatementFactory
   * @see workbench.storage.DmlStatement#getExecutableStatement(SqlLiteralFormatter)
   */
  public List<DmlStatement> getUpdateStatements(WbConnection aConnection)
    throws SQLException
  {
    if (this.updateTable == null) throw new NullPointerException("No update table defined!");
    this.updatePkInformation();

    List<DmlStatement> stmtList = new ArrayList<>(getModifiedCount());
    this.resetUpdateRowCounters();

    DmlStatement dml;
    RowData row;

    StatementFactory factory = new StatementFactory(this.resultInfo, this.originalConnection);
    String le = Settings.getInstance().getInternalEditorLineEnding();

    row = this.getNextDeletedRow();
    while (row != null)
    {
      List<String> deletes = row.getDependencyDeletes();
      if (deletes != null)
      {
        for (String delete : deletes)
        {
          if (delete != null) stmtList.add(new DmlStatement(delete, null));
        }
      }
      dml = factory.createDeleteStatement(row);
      stmtList.add(dml);
      row = this.getNextDeletedRow();
    }

    row = this.getNextChangedRow();
    while (row != null)
    {
      dml = factory.createUpdateStatement(row, false, le);
      stmtList.add(dml);
      row = this.getNextChangedRow();
    }

    row = this.getNextInsertedRow();
    while (row != null)
    {
      dml = factory.createInsertStatement(row, false, le);
      stmtList.add(dml);
      row = this.getNextInsertedRow();
    }
    this.resetUpdateRowCounters();
    return stmtList;
  }

  private boolean ignoreAllUpdateErrors = false;

  private int executeGuarded(WbConnection aConnection, RowData row, DmlStatement dml, JobErrorHandler errorHandler, int rowNum)
    throws SQLException
  {
    int rowsUpdated = 0;
    Statement stmt = null;
    String delete = null;
    boolean retrieveKeys = aConnection.getDbSettings().getRetrieveGeneratedKeys();
    try
    {
      List<String> dependent = row.getDependencyDeletes();
      if (dependent != null)
      {
        try
        {
          stmt = aConnection.createStatement();
          Iterator<String> itr = dependent.iterator();
          while (itr.hasNext())
          {
            delete = itr.next();
            stmt.executeUpdate(delete);
          }
        }
        finally
        {
          SqlUtil.closeStatement(stmt);
        }
      }
      rowsUpdated = dml.execute(aConnection, row.isNew() && hasGeneratedKeys && retrieveKeys);
      row.setDmlSent(true);
    }
    catch (SQLException e)
    {
      this.updateHadErrors = true;

      String esql = (delete == null ? dml.getExecutableStatement(createLiteralFormatter(), this.originalConnection).toString() : delete);
      if (this.ignoreAllUpdateErrors)
      {
        LogMgr.logError("DataStore.executeGuarded()", "Error executing statement " + esql + " for row = " + row + ", error: " + e.getMessage(), null);
      }
      else
      {
        boolean abort = true;
        int choice = JobErrorHandler.JOB_ABORT;
        if (errorHandler != null)
        {
          choice = errorHandler.getActionOnError(rowNum, null, esql, e.getMessage());
        }
        if (choice == JobErrorHandler.JOB_CONTINUE)
        {
          abort = false;
        }
        else if (choice == JobErrorHandler.JOB_IGNORE_ALL)
        {
          abort = false;
          this.ignoreAllUpdateErrors = true;
        }
        if (abort) throw e;
      }
    }
    return rowsUpdated;
  }

  /**
   * Save the changes to this DataStore to the database.
   * The changes are applied in the following order
   * <ul>
   * <li>Delete statements</li>
   * <li>Insert statements</li>
   * <li>Update statements</li>
   * </ul>
   * If everything was successful, the changes will be committed automatically
   * If an error occurs a rollback will be sent to the database
   *
   * @param aConnection the connection where the database should be updated
   * @param errorHandler callback for error handling
   * @return the number of rows affected
   *
   * @see workbench.storage.StatementFactory
   * @see workbench.storage.DmlStatement#getExecutableStatement(SqlLiteralFormatter)
   */
  public synchronized int updateDb(WbConnection aConnection, JobErrorHandler errorHandler)
    throws SQLException
  {
    int rows = 0;
    RowData row;
    this.cancelUpdate = false;
    this.updatePkInformation();
    int totalRows = this.getModifiedCount();
    this.updateHadErrors = false;
    int currentRow = 0;
    if (this.rowActionMonitor != null)
    {
      this.rowActionMonitor.setMonitorType(RowActionMonitor.MONITOR_UPDATE);
    }

    this.ignoreAllUpdateErrors = false;

    // a bit paranoid, but for some reason this situation seems to happen
    // especially when the save button is always enabled.
    if (this.updateTable != null && resultInfo.getUpdateTable() == null)
    {
      LogMgr.logWarning("DataStore.updateDb()", "Update table for ResultInfo not in sync with DataStore!");
      resultInfo.setUpdateTable(this.updateTable);
    }

    StatementFactory factory = new StatementFactory(this.resultInfo, aConnection);
    String le = Settings.getInstance().getInternalEditorLineEnding();
    boolean inCommit = false;

    try
    {
      this.resetUpdateRowCounters();
      row = this.getNextDeletedRow();
      while (row != null)
      {
        currentRow ++;
        this.updateProgressMonitor(currentRow, totalRows);
        if (!row.isDmlSent())
        {
          DmlStatement dml = factory.createDeleteStatement(row);
          rows += this.executeGuarded(aConnection, row, dml, errorHandler, -1);
        }
        Thread.yield();
        if (this.cancelUpdate) return rows;
        row = this.getNextDeletedRow();
      }

      row = this.getNextChangedRow();
      while (row != null)
      {
        currentRow ++;
        this.updateProgressMonitor(currentRow, totalRows);
        if (!row.isDmlSent())
        {
          DmlStatement dml = factory.createUpdateStatement(row, false, le);
          rows += this.executeGuarded(aConnection, row, dml, errorHandler, currentUpdateRow);
        }
        Thread.yield();
        if (this.cancelUpdate) return rows;
        row = this.getNextChangedRow();
      }

      row = this.getNextInsertedRow();
      while (row != null)
      {
        currentRow ++;
        this.updateProgressMonitor(currentRow, totalRows);
        if (!row.isDmlSent())
        {
          DmlStatement dml = factory.createInsertStatement(row, false, le);
          int inserted = this.executeGuarded(aConnection, row, dml, errorHandler, currentInsertRow);
          rows += inserted;
          if (inserted == 1 && dml.hasGeneratedKeys())
          {
            updateGeneratedKeys(row, dml);
          }
        }
        Thread.yield();
        if (this.cancelUpdate) return rows;
        row = this.getNextInsertedRow();
      }

      // If we got here, then either all errors were ignored
      // or no errors occured at all. Even if no rows were updated
      // we are sending a commit() to make sure the transaction
      // is ended. This is especially important for Postgres
      // in case an error occured during update (and the user chose to proceed)
      if (!aConnection.getAutoCommit())
      {
        inCommit = true;
        aConnection.commit();
      }
      resetStatusForSentRows();
      resetStatus();
    }
    catch (SQLException e)
    {
      if (inCommit)
      {
        String msg = ResourceMgr.getFormattedString("ErrCommit", ExceptionUtil.getDisplay(e));
        if (errorHandler != null)
        {
          errorHandler.fatalError(msg);
        }
        else
        {
          WbSwingUtilities.showErrorMessage(null, msg);
        }
      }

      if (!aConnection.getAutoCommit())
      {
        // in case of an exception we have to resetChangedFlags the dmlSent flag for
        // all modified rows otherwise the next attempt to save the changes
        // will not re-send them (but as the transaction has been rolled back,
        // they are not stored in the database)
        resetDmlSentStatus();
        try { aConnection.rollback(); } catch (Throwable th) {}
      }
      LogMgr.logError("DataStore.updateDb()", "Error when saving data for row=" + currentRow + ", error: " + e.getMessage(), null);
      throw e;
    }

    return rows;
  }

  private void updateGeneratedKeys(RowData row, DmlStatement dml)
  {
    if (dml == null) return;
    if (row == null) return;
    if (resultInfo == null) return;

    for (int i=0; i < resultInfo.getColumnCount(); i++)
    {
      if (resultInfo.getColumn(i).isAutoGenerated())
      {
        Object value = dml.getGeneratedKey(resultInfo.getColumnName(i));
        row.setValue(i, value);
        row.resetStatusForColumn(i);
      }
    }
  }

  public boolean lastUpdateHadErrors()
  {
    return updateHadErrors;
  }

  /**
   * Clears the flag for all modified rows that indicates any pending update
   * has already been sent to the database.
   * This is necessary if an error occurs during update, to ensure the
   * rows are re-send the next time.
   */
  public void resetDmlSentStatus()
  {
    int rows = this.getRowCount();
    for (int i=0; i < rows; i++)
    {
      RowData row = this.getRow(i);
      row.setDmlSent(false);
    }
    if (this.deletedRows != null)
    {
      rows = this.deletedRows.size();
      for (int i=0; i < rows; i++)
      {
        RowData row = this.deletedRows.get(i);
        row.setDmlSent(false);
      }
    }
  }

  /**
   * Clears the modified status for those rows where
   * the update has been sent to the database
   */
  public void resetStatusForSentRows()
  {
    int rows = this.getRowCount();
    for (int i=0; i < rows; i++)
    {
      RowData row = this.getRow(i);
      if (row.isDmlSent())
      {
        row.resetStatus();
      }
    }
    if (this.deletedRows != null)
    {
      RowDataList newDeleted = createData(this.deletedRows.size());
      rows = this.deletedRows.size();
      for (int i=0; i < rows; i++)
      {
        RowData row = this.deletedRows.get(i);
        if (!row.isDmlSent())
        {
          newDeleted.add(row);
        }
      }
      this.deletedRows.clear();
      this.deletedRows = newDeleted;
    }
  }

  /**
   * Reset all rows to not modified. After this a call
   * to #isModified() will return false.
   */
  public void resetStatus()
  {
    this.deletedRows = null;
    this.modified = false;
    for (int i=0; i < this.data.size(); i++)
    {
      RowData row = this.getRow(i);
      row.resetStatus();
    }
    this.resetUpdateRowCounters();
  }

  protected RowDataListSorter createSorter(SortDefinition sort)
  {
    RowDataListSorter sorter = new RowDataListSorter(sort);
    sorter.setIgnoreCase(sort.getIgnoreCase());
    return sorter;
  }

  public void sort(SortDefinition sortDef)
  {
    lastSort = sortDef;
    synchronized (this)
    {
      RowDataListSorter sorter = createSorter(sortDef);
      sorter.sort(this.data);
    }
  }

  public void sortByColumn(int col, boolean ascending)
  {
    synchronized (this)
    {
      lastSort = new SortDefinition(col, ascending);
      lastSort.setUseNaturalSort(useNaturalSort);
      RowDataListSorter sorter = createSorter(lastSort);
      sorter.sort(this.data);
    }
  }

  public SortDefinition getLastSort()
  {
    return lastSort;
  }

  /**
   * Convert the value to the approriate class instance
   * for the given column
   *
   * @param aValue the value as entered by the user
   * @param aColumn the column for which this value should be converted
   * @return an Object of the needed class for the column
   * @see ValueConverter#convertValue(Object, int)
   */
  public Object convertCellValue(Object aValue, int aColumn)
    throws ConverterException
  {
    int type = this.getColumnType(aColumn);
    if (aValue == null) return null;

    ValueConverter converter = new ValueConverter();

    return converter.convertValue(aValue, type);
  }

  /**
   * Return the status value for the given row.
   * The status is one of
   * <ul>
   * <li>{@link RowData#NOT_MODIFIED}</li>
   * <li>{@link RowData#MODIFIED}</li>
   * <li>{@link RowData#NEW}</li>
   * </ul>
   * The status object is used by the {@link workbench.gui.renderer.RowStatusRenderer}
   * in the result table to display the approriate icon.
   * @param aRow the row for which the status should be returned
   * @return an int identifying the status
   */
  public int getRowStatus(int aRow)
    throws IndexOutOfBoundsException
  {
    RowData row = this.getRow(aRow);

    if (row == null)
    {
      return RowData.NOT_MODIFIED;
    }
    else if (row.isNew())
    {
      return RowData.NEW;
    }
    else if (row.isModified())
    {
      return RowData.MODIFIED;
    }
    else
    {
      return RowData.NOT_MODIFIED;
    }
  }

  /**
   * Returns a list with the value of all PK columns for the given
   * row. The key to the map is the name of the column.
   *
   * @see workbench.storage.ResultInfo#isPkColumn(int)
   * @see #getValue(int, int)
   */
  public List<ColumnData> getPkValues(int aRow)
  {
    return getPkValues(aRow, false);
  }

  /**
   * Returns a list with the value of all PK columns for the given
   * row. The key to the map is the name of the column.
   *
   * @see workbench.storage.ResultInfo#isPkColumn(int)
   * @see #getValue(int, int)
   */
  public List<ColumnData> getPkValues(int aRow, boolean originalValues)
  {
    if (this.originalConnection == null) return Collections.emptyList();

    try
    {
      this.updatePkInformation();
    }
    catch (SQLException e)
    {
      return Collections.emptyList();
    }

    if (!this.resultInfo.hasPkColumns()) return Collections.emptyList();

    RowData rowdata = this.getRow(aRow);
    if (rowdata == null) return Collections.emptyList();

    int count = this.resultInfo.getColumnCount();
    List<ColumnData> result = new LinkedList<>();
    for (int j=0; j < count ; j++)
    {
      if (this.resultInfo.isPkColumn(j))
      {
        ColumnIdentifier col = this.resultInfo.getColumn(j);
        Object value = (originalValues ? rowdata.getOriginalValue(j) : rowdata.getValue(j));
        result.add(new ColumnData(value, col));
      }
    }
    return result;
  }

  protected void resetUpdateRowCounters()
  {
    currentUpdateRow = 0;
    currentInsertRow = 0;
    currentDeleteRow = 0;
  }

  protected RowData getNextChangedRow()
  {
    if (this.currentUpdateRow >= this.getRowCount()) return null;
    RowData row;

    int count = this.getRowCount();

    while (this.currentUpdateRow < count)
    {
      row = this.getRow(this.currentUpdateRow);
      this.currentUpdateRow ++;

      if (row.isModified() && !row.isNew()) return row;
    }
    return null;
  }

  public int getDeletedRowCount()
  {
    if (this.deletedRows == null) return 0;
    return deletedRows.size();
  }

  public Object getDeletedValue(int row, int col)
  {
    if (this.deletedRows == null || this.deletedRows.size() == 0) return null;
    int count = this.deletedRows.size();
    if (row > count) return null;
    RowData rowData = this.deletedRows.get(row);
    return rowData.getValue(col);
  }

  protected RowData getNextDeletedRow()
  {
    if (this.deletedRows == null || this.deletedRows.size() == 0) return null;
    int count = this.deletedRows.size();

    if (this.currentDeleteRow > count) return null;

    RowData row;

    while (this.currentDeleteRow < count)
    {
      row = this.deletedRows.get(this.currentDeleteRow);
      this.currentDeleteRow ++;
      return row;
    }
    return null;
  }

  protected RowData getNextInsertedRow()
  {
    int count = this.getRowCount();
    if (this.currentInsertRow >= count) return null;

    RowData row;

    while (this.currentInsertRow < count)
    {
      row = this.getRow(this.currentInsertRow);
      this.currentInsertRow ++;
      if (row.isNew() && row.isModified())
      {
        return row;
      }
    }
    return null;
  }

  public void setPKColumns(ColumnIdentifier[] pkColumns)
  {
    this.resultInfo.setPKColumns(pkColumns);
    this.missingPkcolumns = null;
  }

  @Override
  public ResultInfo getResultInfo()
  {
    return this.resultInfo;
  }

  public ColumnIdentifier[] getColumns()
  {
    return this.resultInfo.getColumns();
  }

  public boolean hasPkColumns()
  {
    return this.resultInfo.hasPkColumns();
  }

  /**
   *	Check if the currently defined updateTable
   *	has any Primary keys defined in the database.
   *
   *	If it has, a subsequent call to hasPkColumns() returns true
   */
  public void updatePkInformation()
    throws SQLException
  {
    if (this.resultInfo.hasPkColumns()) return;
    if (this.updateTable == null)
    {
      this.checkUpdateTable();
    }

    if (this.updateTable == null)
    {
      LogMgr.logDebug("Datastore.updatePkInformation()", "No update table found, PK information not available");
    }

    // If we have found a single update table, but no Primary Keys
    // we try to find a user-defined PK mapping.
    if (this.updateTable != null && !this.hasPkColumns())
    {
      LogMgr.logDebug("Datastore.updatePkInformation()", "Trying to retrieve PK information from user-defined PK mapping");
      this.resultInfo.readPkColumnsFromMapping();
    }
  }

  /** Getter for property progressMonitor.
   * @return Value of property progressMonitor.
   *
   */
  public RowActionMonitor getProgressMonitor()
  {
    return this.rowActionMonitor;
  }

  /** Setter for property progressMonitor.
   * @param aMonitor New value of property progressMonitor.
   *
   */
  public void setProgressMonitor(RowActionMonitor aMonitor)
  {
    this.rowActionMonitor = aMonitor;
  }

  @Override
  public String toString()
  {
    return Integer.toString(getRowCount()) + " rows";
  }


}
