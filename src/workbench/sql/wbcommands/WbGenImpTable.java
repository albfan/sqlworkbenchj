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
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.db.importer.detector.SpreadSheetTableDetector;
import workbench.db.importer.detector.TableDetector;
import workbench.db.importer.detector.TextFileTableDetector;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.QuoteEscapeType;
import workbench.util.WbFile;





/**
 *
 * @author Thomas Kellerer
 */
public class WbGenImpTable
  extends SqlCommand
{
  public static final String VERB = "WbGenerateImpTable";

  public static final String ARG_SAMPLE_SIZE = "lines";

  private List<String> supportedTypes = new ArrayList<>(4);
  public WbGenImpTable()
  {
    super();
    isUpdatingCommand = false;
		cmdLine = new ArgumentParser();
    supportedTypes = CollectionUtil.arrayList("text");
    supportedTypes.addAll(WbImport.getSupportedSpreadSheetTypes());

		cmdLine.addArgument(WbImport.ARG_FILE, ArgumentType.Filename);
		cmdLine.addArgument(WbImport.ARG_TYPE, supportedTypes);
		cmdLine.addArgument(WbImport.ARG_QUOTE);
		cmdLine.addArgument(CommonArgs.ARG_DELIM);
		cmdLine.addArgument(WbImport.ARG_CONTAINSHEADER, ArgumentType.BoolSwitch);
		cmdLine.addArgument(WbImport.ARG_SHEET_NR);
		CommonArgs.addDelimiterParameter(cmdLine);
		CommonArgs.addEncodingParameter(cmdLine);
    cmdLine.addArgument(CommonArgs.ARG_DATE_FORMAT);
    cmdLine.addArgument(CommonArgs.ARG_TIMESTAMP_FORMAT);
    cmdLine.addArgument(WbImport.ARG_TARGETTABLE);
    cmdLine.addArgument(ARG_SAMPLE_SIZE);
    cmdLine.addArgument(WbImport.ARG_MULTI_LINE, ArgumentType.BoolArgument);
		cmdLine.addArgument(CommonArgs.ARG_OUTPUT_FILE, ArgumentType.Filename);
		CommonArgs.addQuoteEscaping(cmdLine);
  }

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
    StatementRunnerResult result = new StatementRunnerResult(sql);
    result.setFailure();

    cmdLine.parse(getCommandLine(sql));
    WbFile file = evaluateFileArgument(cmdLine.getValue(WbImport.ARG_FILE));

    if (file == null)
    {
      result.addErrorMessage("Input file required!");
      return result;
    }

    if (!file.exists())
    {
      result.addErrorMessageByKey("ErrFileNotFound", cmdLine.getValue(WbImport.ARG_FILE));
      return result;
    }

    String tableName = cmdLine.getValue(WbImport.ARG_TARGETTABLE);
    if (tableName == null)
    {
      result.addErrorMessage("Table name required!");
      return result;
    }

    boolean header = cmdLine.getBoolean(WbImport.ARG_CONTAINSHEADER, WbImport.getHeaderDefault());


    String typeFromFile = WbImport.findTypeFromFilename(file.getFullPath());
    String type = cmdLine.getValue(WbImport.ARG_TYPE, typeFromFile);

    if (!supportedTypes.contains(type))
    {
      result.addErrorMessage("Type " + type + " not supported!");
      return result;
    }

    int lines = cmdLine.getIntValue(ARG_SAMPLE_SIZE, 100);

    TableDetector detector = null;

    if (type.equals("text"))
    {
      String delim = cmdLine.getValue(CommonArgs.ARG_DELIM);
      String quote = cmdLine.getValue(WbImport.ARG_QUOTE);
      String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING);
      String tsFormat = cmdLine.getValue(CommonArgs.ARG_TIMESTAMP_FORMAT);
      String dateFormat = cmdLine.getValue(CommonArgs.ARG_DATE_FORMAT);
      QuoteEscapeType escaping = CommonArgs.getQuoteEscaping(cmdLine);
      TextFileTableDetector tfd = new TextFileTableDetector(file, delim, quote, dateFormat, tsFormat, header, lines, encoding);

			boolean multi = cmdLine.getBoolean(WbImport.ARG_MULTI_LINE, WbImport.getMultiDefault());
			tfd.setEnableMultiline(multi);
      tfd.setQuoteEscape(escaping);
      detector = tfd;
    }
    else
    {
      detector = new SpreadSheetTableDetector(file, header, lines);
    }

    try
    {
      detector.analyzeFile();

      String ddl = detector.getCreateTable(currentConnection, tableName);
      if (ddl != null)
      {
        result.addMessage(ddl + ";");
        result.setSuccess();
      }
    }
    catch (Exception ex)
    {
      result.setFailure();
      result.addMessage(ex.getMessage());
    }
    return result;
  }

  @Override
  public String getVerb()
  {
    return VERB;
  }

  @Override
  public boolean isWbCommand()
  {
    return true;
  }

  @Override
  protected boolean isConnectionRequired()
  {
    return false;
  }

}
