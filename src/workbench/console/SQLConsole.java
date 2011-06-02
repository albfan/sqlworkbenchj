/*
 * SQLConsole.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.util.HashMap;
import java.util.Map;
import workbench.AppArguments;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.profiles.ProfileKey;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.wbcommands.WbConnInfo;
import workbench.sql.wbcommands.WbDescribeObject;
import workbench.sql.wbcommands.WbHelp;
import workbench.sql.wbcommands.WbInclude;
import workbench.sql.wbcommands.WbListProcedures;
import workbench.sql.wbcommands.WbListSchemas;
import workbench.sql.wbcommands.WbListTables;
import workbench.sql.wbcommands.console.WbDisconnect;
import workbench.sql.wbcommands.console.WbDisplay;
import workbench.sql.wbcommands.console.WbListProfiles;
import workbench.sql.wbcommands.console.WbRun;
import workbench.sql.wbcommands.console.WbToggleDisplay;
import workbench.util.ExceptionUtil;
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
	private ConsolePrompter prompter;
	private static final String DEFAULT_PROMPT = "SQL> ";
	private static final String CONTINUE_PROMPT = "..> ";

	private Map<String, String> abbreviations = new HashMap<String, String>();

	public SQLConsole()
	{
		prompter = new ConsolePrompter();
	}

	public void run()
	{
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

			// Enable console-specific commands for the batch runner
			runner.addCommand(new WbDisconnect());
			runner.addCommand(new WbDisplay());
			runner.addCommand(new WbToggleDisplay());
			runner.addCommand(new WbListProfiles());
			runner.addCommand(new WbRun());

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

			// Some limited psql compatibility
			abbreviations.put("\\x", WbToggleDisplay.VERB);
			abbreviations.put("\\?", WbHelp.VERB);
			abbreviations.put("\\i", WbInclude.VERB);
			abbreviations.put("\\d", WbListTables.VERB);
			abbreviations.put("\\dt", WbDescribeObject.VERB);
			abbreviations.put("\\df", WbListProcedures.VERB);
			abbreviations.put("\\dn", WbListSchemas.VERB);
			abbreviations.put("\\conninfo", WbConnInfo.VERB);

			while (true)
			{
				String line = ConsoleReaderFactory.getConsoleReader().readLine(currentPrompt);
				if (line == null) continue;

				if (startOfStatement && ("exit".equalsIgnoreCase(line.trim()) || "\\q".equals(line.trim())))
				{
					break;
				}

				boolean isCompleteStatement = buffer.addLine(line);

				String firstWord = getFirstWord(line);
				if (isCompleteStatement || (abbreviations.containsKey(firstWord) && startOfStatement)  )
				{
					try
					{
						prompter.resetExecuteAll();
						String longCommand = abbreviations.get(firstWord);
						if (longCommand != null)
						{
							line = line.replace(firstWord, longCommand);
							runner.executeScript(line);
						}
						else
						{
							runner.executeScript(buffer.getScript());
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
			ConsoleReaderFactory.getConsoleReader().shutdown();
			ConnectionMgr.getInstance().disconnectAll();
			WbManager.getInstance().doShutdown(0);
		}
	}

	private String getFirstWord(String input)
	{
		if (StringUtil.isBlank(input)) return null;
		input = input.trim();
		int pos = input.indexOf(' ');
		if (pos <= 0) return input;
		return input.substring(0, pos);
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

			if (StringUtil.isBlank(catalog) && StringUtil.isNonBlank(schema) && !schema.equals(user))
			{
				newprompt = user + "@" + schema;
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
		if (cmdLine.isArgPresent(AppArguments.ARG_SCRIPT))
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
