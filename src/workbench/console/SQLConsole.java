/*
 * SQLConsole.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.console;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.AppArguments;
import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.WindowTitleBuilder;
import workbench.gui.profiles.ProfileKey;

import workbench.sql.BatchRunner;
import workbench.sql.CommandRegistry;
import workbench.sql.OutputPrinter;
import workbench.sql.RefreshAnnotation;
import workbench.sql.StatementHistory;
import workbench.sql.macros.MacroManager;
import workbench.sql.wbcommands.CommandTester;
import workbench.sql.wbcommands.WbConnInfo;
import workbench.sql.wbcommands.WbConnect;
import workbench.sql.wbcommands.WbDescribeObject;
import workbench.sql.wbcommands.WbHelp;
import workbench.sql.wbcommands.WbHistory;
import workbench.sql.wbcommands.WbList;
import workbench.sql.wbcommands.WbListCatalogs;
import workbench.sql.wbcommands.WbListProcedures;
import workbench.sql.wbcommands.WbListSchemas;
import workbench.sql.wbcommands.WbProcSource;
import workbench.sql.wbcommands.WbSysExec;
import workbench.sql.wbcommands.console.WbRun;
import workbench.sql.wbcommands.console.WbToggleDisplay;

import workbench.util.ExceptionUtil;
import workbench.util.PlatformHelper;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * A simple console interface for SQL Workbench/J
 * <br>
 * Commandline editing under Unix-style Operating systems is done using the
 * JLine library.
 *
 * @see jline.ConsoleReader
 * @see workbench.console.ConsoleReaderFactory
 *
 * @author Thomas Kellerer
 */
