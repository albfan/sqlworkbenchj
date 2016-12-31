/*
 * ConstantColumnValues.java
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
package workbench.db.importer;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.ColumnData;

import workbench.sql.VariablePool;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.ConverterException;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;
import workbench.util.WbFile;

/**
 * A class to parse column constants for the DataImporter
 *
 * @author Thomas Kellerer
 */
public class ConstantColumnValues
{
  public static final String VAR_NAME_CURRENT_FILE_PATH = "_wb_import_file_path";
  public static final String VAR_NAME_CURRENT_FILE_NAME = "_wb_import_file_name";
  public static final String VAR_NAME_CURRENT_FILE_DIR = "_wb_import_file_dir";

  private List<String> originalDefinition;
  private ValueConverter originalConverter;
  private TableIdentifier currentTable;

  private List<ColumnData> columnValues;
  private final Map<Integer, ValueStatement> selectStatements = new HashMap<>();
  private final Map<String, String> variables = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
  private boolean usesVariables;

  /**
   * Parses a parameter value for column value definitions.
   * e.g. description=something,firstname=arthur
   * The values from the Commandline are converted to the correct
   * datatype in the targettable.
   * @throws SQLException if the target table was not found
   * @throws ConverterException if a value could not be converted to the target data type
   *
   */
  public ConstantColumnValues(List<String> entries, WbConnection con, String tablename, ValueConverter converter)
    throws SQLException, ConverterException
  {
    if (StringUtil.isNonEmpty(tablename))
    {
      currentTable = new TableIdentifier(tablename, con);
      List<ColumnIdentifier> tableColumns = con.getMetadata().getTableColumns(currentTable, false);
      if (tableColumns.isEmpty()) throw new SQLException("Table \"" + tablename + "\" not found!");
      init(entries, tableColumns, converter);
    }
    else
    {
      originalDefinition = new ArrayList<>(entries);
      originalConverter = converter;
    }
  }

  /**
   * For Unit-Testing without a Database Connection
   */
  ConstantColumnValues(List<String> entries, List<ColumnIdentifier> targetColumns)
    throws SQLException, ConverterException
  {
    originalDefinition = new ArrayList<>(entries);
    originalConverter = new ValueConverter();
    init(entries, targetColumns, originalConverter);
  }

  protected final void init(List<String> entries, List<ColumnIdentifier> tableColumns, ValueConverter converter)
    throws SQLException, ConverterException
  {
    usesVariables = false;
    columnValues = new ArrayList<>(entries.size());
    selectStatements.clear();
    variables.clear();

    Set<String> varNames = CollectionUtil.caseInsensitiveSet(VAR_NAME_CURRENT_FILE_DIR, VAR_NAME_CURRENT_FILE_NAME, VAR_NAME_CURRENT_FILE_PATH);

    for (String entry : entries)
    {
      int pos = entry.indexOf('=');
      if (pos < 0) continue;
      String colname = entry.substring(0, pos);
      String value = entry.substring(pos + 1);

      ColumnIdentifier col = findColumn(tableColumns, colname);

      if (col != null)
      {
        Object data = null;
        if (StringUtil.isEmptyString(value))
        {
          LogMgr.logWarning("ConstanColumnValues.init()", "Empty value for column '" + col + "' assumed as NULL");
        }
        else
        {
          if (value.startsWith("${") || value.startsWith("$@{"))
          {
            // DBMS Function call
            data = value.trim();
          }
          else
          {
            if (SqlUtil.isCharacterType(col.getDataType()) &&
                value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'')
            {
              value = value.substring(1, value.length() - 1);
            }
            data = converter.convertValue(value, col.getDataType());
          }
        }

        if (SqlUtil.isCharacterType(col.getDataType()) && StringUtil.isNonBlank(value) && !usesVariables)
        {
          usesVariables = VariablePool.getInstance().containsVariable(value, varNames);
        }

        this.columnValues.add(new ColumnData(data, col));
      }
      else
      {
        throw new SQLException("Column '" + colname + "' not found in target table!");
      }
    }
  }

  public void initFileVariables(TableIdentifier table, WbConnection con, File currentFile)
    throws SQLException, ConverterException
  {
    variables.clear();
    if (currentFile == null) return;

    if ( (columnValues == null && originalDefinition != null) || (!TableIdentifier.tablesAreEqual(currentTable, table, con)))
    {
      LogMgr.logDebug("ConstantColumnValues.initFileVariables()", "Inititializing new table columns for table: " + table.getTableExpression());
      List<ColumnIdentifier> tableColumns = con.getMetadata().getTableColumns(table, false);
      init(originalDefinition, tableColumns, originalConverter);
      currentTable = table.createCopy();
    }

    LogMgr.logDebug("ConstantColumnValues.initFileVariables()", "Inititializing variables for file: " + currentFile.getAbsolutePath());

    WbFile dir = new WbFile(currentFile.getAbsoluteFile().getParent());
    variables.put(VAR_NAME_CURRENT_FILE_DIR, dir.getFullPath());

    WbFile f = new WbFile(currentFile);
    variables.put(VAR_NAME_CURRENT_FILE_PATH, f.getFullPath());
    variables.put(VAR_NAME_CURRENT_FILE_NAME, f.getName());
  }

