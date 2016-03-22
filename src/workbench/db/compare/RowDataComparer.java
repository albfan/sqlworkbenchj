/*
 * RowDataComparer.java
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
package workbench.db.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.BlobMode;
import workbench.db.exporter.ExportType;
import workbench.db.exporter.SqlRowDataConverter;
import workbench.db.exporter.XmlRowDataConverter;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.WbFile;

/**
 * Compare two {@link workbench.storage.RowData} objects to check for equality.
 * Used to generate the approriate SQL scripts when comparing the data from
 * two tables.
 *
 * @author Thomas Kellerer
 */
public class RowDataComparer
{
  private RowData migrationData;
  private boolean targetWasNull;
  private WbConnection targetDb;
  private ResultInfo resultInfo;
  private BlobMode blobMode;
  private SqlRowDataConverter sqlConverter;
  private XmlRowDataConverter xmlConverter;
  private String sqlDateLiteral;
  private WbFile baseDir;

  /**
   * Compares two database rows.
   */
  public RowDataComparer()
  {
  }

  public void setBaseDir(WbFile dir)
  {
    baseDir = dir;
    if (sqlConverter != null)
    {
      sqlConverter.setOutputFile(dir);
    }
    if (xmlConverter != null)
    {
      xmlConverter.setOutputFile(dir);
    }
  }

  public void setSqlDateLiteralType(String type)
  {
    sqlDateLiteral = type;
    if (sqlConverter != null)
    {
      sqlConverter.setDateLiteralType(type);
    }
  }

  public void setTypeSql()
  {
    sqlConverter = new SqlRowDataConverter(targetDb);
    sqlConverter.setBlobMode(blobMode);
    if (resultInfo != null) sqlConverter.setResultInfo(resultInfo);
    if (sqlDateLiteral != null) sqlConverter.setDateLiteralType(sqlDateLiteral);
    if (blobMode != null)
    {
      sqlConverter.setBlobMode(blobMode);
    }
    sqlConverter.setOutputFile(baseDir);
    xmlConverter = null;
  }

  public boolean isTypeXml()
  {
    return xmlConverter != null;
  }

  public void setTypeXml(boolean useCDATA)
  {
    xmlConverter = new XmlRowDataConverter();
    xmlConverter.setUseVerboseFormat(false);
    xmlConverter.setUseDiffFormat(true);
    xmlConverter.setWriteClobToFile(false);
    xmlConverter.setUseCDATA(useCDATA);
    xmlConverter.setOriginalConnection(targetDb);
    if (resultInfo != null) xmlConverter.setResultInfo(resultInfo);
    sqlConverter = null;
    xmlConverter.setOutputFile(baseDir);
  }

  /**
   * Define the Blob mode when generating SQL statements.
   */
  public void setSqlBlobMode(BlobMode mode)
  {
    blobMode = mode;
    if (sqlConverter != null)
    {
      sqlConverter.setBlobMode(mode);
    }
  }

  public void setConnection(WbConnection target)
  {
    targetDb = target;
  }

  public void setResultInfo(ResultInfo ri)
  {
    resultInfo = ri;
    if (sqlConverter != null)
    {
      sqlConverter.setResultInfo(ri);
    }

    if (xmlConverter != null)
    {
      xmlConverter.setResultInfo(ri);
    }
  }

  public void setRows(RowData referenceRow, RowData targetRow)
  {
    int cols = referenceRow.getColumnCount();
    if (targetRow == null)
    {
      targetWasNull = true;
      migrationData = referenceRow.createCopy();
      migrationData.resetStatus();
      migrationData.setNew();
    }
    else
    {
      targetWasNull = false;
      migrationData = targetRow.createCopy();
      migrationData.resetStatus();

      int tcols = migrationData.getColumnCount();
      if (cols != tcols) throw new IllegalArgumentException("Column counts must match!");

      for (int i=0; i < cols; i++)
      {
        // if the value passed to the target row is
        // identical to the existing value, this will
        // not change the state of the RowData
        migrationData.setValue(i, referenceRow.getValue(i));
      }
    }
  }

  public void ignoreColumns(Collection<String> columnNames, ResultInfo info)
  {
    if (columnNames == null || columnNames.isEmpty()) return;
    for (int i=0; i < info.getColumnCount(); i++)
    {
      if (columnNames.contains(info.getColumnName(i)))
      {
        migrationData.resetStatusForColumn(i);
      }
    }
  }

  public void excludeColumns(Set<String> columns, ResultInfo info)
  {
    ColumnIdentifier[] resultColumns = info.getColumns();
    List<ColumnIdentifier> toExport = new ArrayList<>();
    for (ColumnIdentifier col : resultColumns)
    {
      if (!columns.contains(col.getColumnName()))
      {
        toExport.add(col);
      }
    }
    sqlConverter.setColumnsToExport(toExport);
  }

  /**
   * Returns the representation for the changes between the rows
   * defined by setRows().
   * <br/>
   * Depending on the type this might be a SQL statement (INSERT, UPDATE)
   * or a XML fragment as returned by XmlRowDataConverter.
   *
   * @param rowNumber
   */
  public String getMigration(long rowNumber)
  {
    StringBuilder result = null;
    if (sqlConverter != null)
    {
      if (targetWasNull)
      {
        sqlConverter.setIgnoreColumnStatus(true);
        sqlConverter.setType(ExportType.SQL_INSERT);
      }
      else
      {
        sqlConverter.setIgnoreColumnStatus(false);
        sqlConverter.setType(ExportType.SQL_UPDATE);
      }
      result = sqlConverter.convertRowData(migrationData, rowNumber);
    }
    if (xmlConverter != null)
    {
      if (targetWasNull)
      {
        CharSequence row = xmlConverter.convertRowData(migrationData, rowNumber);
        result = new StringBuilder(row.length() + 20);
        result.append("<insert>");
        result.append(row);
        result.append("</insert>");
      }
      else if (migrationData.isModified())
      {
        StringBuilder row = xmlConverter.convertRowData(migrationData, rowNumber);
        result = new StringBuilder(row.length() + 20);
        result.append("<update>");
        result.append(row);
        result.append("</update>");
      }
    }
    if (result == null) return null;
    return result.toString();
  }

}
