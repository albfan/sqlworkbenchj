/*
 * SQLConsole.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.util.HashMap;
import java.util.Map;

import workbench.AppArguments;
import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.profiles.ProfileKey;

import workbench.sql.BatchRunner;
import workbench.sql.StatementHistory;
import workbench.sql.macros.MacroManager;
import workbench.sql.wbcommands.CommandTester;
import workbench.sql.wbcommands.WbConnInfo;
import workbench.sql.wbcommands.WbDescribeObject;
import workbench.sql.wbcommands.WbHelp;
import workbench.sql.wbcommands.WbHistory;
import workbench.sql.wbcommands.WbListProcedures;
import workbench.sql.wbcommands.WbListSchemas;
import workbench.sql.wbcommands.WbListTables;
import workbench.sql.wbcommands.WbSysExec;
import workbench.sql.wbcommands.console.WbRun;
import workbench.sql.wbcommands.console.WbToggleDisplay;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

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
{
	private static final String HISTORY_FILENAME = "sqlworkbench_history.txt";
	private ConsolePrompter prompter;
	private static final String DEFAULT_PROMPT = "SQL> ";
	private static final String CONTINUE_PROMPT = "..> ";

	private Map<String, String> abbreviations = new HashMap<String, String>();
	private StatementHistory history;

	public SQLConsole()
	{
		prompter = new ConsolePrompter();
	}

	public void run()
	{
		history = new StatementHistory(Settings.getInstance().getConsoleHistorySize());
		history.doAppend(true);

		AppArguments cmdLine = WbManager.getInstance().getCommandLine();

		if (cmdLine.isArgPresent("help"))
		{
			System.out.println(cmdLine.getHelp());
			WbManager.getInstance().doShutdown(0);
		}

		boolean bufferResults = cmdLine.getBoolean(AppArguments.ARG_CONSOLE_BUFFER_RESULTS, true);
		boolean optimizeColWidths = cmdLine.getBoolean(AppArguments.ARG_CONSOLE_OPT_COLS, true);

		BatchRunner runner = BatchRunner.createBatchRunner(cmdLine, false);
		runner.showResultSets(true);
		runner.setShowStatementWithResult(false);
		runner.setShowStatementSummary(false);
		runner.setOptimizeColWidths(optimizeColWidths);
		runner.setShowDataLoading(false);
		runner.setConnectionId("Console");

		// initialize a default max rows.
		// In console mode it doesn't really make sense to display that many rows
		int maxRows = Settings.getInstance().getIntProperty("workbench.console.default.maxrows", 5000);
		runner.setMaxRows(maxRows);

		if (!cmdLine.isArgPresent(AppArguments.ARG_SHOWPROGRESS))
		{
			runner.setShowProgress(true);
		}

		// Make the current directory the base directory for the BatchRunner
		// so that e.g. WbIncludes work properly
		WbFile currentDir = new WbFile(System.getProperty("user.dir"));
		runner.setBaseDir(currentDir.getFullPath());

		String value = cmdLine.getValue(AppArguments.ARG_SHOW_TIMING);
		if (StringUtil.isBlank(value))
		{
			runner.setShowTiming(true);
			runner.setShowStatementTiming(false);
		}

		runner.setHistoryProvider(this.history);

		LogMgr.logInfo("SQLConsole.main()", "SQL Workbench/J Console interface started");

		String currentPrompt = DEFAULT_PROMPT;

		try
		{
			System.out.println(ResourceMgr.getString("MsgConsoleStarted"));
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

			runner.setPersistentConnect(true);

			if (runner.hasProfile())
			{
				ConnectionProfile profile = runner.getProfile();
				if (!profile.getStorePassword())
				{
					String pwd = ConsoleReaderFactory.getConsoleReader().readPassword(ResourceMgr.getString("MsgInputPwd"));
					profile.setInputPassword(pwd);
				}

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
				currentPrompt = checkConnection(runner);
			}

			InputBuffer buffer = new InputBuffer();
			runner.setExecutionController(prompter);
			runner.setParameterPrompter(prompter);

			ResultSetPrinter printer = null;
			if (!bufferResults)
			{
				printer = new ResultSetPrinter(System.out);
				printer.setFormatColumns(optimizeColWidths);
				printer.setPrintRowCount(true);
				runner.setResultSetConsumer(printer);
				ConsoleSettings.getInstance().addChangeListener(printer);
			}

			boolean startOfStatement = true;

			history.readFrom(getHistoryFile());

			CommandTester cmd = new CommandTester();

			// Some limited psql compatibility
			abbreviations.put("\\x", cmd.formatVerb(WbToggleDisplay.VERB));
			abbreviations.put("\\?", cmd.formatVerb(WbHelp.VERB));
			abbreviations.put("\\h", cmd.formatVerb(WbHelp.VERB));
			abbreviations.put("\\i", cmd.formatVerb(WbRun.VERB));
			abbreviations.put("\\d", cmd.formatVerb(WbListTables.VERB));
			abbreviations.put("\\g", cmd.formatVerb(WbHistory.VERB) + " " + WbHistory.KEY_LAST);
			abbreviations.put("\\s", cmd.formatVerb(WbHistory.VERB));
			abbreviations.put("\\!", cmd.formatVerb(WbSysExec.VERB));
			abbreviations.put("\\dt", cmd.formatVerb(WbDescribeObject.VERB));
			abbreviations.put("\\df", cmd.formatVerb(WbListProcedures.VERB));
			abbreviations.put("\\dn", cmd.formatVerb(WbListSchemas.VERB));
			abbreviations.put("\\conninfo", WbConnInfo.VERB);

			// some limited SQL*Plus compatibility
			abbreviations.put("/", WbHistory.VERB + " " + WbHistory.KEY_LAST);

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

				String macro = getMacroText(stmt);
				if (StringUtil.isNonEmpty(macro))
				{
					isCompleteStatement = true;
				}

				String firstWord = getFirstWord(line);
				if (isCompleteStatement || (abbreviations.containsKey(firstWord) && startOfStatement)  )
				{
					try
					{
						prompter.resetExecuteAll();
						String longCommand = abbreviations.get(firstWord);
						if (longCommand != null)
						{
							stmt = line.replace(firstWord, longCommand);
						}

						if (firstWord.equalsIgnoreCase(WbHistory.VERB))
						{
							adjustHistoryDisplay(runner);
						}

						if (StringUtil.isBlank(macro))
						{
							history.add(stmt);
							runner.executeScript(stmt);
						}
						else
						{
							history.add(stmt);
							runner.executeScript(macro);
						}

						if (isHistoryCmd(stmt))
						{
							// WbHistory without parameters was executed
							// prompt for an index to be executed
							System.out.println("");
							String input = ConsoleReaderFactory.getConsoleReader().readLineWithoutHistory(">>> " + ResourceMgr.getString("TxtEnterStmtIndex") + " >>> ");
							int index = StringUtil.getIntValue(input, -1);
							if (index > 0 && index <= history.size())
							{
								runner.executeScript(history.get(index - 1));
							}
						}
					}
					catch (Exception e)
					{
						System.err.println(ExceptionUtil.getDisplay(e));
						LogMgr.logError("SQLConsole.main()", "Error running statement", e);
					}
					buffer.clear();
					currentPrompt = checkConnection(runner);
					startOfStatement = true;

					// Restore the printing consumer in case a WbExport changed it
					if (printer != null && runner.getResultSetConsumer() == null)
					{
						runner.setResultSetConsumer(printer);
					}
				}
				else
				{
					startOfStatement = false;
					currentPrompt = CONTINUE_PROMPT;
				}
			}
		}
		catch (Exception e)
		{
			System.err.println(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			history.saveTo(getHistoryFile());

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
		catch (Exception ex)
		{
			System.err.println(ExceptionUtil.getDisplay(ex));
			System.exit(1);
		}
	}

	private String getMacroText(String sql)
	{
		String macroText = MacroManager.getInstance().getMacroText(SqlUtil.trimSemicolon(sql));
		return macroText;
	}

	private boolean isHistoryCmd(String stmt)
	{
		if (StringUtil.isEmptyString(stmt)) return false;
		String cmd = SqlUtil.trimSemicolon(stmt).trim();
		return cmd.equalsIgnoreCase(WbHistory.SHORT_VERB) || cmd.equalsIgnoreCase(WbHistory.VERB);
	}

	private WbFile getHistoryFile()
	{
		String fname = Settings.getInstance().getProperty("workbench.console.history.file", HISTORY_FILENAME);
		WbFile file = new WbFile(Settings.getInstance().getConfigDir(), fname);
		return file;
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
		if (StringUtil.isBlank(input)) return null;
		input = input.trim();
		int pos = input.indexOf(' ');
		if (pos <= 0) return SqlUtil.trimSemicolon(input);
		return SqlUtil.trimSemicolon(input.substring(0, pos));
	}

	private String checkConnection(BatchRunner runner)
	{
		String newprompt = null;
		WbConnection current = runner.getConnection();
		if (current != null)
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
		return (newprompt == null ? DEFAULT_PROMPT : newprompt + "> ");
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
			console.run();
		}
	}

}
