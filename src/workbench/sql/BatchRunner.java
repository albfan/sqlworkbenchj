/*
 * BatchRunner.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.sql;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import workbench.AppArguments;
import workbench.WbManager;
import workbench.console.ConsolePrompter;
import workbench.console.ConsoleReaderFactory;
import workbench.console.ConsoleSettings;
import workbench.console.ConsoleStatusBar;
import workbench.console.DataStorePrinter;
import workbench.console.RowDisplay;
import workbench.interfaces.ExecutionController;
import workbench.interfaces.ParameterPrompter;
import workbench.interfaces.ResultLogger;
import workbench.interfaces.ResultSetConsumer;
import workbench.interfaces.SqlHistoryProvider;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.components.GenericRowMonitor;
import workbench.gui.profiles.ProfileKey;

import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;

import workbench.sql.wbcommands.ConnectionDescriptor;
import workbench.sql.wbcommands.InvalidConnectionDescriptor;
import workbench.sql.wbcommands.WbConnect;

import workbench.util.ArgumentParser;
import workbench.util.CollectionUtil;
import workbench.util.DurationFormatter;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileDialogUtil;
import workbench.util.MessageBuffer;
import workbench.util.Replacer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbStringTokenizer;

/**
 * A class to run several statements from a script file.
 * This is used when running SQL Workbench in batch/console mode and
 * for the {@link workbench.sql.wbcommands.WbInclude} command.
 *
 * @see workbench.console.SQLConsole
 * @see workbench.sql.wbcommands.WbInclude
 *
 * @author  Thomas Kellerer
 */
