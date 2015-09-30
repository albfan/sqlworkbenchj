/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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
package workbench.db.importer.detector;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.util.CsvLineParser;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.MemoryWatcher;
import workbench.util.QuoteEscapeType;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;

/**
 * A class to detect a table structure from a CSV file.
 *
 * @author Thomas Kellerer
 */
public class TextFileTableDetector
  extends TableDetector
{
  private String encoding;
  private boolean enableMultiline;

  private CsvLineParser parser;

  public TextFileTableDetector(File importFile, String delimiter, String quoteChar, String dateFmt, String timestampFmt, boolean containsHeader, int numLines, String fileEncoding)
  {
    inputFile = importFile;
    withHeader = containsHeader;
    sampleSize = numLines;
    encoding = fileEncoding;

    converter = new ValueConverter(dateFmt, timestampFmt);
    converter.setLogWarnings(false);

    char quote = 0;
    if (StringUtil.isNonEmpty(quoteChar))
    {
      quote = quoteChar.charAt(0);
    }
    parser = new CsvLineParser(delimiter, quote);
    parser.setReturnEmptyStrings(true);
    parser.setTrimValues(true);
  }

  public void setEnableMultiline(boolean flag)
  {
    this.enableMultiline = flag;
  }

  public void setQuoteEscape(QuoteEscapeType type)
  {
    parser.setQuoteEscaping(type);
  }

  @Override
  public void analyzeFile()
  {
    success = false;
    List<String> lines = readLines();

    int minSize = withHeader ? 2 : 1;
    int line = 0;

    if (lines.size() < minSize) return;

    initColumns(lines.get(line));

    if (columns.isEmpty()) return;

    if (withHeader)
    {
      line++;
    }

    String firstDataLine = lines.get(line);

    // try to find the data type of each column from the first line
    // the type detected there will be the first type to be tested for the subsequent lines
    initTypes(firstDataLine);

    line ++;

    for (int ln=line; ln < lines.size(); ln++)
    {
      List<String> values = parseLine(lines.get(ln));
      analyzeValues(values);
    }
  }

  private void initTypes(String firstLine)
  {
    List<String> values = parseLine(firstLine);
    if (values.size() != columns.size()) return;

    for (int i=0; i < values.size(); i ++)
    {
      checkOneValue(values.get(i), columns.get(i));
    }
  }

	private void initColumns(String headerLine)
	{
    List<String> values = parseLine(headerLine);
    columns = new ArrayList<>(values.size());

    for (int i=0; i < values.size(); i++)
    {
      String colName;
      if (withHeader)
      {
        colName = values.get(i);
      }
      else
      {
        colName = "column_" + Integer.valueOf(i+1);
      }
      columns.add(new ColumnStatistics(colName));
    }
	}

  private List<String> parseLine(String line)
  {
		List<String> values= new ArrayList<>();
		parser.setLine(line);
		while (parser.hasNext())
		{
			values.add(parser.getNext());
		}
    return values;
  }

  private List<String> readLines()
  {
    String lineEnd = StringUtil.LINE_TERMINATOR;
    if (enableMultiline)
    {
      lineEnd = FileUtil.getLineEnding(inputFile, encoding);
    }

    List<String> lines = new ArrayList<>(sampleSize);
    BufferedReader reader = null;
    try
    {
      reader = EncodingUtil.createBufferedReader(inputFile, encoding);

			String line;
			while ( (line = reader.readLine()) != null)
			{
        if (StringUtil.isBlank(line)) continue;

        if (MemoryWatcher.isMemoryLow(false))
        {
          LogMgr.logError("TextFileTableDetector.readLines()", "Memory is running low, aborting text file sampling after " + lines.size() + " lines", null);
          break;
        }

        if (enableMultiline && StringUtil.hasOpenQuotes(line, parser.getQuoteChar(), parser.getEscapeType()))
        {
          line = StringUtil.readContinuationLines(reader, line, parser.getQuoteChar(), parser.getEscapeType(), lineEnd);
        }
        else
        {
          line = line.trim();
        }
        lines.add(line);

        if (lines.size() >= sampleSize) break;
			}
    }
    catch (Exception ex)
    {
      LogMgr.logError("TextFileTableDetector.readLines()", "Could not read file " + inputFile.getAbsolutePath(), ex);
    }
    finally
    {
      FileUtil.closeQuietely(reader);
    }
    return lines;
  }

}
