/*
 * WbExport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
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
import workbench.util.CharacterRange;
import workbench.util.EncodingUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.db.TableIdentifier;
import workbench.util.ExceptionUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbExport
	extends SqlCommand
	implements RowActionMonitor
{
	public static final String VERB = "WBEXPORT";
	private ArgumentParser cmdLine;
	private DataExporter exporter;
	private int maxRows = 0;
	private boolean directExport = false;
	private String currentTable;
	private String defaultExtension;
	private boolean showProgress = true;
	private int progressInterval = 1;

	public WbExport()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("type");
		cmdLine.addArgument("file");
		cmdLine.addArgument("title");
		cmdLine.addArgument("table");
		cmdLine.addArgument("delimiter");
		cmdLine.addArgument("quotechar");
		cmdLine.addArgument("dateformat");
		cmdLine.addArgument("timestampformat");
		cmdLine.addArgument("decimal");
		cmdLine.addArgument("cleancr");
		cmdLine.addArgument("charfunc");
		cmdLine.addArgument("concat");
		cmdLine.addArgument("concatfunc");
		cmdLine.addArgument("commitevery");
		cmdLine.addArgument("header");
		cmdLine.addArgument("createtable");
		cmdLine.addArgument("nodata");
		cmdLine.addArgument("encoding");
		cmdLine.addArgument("showprogress");
		cmdLine.addArgument("keycolumns");
		cmdLine.addArgument("append");
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT);
		cmdLine.addArgument("escapehtml");
		cmdLine.addArgument("createfullhtml");
		cmdLine.addArgument("sourcetable");
		cmdLine.addArgument("outputdir");
		cmdLine.addArgument("usecdata");
		cmdLine.addArgument("escapetext");
		cmdLine.addArgument("quotealways");
		cmdLine.addArgument("lineending");
		cmdLine.addArgument("showencodings");
		cmdLine.addArgument("verbosexml");
		cmdLine.addArgument("writeoracleloader");
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		this.currentConnection = aConnection;
		aSql = SqlUtil.makeCleanSql(aSql, false, '"');
		int pos = aSql.indexOf(' ');
		if (pos > -1)
			aSql = aSql.substring(pos);
		else
			aSql = "";

		try
		{
			cmdLine.parse(aSql);
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrorExportWrongParameters"));
			result.setFailure();
			return result;
		}

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
			List params = cmdLine.getUnknownArguments();
			StringBuffer msg = new StringBuffer(ResourceMgr.getString("ErrorUnknownParameter"));
			for (int i=0; i < params.size(); i++)
			{
				msg.append((String)params.get(i));
				if (i > 0) msg.append(',');
			}
			result.addMessage(msg.toString());
			result.addMessage(ResourceMgr.getString("ErrorExportWrongParameters"));
			result.setFailure();
			return result;
		}

		String type = null;
		String file = null;

		this.exporter = new DataExporter();

		type = cmdLine.getValue("type");
		file = cmdLine.getValue("file");
		String tables = cmdLine.getValue("sourcetable");

		String outputdir = cmdLine.getValue("outputdir");

		if (type == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorExportTypeRequired"));
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrorExportWrongParameters"));
			result.setFailure();
			return result;
		}

		if (file == null && outputdir == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorExportFileRequired"));
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrorExportWrongParameters"));
			result.setFailure();
			return result;
		}

		String table = cmdLine.getValue("table");
		type = type.toLowerCase();

		String encoding = cmdLine.getValue("encoding");
		if (encoding != null) exporter.setEncoding(encoding);
		exporter.setAppendToFile(cmdLine.getBoolean("append"));

		if ("text".equalsIgnoreCase(type) || "txt".equalsIgnoreCase(type))
		{
			exporter.setOutputTypeText();
			exporter.setWriteOracleControlFile(cmdLine.getBoolean("writeoracleloader"));
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

			boolean headerDefault = Settings.getInstance().getBoolProperty("workbench.export.text.default.header", false);
			exporter.setExportHeaders(cmdLine.getBoolean("header", headerDefault));
			exporter.setCleanupCarriageReturns(cmdLine.getBoolean("cleancr"));
			String escape = cmdLine.getValue("escapetext");
			if (escape != null)
			{
				if ("control".equalsIgnoreCase(escape)
				   ||"ctrl".equalsIgnoreCase(escape)
				   )
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
				else if ("false".equalsIgnoreCase(escape))
				{
					exporter.setEscapeRange(null);
				}
				else
				{
					exporter.setEscapeRange(null);
					String msg = ResourceMgr.getString("ErrorExportInvalidEscapeRangeIgnored").replaceAll("%value%", escape);
					result.addMessage(msg);
				}
			}
			exporter.setQuoteAlways(cmdLine.getBoolean("quotealways"));

			this.defaultExtension = ".txt";
		}
		else if (type.startsWith("sql"))
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
			exporter.setIncludeCreateTable(cmdLine.getBoolean("createtable"));
			exporter.setChrFunction(cmdLine.getValue("charfunc"));
			exporter.setConcatFunction(cmdLine.getValue("concatfunc"));
			exporter.setConcatString(cmdLine.getValue("concat"));
			int commit = StringUtil.getIntValue(cmdLine.getValue("commitevery"),-1);
			exporter.setCommitEvery(commit);
			exporter.setAppendToFile(cmdLine.getBoolean("append"));
			if (table != null) exporter.setTableName(table);
			String c = cmdLine.getValue("keycolumns");
			if (c != null)
			{
				List cols = StringUtil.stringToList(c, ",");
				exporter.setKeyColumnsToUse(cols);
			}
			this.defaultExtension = ".sql";
		}
		else if ("xml".equalsIgnoreCase(type))
		{
			String format = cmdLine.getValue("dateformat");
			if (format != null) exporter.setDateFormat(format);

			format = cmdLine.getValue("timestampformat");
			if (format != null) exporter.setTimestampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) exporter.setDecimalSymbol(format);

			exporter.setOutputTypeXml();

			String xsl = cmdLine.getValue(WbXslt.ARG_STYLESHEET);
			String output = cmdLine.getValue(WbXslt.ARG_OUTPUT);

			boolean verboseDefault = Settings.getInstance().getBoolProperty("workbench.export.xml.default.verbose", true);
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
					String msg = ResourceMgr.getString("ErrorSpoolXsltNotFound");
					msg = msg.replaceAll("%xslt%", f.getAbsolutePath());
					result.addMessage(msg);
				}
			}
			this.exporter.setUseCDATA(cmdLine.getBoolean("usecdata"));
			if (table != null) exporter.setTableName(table);
			this.defaultExtension = ".xml";
		}
		else if ("html".equalsIgnoreCase(type))
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

			exporter.setOutputTypeHtml();
			if (table != null) exporter.setTableName(table);
			this.defaultExtension = ".html";
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorExportWrongParameters"));
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

		file = StringUtil.trimQuotes(file);
		this.exporter.setOutputFilename(file);

		List tablesToExport = null;
		if (tables != null)
		{
			tablesToExport = StringUtil.stringToList(tables, ",");
			this.directExport = (tablesToExport.size() > 0);
		}

		this.exporter.setConnection(aConnection);

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
			this.runTableExports(tablesToExport, result, outputdir);
		}
		return result;
	}

	private void runTableExports(List tableList, StatementRunnerResult result, String outputdir)
	{
		if (tableList == null || tableList.size() == 0)
		{
			this.directExport = false;
			return;
		}

		TableIdentifier[] tables = null;

		result.setSuccess();

		int count = tableList.size();
		if (count == 1)
		{
			// If only one table is present, we'll have to
			// to check for wildcards e.g. -sourcetable=theschema.*
			// This will be handled by DbMetadata when passing
			// null or wildcards to the getTableList() method
			String t = (String)tableList.get(0);
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
				result.addMessage(ResourceMgr.getString("ErrorExportNoTablesFound") + " " + t);
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

		File outdir = null;
		String outfile = exporter.getOutputFilename();
		String msg = null;

		if (count > 1)
		{
			// when more then table is selected, then we require an output directory
			if (outputdir == null || outputdir.trim().length() == 0)
			{
				result.setFailure();
				result.addMessage(ResourceMgr.getString("ErrorExportOutputDirRequired"));
				return;
			}

			outdir = new File(outputdir);
			if (!outdir.exists())
			{
				msg = ResourceMgr.getString("ErrorExportOutputDirNotFound");
				msg = StringUtil.replace(msg, "%dir%", outdir.getAbsolutePath());
				result.addMessage(msg);
				result.setFailure();
				return;
			}

			if (!outdir.isDirectory())
			{
				msg = ResourceMgr.getString("ErrorExportOutputDirNotDir");
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
				exporter.addTableExportJob(f.getAbsolutePath(), tables[i]);
			}
			exporter.runJobs();
			count = exporter.getNumberExportedTables();
			msg = ResourceMgr.getString("MsgExportNumTables");
			msg = msg.replaceAll("%numtables%", Integer.toString(count));
			msg = StringUtil.replace(msg, "%dir%", outdir.getAbsolutePath());
			result.addMessage(msg);
		}
		else
		{
			// For message and error display purposes we treat a single
			// table export differently
			String table = tables[0].getTableExpression(this.currentConnection);
			if (outfile == null)
			{
				File nf = new File(outputdir, table + defaultExtension);
				outfile = nf.getAbsolutePath();
				exporter.setOutputFilename(outfile);
			}
			this.currentTable = table;
			this.setCurrentObject(table, -1, -1);
			String stmt = "SELECT * FROM " + table;
			exporter.setSql(stmt);
			long rows = 0;
			try
			{
				rows = exporter.startExport();
			}
			catch (Exception e)
			{
				result.setFailure();
				result.addMessage(e.getMessage());
			}
			if (exporter.isSuccess())
			{
				msg = ResourceMgr.getString("MsgExportTableExported");
				msg = StringUtil.replace(msg, "%file%", outfile);
				msg = StringUtil.replace(msg, "%tablename%", table);
				msg = StringUtil.replace(msg, "%rows%", Long.toString(rows));
				result.addMessage(msg);
			}
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
				aResult.setSuccess();
			}
		}
		catch (Exception e)
		{
			aResult.setFailure();
			aResult.addMessage(ResourceMgr.getString("MsgSpoolError"));
			String msg = ExceptionUtil.getDisplay(e, true);
			aResult.addMessage(msg);
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

	public void setMonitorType(int aType)
	{
	}

}