public class SQLConsole
	implements OutputPrinter, Runnable, SignalHandler
{
	private static final String HISTORY_FILENAME = "sqlworkbench_history.txt";
	private final ConsolePrompter prompter;
	private static final String DEFAULT_PROMPT = "SQL> ";
	private static final String CONTINUE_PROMPT = "..> ";
	private static final String PROMPT_END = "> ";
	private final WbThread shutdownHook = new WbThread(this, "ShutdownHook");
	private final Map<String, String> abbreviations = new HashMap<>();
	private final StatementHistory history;
	private BatchRunner runner;
	private WbThread cancelThread;
  private ConsoleRefresh refreshHandler = new ConsoleRefresh();

	private final boolean changeTerminalTitle;
	private final String titlePrefix = "\033]0;";
	private final String titleSuffix = "\007";
	private WindowTitleBuilder titleBuilder = new WindowTitleBuilder();

	public SQLConsole()
	{
		prompter = new ConsolePrompter();
		history = new StatementHistory(Settings.getInstance().getConsoleHistorySize());
		history.doAppend(true);
		installSignalHandler();
		changeTerminalTitle = !PlatformHelper.isWindows() && ConsoleSettings.changeTerminalTitle();
		titleBuilder.setShowWorkspace(false);
		titleBuilder.setShowProductNameAtEnd(ConsoleSettings.termTitleAppNameAtEnd());
		titleBuilder.setShowProfileGroup(false);
		titleBuilder.setShowURL(ConsoleSettings.termTitleIncludeUrl());
		titleBuilder.setShowNotConnected(false);
		CommandRegistry.getInstance().scanForExtensions();
	}

	public void startConsole()
	{
		AppArguments cmdLine = WbManager.getInstance().getCommandLine();

		if (cmdLine.isArgPresent("help"))
		{
			System.out.println(cmdLine.getHelp());
			WbManager.getInstance().doShutdown(0);
		}

		boolean optimizeColWidths = cmdLine.getBoolean(AppArguments.ARG_CONSOLE_OPT_COLS, true);

		runner = initBatchRunner(cmdLine, optimizeColWidths);

		String currentPrompt = DEFAULT_PROMPT;
		try
		{
			showStartupMessage(cmdLine);

			currentPrompt = connectRunner(runner, currentPrompt);

			ResultSetPrinter printer = createPrinter(cmdLine, optimizeColWidths, runner);

			loadHistory();

			initAbbreviations();

			String previousPrompt = null;
			boolean startOfStatement = true;

			InputBuffer buffer = new InputBuffer();
			buffer.setConnection(runner == null ? null : runner.getConnection());
			while (true)
			{
				String line = ConsoleReaderFactory.getConsoleReader().readLine(currentPrompt);
				if (line == null) continue;

				if (buffer.getLength() == 0 && StringUtil.isEmptyString(line)) continue;

				boolean isCompleteStatement = buffer.addLine(line);

				String stmt = buffer.getScript().trim();

				if (startOfStatement && ("exit".equalsIgnoreCase(stmt) || "\\q".equals(stmt)))
				{
					break;
				}

				String firstWord = getFirstWord(line);

				String macro = getMacroText(stmt);
				if (StringUtil.isNonEmpty(macro))
				{
					isCompleteStatement = true;
					stmt = macro;
				}
				else if (startOfStatement && abbreviations.containsKey(firstWord))
				{
          stmt = replaceShortcuts(stmt);
					isCompleteStatement = true;
				}

				boolean changeHistory = false;
				boolean addToHistory = true;

        // WbConnect might change the history file, so we need to detect a change
				WbFile lastHistory = getHistoryFile();

				if (isCompleteStatement)
				{
          stmt = replaceShortcuts(stmt);
					String verb = getFirstWord(stmt);

					try
					{
						prompter.resetExecuteAll();

						if (verb.equalsIgnoreCase(WbHistory.VERB))
						{
							stmt = handleHistory(runner, stmt);
							verb = getFirstWord(stmt);
							addToHistory = false;
						}
            else if (verb.equalsIgnoreCase(RefreshAnnotation.ANNOTATION))
            {
              addToHistory = false;
            }

						if (StringUtil.isNonEmpty(stmt))
						{
							if (addToHistory) history.add(stmt);

							changeHistory = verb.equalsIgnoreCase(WbConnect.VERB) && ConsoleSettings.useHistoryPerProfile();
							if (changeHistory)
							{
								saveHistory();
							}

							setTerminalTitle(runner.getConnection(), true);

              HandlerState state = refreshHandler.handleRefresh(runner, stmt, history);

              if (state == HandlerState.notHandled)
              {
                runner.runScript(stmt);
                if (ConsoleSettings.showScriptFinishTime())
                {
                  printMessage("(" + StringUtil.getCurrentTimestamp() + ")");
                }
              }
						}
					}
					catch (Throwable th)
					{
						System.err.println(ExceptionUtil.getDisplay(th));
						LogMgr.logError("SQLConsole.main()", "Error running statement", th);
					}
					finally
					{
						buffer.clear();
						currentPrompt = checkConnection(runner, previousPrompt == null ? currentPrompt : previousPrompt);
						previousPrompt = null;
						startOfStatement = true;
					}

					// this needs to be done after each statement as the connection might have changed.
					buffer.setConnection(runner.getConnection());

					if (changeHistory && !lastHistory.equals(getHistoryFile()))
					{
						loadHistory();
					}

					// Restore the printing consumer in case a WbExport changed it
					if (printer != null && runner.getResultSetConsumer() == null)
					{
						runner.setResultSetConsumer(printer);
					}
				}
				else
				{
					startOfStatement = false;
					if (previousPrompt == null) previousPrompt = currentPrompt;
					currentPrompt = CONTINUE_PROMPT;
				}
			}
		}
		catch (Throwable th)
		{
			// this should not happen
			System.err.println(ExceptionUtil.getDisplay(th));
		}
		finally
		{
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
			saveHistory();

			ConsoleReaderFactory.getConsoleReader().shutdown();
			ConnectionMgr.getInstance().disconnectAll();

			if (Settings.getInstance().isModified())
			{
				Settings.getInstance().saveSettings(false);
			}
		}

		try
		{
			WbManager.getInstance().doShutdown(0);
		}
		catch (Throwable th)
		{
			System.err.println(ExceptionUtil.getDisplay(th));
			System.exit(1);
		}
	}

  private String replaceShortcuts(String sql)
  {
    if (StringUtil.isEmptyString(sql)) return sql;

    // this will change the original statement
    // but trimming whitespace from the start and end
    // doesn't matter for a valid SQL statement
    sql = sql.trim();

    for (Map.Entry<String, String> entry : abbreviations.entrySet())
    {
      if (sql.startsWith(entry.getKey()))
      {
        return entry.getValue() + sql.substring(entry.getKey().length());
      }
    }
    return sql;
  }

	private ResultSetPrinter createPrinter(AppArguments cmdLine, boolean optimizeColWidths, BatchRunner runner)
		throws SQLException
	{
		boolean bufferResults = cmdLine.getBoolean(AppArguments.ARG_CONSOLE_BUFFER_RESULTS, true);
		ResultSetPrinter printer = null;
		if (!bufferResults)
		{
			printer = new ResultSetPrinter(System.out);
			printer.setFormatColumns(optimizeColWidths);
			printer.setPrintRowCount(true);
			runner.setResultSetConsumer(printer);
			ConsoleSettings.getInstance().addChangeListener(printer);
		}
		return printer;
	}

	private void showStartupMessage(AppArguments cmdLine)
	{
		LogMgr.logInfo("SQLConsole.main()", "SQL Workbench/J Console interface started");
		System.out.println(ResourceMgr.getFormattedString("MsgConsoleStarted", ResourceMgr.getBuildNumber().toString()));
		WbFile f = new WbFile(Settings.getInstance().getConfigDir());
		System.out.println(ResourceMgr.getFormattedString("MsgConfigDir", f.getFullPath()));
		System.out.println("");

		// check the presence of the Profile again to put a possible error message after the startup messages.
		String profilename = cmdLine.getValue(AppArguments.ARG_PROFILE);
		String group = cmdLine.getValue(AppArguments.ARG_PROFILE_GROUP);

		if (StringUtil.isNonBlank(profilename))
		{
			ProfileKey def = new ProfileKey(StringUtil.trimQuotes(profilename), StringUtil.trimQuotes(group));

			ConnectionProfile profile = ConnectionMgr.getInstance().getProfile(def);
			if (profile == null)
			{
				String msg = ResourceMgr.getFormattedString("ErrProfileNotFound", def);
				System.err.println();
				System.err.println(msg);
			}
		}

		if (cmdLine.hasUnknownArguments())
		{
			StringBuilder err = new StringBuilder(ResourceMgr.getString("ErrUnknownParameter"));
			err.append(' ');
			err.append(cmdLine.getUnknownArguments());
			System.err.println(err.toString());
			System.err.println();
		}
	}

	private String connectRunner(BatchRunner runner, String currentPrompt)
	{
		if (runner.hasProfile())
		{
			try
			{
				runner.connect();
			}
			catch (Exception e)
			{
				// nothing to log, already done by the runner
			}

			if (runner.isConnected() && !runner.getVerboseLogging())
			{
				WbConnection conn = runner.getConnection();
				System.out.println(ResourceMgr.getFormattedString("MsgBatchConnectOk", conn.getDisplayString()));

				String warn = conn.getWarnings();
				if (StringUtil.isNonBlank(warn))
				{
					System.out.println(warn);
				}
			}
			currentPrompt = checkConnection(runner, null);
		}
		return currentPrompt;
	}

	private BatchRunner initBatchRunner(AppArguments cmdLine, boolean optimizeColWidths)
	{
		BatchRunner batchRunner = BatchRunner.createBatchRunner(cmdLine, false);
		batchRunner.showResultSets(true);
		batchRunner.setShowStatementWithResult(false);
		batchRunner.setShowStatementSummary(false);
		batchRunner.setOptimizeColWidths(optimizeColWidths);
		batchRunner.setShowDataLoading(false);
		batchRunner.setConnectionId("Console");
		batchRunner.setTraceOutput(this);
		// initialize a default max rows.
		// In console mode it doesn't really make sense to display that many rows
		int maxRows = Settings.getInstance().getIntProperty("workbench.console.default.maxrows", 5000);
		batchRunner.setMaxRows(maxRows);
		if (cmdLine.isArgNotPresent(AppArguments.ARG_SHOWPROGRESS))
		{
			batchRunner.setShowProgress(true);
		}
		// Make the current directory the base directory for the BatchRunner
		// so that e.g. WbIncludes work properly
		WbFile currentDir = new WbFile(System.getProperty("user.dir"));
		batchRunner.setBaseDir(currentDir.getFullPath());
		boolean showTiming = cmdLine.getBoolean(AppArguments.ARG_SHOW_TIMING, false);
		batchRunner.setShowTiming(showTiming);
		batchRunner.setShowStatementTiming(!showTiming);
		batchRunner.setHistoryProvider(this.history);
		batchRunner.setPersistentConnect(true);
		batchRunner.setExecutionController(prompter);
		batchRunner.setParameterPrompter(prompter);
		batchRunner.setShowRowCounts(true);
		return batchRunner;
	}

	private void initAbbreviations()
	{
		CommandTester cmd = new CommandTester();

		// Some limited psql compatibility
		String last = cmd.formatVerb(WbHistory.VERB) + " last";
		abbreviations.put("\\x", cmd.formatVerb(WbToggleDisplay.VERB));
		abbreviations.put("\\?", cmd.formatVerb(WbHelp.VERB));
		abbreviations.put("\\h", cmd.formatVerb(WbHelp.VERB));
		abbreviations.put("\\i", cmd.formatVerb(WbRun.VERB));
		abbreviations.put("\\d", cmd.formatVerb(WbList.VERB));
		abbreviations.put("\\g", last);
		abbreviations.put("\\s", cmd.formatVerb(WbHistory.VERB));
		abbreviations.put("\\!", cmd.formatVerb(WbSysExec.VERB));
		abbreviations.put("\\dt", cmd.formatVerb(WbDescribeObject.VERB));
		abbreviations.put("\\ds", cmd.formatVerb(WbList.VERB) + " -types=sequence");
		abbreviations.put("\\sf", cmd.formatVerb(WbProcSource.VERB));
		abbreviations.put("\\l", cmd.formatVerb(WbListCatalogs.VERB));
		abbreviations.put("\\df", cmd.formatVerb(WbListProcedures.VERB));
		abbreviations.put("\\dn", cmd.formatVerb(WbListSchemas.VERB));
		abbreviations.put("\\conninfo", cmd.formatVerb(WbConnInfo.VERB));
		abbreviations.put("\\connect", cmd.formatVerb(WbConnect.VERB));
		abbreviations.put("\\c", cmd.formatVerb(WbConnect.VERB));
		abbreviations.put("\\watch", RefreshAnnotation.ANNOTATION + " 5s");

		// some limited SQL*Plus compatibility
		abbreviations.put("/", last);
	}

	@Override
	public void printMessage(String trace)
	{
		System.out.println(trace);
	}

	private String handleHistory(BatchRunner runner, String stmt)
		throws IOException
	{
		adjustHistoryDisplay(runner);
		String arg = SqlUtil.stripVerb(SqlUtil.makeCleanSql(stmt, false, false));
		int index = -1;

		if (StringUtil.isBlank(arg))
		{
			RowDisplay display = ConsoleSettings.getInstance().getRowDisplay();
			try
			{
				ConsoleSettings.getInstance().setRowDisplay(RowDisplay.SingleLine);
				runner.runScript(stmt);
				// WbHistory without parameters was executed prompt for an index to be executed
				System.out.println("");
				String input = ConsoleReaderFactory.getConsoleReader().readLineWithoutHistory(">>> " + ResourceMgr.getString("TxtEnterStmtIndex") + " >>> ");
				index = StringUtil.getIntValue(input, -1);
			}
			finally
			{
				ConsoleSettings.getInstance().setRowDisplay(display);
			}
		}
		else
		{
			if (arg.equalsIgnoreCase("last"))
			{
				index = history.size();
			}
			else
			{
				index = StringUtil.getIntValue(arg, -1);
			}
		}

		if (index > 0 && index <= history.size())
		{
			return history.get(index - 1);
		}
		return null;
	}

	private String getMacroText(String sql)
	{
		return MacroManager.getInstance().getMacroText(MacroManager.DEFAULT_STORAGE, SqlUtil.trimSemicolon(sql));
	}

	private void saveHistory()
	{
		history.saveTo(getHistoryFile());
	}

	private void loadHistory()
	{
		history.clear();
		WbFile histFile = getHistoryFile();
		LogMgr.logDebug("SQLConsole.loadHistory()", "Loading history file: " + histFile.getFullPath());
		history.readFrom(histFile);
		WbConsoleReader console = ConsoleReaderFactory.getConsoleReader();
		console.clearHistory();
		console.addToHistory(history.getHistoryEntries());
	}

	private WbFile getHistoryFile()
	{
		String fname = null;

		if (ConsoleSettings.useHistoryPerProfile() && runner != null && runner.getConnection() != null)
		{
			fname = runner.getConnection().createFilename() + "_history.txt";
		}

		if (fname == null)
		{
			fname = Settings.getInstance().getProperty("workbench.console.history.file", HISTORY_FILENAME);
		}
		WbFile result = new WbFile(Settings.getInstance().getConfigDir(), fname);
		return result;
	}

	private void adjustHistoryDisplay(BatchRunner runner)
	{
		int columns = ConsoleReaderFactory.getConsoleReader().getColumns();
		LogMgr.logDebug("SQLConsole.adjustHistoryDisplay()", "Console width: " + columns);
		if (columns < 0)
		{
			columns = Settings.getInstance().getIntProperty("workbench.console.history.displaylength", 100);
		}
		WbHistory wb = (WbHistory)runner.getCommand(WbHistory.VERB);
		wb.setMaxDisplayLength(columns);
	}

	private String getFirstWord(String input)
	{
		// I can't use SqlUtil.getSqlVerb() because that would not return e.g. \!
		if (StringUtil.isBlank(input)) return null;
		input = input.trim();
		int pos = StringUtil.findFirstWhiteSpace(input);
		if (pos <= 0) return SqlUtil.trimSemicolon(input);
		return SqlUtil.trimSemicolon(input.substring(0, pos));
	}

	private String appendSuffix(String prompt)
	{
		if (prompt == null) return null;
		if (prompt.endsWith(PROMPT_END)) return prompt;
		return prompt + PROMPT_END;
	}

	private String checkConnection(BatchRunner runner, String currentPrompt)
	{
		String newprompt = currentPrompt;
		WbConnection current = runner.getConnection();
    if (current != null && ConsoleSettings.showProfileInPrompt())
    {
      newprompt = current.getProfile().getName();
    }
    else if (current != null && !runner.hasPendingActions())
		{
      String user = current.getCurrentUser();
      String catalog = current.getDisplayCatalog();
      if (catalog == null) catalog = current.getCurrentCatalog();

      String schema = current.getDisplaySchema();
      if (schema == null) current.getCurrentSchema();

      if (StringUtil.isBlank(catalog) && StringUtil.isNonBlank(schema))
      {
        if (schema.equalsIgnoreCase(user))
        {
          newprompt = user;
        }
        else
        {
          newprompt = user + "@" + schema;
        }
      }
      else if (StringUtil.isNonBlank(catalog) && StringUtil.isBlank(schema))
      {
        newprompt = user + "@" + catalog;
      }
      else if (StringUtil.isNonBlank(catalog) && StringUtil.isNonBlank(schema))
      {
        newprompt = user + "@" + catalog + "/" + schema;
      }
		}
		setTerminalTitle(current, false);
		return (newprompt == null ? DEFAULT_PROMPT : appendSuffix(newprompt));
	}

	private void setTerminalTitle(WbConnection conn, boolean isRunning)
	{
		if (!changeTerminalTitle) return;
		ConnectionProfile profile = null;
		if (conn != null)
		{
			profile = conn.getProfile();
		}
		String indicator = isRunning ? "> " : "";
		String toPrint = titlePrefix + indicator + titleBuilder.getWindowTitle(profile) + titleSuffix;
		System.out.println(toPrint);
	}

	public static void main(String[] args)
	{
		AppArguments cmdLine = new AppArguments();
		cmdLine.parse(args);
		if (cmdLine.isArgPresent(AppArguments.ARG_SCRIPT) || cmdLine.isArgPresent(AppArguments.ARG_COMMAND))
		{
			// Allow batch mode through SQL Console
			// This way sqlwbconsole.exe can be used to start batch mode as well.
			WbManager.main(args);
		}
		else
		{
			System.setProperty("workbench.log.console", "false");
			WbManager.initConsoleMode(args);
			SQLConsole console = new SQLConsole();
			console.setTerminalTitle(null, false);
			console.startConsole();
		}
	}

	public void abortStatement()
	{
		if (cancelThread != null)
		{
			try
			{
				LogMgr.logInfo("SQLConsole.cancelStatement()", "Trying to forcefully abort current statement");
				printMessage(ResourceMgr.getString("MsgAbortStmt"));
				cancelThread.interrupt();
				cancelThread.stop();
				if (runner != null)
				{
					runner.abort();
				}
			}
			catch (Exception ex)
			{
				LogMgr.logWarning("SQLConsole.cancelStatement()", "Could not cancel statement", ex);
			}
			finally
			{
				cancelThread = null;
			}
		}
	}

	public void cancelStatement()
	{
		if (cancelThread != null)
		{
			abortStatement();
		}
		else if (runner != null && runner.isBusy() && cancelThread == null)
		{
			cancelThread = new WbThread(new Runnable()
			{
				@Override
				public void run()
				{
					LogMgr.logInfo("SQLConsole.cancelStatement()", "Trying to cancel the current statement");
					printMessage(ResourceMgr.getString("MsgCancellingStmt"));
					runner.cancel();
				}
			}, "ConsoleStatementCancel");

			try
			{
				cancelThread.start();
				cancelThread.join(Settings.getInstance().getIntProperty("workbench.sql.cancel.timeout", 5000));
			}
			catch (Exception ex)
			{
				printMessage(ResourceMgr.getString("MsgAbortStmt"));
				LogMgr.logWarning("SQLConsole.cancelStatement()", "Could not cancel statement. Trying to forcefully abort the statemnt", ex);
				abortStatement();
			}
			cancelThread = null;
		}
	}

	public void exit()
	{
		LogMgr.logWarning("SQLConsole.shutdownHook()", "SQL Workbench/J process has been interrupted.");

		cancelStatement();

		boolean exitImmediately = Settings.getInstance().getBoolProperty("workbench.exitonbreak", true);
		if (exitImmediately)
		{
			LogMgr.logWarning("SQLConsole.shutdownHook()", "Aborting process...");
			LogMgr.shutdown();
			Runtime.getRuntime().halt(15); // exit() doesn't work properly from inside a shutdownhook!
		}
		else
		{
			ConnectionMgr.getInstance().abortAll(Collections.singletonList(runner.getConnection()));
			LogMgr.shutdown();
		}
	}

	/**
	 * Callback for the shutdown hook
	 */
	@Override
	public void run()
	{
		exit();
	}

	private void installSignalHandler()
	{
		List<String> signals = Settings.getInstance().getListProperty("workbench.console.signal", false, "INT,QUIT");

		for (String name : signals)
		{
			try
			{
				Signal signal = new Signal(name.toUpperCase());
				Signal.handle(signal, this);
				LogMgr.logInfo("SQLConsole.installSignalHandler()", "Installed signal handler for " + name);
			}
			catch (Throwable th)
			{
				LogMgr.logInfo("SQLConsole.installSignalHandler()", "could not register signal handler for: " + name, th);
			}
		}
	}

	@Override
	public void handle(Signal signal)
	{
		LogMgr.logDebug("SQLConsole.handl()", "Received signal: " + signal.getName());
		if (signal.getName().equals("INT"))
		{
			cancelStatement();
		}
		if (signal.getName().equals("QUIT"))
		{
			exit();
		}
	}

}
