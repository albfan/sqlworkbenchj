/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
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

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.importer.TextFileParser;
import workbench.db.importer.detector.SpreadSheetTableDetector;
import workbench.db.importer.detector.TableDetector;
import workbench.db.importer.detector.TextFileTableDetector;

import workbench.gui.dbobjects.RunScriptPanel;

import workbench.sql.BatchRunner;
import workbench.sql.ErrorReportLevel;
import workbench.sql.ExecutionStatus;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.QuoteEscapeType;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import static workbench.sql.wbcommands.CommonArgs.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbGenImpTable
  extends SqlCommand
{
  public static final String VERB = "WbGenerateImpTable";
  public static final String VERB_SHORT = "WbGenImpTable";

  public static final String ARG_SAMPLE_SIZE = "lines";
  public static final String ARG_RUN_SCRIPT = "runScript";
  public static final String ARG_DO_PROMPT = "prompt";
  public static final String ARG_CLIPBOARD = "clipboard";
  public static final String ARG_ALL_VARCHAR = "useVarchar";

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
		cmdLine.addArgument(ARG_ALL_VARCHAR, ArgumentType.BoolSwitch);
		cmdLine.addArgument(WbImport.ARG_SHEET_NR);
		CommonArgs.addDelimiterParameter(cmdLine);
		CommonArgs.addEncodingParameter(cmdLine);
    cmdLine.addArgument(CommonArgs.ARG_DATE_FORMAT);
    cmdLine.addArgument(CommonArgs.ARG_TIMESTAMP_FORMAT);
    cmdLine.addArgument(WbImport.ARG_TARGETTABLE);
    cmdLine.addArgument(CommonArgs.ARG_DECIMAL_CHAR);
    cmdLine.addArgument(ARG_SAMPLE_SIZE);
    cmdLine.addArgument(ARG_RUN_SCRIPT, ArgumentType.BoolArgument);
    cmdLine.addArgument(ARG_DO_PROMPT, ArgumentType.BoolArgument);
    cmdLine.addArgument(ARG_CLIPBOARD, ArgumentType.BoolSwitch);
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

    if (displayHelp(result))
    {
      return result;
    }

    String fileName = cmdLine.getValue(WbImport.ARG_FILE);

    if (StringUtil.isBlank(fileName))
    {
      result.addErrorMessageByKey("ErrInputFileRqd");
      return result;
    }

    List<WbFile> files = evaluateWildardFileArgs(fileName);
    if (files.isEmpty())
    {
      result.addErrorMessageByKey("ErrFileNotFound", cmdLine.getValue(WbImport.ARG_FILE));
      return result;
    }

    String script = "";
    try
    {
      result.setSuccess();
      int statementCount = 0;

      for (WbFile file : files)
      {
        String ddl = processOneFile(file, result);
        if (ddl == null) continue;

        if (statementCount > 0)
        {
          script += "\n\n";
        }
        script += ddl;
        statementCount ++;
      }

      boolean createTable = cmdLine.getBoolean(ARG_RUN_SCRIPT, false);
      boolean showSQL = true;

      WbFile outFile = evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_OUTPUT_FILE));
      if (outFile != null)
      {
        try
        {
          FileUtil.writeString(outFile, script, false);
          String msg = ResourceMgr.getFormattedString("MsgScriptWritten", outFile.getFullPath());
          result.addMessage(msg);
          result.setSuccess();
          showSQL = false;
        }
        catch (IOException io)
        {
          result.setFailure();
          result.addErrorMessageByKey("ErrFileCreate", ExceptionUtil.getDisplay(io));
        }
      }

      StatementRunnerResult createResult = null;
      if (createTable)
      {
        boolean doPrompt = cmdLine.getBoolean(ARG_DO_PROMPT, true);

        if (WbManager.getInstance().isGUIMode() && doPrompt)
        {
          createResult = runDDLWithPrompt(script);
          showSQL = createResult.promptingWasCancelled();
        }
        else
        {
          createResult = runDDL(script);
        }

        if (!createResult.isSuccess())
        {
          result.setFailure();
          result.setFailure(createResult.getErrorDescriptor());
        }
      }

      if (showSQL)
      {
        result.addMessage(script);
      }

      if (createResult != null)
      {
        result.addMessageNewLine();
        result.addMessage(createResult.getMessageBuffer());
        createResult.clear();
      }

      if (cmdLine.getBoolean(ARG_CLIPBOARD, false))
      {
        putToClipboard(script);
      }
    }
    catch (Exception ex)
    {
      result.addErrorMessage(ex.getMessage());
    }
    return result;
  }

  private String processOneFile(WbFile file, StatementRunnerResult result)
  {
    boolean header = cmdLine.getBoolean(WbImport.ARG_CONTAINSHEADER, WbImport.getHeaderDefault());

    String typeFromFile = WbImport.findTypeFromFilename(file.getFullPath());
    String type = cmdLine.getValue(WbImport.ARG_TYPE, typeFromFile);

    TableDetector detector = null;

    if (type.equals("text"))
    {
      String delim = cmdLine.getValue(CommonArgs.ARG_DELIM, TextFileParser.DEFAULT_DELIMITER);
      String quote = cmdLine.getValue(WbImport.ARG_QUOTE);
      String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING);
      String tsFormat = cmdLine.getValue(CommonArgs.ARG_TIMESTAMP_FORMAT);
      String dateFormat = cmdLine.getValue(CommonArgs.ARG_DATE_FORMAT);
      QuoteEscapeType escaping = CommonArgs.getQuoteEscaping(cmdLine);
      TextFileTableDetector tfd = new TextFileTableDetector(file, delim, quote, dateFormat, tsFormat, header, encoding);

			boolean multi = cmdLine.getBoolean(WbImport.ARG_MULTI_LINE, WbImport.getMultiDefault());
			tfd.setEnableMultiline(multi);
      tfd.setQuoteEscape(escaping);

      String decimal = cmdLine.getValue(ARG_DECIMAL_CHAR);
      if (decimal != null) tfd.setDecimalChar(decimal.charAt(0));

      detector = tfd;
    }
    else
    {
      String index = cmdLine.getValue(WbImport.ARG_SHEET_NR, null);
      int sheetNr = 1;
      if ("*".equals(index))
      {
        sheetNr = 0;
      }
      else
      {
        sheetNr = cmdLine.getIntValue(WbImport.ARG_SHEET_NR, 1);
      }
      // the index is zero-based, but the user supplies a one-based index
      detector = new SpreadSheetTableDetector(file, header, sheetNr - 1);
    }

    detector.setAlwaysUseVarchar(cmdLine.getBoolean(ARG_ALL_VARCHAR, false));
    detector.setSampleSize(cmdLine.getIntValue(ARG_SAMPLE_SIZE, TableDetector.DEFAULT_SAMPLE_SIZE));
    detector.setTableName(cmdLine.getValue(WbImport.ARG_TARGETTABLE));

    try
    {
      detector.analyzeFile();

      if (detector.hasMessages())
      {
        result.addMessage(detector.getMessages());
      }

      return detector.getCreateTable(currentConnection);
    }
    catch (Exception ex)
    {
      result.addErrorMessage(type);
    }
    return null;
  }

  private void putToClipboard(String ddl)
  {
    if (GraphicsEnvironment.isHeadless()) return;

    try
    {
			Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
      StringSelection sel = new StringSelection(ddl);
			clp.setContents(sel, sel);
    }
    catch (Throwable th)
    {
      LogMgr.logError("WbGenImpTable.putToClipboard()", "Could not copy SQL to clipboard", th);
    }
  }

  private StatementRunnerResult runDDL(String ddl)
  {
    BatchRunner batchRunner = new BatchRunner();
    batchRunner.setAbortOnError(true);
    batchRunner.setStoreErrors(false);
    batchRunner.setPrintStatements(false);
    batchRunner.setShowProgress(false);
    batchRunner.setShowTiming(false);
    batchRunner.setStoreErrors(false);
    batchRunner.setShowStatementWithResult(false);
    batchRunner.setShowStatementSummary(false);
    batchRunner.setErrorStatementLogging(ErrorReportLevel.none);
    batchRunner.setRowMonitor(rowMonitor);

    StringResultLogger logger = new StringResultLogger();
    batchRunner.setResultLogger(logger);
    batchRunner.setConnection(currentConnection);

    StatementRunnerResult result = new StatementRunnerResult();
    result.setSuccess();
    try
    {
      ExecutionStatus status = batchRunner.runScript(ddl);
      result.addMessage(logger.getMessages());
      if (status == ExecutionStatus.Error)
      {
        result.setFailure(batchRunner.getLastError());
      }
    }
    catch (Exception sql)
    {
      // already handled
    }
    return result;
  }

  private StatementRunnerResult runDDLWithPrompt(String ddl)
  {
    StatementRunnerResult result = new StatementRunnerResult(ddl);
    result.setSuccess();
    try
    {
      // The panel will refuse to start the script if the connection is marked as busy
      currentConnection.setBusy(false);

      RunScriptPanel panel = new RunScriptPanel(currentConnection, ddl);
      panel.openWindow(WbManager.getInstance().getCurrentWindow(), ResourceMgr.getString("TxtWindowTitleGeneratedScript"), null);

      if (!panel.wasRun())
      {
        result.setPromptingWasCancelled();
      }
      else if (!panel.isSuccess())
      {
        result.setFailure(panel.getError());
      }
    }
    finally
    {
      currentConnection.setBusy(true);
    }
    return result;
  }


  @Override
  public String getVerb()
  {
    return VERB;
  }

  @Override
  public String getAlternateVerb()
  {
    return VERB_SHORT;
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
