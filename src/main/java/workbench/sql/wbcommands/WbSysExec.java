/*
 * WbSysExec.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql.wbcommands;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.PlatformHelper;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A workbench command to call an operating system program (or command)
 *
 * @author Thomas Kellerer
 */
public class WbSysExec
	extends SqlCommand
{
	public static final String VERB = "WbSysExec";
	public static final String ARG_PROGRAM = "program";
	public static final String ARG_PRG_ARG = "argument";
	public static final String ARG_WORKING_DIR = "dir";
	public static final String ARG_DOCUMENT = "document";
	public static final String ARG_ENCODING = "encoding";

	private Process task;

	public WbSysExec()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_PROGRAM);
		cmdLine.addArgument(ARG_WORKING_DIR);
		cmdLine.addArgument(ARG_DOCUMENT);
		cmdLine.addArgument(ARG_PRG_ARG, ArgumentType.Repeatable);
		cmdLine.addArgument(ARG_ENCODING);
		ConditionCheck.addParameters(cmdLine);
	}

	@Override
	public void cancel()
		throws SQLException
	{
		this.isCancelled = true;
		if (this.task != null)
		{
			task.destroy();
		}
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);
		String command = getCommandLine(sql);

		if (StringUtil.isBlank(command))
		{
			result.setFailure();
			result.addMessageByKey("ErrExecNoParm");
			return result;
		}

		cmdLine.parse(command);
		if (!ConditionCheck.isCommandLineOK(result, cmdLine))
		{
			return result;
		}

		BufferedReader stdIn = null;
		BufferedReader stdError = null;
		try
		{
			String prg = cmdLine.getValue(ARG_PROGRAM);
			String doc = cmdLine.getValue(ARG_DOCUMENT);

			ConditionCheck.Result check = ConditionCheck.checkConditions(cmdLine);
			if (!check.isOK())
			{
				result.addMessage(ConditionCheck.getMessage("ErrExec", check));
				result.setSuccess();
				return result;
			}

			if (StringUtil.isNonBlank(doc) && Desktop.isDesktopSupported())
			{
				try
				{
					Desktop.getDesktop().open(new File(doc));
					result.setSuccess();
				}
				catch (IOException io)
				{
					result.setFailure();
					result.addMessage(io.getLocalizedMessage());
				}
				return result;
			}

			File cwd = new File(getBaseDir());
			List<String> args = new ArrayList<>();

			List<String> params = cmdLine.getList(ARG_PRG_ARG);

			if (StringUtil.isNonBlank(prg))
			{
				if (prg.startsWith("."))
				{
					WbFile f = evaluateFileArgument(prg);
					prg = f.getFullPath();
				}
				args.add(prg);
				args.addAll(params);
			}
			else
			{
				List<String> cmd = StringUtil.stringToList(command, " ", true, true, false, true);
				args.addAll(cmd);
			}

			if (useShell(args.get(0)))
			{
				args = getShell(args);
			}

			// it seems that Windows actually needs IBM437...
			String encoding = cmdLine.getValue(ARG_ENCODING, System.getProperty("file.encoding"));
			LogMgr.logDebug("WbSysExec.execute()", "Using encoding: " + encoding);

			ProcessBuilder pb = new ProcessBuilder(args);
			String dir = cmdLine.getValue(ARG_WORKING_DIR);
			if (StringUtil.isNonBlank(dir))
			{
				File f = evaluateFileArgument(dir);
				pb.directory(f);
			}
			else
			{
				pb.directory(cwd);
			}

			LogMgr.logDebug("WbSysExec.execute()", "Running program: " + pb.command());
			this.task = pb.start();

			stdIn = new BufferedReader(new InputStreamReader(task.getInputStream(), encoding));
			stdError = new BufferedReader(new InputStreamReader(task.getErrorStream(), encoding));

			String out = stdIn.readLine();
			while (out != null)
			{
        if (resultLogger != null)
        {
          resultLogger.appendToLog(out + "\n");
        }
				else
        {
          result.addMessage(out);
        }
				out = stdIn.readLine();
			}

			String err = stdError.readLine();
			if (err != null)
			{
				result.setFailure();
			}

			while (err != null)
			{
				result.addErrorMessage(err);
				err = stdError.readLine();
			}
			task.waitFor();

			int exitValue = task.exitValue();
			if (exitValue != 0)
			{
				result.addMessage("Exit code: " + exitValue);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbExec.execute()", "Error calling external program", e);
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
		}
		finally
		{
			FileUtil.closeQuietely(stdIn);
			FileUtil.closeQuietely(stdError);
			this.task = null;
		}
		return result;
	}

	private List<String> getShell(List<String> command)
	{
		if (CollectionUtil.isEmpty(command)) return command;

		String os = getOSID();
		List<String> args = new ArrayList<>(command.size() + 2);

		String first = StringUtil.getFirstWord(command.get(0)).toLowerCase();
		String shell = System.getenv("SHELL");

		if ("windows".equals(os))
		{
			if (!first.startsWith("cmd"))
			{
				args.add("cmd");
				args.add("/c");
			}
      args.addAll(command);
		}
		else if (!first.startsWith(shell))
		{
			args.add(shell);
			args.add("-c");
			args.add(StringUtil.listToString(command, ' '));
		}
    else
    {
      args.addAll(command);
    }
		return args;
	}

	private boolean useShell(String command)
	{
		if (StringUtil.isEmptyString(command)) return false;
		String os = getOSID();
		if (os == null) return false;

		command = StringUtil.getFirstWord(command);

		boolean ignoreCase = PlatformHelper.isWindows() || PlatformHelper.isMacOS();

		List<String> cmdlist = Settings.getInstance().getListProperty("workbench.exec." + os + ".useshell", false, "*");
		if (cmdlist.contains("*")) return true;
		for (String cmd : cmdlist)
		{
      if (StringUtil.compareStrings(cmd, command, ignoreCase) == 0) return true;
		}
		return false;
	}

	private String getOSID()
	{
		if (PlatformHelper.isWindows())
		{
			return "windows";
		}
		if (PlatformHelper.isMacOS())
		{
			return "macos";
		}
		if (System.getProperty("os.name").toLowerCase().contains("linux"))
		{
			return "linux";
		}
		return null;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
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

}
