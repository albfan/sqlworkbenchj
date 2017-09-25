/*
 * TableDataDiff.java
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
package workbench.db.compare;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.BlobMode;
import workbench.interfaces.ErrorReporter;
import workbench.interfaces.ProgressReporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.ColumnData;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.storage.RowDataReader;
import workbench.storage.RowDataReaderFactory;
import workbench.storage.SqlLiteralFormatter;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A class to compare the data of two tables and generate approriate INSERT or UPDATE
 * statements in order to sync the tables.
 *
 * The table that should be synchronized needs to exist in both the target and
 * the reference database and it is expected that both tables have the same primary
 * key definition.
 *
 * To improve performance (a bit), the rows are retrieved in chunks from the
 * target table by dynamically constructing a WHERE clause for the rows
 * that were retrieved from the reference table. The chunk size
 * can be controlled using the property workbench.sql.sync.chunksize
 * The chunk size defaults to 25. This is a conservative setting to avoid
 * problems with long SQL statements when processing tables that have
 * a PK with multiple columns.
 *
 * @see workbench.resource.Settings#getSyncChunkSize()
 *
 * @author Thomas Kellerer
 */
public class TableDataDiff
  implements ProgressReporter, ErrorReporter
{
  private WbConnection toSync;
  private WbConnection reference;
  private TableIdentifier referenceTable;
  private TableIdentifier tableToSync;
  private TableDefinition toSyncDef;

  private int chunkSize = 15;

  private Statement checkStatement;
  private RowActionMonitor monitor;
  private Writer updateWriter;
  private Writer insertWriter;

  private boolean firstUpdate;
  private boolean firstInsert;

  private SqlLiteralFormatter formatter;
  private List<ColumnIdentifier> pkColumns = new ArrayList<>();
  private String lineEnding = "\n";
  private String encoding = "UTF-8";

  private boolean cancelExecution;
  private int progressInterval = 10;

  private Set<String> columnsToIgnore = CollectionUtil.caseInsensitiveSet();
  private RowDataComparer comparer;

  private MessageBuffer warnings = new MessageBuffer();
  private MessageBuffer errors = new MessageBuffer();
  private long currentRowNumber;

  private Map<String, Set<String>> alternateKeys;
  private Set<String> realPKCols = CollectionUtil.caseInsensitiveSet();
  private boolean excludeRealPK;
  private boolean excludeIgnoredColumns;
  private boolean ignoreMissingTarget;
  private String targetSchema;

  public TableDataDiff(WbConnection original, WbConnection compareTo)
    throws SQLException
  {
    toSync = compareTo;
    reference = original;
    formatter = new SqlLiteralFormatter(toSync);
    formatter.setDateLiteralType("jdbc");
    chunkSize = Settings.getInstance().getSyncChunkSize();
    comparer = new RowDataComparer();
    comparer.setConnection(toSync);
    comparer.setTypeSql();
  }

  public void setTargetSchema(String schema)
  {
    this.targetSchema = schema;
  }

  public void setTypeXml(boolean useCDATA)
  {
    comparer.setTypeXml(useCDATA);
  }

  public void setTypeSql()
  {
    comparer.setTypeSql();
  }

  public void setRowMonitor(RowActionMonitor rowMonitor)
  {
    this.monitor = rowMonitor;
  }

  @Override
  public void addWarning(String msg)
  {
    this.warnings.append(msg);
    this.warnings.appendNewLine();
  }

  @Override
  public void addError(String msg)
  {
    this.errors.append(msg);
    this.errors.appendNewLine();
  }

  /**
   * Controls the usage of the real PK columns in case alternate keys are defined.
   *
   * If this is set to true, the real PK will not be included in generated INSERT statements.
   *
   * If no alternate keys are defined, this flag is ignored.
   *
   * @param flag  true exclude real PK in INSERT statements
   *              false include real PK in INSERT statements
   */
  public void setExcludeRealPK(boolean flag)
  {
    this.excludeRealPK = flag;
  }

  /**
   * Controls if ignored columns are also excluded from the generate SQL statements.
   *
   * @param flag  if true, ignored columns are excluded
   * @see #setColumnsToIgnore(java.util.List)
   */
  public void setExcludeIgnoredColumns(boolean flag)
  {
    this.excludeIgnoredColumns = flag;
  }

  public void setIgnoreMissingTarget(boolean flag)
  {
    this.ignoreMissingTarget = flag;
  }


  /**
   * Define a alternate PK columns.
   *
   * @param mapping the key of the map is the table name, the value is the list of column names to be used as the key
   * @see #setExcludeRealPK(boolean)
   */
  public void setAlternateKeys(Map<String, Set<String>> mapping)
  {
    if (CollectionUtil.isEmpty(mapping))
    {
      this.alternateKeys = null;
    }
    else
    {
      CaseInsensitiveComparator comp = new CaseInsensitiveComparator();
      comp.setIgnoreSQLQuotes(true);
      this.alternateKeys = new TreeMap<>(comp);
      this.alternateKeys.putAll(mapping);
    }
  }

  private Set<String> getAlternatePKs(String tableName)
  {
    if (this.alternateKeys == null)
    {
      return Collections.emptySet();
    }
    Set<String> cols = alternateKeys.get(tableName);
    if (cols == null)
    {
      return Collections.emptySet();
    }
    return cols;
  }

  /**
   * Define how blobs should be handled during export.
   *
   * @param type the blob mode to be used.
   *        null means no special treatment (toString() will be called)
   */
  public void setBlobMode(String type)
  {
    BlobMode mode = BlobMode.getMode(type);
    if (mode == null)
    {
      String msg = ResourceMgr.getString("ErrExpInvalidBlobType");
      msg = StringUtil.replace(msg, "%paramvalue%", type);
      this.addWarning(msg);
    }
    else
    {
      comparer.setSqlBlobMode(mode);
    }
  }

  /**
   * Define a list of column names which should not considered when
   * checking for differences (e.g. a "MODIFIED" column)
   *
   * @param columnNames
   */
  public void setColumnsToIgnore(List<String> columnNames)
  {
    this.columnsToIgnore.clear();
    if (columnNames != null)
    {
      this.columnsToIgnore.addAll(columnNames);
    }
  }

  @Override
  public void setReportInterval(int interval)
  {
    this.progressInterval = interval;
  }

  /**
   * Define the literal type of the date literals.
   * This is simply delegated to the instance of the
   * SqlRowDataConverte} that is used internally.
   *
   * @param type
   */
  public void setSqlDateLiteralType(String type)
  {
    comparer.setSqlDateLiteralType(type);
  }

  public void setBaseDir(WbFile dir)
  {
    comparer.setBaseDir(dir);
  }

  /**
   * Set the Writers to write the generated UPDATE and INSERT statements.
   *
   * @param updates the Writer to write UPDATEs to
   * @param inserts the Writer to write INSERTs to
   * @param lineEnd the line end character(s) to be used when writing the text files
   * @param encoding the encoding used by the writers (this will be written into the XML files)
   */
  public void setOutputWriters(Writer updates, Writer inserts, String lineEnd, String encoding)
  {
    this.updateWriter = updates;
    this.insertWriter = inserts;
    this.lineEnding = (lineEnd == null ? "\n" : lineEnd);
    this.encoding = encoding;
  }

  /**
   * Define the tables to be compared.
   *
   * @param refTable the table with the "reference" data
   * @param tableToVerify the table from which obsolete rows should be deleted
   * @return status information if the tables can be compared
   *
   * @throws java.sql.SQLException if something went wrong retrieving the table meta data
   */
  public TableDiffStatus setTableName(TableIdentifier refTable, TableIdentifier tableToVerify)
    throws SQLException
  {
    firstUpdate = true;
    firstInsert = true;
    pkColumns.clear();
    realPKCols.clear();

    referenceTable = this.reference.getMetadata().findSelectableObject(refTable);
    if (referenceTable == null)
    {
      LogMgr.logError("TableDataDiff.setTableName()", "Reference table " + refTable.getTableName() + " not found!", null);
      return TableDiffStatus.ReferenceNotFound;
    }

    List<ColumnIdentifier> cols = this.reference.getMetadata().getTableColumns(referenceTable);
    Set<String> alternatePK = getAlternatePKs(refTable.getTableName());
    boolean useAlternatePK = CollectionUtil.isNonEmpty(alternatePK);

    for (ColumnIdentifier col : cols)
    {
      if ((!useAlternatePK && col.isPkColumn()) || alternatePK.contains(col.getColumnName()))
      {
        pkColumns.add(col);
      }

      if (useAlternatePK && col.isPkColumn())
      {
        realPKCols.add(col.getColumnName());
      }
    }

    if (CollectionUtil.isEmpty(pkColumns))
    {
      return TableDiffStatus.NoPK; //throw new SQLException("No primary key found for table " + referenceTable);
    }

    tableToSync = this.toSync.getMetadata().findSelectableObject(tableToVerify);
    if (tableToSync == null && !ignoreMissingTarget)
    {
      LogMgr.logError("TableDataDiff.setTableName()", "Target table " + tableToVerify.getTableName() + " not found!", null);
      return TableDiffStatus.TargetNotFound;
    }

    TableDiffStatus status = TableDiffStatus.OK;

    if (tableToSync != null)
    {
      toSyncDef = this.toSync.getMetadata().getTableDefinition(tableToSync);
      tableToSync = toSyncDef.getTable();

      for (ColumnIdentifier col : cols)
      {
        if (findTargetColumn(col) == null)
        {
          status = TableDiffStatus.ColumnMismatch;
          LogMgr.logError("TableDataDiff.setTableName()", "Reference column " + col.getColumnName() + " not found in target table!", null);
        }
      }
    }

    return status;
  }

  public void cancel()
  {
    this.cancelExecution = true;
  }

  /**
   * This starts the actual creation of the necessary update and inserts
   * statements.
   *
   * @throws java.sql.SQLException
   * @throws java.io.IOException
   */
  public void doSync()
    throws SQLException, IOException
  {
    String retrieve = "SELECT * FROM " + this.referenceTable.getTableExpression(this.reference);

    LogMgr.logDebug("TableDataDiff.doSync()", "Using " + retrieve + " to retrieve rows from reference database");

    checkStatement = toSync.createStatementForQuery();

    cancelExecution = false;

    ResultSet rs = null;
    Statement stmt = null;
    currentRowNumber = 0;
    RowDataReader reader = null;
    try
    {
      // Process all rows from the reference table to be synchronized
      stmt = this.reference.createStatementForQuery();
      rs = stmt.executeQuery(retrieve);
      ResultInfo info = new ResultInfo(rs.getMetaData(), this.reference);

      if (this.monitor != null)
      {
        this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
        String msg = ResourceMgr.getFormattedString("MsgDataDiffProcessUpd", referenceTable.getTableName());
        this.monitor.setCurrentObject(msg, -1, -1);
      }

      List<RowData> packetRows = new ArrayList<>(chunkSize);

      reader = RowDataReaderFactory.createReader(info, this.reference);
      while (rs.next())
      {
        if (cancelExecution) break;

        RowData row = reader.read(rs, false);
        packetRows.add(row);

        if (packetRows.size() == chunkSize)
        {
          checkRows(packetRows, info);
          packetRows.clear();
          reader.closeStreams();
        }
      }

      if (packetRows.size() > 0 && !cancelExecution)
      {
        checkRows(packetRows, info);
      }

      if (!firstUpdate) writeEnd(updateWriter);
      if (!firstInsert) writeEnd(insertWriter);
    }
    finally
    {
      SqlUtil.closeResult(rs);
      SqlUtil.closeStatement(stmt);
      SqlUtil.closeStatement(this.checkStatement);
      if (reader != null)
      {
        reader.closeStreams();
      }
    }
  }

  private void checkRows(List<RowData> referenceRows, ResultInfo info)
    throws SQLException, IOException
  {
    String sql = buildCheckSql(referenceRows, info);
    ResultSet rs = null;
    RowDataReader reader = null;
    ResultInfo ri = null;
    try
    {
      List<RowData> checkRows = new ArrayList<>(referenceRows.size());
      if (sql != null)
      {
        rs = checkStatement.executeQuery(sql);
        ri = new ResultInfo(rs.getMetaData(), toSync);
        ri.setPKColumns(this.pkColumns);
        ri.setUpdateTable(this.tableToSync);

        reader = RowDataReaderFactory.createReader(ri, toSync);
        while (rs.next())
        {
          RowData r = reader.read(rs, false);
          checkRows.add(r);
          if (cancelExecution) break;
        }
      }
      else
      {
        ri = info.createCopy();
        ri.setPKColumns(this.pkColumns);
        TableIdentifier target = this.referenceTable.createCopy();
        target.setSchema(targetSchema);
        if (target.getSchema() == null)
        {
          target.setSchema(toSync.getCurrentSchema());
        }
        ri.setUpdateTable(this.referenceTable);
      }

      if (currentRowNumber == 0)
      {
        comparer.setResultInfo(ri);
      }

      for (RowData toInsert : referenceRows)
      {
        if (cancelExecution) break;

        int i = findRowByPk(checkRows, info, toInsert, ri);

        currentRowNumber ++;
        if (this.monitor != null && (currentRowNumber % progressInterval == 0))
        {
          monitor.setCurrentRow(currentRowNumber, -1);
        }

        Writer writerToUse = null;
        comparer.setRows(toInsert, i > -1 ? checkRows.get(i) : null);

        Set<String> toIgnore = CollectionUtil.caseInsensitiveSet();
        toIgnore.addAll(columnsToIgnore);
        toIgnore.addAll(realPKCols);
        comparer.ignoreColumns(toIgnore, ri);

        Set<String> toExclude = CollectionUtil.caseInsensitiveSet();
        if (excludeRealPK && CollectionUtil.isNonEmpty(realPKCols))
        {
          toExclude.addAll(realPKCols);
        }

        if (excludeIgnoredColumns)
        {
          toExclude.addAll(columnsToIgnore);
        }

        if (CollectionUtil.isNonEmpty(toExclude))
        {
          comparer.excludeColumns(toExclude, info);
        }

        String output = comparer.getMigration(currentRowNumber);

        if (output != null)
        {
          if (i > -1)
          {
            // Row is present, check for modifications
            if (firstUpdate)
            {
              firstUpdate = false;
              writeHeader(updateWriter);
            }
            writerToUse = updateWriter;
          }
          else
          {
            if (firstInsert)
            {
              firstInsert = false;
              writeHeader(insertWriter);
            }
            writerToUse = insertWriter;
          }

          writerToUse.write(output);
          if (comparer.isTypeXml())
          {
            writerToUse.write(lineEnding);
          }
          else
          {
            writerToUse.write(lineEnding + lineEnding);
          }
        }
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("TableDataDiff.checkRows()", "Error when running check SQL " + sql, e);
      throw e;
    }
    finally
    {
      SqlUtil.closeResult(rs);
      if (reader != null)
      {
        reader.closeStreams();
      }
    }
  }

  protected int findRowByPk(List<RowData> reference, ResultInfo refInfo, RowData toFind, ResultInfo findInfo)
  {
    if (findInfo == null) return -1;
    if (toFind == null) return -1;

    int index = 0;
    for (RowData refRow : reference)
    {
      int equalCount = 0;
      for (ColumnIdentifier col : pkColumns)
      {
        int fIndex = findInfo.findColumn(col.getColumnName());
        int rIndex = refInfo.findColumn(col.getColumnName());
        Object ref = refRow.getValue(rIndex);
        Object find = toFind.getValue(fIndex);
        if (RowData.objectsAreEqual(ref, find)) equalCount ++;
      }
      if (equalCount == pkColumns.size()) return index;
      index ++;
    }
    return -1;
  }

  /**
   * Creates the Statement to retrieve the corresponding rows of the target
   * table based on the data retrieved from the reference table
   *
   * @param rows the data from the reference table
   * @param info the result set definition of the reference table
   * @return the statement to select the rows
   */
  private String buildCheckSql(List<RowData> rows, ResultInfo info)
  {
    if (tableToSync == null) return null;

    StringBuilder sql = new StringBuilder(150);
    sql.append("SELECT ");
    for (int i=0; i < info.getColumnCount(); i++)
    {
      ColumnIdentifier targetCol = findTargetColumn(info.getColumn(i));
      if (targetCol == null) continue;
      if (i > 0) sql.append(',');
      sql.append(targetCol.getColumnName(toSync));
    }
    sql.append(" FROM ");
    sql.append(this.tableToSync.getTableExpression(toSync));
    sql.append(" WHERE ");

    for (int row=0; row < rows.size(); row++)
    {
      if (row > 0) sql.append (" OR ");
      sql.append('(');
      int pkCount = 0;
      for (int c=0; c < info.getColumnCount(); c++)
      {
        ColumnIdentifier column = info.getColumn(c);
        if (pkColumns.contains(column))
        {
          if (pkCount > 0) sql.append(" AND ");
          ColumnIdentifier targetCol = findTargetColumn(column);
          sql.append(targetCol.getColumnName(toSync));
          sql.append(" = ");
          Object value = rows.get(row).getValue(c);
          ColumnData data = new ColumnData(value, column);
          sql.append(formatter.getDefaultLiteral(data));
          pkCount++;
        }
      }
      sql.append(") ");
    }
    return sql.toString();
  }

  private ColumnIdentifier findTargetColumn(ColumnIdentifier toFind)
  {
    String cname = SqlUtil.removeObjectQuotes(toFind.getColumnName());
    for (ColumnIdentifier col : toSyncDef.getColumns())
    {
      if (SqlUtil.removeObjectQuotes(col.getColumnName()).equalsIgnoreCase(cname))
      {
        return col;
      }
    }
    return null;
  }

  private void writeHeader(Writer out)
    throws IOException
  {
    String genInfo = "Generated by " + ResourceMgr.TXT_PRODUCT_NAME + " at: " + StringUtil.getCurrentTimestampWithTZString();
    if (comparer.isTypeXml())
    {
      out.write("<?xml version=\"1.0\" encoding=\"" + this.encoding + "\"?>");
      out.write(lineEnding);
      out.write("<!-- ");
      out.write(genInfo);
      out.write(" -->");
      out.write(lineEnding);
      writeTableNameTag(out, "table-data-diff", tableToSync);
      out.write(">");
      out.write(lineEnding);
    }
    else
    {
      out.write("-- ----------------------------------------------------------------");
      out.write(lineEnding);
      out.write("-- ");
      out.write(genInfo);
      out.write(lineEnding);
      out.write("-- ----------------------------------------------------------------");
      out.write(lineEnding);
    }
  }

  public static void writeTableNameTag(Writer out, String tag, TableIdentifier table)
    throws IOException
  {
    out.write("<" + tag);
    out.write(" name=\"");
    out.write(table.getRawTableName());
    out.write("\"");
    if (StringUtil.isNonEmpty(table.getRawSchema()))
    {
      out.write(" schema=\"");
      out.write(table.getRawSchema());
      out.write("\"");
    }
    if (StringUtil.isNonEmpty(table.getRawCatalog()))
    {
      out.write(" catalog=\"");
      out.write(table.getRawCatalog());
      out.write("\"");
    }
    out.write(">");
  }

  private void writeEnd(Writer out)
    throws IOException
  {
    if (comparer.isTypeXml())
    {
      out.write("</table-data-diff>");
      out.write(lineEnding);
    }
  }
}