public class BatchRunner
	implements PropertyChangeListener
{
	public static final String CMD_LINE_PROFILE_NAME = "$Wb$CommandLineProfile";
	private List<String> filenames;
	private StatementRunner stmtRunner;
	private WbConnection connection;
	private boolean abortOnError;
	private String successScript;
	private String errorScript;
	private DelimiterDefinition delimiter;
	private boolean showResultSets;
	private boolean showTiming = true;
	private boolean success = true;
	private ConnectionProfile profile;
	private ResultLogger resultDisplay;
	private boolean cancelExecution;
	private RowActionMonitor rowMonitor;
	private boolean verboseLogging = true;
	private boolean checkEscapedQuotes;
	private String encoding;
	private boolean showProgress;
	private PrintStream console = System.out;
	private boolean consolidateMessages;
	private boolean showStatementWithResult = true;
	private boolean showSummary = verboseLogging;
	private boolean optimizeCols = true;
	private boolean showStatementTiming = true;
	private boolean printStatements;
	private String connectionId = "BatchRunner";
	private String command;
	private boolean storeErrorMessages;
	private MessageBuffer errors;
	private final List<DataStore> queryResults = new ArrayList<>();
	private Replacer replacer;
	private boolean isBusy;

	public BatchRunner()
	{
		this.stmtRunner = new StatementRunner();
		this.stmtRunner.setErrorReportLevel(ErrorReportLevel.full);
		this.stmtRunner.addChangeListener(this);
	}

	public BatchRunner(String aFilelist)
	{
		this();
		WbStringTokenizer tok = new WbStringTokenizer(aFilelist, ",", false, "\"'", false);
		List<String> names = tok.getAllTokens();
		filenames = new ArrayList<>(names.size());
		for (String name : names)
		{
			filenames.add(name.trim());
		}
	}

	public BatchRunner(List<File> files)
	{
		this();
		filenames = new ArrayList<>(files.size());
		for (File f : files)
		{
			filenames.add(f.getAbsolutePath());
		}
	}

	public void done()
	{
		if (errors != null) errors.clear();
		queryResults.clear();
	}

	public void setTraceOutput(OutputPrinter tracer)
	{
		this.stmtRunner.setMessagePrinter(tracer);
	}

	public void setReplacer(Replacer replacer)
	{
		this.replacer = replacer;
	}

	public void setStoreErrors(boolean flag)
	{
		this.storeErrorMessages = flag;
	}

	public void setPrintStatements(boolean flag)
	{
		this.printStatements = flag;
	}

	public boolean isBusy()
	{
		return isBusy;
	}

	/**
	 * The baseDir is used when including other scripts using WbInclude.
	 *
	 * If the filename of the included script is a relative filename
	 * then the StatementRunner will assume the script is located relative
	 * to the baseDir. This call is delegated to
	 * {@link StatementRunner#setBaseDir(String)}
	 *
	 * @param dir the base directory to be used
	 * @see StatementRunner#setBaseDir(String)
	 */
	public void setBaseDir(String dir)
	{
		this.stmtRunner.setBaseDir(dir);
	}

	public void setHistoryProvider(SqlHistoryProvider provider)
	{
		this.stmtRunner.setHistoryProvider(provider);
	}

	public void setUseSavepoint(boolean flag)
	{
		stmtRunner.setUseSavepoint(flag);
	}

	public void setConnectionId(String id)
	{
		this.connectionId = id;
	}

	public WbConnection getConnection()
	{
		return this.connection;
	}

	public SqlCommand getCommand(String verb)
	{
		return stmtRunner.cmdMapper.getCommandToUse(verb);
	}

	public void addCommand(SqlCommand cmd)
	{
		stmtRunner.cmdMapper.addCommand(cmd);
	}

	public void setExecutionController(ExecutionController controller)
	{
		this.stmtRunner.setExecutionController(controller);
	}

	public void setParameterPrompter(ParameterPrompter p)
	{
		this.stmtRunner.setParameterPrompter(p);
	}

	public void setConsole(PrintStream output)
	{
		this.console = output;
	}

	public void setShowDataLoading(boolean flag)
	{
		stmtRunner.setShowDataLoadingProgress(flag);
	}

	public void setShowStatementWithResult(boolean flag)
	{
		this.showStatementWithResult = flag;
	}

	public void showResultSets(boolean flag)
	{
		this.showResultSets = flag;
	}

	public void setOptimizeColWidths(boolean flag)
	{
		this.optimizeCols = flag;
	}

	public void setShowStatementSummary(boolean flag)
	{
		this.showSummary = flag;
	}

	public boolean getVerboseLogging()
	{
		return this.verboseLogging;
	}

	public void setVerboseLogging(boolean flag)
	{
		this.verboseLogging = flag;
		this.showSummary = flag;
		this.stmtRunner.setVerboseLogging(flag);
		this.showTiming = this.verboseLogging;
	}

	public void setConsolidateLog(boolean flag)
	{
		this.consolidateMessages = flag;
	}

	public void setIgnoreDropErrors(boolean flag)
	{
		this.stmtRunner.setIgnoreDropErrors(flag);
	}

	public ConnectionProfile getProfile()
	{
		return this.profile;
	}

	public boolean hasProfile()
	{
		return this.profile != null;
	}

	public void setProfile(ConnectionProfile aProfile)
	{
		this.profile = aProfile;
	}

	public void setDelimiter(DelimiterDefinition delim)
	{
		if (delim != null) this.delimiter = delim;
	}

	public void setConnection(WbConnection conn)
	{
		this.connection = conn;
		this.stmtRunner.setConnection(this.connection);
	}

	public boolean isConnected()
	{
		return this.connection != null;
	}

	public void setShowProgress(boolean flag)
	{
		showProgress = flag;
		if (showProgress)
		{
			setRowMonitor(new GenericRowMonitor(new ConsoleStatusBar()));
		}
		else
		{
			setRowMonitor(null);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == this.stmtRunner)
		{
			this.connection = stmtRunner.getConnection();
		}
	}

	public void connect()
		throws SQLException, ClassNotFoundException
	{
		this.connection = null;

		if (this.profile == null)
		{
			// Allow batch runs without a profile for e.g. running a single WbCopy
			LogMgr.logWarning("BatchRunner.connect()", "No profile defined, proceeding without a connection.");
			success = true;
			return;
		}

		if (!profile.getStorePassword())
		{
			String pwd = ConsoleReaderFactory.getConsoleReader().readPassword(ResourceMgr.getString("MsgInputPwd"));
			profile.setInputPassword(pwd);
		}

		try
		{
			ConnectionMgr mgr = ConnectionMgr.getInstance();
			WbConnection c = mgr.getConnection(this.profile, connectionId);

			this.setConnection(c);
			String info = c.getDisplayString();
			LogMgr.logInfo("BatchRunner.connect()",  ResourceMgr.getFormattedString("MsgBatchConnectOk", c.getDisplayString()));
			if (verboseLogging)
			{
				this.printMessage(ResourceMgr.getFormattedString("MsgBatchConnectOk", info));
				String warn = c.getWarnings();
				if (!StringUtil.isEmptyString(warn) && !c.getProfile().isHideWarnings())
				{
					printMessage(warn);
					LogMgr.logWarning("BatchRunner.connect()", "Connection returned warnings: " + warn);
				}
			}
			success = true;
		}
		catch (ClassNotFoundException e)
		{
			String error = ResourceMgr.getString("ErrDriverNotFound");
			error = StringUtil.replace(error, "%class%", profile.getDriverclass());
			LogMgr.logError("BatchRunner.connect()", error, null);
			printMessage(error);
			success = false;
			throw e;
		}
		catch (SQLException e)
		{
			success = false;
			LogMgr.logError("BatchRunner.connect()", "Connection failed", e);
			printMessage(ResourceMgr.getString("ErrConnectFailed"));
			printMessage(ExceptionUtil.getDisplay(e));
			throw e;
		}
	}

	public void setMaxRows(int rows)
	{
		if (this.stmtRunner != null)
		{
			this.stmtRunner.setMaxRows(rows);
		}
	}

	public void setPersistentConnect(boolean flag)
	{
		WbConnect connect = (WbConnect)getCommand(WbConnect.VERB);
		connect.setPersistentChange(flag);
	}

	public List<DataStore> getQueryResults()
	{
		return queryResults;
	}

	public boolean isSuccess()
	{
		return this.success;
	}

	public void setSuccessScript(String aFilename)
	{
		if (aFilename == null) return;
		WbFile f = new WbFile(aFilename);
		if (f.exists() && !f.isDirectory())
		{
			this.successScript = aFilename;
		}
		else
		{
			LogMgr.logWarning("BatchRunner.setErrorScript()", "File '" + aFilename + "' specified for success script not found. No success script is used!");
			this.successScript = null;
		}
	}

	public void setErrorScript(String aFilename)
	{
		if (aFilename == null) return;
		File f = new WbFile(aFilename);
		if (f.exists() && !f.isDirectory())
		{
			this.errorScript = aFilename;
		}
		else
		{
			LogMgr.logWarning("BatchRunner.setErrorScript()", "File '" + aFilename + "' specified for error script not found. No error script is used!");
			this.errorScript = null;
		}
	}

	public void setRowMonitor(RowActionMonitor mon)
	{
		this.rowMonitor = mon;
		this.stmtRunner.setRowMonitor(this.rowMonitor);
	}

	/**
	 * Define a sql script that should be run instead of a list of files.
	 * @param sql
	 */
	public void setScriptToRun(String sql)
	{
		this.command = sql;
	}

	protected void runScript()
	{
		try
		{
			runScript(command);
			this.success = true;
		}
		catch (Exception e)
		{
			LogMgr.logError("BatchRunner.execute()", ResourceMgr.getString("MsgBatchStatementError"), e);
			String msg = ExceptionUtil.getDisplay(e);
			if (showProgress) printMessage(""); // force newline in case progress reporting was turned on
			printMessage(ResourceMgr.getString("TxtError") + ": " + msg);
			this.success = false;
		}
	}

	public void execute()
	{
		queryResults.clear();
		if (CollectionUtil.isNonEmpty(filenames))
		{
			runFiles();
		}
		else
		{
			runScript();
		}
	}

	protected void runFiles()
	{
		boolean error = false;
		int count = this.filenames.size();

		String typeKey = "batchRunnerFileLoop";

		if (this.rowMonitor  != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
			this.rowMonitor.saveCurrentType(typeKey);
		}

		int currentFileIndex = 0;

		for (String file : filenames)
		{
			currentFileIndex ++;

			WbFile fo = new WbFile(file);

			if (this.rowMonitor != null)
			{
				this.rowMonitor.restoreType(typeKey);
				this.rowMonitor.setCurrentObject(file, currentFileIndex, count);
			}

			try
			{
				String msg = ResourceMgr.getString("MsgBatchProcessingFile") + " " + fo.getFullPath();
				LogMgr.logInfo("BatchRunner.execute()", msg);
				if (this.resultDisplay != null && verboseLogging)
				{
					this.resultDisplay.appendToLog(msg);
					this.resultDisplay.appendToLog("\n");
				}
				String dir = fo.getCanonicalFile().getParent();
				this.setBaseDir(dir);

				error = this.executeScript(fo);
			}
			catch (Exception e)
			{
				error = true;
				LogMgr.logError("BatchRunner.execute()", ResourceMgr.getString("MsgBatchScriptFileError") + " " + file, e);
				String msg = null;

				if (e instanceof FileNotFoundException)
				{
					msg = ResourceMgr.getFormattedString("ErrFileNotFound", file);
				}
				else
				{
					msg = e.getMessage();
				}
				if (showProgress) printMessage(""); // force newline in case progress reporting was turned on
				printMessage(ResourceMgr.getString("TxtError") + ": " + msg);
			}
			if (error && abortOnError)
			{
				break;
			}
		}

		if (this.rowMonitor  != null)
		{
			this.rowMonitor.jobFinished();
		}

		if (abortOnError && error)
		{
			this.success = false;
			try
			{
				if (this.errorScript != null)
				{
					WbFile f = new WbFile(errorScript);
					LogMgr.logInfo("BatchRunner.runFiles()", ResourceMgr.getString("MsgBatchExecutingErrorScript") + " " + f.getFullPath());
					this.executeScript(f);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner.runFiles()", ResourceMgr.getString("MsgBatchScriptFileError") + " " + this.errorScript, e);
			}
		}
		else
		{
			this.success = true;
			try
			{
				if (this.successScript != null)
				{
					WbFile f = new WbFile(successScript);
					LogMgr.logInfo("BatchRunner.runFiles()", ResourceMgr.getString("MsgBatchExecutingSuccessScript") + " " + f.getFullPath());
					this.executeScript(f);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner.runFiles()", ResourceMgr.getString("MsgBatchScriptFileError") + " " + this.successScript, e);
			}
		}
	}

	public String getMessages()
	{
		if (errors == null) return null;
		return errors.getBuffer().toString();
	}

	public void cancel()
	{
		this.cancelExecution = true;
		if (this.stmtRunner != null)
		{
			this.stmtRunner.cancel();
		}
	}

	public ResultSetConsumer getResultSetConsumer()
	{
		if (this.stmtRunner != null)
		{
			return stmtRunner.getConsumer();
		}
		return null;
	}

	public void setResultSetConsumer(ResultSetConsumer consumer)
	{
		if (this.stmtRunner != null)
		{
			this.stmtRunner.setConsumer(consumer);
		}
	}

	private ScriptParser createParser()
	{
		ScriptParser parser = new ScriptParser(ParserType.getTypeFromConnection(connection));
		// If no delimiter has been defined, than use the default fallback
		if (this.delimiter == null)
		{
			DelimiterDefinition altDelim = Settings.getInstance().getAlternateDelimiter(this.connection, null);
			parser.setDelimiters(DelimiterDefinition.STANDARD_DELIMITER, altDelim);
		}
		else
		{
			// If the ScriptParser supports dynamic alternate delimiters
			// use the defined delimiter as the alternate delimiter
			if (parser.supportsMixedDelimiter())
			{
				parser.setAlternateDelimiter(this.delimiter);
			}
			else
			{
				// if the parser only supports a single delimiter
				// use the defined one, and don't use an alternate delimiter
				parser.setDelimiters(this.delimiter, null);
			}
		}

		parser.setCheckEscapedQuotes(this.checkEscapedQuotes);
		return parser;
	}

	private boolean executeScript(WbFile scriptFile)
		throws IOException
	{
		try
		{
			isBusy = true;
			ScriptParser parser = createParser();
			parser.setFile(scriptFile, this.encoding);
			return executeScript(parser);
		}
		finally
		{
			isBusy = false;
		}
	}

	/**
	 * Execute the given script
	 * @param script
	 * @return true if an error occurred
	 * @throws IOException
	 */
	public boolean runScript(String script)
		throws IOException
	{
		ScriptParser parser = createParser();
		parser.setScript(script);
		try
		{
			isBusy = true;
			boolean result = executeScript(parser);
			if (this.rowMonitor  != null)
			{
				this.rowMonitor.jobFinished();
			}
			return result;
		}
		finally
		{
			isBusy = false;
		}
	}

	private boolean executeScript(ScriptParser parser)
		throws IOException
	{
		boolean error = false;
		errors = null;

		this.cancelExecution = false;

		int executedCount = 0;
		long start, end;

		int interval;
		int length = parser.getScriptLength();
		long totalStatements = parser.getStatementCount();

		if (totalStatements < 0)
		{
			if (length < 50000)
			{
				interval = 1;
			}
			else if (length < 250000)
			{
				interval = 10;
			}
			else
			{
				interval = 100;
			}
		}
		else
		{
			if (totalStatements > 100)
			{
				interval = (int)(totalStatements / (totalStatements * 0.5));
			}
			else
			{
				interval = 1;
			}
		}

		start = System.currentTimeMillis();

		parser.startIterator();
		long totalRows = 0;
		long errorCount = 0;

		boolean logAllStatements = Settings.getInstance().getLogAllStatements();
		while (parser.hasNext())
		{
			String sql = parser.getNextCommand();
			if (sql == null) continue;

			try
			{
				if (replacer != null)
				{
					sql = replacer.replace(sql);
				}

				if (printStatements)
				{
					printMessage(sql.trim());
					if (!logAllStatements)
					{
						StatementRunner.logStatement(sql, -1, connection);
					}
				}
				else if (!logAllStatements)
				{
					LogMgr.logDebug("BatchRunner.executeScript()", ResourceMgr.getString("MsgBatchExecutingStatement") + ": "  + sql);
				}

				long verbstart = System.currentTimeMillis();
				this.stmtRunner.runStatement(sql);
				long verbend = System.currentTimeMillis();
				this.stmtRunner.statementDone();

				error = false;

				StatementRunnerResult result = this.stmtRunner.getResult();

				if (result != null)
				{
					error = !result.isSuccess();

					// We have to store the result of hasMessages()
					// as the getMessageBuffer() will clear the buffer
					// and a subsequent call to hasMessages() will return false;
					boolean hasMessage = result.hasMessages();
					String feedback = result.getMessageBuffer().toString();

					if (error)
					{
						LogMgr.logError("BatchRunner.execute()", feedback, null);
						errorCount ++;
						if (storeErrorMessages)
						{
							if (errors == null)
							{
								errors = new MessageBuffer();
							}
							errors.appendNewLine();
							errors.append(feedback);
						}
					}
					else
					{
						if (result.hasWarning()) LogMgr.logWarning("BatchRunner.execute()", feedback);
						totalRows += result.getTotalUpdateCount();
					}

					printResults(sql, result);

					if (hasMessage && (this.stmtRunner.getVerboseLogging() || error))
					{
						if (!this.consolidateMessages)
						{
							if (!showResultSets) printMessage(""); // force newline
							this.printMessage(feedback);
						}
					}
					else if (result.hasWarning() && stmtRunner.getVerboseLogging())
					{
						String verb = SqlUtil.getSqlVerb(sql);
						String msg = StringUtil.replace(ResourceMgr.getString("MsgStmtCompletedWarn"), "%verb%", verb);
						this.printMessage("\n" + msg);
					}
					executedCount ++;
				}

				if (this.showTiming && showStatementTiming && !consolidateMessages)
				{
					DurationFormatter f = new DurationFormatter();
					long millis = (verbend - verbstart);
					boolean includeFraction = (millis < DurationFormatter.ONE_MINUTE);
					String time = f.formatDuration(millis, includeFraction);
					this.printMessage(ResourceMgr.getString("MsgSqlVerbTime") + " " + time);
				}

				if (this.rowMonitor != null && (executedCount % interval == 0) && !printStatements)
				{
					this.rowMonitor.setCurrentRow(executedCount, totalStatements);
				}

				if (result != null && result.stopScript())
				{
					String cancelMsg = ResourceMgr.getString("MsgScriptCancelled");
					printMessage(cancelMsg);
					break;
				}

				if (this.cancelExecution)
				{
					if (verboseLogging) this.printMessage(ResourceMgr.getString("MsgStatementCancelled"));
					break;
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner", ResourceMgr.getString("MsgBatchStatementError") + " "  + sql, e);
				printMessage(ExceptionUtil.getDisplay(e));
				error = true;
				break;
			}
			if (error && abortOnError) break;
		}

		end = System.currentTimeMillis();

		if (showSummary)
		{
			StringBuilder msg = new StringBuilder(50);
			WbFile scriptFile = parser.getScriptFile();
			if (scriptFile != null)
			{
				msg.append(scriptFile.getFullPath());
				msg.append(": ");
			}
			msg.append(ResourceMgr.getFormattedString("MsgTotalStatementsExecuted", executedCount));
			if (resultDisplay == null) msg.insert(0, '\n'); // force newline on console
			this.printMessage(msg.toString());
		}

		if (consolidateMessages)
		{
			if (errorCount > 0)
			{
				printMessage((resultDisplay == null ? "\n" : "") + ResourceMgr.getFormattedString("MsgTotalStatementsFailed", errorCount));
			}
			if (verboseLogging) this.printMessage(ResourceMgr.getFormattedString("MsgRowsAffected", totalRows));
		}

		parser.done();

		if (this.showTiming)
		{
			long millis = (end - start);
			DurationFormatter f = new DurationFormatter();
			boolean includeFraction = (millis < DurationFormatter.ONE_MINUTE);
			String time = f.formatDuration(millis, includeFraction);
			String m = ResourceMgr.getString("MsgExecTime") + " " + time;
			this.printMessage(m);
		}

		return error;
	}

	private void printResults(String sql, StatementRunnerResult result)
	{
		if (!this.showResultSets) return;

		if (result == null) return;
		if (!result.isSuccess()) return;
		if (!result.hasDataStores()) return;

		List<DataStore> data = result.getDataStores();

		if (console == null)
		{
			for (DataStore ds : data)
			{
				queryResults.add(ds);
			}
			return;
		}


		if (showStatementWithResult && !printStatements)
		{
			console.println(sql.trim());
		}

		RowDisplay current = ConsoleSettings.getInstance().getNextRowDisplay();
		boolean rowsAsLine = (current != null && current == RowDisplay.SingleLine);

		for (int i=0; i < data.size(); i++)
		{
			DataStore ds = data.get(i);
			if (ds != null)
			{
				DataStorePrinter printer = new DataStorePrinter(ds);
				printer.setFormatColumns(optimizeCols);
				printer.setPrintRowCount(result.getShowRowCount());
				printer.setPrintRowsAsLine(rowsAsLine);
				printer.printTo(console);
				if (i < data.size() -1) console.println();
			}
		}

	}

	public void setEncoding(String enc)
		throws UnsupportedEncodingException
	{
		if (enc == null)
		{
			this.encoding = null;
		}
		else
		{
			if (!EncodingUtil.isEncodingSupported(enc))
				throw new UnsupportedEncodingException(enc + " encoding not supported!");
			this.encoding = EncodingUtil.cleanupEncoding(enc);
		}
	}

	public void setShowTiming(boolean flag)
	{
		this.showTiming = flag;
		this.showStatementTiming = flag;
	}

	public void setShowStatementTiming(boolean flag)
	{
		this.showStatementTiming = flag;
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
			if (msg != null && msg.length() > 0)
			{
				System.out.print('\r');
				System.out.println(msg);
			}
		}
		else
		{
			this.resultDisplay.appendToLog(msg);
			this.resultDisplay.appendToLog("\n");
		}
	}

	public static boolean hasConnectionArgument(ArgumentParser cmdLine)
	{
		if (!cmdLine.hasArguments()) return false;
		if (cmdLine.isArgPresent(AppArguments.ARG_CONN_DESCRIPTOR)) return true;
		if (cmdLine.isArgPresent(AppArguments.ARG_CONN_URL)) return true;
		if (cmdLine.isArgPresent(AppArguments.ARG_CONN_USER)) return true;
		if (cmdLine.isArgPresent(AppArguments.ARG_CONN_JAR)) return true;
		if (cmdLine.isArgPresent(AppArguments.ARG_CONN_DRIVER)) return true;
		if (cmdLine.isArgPresent(AppArguments.ARG_CONN_PWD)) return true;
		return false;
	}

	public static ConnectionProfile createCmdLineProfile(ArgumentParser cmdLine)
	{
		if (!hasConnectionArgument(cmdLine)) return null;

		ConnectionProfile result = null;

		String url = cmdLine.getValue(AppArguments.ARG_CONN_URL);
		String driverclass = cmdLine.getValue(AppArguments.ARG_CONN_DRIVER);
		if (driverclass == null)
		{
			driverclass = cmdLine.getValue(AppArguments.ARG_CONN_DRIVER_CLASS);
		}

		String descriptor = cmdLine.getValue(AppArguments.ARG_CONN_DESCRIPTOR);

		if (descriptor == null)
		{
			if (url == null)
			{
				LogMgr.logWarning("BatchRunner.createCmdLineProfile()", "Cannot connect using command line settings without a connection URL!", null);
				return null;
			}

			if (driverclass == null)
			{
				driverclass = ConnectionDescriptor.findDriverClassFromUrl(url);
			}

			if (driverclass == null)
			{
				LogMgr.logWarning("BatchRunner.createCmdLineProfile()", "Cannot connect using command line settings without a driver class!", null);
				return null;
			}

			String user = cmdLine.getValue(AppArguments.ARG_CONN_USER);
			String pwd = cmdLine.getValue(AppArguments.ARG_CONN_PWD);
			String jar = cmdLine.getValue(AppArguments.ARG_CONN_JAR);

			if (jar != null)
			{
				WbFile jarFile = new WbFile(jar);
				ConnectionMgr.getInstance().registerDriver(driverclass, jarFile.getFullPath());
			}
			result = ConnectionProfile.createEmptyProfile();
			result.setDriverclass(driverclass);
			result.setUrl(url);

			if (cmdLine.isArgPresent(AppArguments.ARG_CONN_PWD))
			{
				result.setPassword(pwd);
				result.setStorePassword(true);
			}
			else
			{
				result.setStorePassword(false);
			}
			result.setUsername(user);
		}
		else
		{
			try
			{
				ConnectionDescriptor parser = new ConnectionDescriptor();
				result = parser.parseDefinition(descriptor);
			}
			catch (InvalidConnectionDescriptor icd)
			{
				LogMgr.logError("BatchRunner.createCmdLineProfile()", "Invalid connection descriptor: " + descriptor, icd);
				return null;
			}
		}

		try
		{
			String commit =  cmdLine.getValue(AppArguments.ARG_CONN_AUTOCOMMIT);
			String wksp = cmdLine.getValue(AppArguments.ARG_WORKSPACE);
			String delimDef = cmdLine.getValue(AppArguments.ARG_ALT_DELIMITER);
			String title = cmdLine.getValue(AppArguments.ARG_CONN_NAME, CMD_LINE_PROFILE_NAME);
			DelimiterDefinition delim = DelimiterDefinition.parseCmdLineArgument(delimDef);
			boolean trimCharData = cmdLine.getBoolean(AppArguments.ARG_CONN_TRIM_CHAR, false);
			boolean rollback = cmdLine.getBoolean(AppArguments.ARG_CONN_ROLLBACK, false);
			boolean separate = cmdLine.getBoolean(AppArguments.ARG_CONN_SEPARATE, true);

			Map<String, String> props = cmdLine.getMapValue(AppArguments.ARG_CONN_PROPS);

			result.setTemporaryProfile(true);
			result.setName(title);
			result.setDriverName(null);
			result.setStoreExplorerSchema(false);
			result.setRollbackBeforeDisconnect(rollback);
			result.setAlternateDelimiter(delim);
			result.setTrimCharData(trimCharData);
			result.setUseSeparateConnectionPerTab(separate);
			result.setEmptyStringIsNull(cmdLine.getBoolean(AppArguments.ARG_CONN_EMPTYNULL, false));
			result.setRemoveComments(cmdLine.getBoolean(AppArguments.ARG_CONN_REMOVE_COMMENTS, false));
			result.setDetectOpenTransaction(cmdLine.getBoolean(AppArguments.ARG_CONN_CHECK_OPEN_TRANS, false));
			result.setReadOnly(cmdLine.getBoolean(AppArguments.ARG_READ_ONLY, false));
			result.setHideWarnings(cmdLine.getBoolean(AppArguments.ARG_HIDE_WARNINGS, false));
			int fetchSize = cmdLine.getIntValue(AppArguments.ARG_CONN_FETCHSIZE, -1);
			if (fetchSize > -1)
			{
				result.setDefaultFetchSize(fetchSize);
			}

			if (props != null && props.size() > 0)
			{
				Properties p = new Properties();
				p.putAll(props);
				result.setConnectionProperties(p);
			}

			if (!StringUtil.isEmptyString(wksp))
			{
				wksp = FileDialogUtil.replaceConfigDir(wksp);
				File f = new WbFile(wksp);
				if (!f.exists() && !f.isAbsolute())
				{
					f = new WbFile(Settings.getInstance().getConfigDir(), wksp);
				}
				if (f.exists())
				{
					result.setWorkspaceFile(f.getAbsolutePath());
				}
			}
			if (!StringUtil.isEmptyString(commit))
			{
				result.setAutocommit(StringUtil.stringToBool(commit));
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("BatchRunner.initFromCommandLine()", "Error creating temporary profile", e);
			result = null;
		}
		return result;
	}

	public static BatchRunner createBatchRunner(ArgumentParser cmdLine)
	{
		return createBatchRunner(cmdLine, true);
	}

	public static BatchRunner createBatchRunner(ArgumentParser cmdLine, boolean checkScriptPresence)
	{
		String scripts = cmdLine.getValue(AppArguments.ARG_SCRIPT);
		String sqlcmd = cmdLine.getValue(AppArguments.ARG_COMMAND);

		if (checkScriptPresence && StringUtil.isBlank(scripts) && StringUtil.isBlank(sqlcmd)) return null;

		String profilename = cmdLine.getValue(AppArguments.ARG_PROFILE);

		boolean abort = cmdLine.getBoolean(AppArguments.ARG_ABORT, true);
		boolean showResult = cmdLine.getBoolean(AppArguments.ARG_DISPLAY_RESULT);
		boolean showProgress = cmdLine.getBoolean(AppArguments.ARG_SHOWPROGRESS, false);
		boolean consolidateLog = cmdLine.getBoolean(AppArguments.ARG_CONSOLIDATE_LOG, false);

		String encoding = cmdLine.getValue(AppArguments.ARG_SCRIPT_ENCODING);

		ConnectionProfile profile = null;
		if (profilename == null)
		{
			profile = createCmdLineProfile(cmdLine);
		}
		else
		{
			String group = cmdLine.getValue(AppArguments.ARG_PROFILE_GROUP);
			ProfileKey def = new ProfileKey(StringUtil.trimQuotes(profilename), StringUtil.trimQuotes(group));

			profile = ConnectionMgr.getInstance().getProfile(def);
			if (profile == null)
			{
				String msg = ResourceMgr.getFormattedString("ErrProfileNotFound", def);
				LogMgr.logError("BatchRunner.createBatchRunner()", msg, null);
			}
			boolean readOnly = cmdLine.getBoolean(AppArguments.ARG_READ_ONLY, false);
			if (readOnly)
			{
				profile.setReadOnly(readOnly);
				// Reset the changed flag to make sure the "modified" profile is not saved
				profile.reset();
			}
		}

		if (cmdLine.hasUnknownArguments())
		{
			StringBuilder err = new StringBuilder(ResourceMgr.getString("ErrUnknownParameter"));
			if (!WbManager.getInstance().isGUIMode())
			{
				err.append(' ');
				err.append(cmdLine.getUnknownArguments());
				System.err.println(err.toString());
			}
			LogMgr.logWarning("BatchRunner.createBatchRunner()", err.toString());
		}

		if (profile != null)
		{
			boolean ignoreDrop = cmdLine.getBoolean(AppArguments.ARG_IGNORE_DROP, true);
			profile.setIgnoreDropErrors(ignoreDrop);
		}

		String success = cmdLine.getValue(AppArguments.ARG_SUCCESS_SCRIPT);
		String error = cmdLine.getValue(AppArguments.ARG_ERROR_SCRIPT);
		String feed = cmdLine.getValue(AppArguments.ARG_FEEDBACK);
		boolean feedback = cmdLine.getBoolean(AppArguments.ARG_FEEDBACK, true);
		boolean interactive = cmdLine.getBoolean(AppArguments.ARG_INTERACTIVE, false);

		BatchRunner runner = null;
		if (StringUtil.isNonBlank(scripts))
		{
			runner = new BatchRunner(scripts);
		}
		else
		{
			runner = new BatchRunner();
			runner.setScriptToRun(sqlcmd);
		}

		runner.showResultSets(showResult);
		try
		{
			runner.setEncoding(encoding);
		}
		catch (Exception e)
		{
			LogMgr.logError("BatchRunner.createBatchRunner()", "Invalid encoding '" + encoding + "' specified. Using platform default'", null);
		}

		runner.setAbortOnError(abort);
		runner.setErrorScript(error);
		runner.setSuccessScript(success);
		runner.setProfile(profile);
		runner.setVerboseLogging(feedback);
		if (!feedback)
		{
			runner.setShowStatementSummary(false);
			runner.setShowStatementWithResult(false);
		}
		runner.setConsolidateLog(consolidateLog);
		runner.setShowProgress(showProgress);

		if (interactive)
		{
			ConsolePrompter prompter = new ConsolePrompter();
			runner.setExecutionController(prompter);
			runner.setParameterPrompter(prompter);
		}

		// if no showTiming argument was provided but feedback was disabled
		// disable the display of the timing information as well.
		String tim = cmdLine.getValue(AppArguments.ARG_SHOW_TIMING);
		if (tim == null && feed != null && !feedback)
		{
			runner.showTiming = false;
		}
		else
		{
			runner.showTiming = cmdLine.getBoolean(AppArguments.ARG_SHOW_TIMING, true);
		}

		DelimiterDefinition delim = DelimiterDefinition.parseCmdLineArgument(cmdLine.getValue(AppArguments.ARG_DELIMITER));

		if (delim != null)
		{
			runner.setDelimiter(delim);
		}

		return runner;
	}
}
