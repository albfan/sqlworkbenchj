/*
 * DataStoreReplacer.java
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import workbench.gui.editor.SearchAndReplace;
import workbench.log.LogMgr;
import workbench.util.ConverterException;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to replace values in the data of a DataStore.
 *
 * @author Thomas Kellerer
 */
public class DataStoreReplacer
{
  private DataStore client;
  private Position lastFoundPosition;
  private Pattern lastPattern;
  private String currentReplacementValue;
  private int[] selectedRows;
  private int lastSelectedRowIndex;
  private String lastCriteria;
  private boolean isRegexSearch;

  public DataStoreReplacer()
  {
  }

  /**
   * Create a new DataStoreReplacer for the given DataStore.
   *
   * The datastore is not checked if it is updateable!
   *
   * @param store the DataStore to search and replace in
   */
  public DataStoreReplacer(DataStore store)
  {
    setDataStore(store);
  }

  /**
   * Define the DataStore to be used by this replacer.
   *
   * @param ds
   */
  public final void setDataStore(DataStore ds)
  {
    this.client = ds;
    this.reset();
  }
  /**
   * Limit all search and replace actions to the selected rows.
   *
   * To reset search & replace in the selected rows, setSelecteRows()
   * has to be called again with a null value.
   *
   * @param rows the selected rows to be searched, null to reset row selection
   */
  public void setSelectedRows(int[] rows)
  {
    this.selectedRows = rows;
    this.lastSelectedRowIndex = 0;
  }

  public Position getLastFoundPosition()
  {
    return this.lastFoundPosition;
  }

  public String getLastCriteria()
  {
    return this.lastCriteria;
  }

  /**
   * Find the given text in the datastore.
   *
   * @param text the text to search for
   * @param ignoreCase if true, search is case-insesitive
   * @param wholeWord if true, only text in word bounderies is found
   * @param useRegex treat the text as a regular expression
   * @return the position where the text was found
   *
   * @see workbench.gui.editor.SearchAndReplace#getSearchExpression(String, boolean, boolean, boolean)
   */
  public Position find(String text, boolean ignoreCase, boolean wholeWord, boolean useRegex)
    throws PatternSyntaxException
  {
    lastCriteria = text;
    lastFoundPosition = Position.NO_POSITION;
    lastSelectedRowIndex = 0;
    if (StringUtil.isEmptyString(text)) return Position.NO_POSITION;
    this.isRegexSearch = useRegex;
    String expression = SearchAndReplace.getSearchExpression(text, ignoreCase, wholeWord, useRegex);
    Pattern p = null;
    try
    {
      p = Pattern.compile(expression);
    }
    catch (PatternSyntaxException e)
    {
      LogMgr.logError("DataStoreReplacer.find()", "Error compiling search pattern", e);
      throw e;
    }
    return findPattern(p);
  }

  /**
   * Find the next occurance of the search string.
   *
   * This returns NO_POSITION if find(String, boolean) has not been called before.
   *
   * @return the position of the next occurance
   */
  public Position findNext()
  {
    if (lastPattern == null)
    {
      reset();
      return lastFoundPosition;
    }
    return findPattern(lastPattern);
  }

  public void reset()
  {
    lastPattern = null;
    lastCriteria = null;
    currentReplacementValue = null;
    lastSelectedRowIndex = 0;
    lastFoundPosition = Position.NO_POSITION;
  }

  private Position findPattern(Pattern p)
  {
    int startRow = 0;
    int startCol = 0;

    int rowCount = this.client.getRowCount();
    int colCount = this.client.getColumnCount();

    if (this.lastFoundPosition.isValid())
    {
      startRow = this.lastFoundPosition.getRow();
      startCol = this.lastFoundPosition.getColumn() + 1;
      if (startCol >= colCount)
      {
        startCol = 0;
        startRow ++;
      }
    }

    this.lastPattern = p;

    int startIndex = startRow;

    if (this.selectedRows != null)
    {
      startIndex = this.lastSelectedRowIndex;
      rowCount = this.selectedRows.length;
    }

    if (startIndex < 0) startIndex = 0;

    for (int index = startIndex; index < rowCount; index++)
    {
      int row = index;
      if (selectedRows != null)
      {
        this.lastSelectedRowIndex = index;
        row = this.selectedRows[index];
      }

      for (int col=startCol; col < colCount; col++)
      {
        int type = client.getColumnType(col);
        if (SqlUtil.isBlobType(type)) continue;
        String colValue = client.getValueAsString(row, col);
        if (StringUtil.isEmptyString(colValue)) continue;
        Matcher m = p.matcher(colValue);
        if (m.find())
        {
          this.lastFoundPosition = new Position(row, col);
          return this.lastFoundPosition;
        }
      }
      startCol = 0;
    }

    return Position.NO_POSITION;
  }

  /**
   * Replace all occurances of a value with the given replacement value.
   *
   * @param text the value to search for
   * @param replacement the replacement value
   * @param rows if not null search and replace is only done in these rows
   * @param ignoreCase should the search pattern be applied case-insensitive
   * @param wholeWord if true, only whole words are found
   * @param useRegex if true, expression is treated as a regular expression
   *
   * @return the number of occurances replaced
   * @see workbench.gui.editor.SearchAndReplace#getSearchExpression(String, boolean, boolean, boolean)
   * @see workbench.gui.editor.SearchAndReplace#fixSpecialReplacementChars(String, boolean)
   */
  public int replaceAll(String text, String replacement, int[] rows, boolean ignoreCase, boolean wholeWord, boolean useRegex)
    throws ConverterException, PatternSyntaxException
  {
    reset();
    String expression = SearchAndReplace.getSearchExpression(text, ignoreCase, wholeWord, useRegex);

    this.isRegexSearch = useRegex;

    currentReplacementValue = SearchAndReplace.fixSpecialReplacementChars(replacement, isRegexSearch);

    int replaced = 0;
    Pattern p = Pattern.compile(expression);

    this.setSelectedRows(rows);

    Position pos = findPattern(p);

    while (pos.isValid())
    {
      replaceValueAt(pos, this.currentReplacementValue, this.lastPattern);
      replaced ++;
      pos = findNext();
    }
    return replaced;
  }

  public boolean replaceCurrent(String replacement)
    throws ConverterException
  {
    if (this.lastFoundPosition == null) return false;

    if (this.lastFoundPosition.isValid())
    {
      currentReplacementValue = SearchAndReplace.fixSpecialReplacementChars(replacement, isRegexSearch);
      replaceValueAt(lastFoundPosition, this.currentReplacementValue, this.lastPattern);
      return true;
    }
    return false;
  }

  private void replaceValueAt(Position pos, String replacement, Pattern p)
    throws ConverterException
  {
    String value = this.client.getValueAsString(pos.getRow(), pos.getColumn());
    if (!StringUtil.isEmptyString(value))
    {
      Matcher m = p.matcher(value);
      String newValue = m.replaceAll(replacement);
      try
      {
        client.setInputValue(pos.getRow(), pos.getColumn(), newValue);
      }
      catch (ConverterException e)
      {
        LogMgr.logError("DataStoreReplacer.replaceAll()", "Could not convert the replacement data", e);
        throw e;
      }
    }
  }
}
