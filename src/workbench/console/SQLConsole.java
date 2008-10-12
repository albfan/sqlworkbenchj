/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.console;

import workbench.AppArguments;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.wbcommands.WbConnect;
import workbench.sql.wbcommands.console.WbDisconnect;
import workbench.sql.wbcommands.console.WbStoreProfile;
import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A simple console interface SQL Workbench/J
 *
 * @author support@sql-workbench.net
 */
public class SQLConsole
{
	public static void main(String[] args)
	{
		WbManager.initConsoleMode(args);

		ArgumentParser cmdLine = WbManager.getInstance().getCommandLine();
		BatchRunner runner = BatchRunner.createBatchRunner(cmdLine, false);
		runner.showResultSets(true);
		runner.setShowStatementWithResult(false);
		runner.setShowStatementSummary(false);
		runner.setShowResultBorders(false);

		String value = cmdLine.getValue(AppArguments.ARG_SHOW_TIMING);
		if (StringUtil.isBlank(value))
		{
			runner.setShowTiming(false);
		}
		
		String prompt = "SQL";
		String currentPrompt = prompt;
		String continuePrompt = "..";

		LogMgr.logInfo("SQLConsole.main()", "SQL Workbench/J Console interface started");
		
		try
		{
			System.out.println(ResourceMgr.getString("MsgConsoleStarted"));
			WbFile f = new WbFile(Settings.getInstance().getConfigDir());
			System.out.println(ResourceMgr.getFormattedString("MsgConfigDir", f.getFullPath()));
			System.out.println("");
			
			// Enable console-specific commands for the batch runner
			runner.addCommand(new WbDisconnect());
			runner.addCommand(new WbStoreProfile());
			WbConnect connect = (WbConnect)runner.getCommand(WbConnect.VERB);
			connect.setPersistentChange(true);
			
			if (runner.hasProfile())
			{
				runner.connect();
			}

			InputReader input = new InputReader();
			InputBuffer buffer = new InputBuffer();
			
			while (true)
			{
				String line = input.readLine(currentPrompt + "> ");
				if (line == null) continue;

				if ("exit".equalsIgnoreCase(line.trim()))
				{
					break;
				}
				boolean isCompleteStatement = buffer.addLine(line);
				if (isCompleteStatement)
				{
					try
					{
						runner.executeScript(buffer.getScript());
					}
					catch (Exception e)
					{
						System.err.println(ExceptionUtil.getDisplay(e));
						LogMgr.logError("SQLConsole.main()", "Error running statement", e);
					}
					buffer.clear();
					currentPrompt = prompt;
				}
				else
				{
					currentPrompt = continuePrompt;
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

}
