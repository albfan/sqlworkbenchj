/*
 * WbExec.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.log.LogMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.*;

/**
 * A workbench command to call an operating system program (or command)
 *
 * @author Thomas Kellerer
 */
public class WbSysExec
	extends SqlCommand
{
	public static final String VERB = "WBSYSEXEC";
	public static final String ARG_PROGRAM = "program";
	public static final String ARG_PRG_ARG = "argument";
	public static final String ARG_WORKING_DIR = "dir";

	private Process task;

	public WbSysExec()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_PROGRAM);
		cmdLine.addArgument(ARG_WORKING_DIR);
		cmdLine.addArgument(ARG_PRG_ARG, ArgumentType.Repeatable);
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
		BufferedReader stdIn = null;
		BufferedReader stdError = null;
		try
		{
			cmdLine.parse(command);
			String prg = cmdLine.getValue(ARG_PROGRAM);
			if (StringUtil.isNonBlank(prg))
			{
				List<String> args = new ArrayList<String>();
				args.add(prg);
				List<String> params = cmdLine.getList(ARG_PRG_ARG);
				args.addAll(params);
				ProcessBuilder pb = new ProcessBuilder(args);
				String dir = cmdLine.getValue(ARG_WORKING_DIR);
				if (StringUtil.isNonBlank(dir))
				{
					pb.directory(new File(dir));
				}
				this.task = pb.start();
			}
			else
			{
				this.task = Runtime.getRuntime().exec(command);
			}

			stdIn = new BufferedReader(new InputStreamReader(task.getInputStream()));
			stdError = new BufferedReader(new InputStreamReader(task.getErrorStream()));

			String out = stdIn.readLine();
			while (out != null)
			{
				result.addMessage(out);
				out = stdIn.readLine();
			}

			out = stdError.readLine();
			if (out != null)
			{
				result.setFailure();
			}
			while (out != null)
			{
				result.addMessage(out);
				out = stdError.readLine();
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

}
