/*
 * DataStoreImporter.java
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
package workbench.db.importer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import workbench.interfaces.ImportFileParser;
import workbench.interfaces.Interruptable;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;

import workbench.util.ClipboardFile;
import workbench.util.MessageBuffer;

/**
 * A DataReceiver to import text files (either from a file or from a String)
 * into a DataStore.
 * @author Thomas Kellerer
 */
public class DataStoreImporter
  implements DataReceiver, Interruptable
{
  private DataStore target;
  private RowDataProducer source;
  private RowActionMonitor rowMonitor;
  private JobErrorHandler errorHandler;
  private int currentRowNumber;

  public DataStoreImporter(DataStore data, RowActionMonitor monitor, JobErrorHandler handler)
  {
    this.target = data;
    this.rowMonitor = monitor;
    if (this.rowMonitor != null)
    {
      this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_INSERT);
    }
    this.errorHandler = handler;
  }

  @Override
  public void processFile(StreamImporter stream)
    throws SQLException, IOException
  {
  }

  public void startImport()
  {
    try
    {
      this.currentRowNumber = 0;
      this.source.start();
    }
    catch (Exception e)
    {
      LogMgr.logError("DataStoreImporter.startImport()", "Error ocurred during import", e);
    }
  }

  @Override
  public boolean isTransactionControlEnabled()
  {
    return true;
  }

  @Override
  public void setTableList(List<TableIdentifier> targetTables)
  {
    // Nothing to do as only one table can be imported
  }

  @Override
  public void deleteTargetTables()
    throws SQLException
  {
    // Nothing to do as only one table can be imported
  }

  @Override
  public void beginMultiTable()
    throws SQLException
  {
    // Nothing to do as only one table can be imported
  }

  @Override
  public void endMultiTable()
  {
    // Nothing to do as only one table can be imported
  }

  @Override
  public boolean getCreateTarget()
  {
    return false;
  }

  @Override
  public boolean shouldProcessNextRow()
  {
    return true;
  }

  @Override
  public void nextRowSkipped()
  {
  }

  public void importString(String contents)
  {
    importString(contents, "\t", "\"");
  }

  public void importString(String contents, String delimiter, String quoteChar)
  {
    ClipboardFile file = new ClipboardFile(contents);
    setImportOptions(file, ProducerFactory.ImportType.Text, new DefaultImportOptions(), new DefaultTextImportOptions(delimiter, quoteChar));
  }

  public void importString(String content, ImportOptions options, TextImportOptions textOptions)
  {
    ClipboardFile file = new ClipboardFile(content);
    setImportOptions(file, ProducerFactory.ImportType.Text, options, textOptions);
  }

  public void setImportOptions(File file, ProducerFactory.ImportType type, ImportOptions generalOptions, TextImportOptions textOptions)
  {
    ResultInfo info = target.getResultInfo();

    ProducerFactory factory = new ProducerFactory(file);
    factory.setTextOptions(textOptions);
    factory.setGeneralOptions(generalOptions);
    factory.setType(type);
    factory.setTargetTable(info.getUpdateTable());
    factory.setConnection(target.getOriginalConnection());

    this.source = factory.getProducer();
    ((ImportFileParser)source).setIgnoreMissingColumns(true);

    try
    {
      List<ColumnIdentifier> targetColumns = Arrays.asList(info.getColumns());
      List<ColumnIdentifier> fileColumns = factory.getFileColumns();
      factory.setColumnMap(fileColumns, targetColumns);
    }
    catch (Exception e)
    {
      LogMgr.logError("DataStoreImporter.setImportOptions()", "Error setting import columns", e);
    }
    this.source.setReceiver(this);
    this.source.setAbortOnError(false);
    this.source.setErrorHandler(this.errorHandler);
  }

  public MessageBuffer getMessage()
  {
    return this.source.getMessages();
  }

  @Override
  public void processRow(Object[] row) throws SQLException
  {
    RowData data = new RowData(row.length);
    for (int i = 0; i < row.length; i++)
    {
      data.setValue(i, row[i]);
    }
    target.addRow(data);
    currentRowNumber ++;
    if (this.rowMonitor != null) this.rowMonitor.setCurrentRow(currentRowNumber, -1);
  }

  @Override
  public void recordRejected(String record, long importRow, Throwable error)
  {

  }

  @Override
  public void setTableCount(int total)
  {
  }

  @Override
  public void setCurrentTable(int current)
  {
  }

  @Override
  public void setTargetTable(TableIdentifier table, List<ColumnIdentifier> columns, File currentFile)
    throws SQLException
  {
    if (columns.size() != this.target.getColumnCount())
    {
      if (errorHandler != null) errorHandler.fatalError(ResourceMgr.getString("ErrImportInvalidColumnStructure"));
      throw new SQLException("Invalid column count");
    }
  }

  @Override
  public void tableImportFinished()
    throws SQLException
  {

  }

  @Override
  public void importFinished()
  {
  }

  @Override
  public void importCancelled()
  {
  }

  @Override
  public void tableImportError()
  {
  }

  @Override
  public void cancelExecution()
  {
    this.source.cancel();
  }

  @Override
  public boolean confirmCancel()
  {
    return true;
  }

}
