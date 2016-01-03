/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;


/**
 *
 * @author Thomas Kellerer
 */
public class CsvLineReader
  implements Closeable
{
  private final BufferedReader reader;
  private final char quoteChar;
  private final QuoteEscapeType escapeType;
  private final boolean enableMultiline;
  private final String lineEnd;
  private boolean ignoreEmptyLines;

  public CsvLineReader(BufferedReader originalReader, char quote, QuoteEscapeType type, boolean multine, String lineEnd)
  {
    this.reader = originalReader;
    this.quoteChar = quote;
    this.escapeType = type;
    this.enableMultiline = multine;
    this.lineEnd = lineEnd == null ? StringUtil.LINE_TERMINATOR : lineEnd;
  }

  public boolean isIgnoreEmptyLines()
  {
    return ignoreEmptyLines;
  }

  public void setIgnoreEmptyLines(boolean flag)
  {
    this.ignoreEmptyLines = flag;
  }

  public String readLine()
    throws IOException
  {
    String line = readNextLine();

    if (ignoreEmptyLines && StringUtil.isEmptyString(line))
    {
      while (line != null && StringUtil.isEmptyString(line))
      {
        line = readNextLine();
      }
    }
    return line;
  }

  private String readNextLine()
    throws IOException
  {
    String line = reader.readLine();
    if (enableMultiline && StringUtil.hasOpenQuotes(line, quoteChar, escapeType))
    {
      line = readContinuationLines(line, quoteChar, escapeType);
    }
    return line;
  }

  private String readContinuationLines(String currentLine, char quoteChar, QuoteEscapeType escapeType)
    throws IOException
  {
    String result = currentLine == null ? "" : currentLine;
    String line;
    while (StringUtil.hasOpenQuotes(result, quoteChar, escapeType) && (line = reader.readLine()) != null)
    {
      if (line != null)
      {
        result += lineEnd + line;
      }
    }
    return result.trim();
  }

  @Override
  public void close()
    throws IOException
  {
    reader.close();
  }

}
