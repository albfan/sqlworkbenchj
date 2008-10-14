/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.console;

import java.sql.SQLException;
import workbench.AppArguments;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.VariablePool;
import workbench.interfaces.ExecutionController;
import workbench.interfaces.ParameterPrompter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.wbcommands.WbConnect;
import workbench.sql.wbcommands.console.WbDeleteProfile;
import workbench.sql.wbcommands.console.WbDisconnect;
import workbench.sql.wbcommands.console.WbListProfiles;
import workbench.sql.wbcommands.console.WbStoreProfile;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A simple console interface SQL Workbench/J
 *
 * @author support@sql-workbench.net
 */
public class SQLConsole
	implements ExecutionController, ParameterPrompter
{
	private InputReader input;
	
	public SQLConsole()
	{
		input = new InputReader();
	}

	public void run()
	{
		AppArguments cmdLine = WbManager.getInstance().getCommandLine();

		if (cmdLine.isArgPresent("help"))
		{
			System.out.println(cmdLine.getHelp());
			WbManager.getInstance().doShutdown(0);
		}
		
		BatchRunner runner = BatchRunner.createBatchRunner(cmdLine, false);
		runner.showResultSets(true);
		runner.setShowStatementWithResult(false);
		runner.setShowStatementSummary(false);
		runner.setShowResultBorders(false);

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

		String prompt = "SQL> ";
		String currentPrompt = prompt;
		String continuePrompt = "..> ";

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
			runner.addCommand(new WbDeleteProfile());
			runner.addCommand(new WbListProfiles());
			
			WbConnect connect = (WbConnect)runner.getCommand(WbConnect.VERB);
			connect.setPersistentChange(true);
			
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
			}

			InputBuffer buffer = new InputBuffer();
			runner.setExecutionController(this);
			runner.setParameterPrompter(this);

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
						runner.executeScript(buffer.getScript());
					}
					catch (Exception e)
					{
						System.err.println(ExceptionUtil.getDisplay(e));
						LogMgr.logError("SQLConsole.main()", "Error running statement", e);
					}
					buffer.clear();
					currentPrompt = prompt;
					startOfStatement = true;
				}
				else
				{
					startOfStatement = false;
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

	public boolean processParameterPrompts(String sql)
	{
		VariablePool pool = VariablePool.getInstance();
		
		DataStore ds = pool.getParametersToBePrompted(sql);
		if (ds == null || ds.getRowCount() == 0) return true;
		
		System.out.println(ResourceMgr.getString("TxtVariableInputText"));
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			String varName = ds.getValueAsString(row, 0);
			String value = ds.getValueAsString(row, 1);

			String newValue = input.readLine(varName + " [" + value + "]: ");
			ds.setValue(row, 1, newValue);
		}
		
		try
		{
			ds.updateDb(null, null);
		}
		catch (SQLException ignore)
		{
			// Cannot happen
		}
		return true;
	}

	public boolean confirmExecution(String prompt)
	{
		String yes = ResourceMgr.getString("MsgConfirmConsoleYes");
		String yesNo = yes + "/" + ResourceMgr.getString("MsgConfirmConsoleNo");

		String msg = prompt + " (" + yesNo + ")";
		String choice = input.readLine(msg + " ");
		return yes.equalsIgnoreCase(choice);
	}

	public boolean confirmStatementExecution(String command)
	{
		String verb = SqlUtil.getSqlVerb(command);
		String yes = ResourceMgr.getString("MsgConfirmConsoleYes");
		String yesNo = yes + "/" + ResourceMgr.getString("MsgConfirmConsoleNo");

		String msg = ResourceMgr.getFormattedString("MsgConfirmConsoleExec", verb, yesNo);
		String choice = input.readLine(msg + " ");
		return yes.equalsIgnoreCase(choice);
	}
	
	public static void main(String[] args)
	{
		WbManager.initConsoleMode(args);
		SQLConsole console = new SQLConsole();
		console.run();
	}

}
