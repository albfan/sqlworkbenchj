/*
 * BatchRunner.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;

/**
 *
 * @author  info@sql-workbench.net
 */
public class BatchRunner
{
	private List files;
	private StatementRunner stmtRunner;
	private WbConnection connection;
	private boolean abortOnError = false;
	private String successScript;
	private String errorScript;
	private String delimiter = ";";
	private boolean showResultSets = false;
	private boolean showTiming = true;
	private boolean success = true;
	private ConnectionProfile profile;
	private ResultLogger resultDisplay;
	private boolean cancelExecution = false;
	private RowActionMonitor rowMonitor;
	private boolean verboseLogging = true;
	private boolean checkEscapedQuotes = false;

	public BatchRunner(String aFilelist)
	{
		this.files = StringUtil.stringToList(aFilelist, ",");
	}

	public void showResultSets(boolean flag)
	{
		this.showResultSets = flag;
	}

	public void setVerboseLogging(boolean flag)
	{
		this.verboseLogging = flag;
		if (this.stmtRunner != null)
		{
			this.stmtRunner.setVerboseLogging(flag);
		}
		this.showTiming = this.verboseLogging;
	}

	public void setProfile(String aProfilename)
	{
		LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchConnecting") + " [" + aProfilename + "]");
		ConnectionMgr mgr = ConnectionMgr.getInstance();
		ConnectionProfile prof = mgr.getProfile(aProfilename);
		if (prof == null)
		{
			LogMgr.logError("BatchRunner", ResourceMgr.getString("ErrorConnectionError"),null);
			throw new IllegalArgumentException("Could not find profile " + aProfilename);
		}
		this.setProfile(prof);
	}

	public void setProfile(ConnectionProfile aProfile)
	{
		this.profile = aProfile;
	}

	public void setDelimiter(String delim) { this.delimiter = delim; }

	public void setConnection(WbConnection conn)
	{
		this.connection = conn;
		this.stmtRunner = new StatementRunner();
		this.stmtRunner.setConnection(this.connection);
		this.stmtRunner.setVerboseLogging(this.verboseLogging);
	}

	public void connect()
		throws Exception
	{
		if (this.profile == null)
		{
			LogMgr.logWarning("BatchRunner.setProfile()", "Called with a <null> profile!");
			return;
		}
		try
		{
			ConnectionMgr mgr = ConnectionMgr.getInstance();
			WbConnection c = mgr.getConnection(this.profile, "BatchRunner");
			this.setConnection(c);
			LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchConnectOk"));
			System.out.println(ResourceMgr.getString("MsgBatchConnectOk"));
		}
		catch (Exception e)
		{
			success = false;
			LogMgr.logError("BatchRunner", ResourceMgr.getString("MsgBatchConnectError") + ": " + ExceptionUtil.getDisplay(e), null);
			System.out.println(ResourceMgr.getString("MsgBatchConnectError") + ":\n" + ExceptionUtil.getDisplay(e));
		}
	}

	public boolean isSuccess()
	{
		return this.success;
	}

	public void setSuccessScript(String aFilename)
	{
		if (aFilename == null) return;
		File f = new File(aFilename);
		if (f.exists() && !f.isDirectory())
			this.successScript = aFilename;
		else
			this.successScript = null;
	}

	public void setErrorScript(String aFilename)
	{
		if (aFilename == null) return;
		File f = new File(aFilename);
		if (f.exists() && !f.isDirectory())
			this.errorScript = aFilename;
		else
			this.errorScript = null;
	}