  private ColumnIdentifier findColumn(List<ColumnIdentifier> columns, String name)
  {
    for (ColumnIdentifier col : columns)
    {
      if (col.getColumnName().equalsIgnoreCase(name)) return col;
    }
    return null;
  }

  public String getFunctionLiteral(int index)
  {
    if (!isFunctionCall(index)) return null;
    String value = (String)this.getValue(index);

    // The function call is enclosed in ${...}
    return value.substring(2, value.length() - 1);
  }

  public List<String> getInputColumnsForFunction(int index)
  {
    String func = getFunctionLiteral(index);
    if (func == null) return null;
    List<String> args = SqlUtil.getFunctionParameters(func);
    List<String> result = CollectionUtil.arrayList();
    for (String f : args)
    {
      String arg = StringUtil.trimQuotes(f);
      if (arg.startsWith("$"))
      {
        result.add(arg.substring(1));
      }
    }
    return result;
  }

  public ValueStatement getStatement(int index)
  {
    ValueStatement stmt = selectStatements.get(index);
    if (stmt == null)
    {
      if (!isSelectStatement(index)) return null;
      String value = (String)getValue(index);
      String sql = value.substring(3, value.length() - 1);
      stmt = new ValueStatement(sql);
      selectStatements.put(index, stmt);
    }
    return stmt;
  }

  public boolean isSelectStatement(int index)
  {
    Object value = getValue(index);
    if (value == null) return false;

    if (value instanceof String)
    {
      String f = (String)value;
      return f.startsWith("$@{") && f.endsWith("}");
    }
    return false;
  }

  public boolean isFunctionCall(int index)
  {
    Object value = getValue(index);
    if (value == null) return false;

    if (value instanceof String)
    {
      String f = (String)value;
      return f.startsWith("${") && f.endsWith("}");
    }
    return false;
  }

  public int getColumnCount()
  {
    if (columnValues == null)
    {
      if (originalDefinition == null) return 0;
      return originalDefinition.size();
    }
    return columnValues.size();
  }

  public ColumnIdentifier getColumn(int index)
  {
    return columnValues.get(index).getIdentifier();
  }

  public ColumnData getColumnData(int index)
  {
    return replaceVariables(columnValues.get(index));
  }

  public ColumnData getColumnData(String columnName)
  {
    for (ColumnData col : columnValues)
    {
      if (col.getIdentifier().getColumnName().equalsIgnoreCase(columnName))
      {
        return replaceVariables(col);
      }
    }
    return null;
  }

  public Object getValue(int index)
  {
    ColumnData data = replaceVariables(columnValues.get(index));
    return data.getValue();
  }

  private ColumnData replaceVariables(ColumnData data)
  {
    if (data == null) return null;
    if (usesVariables && variables.size() > 0 && SqlUtil.isCharacterType(data.getIdentifier().getDataType()))
    {
      String value = (String)data.getValue();
      String realValue = VariablePool.getInstance().replaceAllParameters(value, variables);
      return data.createCopy(realValue);
    }
    return data;
  }

  public boolean removeColumn(ColumnIdentifier col)
  {
    if (columnValues == null) return false;
    if (col == null) return false;

    int index = -1;
    for (int i=0; i < columnValues.size(); i++)
    {
      if (columnValues.get(i).getIdentifier().equals(col))
      {
        index = i;
        break;
      }
    }

    if (index > -1)
    {
      this.columnValues.remove(index);
    }
    return (index > -1);
  }

  public void setParameter(PreparedStatement pstmt, int statementIndex, int columnIndex)
    throws SQLException
  {
    Object value = getValue(columnIndex);

    // If the column value is a function call, this will not
    // be used in a prepared statement. It is expected that the caller
    // (that prepared the statement) inserted the literal value of the
    // function call into the SQL instead of a ? placeholder
    if (!isFunctionCall(columnIndex))
    {
      pstmt.setObject(statementIndex, value);
    }
  }

  public void done()
  {
    for (ValueStatement stmt : selectStatements.values())
    {
      if (stmt != null) stmt.done();
    }
  }
}

