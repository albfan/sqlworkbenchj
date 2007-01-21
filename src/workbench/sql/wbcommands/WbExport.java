/*
 * WbExport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CharacterRange;
import workbench.util.EncodingUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.db.TableIdentifier;
import workbench.util.ExceptionUtil;
import workbench.util.WbFile;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbExport
	extends SqlCommand
	implements RowActionMonitor
{
	public static final String VERB = "WBEXPORT";
	private DataExporter exporter;
	private boolean directExport = false;
	private boolean continueOnError = false;
	private String currentTable;
	private String defaultExtension;
	private boolean showProgress = true;
	private int progressInterval = 1;

	public WbExport()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("type", StringUtil.stringToList("text,xml,sqlinsert,sqlupdate,sqldeleteinsert,html"));
		cmdLine.addArgument("file");
		cmdLine.addArgument("title");
		cmdLine.addArgument("table");
		cmdLine.addArgument("delimiter");
		cmdLine.addArgument("quotechar");
		cmdLine.addArgument("dateFormat");
		cmdLine.addArgument("timestampFormat");
		cmdLine.addArgument("decimal");
		cmdLine.addArgument("charFunc");
		cmdLine.addArgument("concat");
		cmdLine.addArgument("concatFunc");
		cmdLine.addArgument("commitEvery");
		cmdLine.addArgument("header", ArgumentType.BoolArgument);
		cmdLine.addArgument("createTable", ArgumentType.BoolArgument);
		cmdLine.addArgument("nodata", ArgumentType.BoolArgument);
		cmdLine.addArgument("encoding");
		cmdLine.addArgument("showProgress");
		cmdLine.addArgument("keyColumns");
		cmdLine.addArgument("append", ArgumentType.BoolArgument);
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT);
		cmdLine.addArgument("escapeHTML", ArgumentType.BoolArgument);
		cmdLine.addArgument("createFullHTML", ArgumentType.BoolArgument);
		cmdLine.addArgument("sourceTable", ArgumentType.TableArgument);
		cmdLine.addArgument("outputDir");
		cmdLine.addArgument("useCDATA", ArgumentType.BoolArgument);
		cmdLine.addArgument("escapeText", StringUtil.stringToList("control,7bit,8bit,extended,none"));
		cmdLine.addArgument("quoteAlways", ArgumentType.BoolArgument);
		cmdLine.addArgument("lineEnding");
		cmdLine.addArgument("showEncodings");
		cmdLine.addArgument("verboseXML", ArgumentType.BoolArgument);
		//cmdLine.addArgument("oraldr");
		cmdLine.addArgument("writeOracleLoader", ArgumentType.BoolArgument);
		cmdLine.addArgument("compress", ArgumentType.BoolArgument);
		cmdLine.addArgument("blobIdCols");
		cmdLine.addArgument("lobIdCols");
		cmdLine.addArgument("blobType");
		cmdLine.addArgument("clobAsFile", ArgumentType.BoolArgument);
		cmdLine.addArgument("continueOnError", ArgumentType.BoolArgument);
	}
	
	public String getVerb() { return VERB; }

	private String getWrongArgumentsMessage()
	{
		String msg = ResourceMgr.getString("ErrExportWrongParameters");
		msg = StringUtil.replace(msg, "%header_flag_default%", Boolean.toString(getTextHeaderDefault()));
		msg = StringUtil.replace(msg, "%verbose_default%", Boolean.toString(getVerboseXmlDefault()));
		return msg;
	}
	
	private boolean getVerboseXmlDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.export.xml.default.verbose", true);
	}
	
	private boolean getTextHeaderDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.export.text.default.header", false);
	}
	
	public StatementRunnerResult execute(WbConnection aConnection, String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		this.currentConnection = aConnection;
		
		sql = SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql,false,false,'\''));

		cmdLine.parse(sql);
		
		if (cmdLine.isArgPresent("showencodings"))
		{
			result.addMessage(ResourceMgr.getString("MsgAvailableEncodings"));
			result.addMessage("");
			String[] encodings = EncodingUtil.getEncodings();
			for (int i=0; i < encodings.length; i++)
			{
				result.addMessage(encodings[i]);
			}
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, getWrongArgumentsMessage());
			return result;
		}
		
		String type = null;
		String file = null;


		type = cmdLine.getValue("type");
		if (!isTypeValid(type))
		{
			result.addMessage(ResourceMgr.getString("ErrExportWrongType"));
			result.addMessage("");
			result.addMessage(getWrongArgumentsMessage());
			result.setFailure();
			return result;
		}
		
		this.exporter = new DataExporter(this.currentConnection);
		file = cmdLine.getValue("file");
		String tables = cmdLine.getValue("sourcetable");

		String outputdir = cmdLine.getValue("outputdir");

		if (type == null)
		{
			result.addMessage(ResourceMgr.getString("ErrExportTypeRequired"));
			result.addMessage("");
			result.addMessage(getWrongArgumentsMessage());
			result.setFailure();
			return result;
		}

		if (file == null && outputdir == null)
		{
			result.addMessage(ResourceMgr.getString("ErrExportFileRequired"));
			result.addMessage("");
			result.addMessage(getWrongArgumentsMessage());
			result.setFailure();
			return result;
		}

		String updateTable = cmdLine.getValue("table");
		type = type.trim().toLowerCase();

		String encoding = cmdLine.getValue("encoding");
		if (encoding != null) exporter.setEncoding(encoding);
		exporter.setAppendToFile(cmdLine.getBoolean("append"));
		exporter.setWriteClobAsFile(cmdLine.getBoolean("clobasfile", false));
		
		this.continueOnError = cmdLine.getBoolean("continueonerror", false);
		
		if ("text".equals(type) || "txt".equals(type))
		{
			exporter.setWriteOracleControlFile(cmdLine.getBoolean("writeoracleloader", false));
			
			String delimiter = cmdLine.getValue("delimiter");
			if (delimiter != null) exporter.setTextDelimiter(delimiter);

			String quote = cmdLine.getValue("quotechar");
			if (quote != null) exporter.setTextQuoteChar(quote);

			String format = cmdLine.getValue("dateformat");
			if (format != null) exporter.setDateFormat(format);

			format = cmdLine.getValue("timestampformat");
			if (format != null) exporter.setTimestampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) exporter.setDecimalSymbol(format);

			exporter.setExportHeaders(cmdLine.getBoolean("header", getTextHeaderDefault()));

			String escape = cmdLine.getValue("escapetext");
			if (escape != null)
			{
				if ("control".equalsIgnoreCase(escape) ||"ctrl".equalsIgnoreCase(escape))
				{
					exporter.setEscapeRange(CharacterRange.RANGE_CONTROL);
				}
				else if ("7bit".equalsIgnoreCase(escape))
				{
					exporter.setEscapeRange(CharacterRange.RANGE_7BIT);
				}
				else if ("8bit".equalsIgnoreCase(escape) || "true".equalsIgnoreCase(escape))
				{
					exporter.setEscapeRange(CharacterRange.RANGE_8BIT);
				}
				else if ("extended".equalsIgnoreCase(escape))
				{
					exporter.setEscapeRange(CharacterRange.RANGE_8BIT_EXTENDED);
				}
				else if ("none".equalsIgnoreCase(escape) || "false".equalsIgnoreCase(escape))
				{
					exporter.setEscapeRange(null);
				}
				else
				{
					exporter.setEscapeRange(null);
					String msg = ResourceMgr.getString("ErrExportInvalidEscapeRangeIgnored").replaceAll("%value%", escape);
					result.addMessage(msg);
				}
			}
			exporter.setQuoteAlways(cmdLine.getBoolean("quotealways"));
			this.defaultExtension = ".txt";
		}
		else if (type.startsWith("sql"))
		{
			exporter.setIncludeCreateTable(cmdLine.getBoolean("createtable"));
			exporter.setChrFunction(cmdLine.getValue("charfunc"));
			exporter.setConcatFunction(cmdLine.getValue("concatfunc"));
			exporter.setConcatString(cmdLine.getValue("concat"));
			int commit = StringUtil.getIntValue(cmdLine.getValue("commitevery"),-1);
			exporter.setCommitEvery(commit);
			exporter.setAppendToFile(cmdLine.getBoolean("append"));
			if (updateTable != null) exporter.setTableName(updateTable);
			String c = cmdLine.getValue("keycolumns");
			if (c != null)
			{
				List cols = StringUtil.stringToList(c, ",");
				exporter.setKeyColumnsToUse(cols);
			}
			String bmode = cmdLine.getValue("blobtype");
			exporter.setBlobMode(bmode);
			this.defaultExtension = ".sql";
		}
		else if ("xml".equals(type))
		{
			String format = cmdLine.getValue("dateformat");
			if (format != null) exporter.setDateFormat(format);

			format = cmdLine.getValue("timestampformat");
			if (format != null) exporter.setTimestampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) exporter.setDecimalSymbol(format);

			String xsl = cmdLine.getValue(WbXslt.ARG_STYLESHEET);
			String output = cmdLine.getValue(WbXslt.ARG_OUTPUT);

			boolean verboseDefault = getVerboseXmlDefault(); 
			boolean verbose = cmdLine.getBoolean("verbosexml", verboseDefault);
			exporter.setUseVerboseFormat(verbose);

			if (xsl != null && output != null)
			{
				File f = new File(xsl);
				if (f.exists())
				{
					exporter.setXsltTransformation(xsl);
					exporter.setXsltTransformationOutput(output);
				}
				else
				{
					String msg = ResourceMgr.getString("ErrSpoolXsltNotFound");
					msg = msg.replaceAll("%xslt%", f.getAbsolutePath());
					result.addMessage(msg);
				}
			}
			this.exporter.setUseCDATA(cmdLine.getBoolean("usecdata"));
			if (updateTable != null) exporter.setTableName(updateTable);
			this.defaultExtension = ".xml";
		}
		else if ("html".equals(type))
		{
			// change the contents of type in order to display it properly
			String format = cmdLine.getValue("dateformat");
			if (format != null) exporter.setDateFormat(format);

			format = cmdLine.getValue("timestampformat");
			if (format != null) exporter.setTimestampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) exporter.setDecimalSymbol(format);

			exporter.setHtmlTitle(cmdLine.getValue("title"));

			String value = cmdLine.getValue("escapehtml");
			if (value != null)
			{
				exporter.setEscapeHtml("true".equalsIgnoreCase(value));
			}
			value = cmdLine.getValue("createfullhtml");
			if (value != null)
			{
				exporter.setCreateFullHtmlPage("true".equalsIgnoreCase(value));
			}
			if (updateTable != null) exporter.setTableName(updateTable);
			this.defaultExtension = ".html";
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrExportWrongParameters"));
			result.setFailure();
			return result;
		}
		String ending = cmdLine.getValue("lineending");
		if (ending != null)
		{
			if ("crlf".equalsIgnoreCase(ending) ||
			    "dos".equalsIgnoreCase(ending) ||
			    "win".equalsIgnoreCase(ending) ||
				  "\\r\\n".equals(ending))
			{
				exporter.setLineEnding("\r\n");
			}
			else if ("lf".equalsIgnoreCase(ending) ||
			    "unix".equalsIgnoreCase(ending) ||
			    "linux".equalsIgnoreCase(ending) ||
				  "\\n".equals(ending))
			{
				exporter.setLineEnding("\n");
			}
		}

		String cols = cmdLine.getValue("lobidcols");
		if (cols == null) 
		{
			cols = cmdLine.getValue("blobidcols");
			if (cols != null) 
			{
				result.addMessage("The blobIdCols parameter is deprecated, please use lobIdCols");
			}
		}
		List columns = StringUtil.stringToList(cols, ",", true, true, false);
		this.exporter.setBlobIdColumns(columns);
		this.exporter.setCompressOutput(cmdLine.getBoolean("compress", false));
		
		file = evaluateFileArgument(file);
		this.exporter.setOutputFilename(file);

		// Setting the output type should be the last step in the configuration
		// of the exporter as this will trigger some initialization 
		// that depends on the other properties
		setExportType(exporter, type);
		
		List tablesToExport = null;
		if (tables != null)
		{
			tablesToExport = StringUtil.stringToList(tables, ",");
			this.directExport = (tablesToExport.size() > 0);
		}

		String value = cmdLine.getValue("showprogress");
		if (value == null || "true".equalsIgnoreCase(value))
		{
			this.progressInterval = DataExporter.DEFAULT_PROGRESS_INTERVAL;
		}
		else if ("false".equalsIgnoreCase(value))
		{
			this.progressInterval = 0;
		}
		else
		{
			this.progressInterval = StringUtil.getIntValue(value, -1);
		}
		this.showProgress = (this.progressInterval > 0);

		if (file != null)
		{
			// Check the outputfile right now, so the user does not have
			// to wait for a possible error message until the ResultSet
			// from the SELECT statement comes in...
			WbFile f = new WbFile(file);
			boolean canWrite = true;
			String msg = null;
			try
			{
				// File.canWrite() does not work reliably. It will report
				// an error if the file does not exist, but still could 
				// be written. 
				if (f.exists())
				{
					msg = ResourceMgr.getString("ErrExportFileWrite");
					canWrite = f.canWrite();
				}
				else
				{
					// try to create the file
					f.tryCreate();
				}
			}
			catch (IOException e)
			{
				canWrite = false;
				msg = ResourceMgr.getString("ErrExportFileCreate") + " " + e.getMessage();
			}
			if (!canWrite)
			{
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
		}
		
		if (!this.directExport)
		{

			// Waiting for the next SQL Statement...
			this.exporter.setRowMonitor(this.rowMonitor);
			this.exporter.setProgressInterval(this.progressInterval);

			String msg = ResourceMgr.getString("MsgSpoolInit");
			msg = StringUtil.replace(msg, "%type%", exporter.getTypeDisplay());
			msg = StringUtil.replace(msg, "%file%", file);
			//msg = msg + " quote=" + exporter.getTextQuoteChar();
			result.addMessage(msg);
			if (this.maxRows > 0)
			{
				msg = ResourceMgr.getString("MsgExportMaxRowsWarning").replaceAll("%maxrows%", Integer.toString(maxRows));
				result.addMessage(msg);
			}
		}
		else
		{
			try
			{
				runTableExports(tablesToExport, result, outputdir);
			}
			catch (Exception e)
			{
				LogMgr.logError("WbExport.execute()", "Error when running table export", e);
				result.addMessage(e.getMessage());
				result.setFailure();
			}
		}
		return result;
	}

	private boolean isTypeValid(String type)
	{
		if (type == null) return false;
		if (type.equals("sql") || type.equals("sqlinsert"))
		{
			return true;
		}
		else if (type.equals("sqlupdate"))
		{
			return true;
		}
		else if (type.equals("sqldeleteinsert"))
		{
			return true;
		}
		else if (type.equals("xml"))
		{
			return true;
		}
		else if (type.equals("text") || type.equals("txt"))
		{
			return true;
		}
		else if (type.equals("html"))
		{
			return true;
		}
		return false;
	}
	
	private void setExportType(DataExporter exporter, String type)
	{
		if (type.equals("sql") || type.equals("sqlinsert"))
		{
			exporter.setOutputTypeSqlInsert();
		}
		else if (type.equals("sqlupdate"))
		{
			exporter.setOutputTypeSqlUpdate();
		}
		else if (type.equals("sqldeleteinsert"))
		{
			exporter.setOutputTypeSqlDeleteInsert();
		}
		else if (type.equals("xml"))
		{
			exporter.setOutputTypeXml();	
		}
		else if (type.equals("text") || type.equals("txt"))
		{
			exporter.setOutputTypeText();
		}
		else if (type.equals("html"))
		{
			exporter.setOutputTypeHtml();
		}
		
	}
	private void runTableExports(List tableList, StatementRunnerResult result, String outputdir)
		throws SQLException
	{
		if (tableList == null || tableList.size() == 0)
		{
			this.directExport = false;
			return;
		}

		TableIdentifier[] tables = null;

		result.setSuccess();

		int count = tableList.size();
		
		String t = (String)tableList.get(0);
		
		// If only one table argument is present, we'll have to
		// to check for wildcards e.g. -sourcetable=theschema.*
		if (count == 1 && (t.indexOf('*') > -1 || t.indexOf('%') > -1))
		{
			TableIdentifier tbl = new TableIdentifier(t);
			if (tbl.getSchema() == null)
			{
				tbl.setSchema(this.currentConnection.getMetadata().getSchemaToUse());
			}
			tbl.adjustCase(this.currentConnection);
			List l = null;
			try
			{
				l = this.currentConnection.getMetadata().getTableList(tbl.getTableName(), tbl.getSchema());
			}
			catch (SQLException e)
			{
				LogMgr.logError("WbExport.runTableExports()", "Could not retrieve table list", e);
				result.addMessage(ExceptionUtil.getDisplay(e));
				result.setFailure();
				return;
			}

			if (l.size() == 0)
			{
				result.addMessage(ResourceMgr.getString("ErrExportNoTablesFound") + " " + t);
				result.setFailure();
				directExport = false;
				return;
			}
			count = l.size();
			tables = new TableIdentifier[count];
			for (int i=0; i < count; i++)
			{
				tables[i] = (TableIdentifier)l.get(i);
			}
		}
		else
		{
			tables = new TableIdentifier[count];
			for (int i=0; i < count; i++)
			{
				tables[i] = new TableIdentifier((String)tableList.get(i));
			}
		}

		File outdir = (outputdir == null ? null : new File(outputdir));
		String outfile = exporter.getOutputFilename();
		String msg = null;

		if (count > 1 || outfile == null)
		{
			// when more then table is selected or no outputfile is specified
			// then we require an output directory
			if (outputdir == null || outputdir.trim().length() == 0)
			{
				result.setFailure();
				result.addMessage(ResourceMgr.getString("ErrExportOutputDirRequired"));
				return;
			}

			if (outdir == null || !outdir.exists())
			{
				msg = ResourceMgr.getString("ErrExportOutputDirNotFound");
				msg = StringUtil.replace(msg, "%dir%", outdir.getAbsolutePath());
				result.addMessage(msg);
				result.setFailure();
				return;
			}

			if (!outdir.isDirectory())
			{
				msg = ResourceMgr.getString("ErrExportOutputDirNotDir");
				msg = StringUtil.replace(msg, "%dir%", outdir.getAbsolutePath());
				result.addMessage(msg);
				result.setFailure();
				return;
			}
		}

		exporter.setRowMonitor(this);
		exporter.setProgressInterval(this.progressInterval);

		if (count > 1)
		{
			for (int i = 0; i < count; i ++)
			{
				String fname = StringUtil.makeFilename(tables[i].getTableExpression());
				File f = new File(outdir, fname + defaultExtension);
				try
				{
					exporter.addTableExportJob(f.getAbsolutePath(), tables[i]);
				}
				catch (SQLException e)
				{
					if (continueOnError) 
					{
						result.addMessage(ResourceMgr.getString("TxtWarning") + ": " + e.getMessage());
						result.setWarning(true);
					}
					else
					{
						throw e;
					}
				}
				
			}
		}
		else
		{
			// if only one table should be exported
			// we have to use the supplied filename, and cannot use
			// the above loop
			if (StringUtil.isEmptyString(outfile))
			{
				outfile = StringUtil.makeFilename(tables[0].getTableName());
				File f = new File(outdir, outfile + defaultExtension);
				outfile = f.getAbsolutePath();
			}
			exporter.addTableExportJob(outfile, tables[0]);
		}
		
		exporter.setContinueOnError(this.continueOnError);
		exporter.runJobs();
			
		if (exporter.isSuccess())
		{
			if (count > 1)
			{
				count = exporter.getNumberExportedTables();
				msg = ResourceMgr.getString("MsgExportNumTables");
				msg = msg.replaceAll("%numtables%", Integer.toString(count));
				msg = StringUtil.replace(msg, "%dir%", outdir.getAbsolutePath());
				result.addMessage(msg + "\n");
			}
			else
			{
				long rows = exporter.getTotalRows();
				msg = ResourceMgr.getString("MsgExportTableExported");
				msg = StringUtil.replace(msg, "%file%", exporter.getFullOutputFilename());
				msg = StringUtil.replace(msg, "%tablename%", tables[0].getTableExpression());
				msg = StringUtil.replace(msg, "%rows%", Long.toString(rows));
				result.addMessage(msg + "\n");
			}
			result.setSuccess();
		}
		else 
		{
			result.setFailure();
		}
		
		if (exporter.hasWarning())
		{
			result.addMessages(exporter.getWarnings());
		}
		if (exporter.hasError())
		{
			result.addMessages(exporter.getErrors());
		}
	}

	public boolean isResultSetConsumer()
	{
		return !this.directExport;
	}

	public void setMaxRows(int rows)
	{
		this.maxRows = rows;
	}

	public void consumeResult(StatementRunnerResult aResult)
	{
		// Run an export that is defined by a SQL Statement
		// i.e. no sourcetable given in the initial wbexport command
		try
		{
			if (aResult.hasResultSets())
			{
				ResultSet[] data = aResult.getResultSets();
				String sql = aResult.getSourceCommand();
				this.exporter.setSql(sql);
				long rowCount = this.exporter.startExport(data[0]);

				String msg = null;

				if (exporter.isSuccess())
				{
					msg = ResourceMgr.getString("MsgSpoolOk").replaceAll("%rows%", Long.toString(rowCount));
					aResult.addMessage(""); // force new line in output
					aResult.addMessage(msg);
				}
				String[] spoolMsg = this.exporter.getErrors();
				if (spoolMsg.length > 0)
				{
					for (int i=0; i < spoolMsg.length; i++)
					{
						aResult.addMessage(spoolMsg[i]);
					}
					aResult.addMessage("");
				}

				String warn = ResourceMgr.getString("TxtWarning");
				spoolMsg = this.exporter.getWarnings();
				if (spoolMsg.length > 0)
				{
					for (int i=0; i < spoolMsg.length; i++)
					{
						aResult.addMessage(warn + ": " + spoolMsg[i]);
					}
					aResult.addMessage("");
				}
				msg = ResourceMgr.getString("MsgSpoolTarget") + " " + this.exporter.getFullOutputFilename();
				aResult.addMessage(msg);
				aResult.clearResultSets();
				if (exporter.isSuccess())
				{
					aResult.setSuccess();
				}
				else
				{
					aResult.setFailure();
				}
			}
		}
		catch (Exception e)
		{
			aResult.setFailure();
			aResult.addMessage(ResourceMgr.getString("MsgSpoolError"));
			aResult.addMessage(ExceptionUtil.getAllExceptions(e));
			LogMgr.logError("WbExportCommand.consumeResult()", "Error spooling data", e);
		}
	}

	public void done()
	{
		super.done();
		exporter = null;
		maxRows = 0;
		directExport = false;
		currentTable = null;
		defaultExtension = null;
	}

	public void cancel()
		throws SQLException
	{
		if (this.exporter != null)
		{
			this.exporter.cancelExecution();
		}
		super.cancel();
	}

	public void jobFinished()
	{
	}

	public void setCurrentObject(String object, long number, long total)
	{
		this.currentTable = object;
		if (!this.showProgress && this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentObject(this.currentTable, -1, -1);
		}
	}

	public void setCurrentRow(long currentRow, long totalRows)
	{
		if (this.showProgress && this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentObject(this.currentTable, currentRow, -1);
		}
	}

	public int getMonitorType() { return RowActionMonitor.MONITOR_PLAIN; }
	public void setMonitorType(int aType) {}
	public void saveCurrentType(String type) {}
	public void restoreType(String type) {}

}
