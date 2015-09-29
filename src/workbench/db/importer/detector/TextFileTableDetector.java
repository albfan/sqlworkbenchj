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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.ValueConverter;
import workbench.util.WbStringTokenizer;

/**
 * A class to detect a table structure from a CSV file.
 *
 * @author Thomas Kellerer
 */
public class TextFileTableDetector
  extends TableDetector
{
  private String delimiter;
  private String quoteChar;
  private boolean withHeader;
  private String encoding;
  private WbStringTokenizer tokenizer;

  public TextFileTableDetector(File importFile, String delim, String quote, String dateFmt, String timestampFmt, boolean containsHeader, int numLines, String fileEncoding)
  {
    this.inputFile = importFile;
    this.delimiter = delim;
    this.quoteChar = quote;
    this.withHeader = containsHeader;
    this.sampleSize = numLines;
    encoding = fileEncoding;
    converter = new ValueConverter(dateFmt, timestampFmt);
		tokenizer = new WbStringTokenizer(delimiter, this.quoteChar, false);
		tokenizer.setDelimiterNeedsWhitspace(false);
  }

  @Override
  public void analyzeFile()
    throws IOException
  {
    success = false;
    BufferedReader reader = EncodingUtil.createBufferedReader(inputFile, encoding);
    List<String> lines = FileUtil.getLines(reader, false, false, sampleSize);

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
      parseLine(lines.get(ln));
    }
  }


  private void parseLine(String line)
  {
    tokenizer.setSourceString(line);
    List<String> values = tokenizer.getAllTokens();
    analyzeValues(values);
  }

  private void initTypes(String firstLine)
  {
    tokenizer.setSourceString(firstLine);
    List<String> values = tokenizer.getAllTokens();
    if (values.size() != columns.size()) return;

    for (int i=0; i < values.size(); i ++)
    {
      checkOneValue(values.get(i), columns.get(i));
    }
  }

	private void initColumns(String headerLine)
	{
    tokenizer.setSourceString(headerLine);
    List<String> values = tokenizer.getAllTokens();
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

}
