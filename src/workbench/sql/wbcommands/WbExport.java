/*
 * WbExport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.gui.components.EncodingPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  info@sql-workbench.net
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
	private List tablesToExport = null;
	private String currentTable;
	private String defaultExtension;

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
		//cmdLine.addArgument("sqlinsert");
		//cmdLine.addArgument("sqlupdate");
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
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
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
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}

		if (cmdLine.isArgPresent("showencodings"))
		{
			result.addMessage(ResourceMgr.getString("MsgAvailableEncodings"));
			result.addMessage("");
			String[] encodings = EncodingPanel.getEncodings();
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
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}

		String type = null;
		String file = null;

		this.exporter = new DataExporter();

		type = cmdLine.getValue("type");
		file = cmdLine.getValue("file");
		String outputdir = cmdLine.getValue("outputdir");
		String tables = cmdLine.getValue("sourcetable");

		if (type == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorExportTypeRequired"));
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}

		if (file == null && outputdir == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorExportFileRequired"));
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}

		String table = cmdLine.getValue("table");
		type = type.toLowerCase();

		String typeDisplay = null;

		String encoding = cmdLine.getValue("encoding");
		if (encoding != null) exporter.setEncoding(encoding);
		exporter.setAppendToFile(cmdLine.getBoolean("append"));

		if ("text".equalsIgnoreCase(type) || "txt".equalsIgnoreCase(type))
		{
			// change the contents of type in order to display it properly
			typeDisplay = "Text";
			exporter.setOutputTypeText();
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

			exporter.setExportHeaders(cmdLine.getBoolean("header"));
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
				typeDisplay = "SQL INSERT";
			}
			else if (type.equals("sqlupdate"))
			{
				exporter.setOutputTypeSqlUpdate();
				typeDisplay = "SQL UPDATE";
			}
			else if (type.equals("sqldeleteinsert"))
			{
				exporter.setOutputTypeSqlDeleteInsert();
				typeDisplay = "SQL DELETE/INSERT";
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
			// change the contents of type in order to display it properly
			typeDisplay = "XML";
			String format = cmdLine.getValue("dateformat");
			if (format != null) exporter.setDateFormat(format);

			format = cmdLine.getValue("timestampformat");
			if (format != null) exporter.setTimestampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) exporter.setDecimalSymbol(format);

			exporter.setOutputTypeXml();

			String xsl = cmdLine.getValue(WbXslt.ARG_STYLESHEET);
			String output = cmdLine.getValue(WbXslt.ARG_OUTPUT);

			boolean verbose = cmdLine.getBoolean("verbosexml", true);
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
			typeDisplay = "HTML";
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
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
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

		if (tables != null)
		{
			this.tablesToExport = StringUtil.stringToList(tables, ",");
			this.directExport = (this.tablesToExport.size() > 0);
		}

		this.exporter.setConnection(aConnection);

		String progress = cmdLine.getValue("showprogress");
		this.exporter.setShowProgress("true".equalsIgnoreCase(progress));
		if (!this.directExport)
		{
			this.exporter.setRowMonitor(this.rowMonitor);
			String msg = ResourceMgr.getString("MsgSpoolInit");
			msg = StringUtil.replace(msg, "%type%", typeDisplay);
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
			this.runTableExports(result, outputdir);
		}
		return result;
	}

	private void runTableExports(StatementRunnerResult result, String outputdir)
	{
		if (this.tablesToExport == null || this.tablesToExport.size() == 0)
		{
			this.directExport = false;
			return;
		}

		result.setSuccess();

		int count = this.tablesToExport.size();

		File outdir = null;
		String outfile = exporter.getOutputFilename();
		String msg = null;

		if (count > 1)
		{
			outdir = new File(outputdir);
			if (!outdir.isDirectory())
			{
				result.setFailure();
				msg = ResourceMgr.getString("ErrorExportOutputDirNotDir");
				msg = StringUtil.replace(msg, "%dir%", outdir.getAbsolutePath());
				result.addMessage(msg);
				result.setFailure();
				return;
			}

			if (!outdir.exists())
			{
				result.setFailure();
				msg = ResourceMgr.getString("ErrorExportOutputDirNotFound");
				msg = StringUtil.replace(msg, "%dir%", outdir.getAbsolutePath());
				result.addMessage(msg);
				result.setFailure();
				return;
			}
		}

		exporter.setRowMonitor(this);
		if (count > 1)
		{
			for (int i = 0; i < count; i ++)
			{
				String table = (String)this.tablesToExport.get(i);
				if (table == null) continue;

				String fname = StringUtil.makeFilename(table);
				File f = new File(outdir, fname + defaultExtension);
				exporter.addTableExportJob(f.getAbsolutePath(), table);
			}
			exporter.runJobs();
			msg = ResourceMgr.getString("MsgExportNumTables");
			msg = msg.replaceAll("%numtables%", Integer.toString(count));
			msg = StringUtil.replace(msg, "%dir%", outdir.getAbsolutePath());
			result.addMessage(msg);
		}
		else
		{
			String table = (String)this.tablesToExport.get(0);
			if (outfile == null)
			{
				File nf = new File(outputdir, table + defaultExtension);
				outfile = nf.getAbsolutePath();
				exporter.setOutputFilename(outfile);
			}
			this.currentTable = table;
			String stmt = "SELECT * FROM " + SqlUtil.quoteObjectname(table);
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
		result.addMessage("");
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
				//msg = ResourceMgr.getString("MsgSpoolSource") + " " + aResult.getSourceCommand();
				//aResult.addMessage(msg);
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
			String msg = e.getMessage();
			if (msg == null)
			{
				msg = StringUtil.getStackTrace(e);
			}
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
		tablesToExport = null;
		currentTable = null;
		defaultExtension = null;
	}

	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.exporter != null)
		{
			this.exporter.cancelExecution();
		}
	}

	public void jobFinished()
	{
	}

	public void setCurrentObject(String object, int number, int total)
	{
		this.currentTable = object;
	}

	public void setCurrentRow(int currentRow, int totalRows)
	{
		if (this.rowMonitor != null && this.currentTable != null)
		{
			this.rowMonitor.setCurrentObject(this.currentTable, currentRow, -1);
		}
	}

	public void setMonitorType(int aType)
	{
	}

}