	private String readFile(String aFilename)
	{
		BufferedReader in = null;
		StringBuffer content = null;
		try
		{
			File f = new File(aFilename);
			if (!f.exists() )
			{
				String msg = ResourceMgr.getString("MsgBatchScriptFileNotFound");
				msg = StringUtil.replace(msg, "%file%", aFilename);
				LogMgr.logWarning("BatchRunner.readFile()", "Scriptfile not found: "+ aFilename);
				this.printMessage(msg + "\n");
				return null;
			}
			content = new StringBuffer((int)f.length());
			in = new BufferedReader(new FileReader(f));
			String line = in.readLine();
			while (line != null)
			{
				content.append(line);
				content.append('\n');
				line = in.readLine();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("BatchRunner.readFile()", "Error reading file " + aFilename, e);
			System.err.println(ResourceMgr.getString("MsgBatchScriptFileReadError") + " " + aFilename);
			content = new StringBuffer();
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return content.toString();
	}

	public void setRowMonitor(RowActionMonitor mon)
	{
		this.rowMonitor = mon;
	}

	public void execute()
		throws IOException
	{
		String file = null;
		boolean error = false;
		int count = this.files.size();

		if (this.rowMonitor  != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
		}

		for (int i=0; i < count; i++)
		{
			file = (String)this.files.get(i);

			File fo = new File(file);

			if (this.rowMonitor != null)
			{
				this.rowMonitor.setCurrentObject(file, i+1, count);
			}

			try
			{
				String msg = ResourceMgr.getString("MsgBatchProcessingFile") + " " + file;
				LogMgr.logInfo("BatchRunner", msg);
				if (this.resultDisplay != null)
				{
					this.resultDisplay.appendToLog("\n");
					this.resultDisplay.appendToLog(msg);
					this.resultDisplay.appendToLog("\n\n");
				}

				error = this.executeScript(fo);
				msg = ResourceMgr.getString("MsgBatchProcessingFileDone") + " " + file;
				if (this.resultDisplay != null)
				{
					this.resultDisplay.appendToLog("\n");
					this.resultDisplay.appendToLog(msg);
					this.resultDisplay.appendToLog("\n\n");
				}
				LogMgr.logInfo("BatchRunner", msg);
			}
			catch (Exception e)
			{
				error = true;
				LogMgr.logError("BatchRunner", ResourceMgr.getString("MsgBatchScriptFileError") + " " + file, e);
			}
			if (error && abortOnError)
			{
				break;
			}
		}

		if (abortOnError && error)
		{
			this.success = false;
			try
			{
				if (this.errorScript != null)
				{
					LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchExecutingErrorScript") + " " + this.errorScript);
					this.executeScript(new File(this.errorScript));
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner.execute()", ResourceMgr.getString("MsgBatchScriptFileError") + " " + this.errorScript, e);
			}
		}
		else
		{
			this.success = true;
			try
			{
				if (this.successScript != null)
				{
					LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchExecutingSuccessScript") + " " + this.successScript);
					this.executeScript(new File(this.successScript));
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner.execute()", ResourceMgr.getString("MsgBatchScriptFileError") + " " + this.successScript, e);
			}
		}
	}

	public void cancel()
	{
		this.cancelExecution = true;
		if (this.stmtRunner != null)
		{
			this.stmtRunner.cancel();
		}
	}

	private boolean executeScript(File scriptFile)
		throws IOException
	{
		boolean error = false;
		StatementRunnerResult result = null;
		ScriptParser parser = new ScriptParser(scriptFile);
		parser.setAlternateDelimiter(Settings.getInstance().getAlternateDelimiter());
		parser.setDelimiter(this.delimiter);
		parser.setCheckEscapedQuotes(this.checkEscapedQuotes);
		String sql = null;
		this.cancelExecution = false;

		int executedCount = 0;
		long start, end;

		int interval;
		if (scriptFile.length() < 5000)
		{
			interval = 1;
		}
		if (scriptFile.length() < 100000)
		{
			interval = 10;
		}
		else
		{
			interval = 100;
		}

		start = System.currentTimeMillis();

		Iterator itr = parser.getIterator();
		while (itr.hasNext())
		{
			sql = itr.next().toString();

			try
			{
				if (this.resultDisplay == null) LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchExecutingStatement") + " "  + sql);
				long verbstart = System.currentTimeMillis();
				this.stmtRunner.runStatement(sql, 0);
				long verbend = System.currentTimeMillis();
				result = this.stmtRunner.getResult();
				if (result.hasMessages() && (this.stmtRunner.getVerboseLogging() || !result.isSuccess()))
				{
					String[] msg = result.getMessages();
					for (int m=0; m < msg.length; m++)
					{
						this.printMessage(msg[m]);
					}
				}
				executedCount ++;

				if (this.showTiming)
				{
					this.printMessage(ResourceMgr.getString("MsgSqlVerbTime") + " " + (((double)(verbend - verbstart)) / 1000.0) + "s");
				}

				if (this.rowMonitor != null && (executedCount % interval == 0))
				{
					this.rowMonitor.setCurrentRow(executedCount, -1);
				}

				if (this.cancelExecution)
				{
					this.printMessage(ResourceMgr.getString("MsgStatementCancelled"));
					break;
				}

				if (this.showResultSets && result.isSuccess() && result.hasDataStores())
				{
					System.out.println();
					System.out.println(sql);
					System.out.println("---------------- " + ResourceMgr.getString("MsgResultLogStart") + " ----------------------------");
					DataStore[] data = result.getDataStores();
					for (int nr=0; nr < data.length; nr++)
					{
						System.out.println(data[nr].getDataString(StringUtil.LINE_TERMINATOR, true));
					}
					System.out.println("---------------- " + ResourceMgr.getString("MsgResultLogEnd") + " ----------------------------");
				}
				if (!result.isSuccess())
				{
					error = true;
				}
			}
			catch (Throwable e)
			{
				LogMgr.logError("BatchRunner", ResourceMgr.getString("MsgBatchStatementError") + " "  + sql, e);
				error = true;
				break;
			}
			if (error && abortOnError) break;
		}
		end = System.currentTimeMillis();
		String msg = "\n" + executedCount + " " + ResourceMgr.getString("MsgTotalStatementsExecuted");
		this.printMessage(msg);

		if (this.showTiming)
		{
			long execTime = (end - start);
			msg = ResourceMgr.getString("MsgExecTime") + " " + (((double)execTime) / 1000.0) + "s";
			this.printMessage(msg);
		}

		return error;
	}

	public void done()
	{
		ConnectionMgr mgr = ConnectionMgr.getInstance();
		mgr.disconnectAll();
	}

	public void setAbortOnError(boolean aFlag)
	{
		this.abortOnError = aFlag;
	}

	public void setCheckEscapedQuotes(boolean flag)
	{
		this.checkEscapedQuotes = flag;
	}

	public void setResultLogger(ResultLogger logger)
	{
		this.resultDisplay = logger;
		if (this.stmtRunner != null)
		{
			this.stmtRunner.setResultLogger(logger);
		}
	}

	private void printMessage(String msg)
	{
		if (this.resultDisplay == null)
		{
			if (msg != null && msg.length() > 0) System.out.println(msg);
		}
		else
		{
			this.resultDisplay.appendToLog(msg);
			this.resultDisplay.appendToLog("\n");
		}
	}

	public static BatchRunner initFromCommandLine(ArgumentParser cmdLine)
	{
		String scripts = cmdLine.getValue(WbManager.ARG_SCRIPT);
		if (scripts == null || scripts.trim().length() == 0) return null;

		String profilename = cmdLine.getValue(WbManager.ARG_PROFILE);
		String errorHandling = cmdLine.getValue(WbManager.ARG_ABORT);
		boolean showResult = cmdLine.getBoolean(WbManager.ARG_DISPLAY_RESULT);
		boolean abort = true;
		if (errorHandling != null)
		{
			abort = StringUtil.stringToBool(errorHandling);
		}

		ConnectionProfile profile = null;
		if (profilename == null)
		{
			// No connection profile given, create a temporary profile
			// to be used for the batch runner.
			String url = StringUtil.trimQuotes(cmdLine.getValue(WbManager.ARG_CONN_URL));
			String driver = StringUtil.trimQuotes(cmdLine.getValue(WbManager.ARG_CONN_DRIVER));
			String user = StringUtil.trimQuotes(cmdLine.getValue(WbManager.ARG_CONN_USER));
			String pwd = StringUtil.trimQuotes(cmdLine.getValue(WbManager.ARG_CONN_PWD));
			String jar = StringUtil.trimQuotes(cmdLine.getValue(WbManager.ARG_CONN_JAR));
			DbDriver drv = ConnectionMgr.getInstance().findRegisteredDriver(driver);
			if (drv == null)
			{
				ConnectionMgr.getInstance().registerDriver(driver, jar);
			}
			profile = new ConnectionProfile(driver, url, user, pwd);
		}
		else
		{
			profile = ConnectionMgr.getInstance().getProfile(StringUtil.trimQuotes(profilename));
		}
		boolean ignoreDrop = "true".equalsIgnoreCase(cmdLine.getValue(WbManager.ARG_IGNORE_DROP));
		profile.setIgnoreDropErrors(ignoreDrop);

		String success = cmdLine.getValue(WbManager.ARG_SUCCESS_SCRIPT);
		String error = cmdLine.getValue(WbManager.ARG_ERROR_SCRIPT);

		BatchRunner runner = new BatchRunner(scripts);
		runner.showResultSets(showResult);
		runner.setAbortOnError(abort);
		runner.setErrorScript(error);
		runner.setSuccessScript(success);
		runner.setProfile(profile);
		runner.showTiming = cmdLine.getBoolean(WbManager.ARG_SHOW_TIMING, true);

		return runner;
	}
}