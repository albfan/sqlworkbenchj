/*
 * SQLConsole.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import workbench.AppArguments;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.wbcommands.console.WbDeleteProfile;
import workbench.sql.wbcommands.console.WbDisconnect;
import workbench.sql.wbcommands.console.WbDisplay;
import workbench.sql.wbcommands.console.WbListProfiles;
import workbench.sql.wbcommands.console.WbRun;
import workbench.sql.wbcommands.console.WbStoreProfile;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A simple console interface for SQL Workbench/J
 *
 * @author support@sql-workbench.net
 */
public class SQLConsole
{
	private InputReader input;
	private ConsolePrompter prompter;
	private static final String DEFAULT_PROMPT = "SQL> ";
	private static final String CONTINUE_PROMPT = "..> ";

	public SQLConsole()
	{
		input = new InputReader();
		prompter = new ConsolePrompter(input);
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
			
			// Enable console-specific commands for the batch runner
			runner.addCommand(new WbDisconnect());
			runner.addCommand(new WbStoreProfile());
			runner.addCommand(new WbDeleteProfile());
			runner.addCommand(new WbDisplay());
			runner.addCommand(new WbListProfiles());
			runner.addCommand(new WbRun());

			runner.setPersistentConnect(true);

			if (runner.hasProfile())
			{
				runner.connect();
				if (runner.isConnected() && !runner.getVerboseLogging())
				{
					WbConnection conn = runner.getConnection();
					System.out.println(ResourceMgr.getFormattedString("MsgBatchConnectOk", conn.getDisplayString()));
					
					String warn = conn.getWarnings();
					if (!StringUtil.isEmptyString(warn))
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
			}

			boolean startOfStatement = true;

			while (true)
			{
				String line = input.readLine(currentPrompt);
				if (line == null) continue;

				if (startOfStatement && "exit".equalsIgnoreCase(line.trim()))
				{
					break;
				}

				boolean isCompleteStatement = buffer.addLine(line);
				if (isCompleteStatement)
				{
					try
					{
						prompter.resetExecuteAll();
						runner.executeScript(buffer.getScript());
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
						printer.setCurrentConnection(runner.getConnection());
					}

					if (printer != null)
					{
						// As the BatchRunner will keep track of the desired output format
						// we need to synchronize the output format to our printer
						boolean rowsAsLine = ConsoleSettings.getInstance().getNextRowDisplay() == RowDisplay.SingleLine;
						printer.setPrintRowsAsLine(rowsAsLine);
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
			ConnectionMgr.getInstance().disconnectAll();
			WbManager.getInstance().doShutdown(0);
		}
	}


	private String checkConnection(BatchRunner runner)
	{
		String newprompt = null;
		WbConnection current = runner.getConnection();
		if (current != null)
		{
			newprompt = current.getCurrentUser();
			String catalog = current.getDisplayCatalog();
			String schema = current.getDisplaySchema();
			if (StringUtil.isBlank(catalog) && StringUtil.isNonBlank(schema))
			{
				newprompt += "@" + schema;
			}
			else if (StringUtil.isNonBlank(catalog) && StringUtil.isBlank(schema))
			{
				newprompt += "@" + catalog;
			}
			else if (StringUtil.isNonBlank(catalog) && StringUtil.isNonBlank(schema))
			{
				newprompt += "@" + schema + "/" + catalog;
			}
		}
		return (newprompt == null ? DEFAULT_PROMPT : newprompt + "> ");
	}



	public static void main(String[] args)
	{
		System.setProperty("workbench.log.console", "false");
		WbManager.initConsoleMode(args);
		SQLConsole console = new SQLConsole();
		console.run();
	}

}
