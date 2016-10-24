/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.io.IOException;
import java.io.Writer;
import java.sql.Types;

/**
 *
 * @author Thomas Kellerer
 */
public class DataStoreWriter
  extends Writer
{
  private final StringBuilder currentLine;
  private final DataStore result;

  public DataStoreWriter(String columnName)
  {
    currentLine = new StringBuilder(100);
    result = new DataStore(new String[] { columnName }, new int[] { Types.VARCHAR });
  }

  @Override
  public void write(char[] cbuf, int off, int len)
    throws IOException
  {
    if (cbuf == null) return;

    currentLine.ensureCapacity(currentLine.length() + len + 10);
    char last = 0;

    for (int i=off; i < len; i++)
    {
      char ch = cbuf[i];
      if (ch != '\n' && ch != '\r')
      {
        currentLine.append(ch);
        last = ch;
      }
      else if (last != '\n')
      {
        appendCurrentLine(true);
        last = '\n';
      }
    }
  }

  @Override
  public void flush()
    throws IOException
  {
    appendCurrentLine(false);
    result.resetStatus();
  }

  @Override
  public void close()
    throws IOException
  {
    flush();
  }

  public DataStore getResult()
  {
    return result;
  }

  private void appendCurrentLine(boolean addEmpty)
  {
    int lineLength = currentLine.indexOf("\n");
    String line = null;

    if (addEmpty == false && lineLength == 0) return;

    if (lineLength < 1)
    {
      line = currentLine.toString();
      currentLine.setLength(0);
    }
    else
    {
      line = currentLine.substring(0, lineLength - 1);
      currentLine.delete(0, lineLength - 1);
    }
    int row = result.addRow();
    result.setValue(row, 0, line);
  }

  public void reset()
  {
    result.reset();
    currentLine.setLength(0);
  }
}
