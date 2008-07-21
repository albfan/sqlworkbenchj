/*
 * WbExport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
import java.util.Collection;
import java.util.List;
import workbench.WbManager;
import workbench.db.exporter.DataExporter;
import workbench.interfaces.ProgressReporter;
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
import workbench.util.StringUtil;
import workbench.db.TableIdentifier;
import workbench.db.exporter.BlobMode;
import workbench.db.exporter.ControlFileFormat;
import workbench.db.exporter.PoiHelper;
import workbench.util.ExceptionUtil;
import workbench.util.WbFile;

/**
 * SQL Command for running an export.
 * @see workbench.db.exporter.DataExporter
 *
 * @author  support@sql-workbench.net
 */
public class WbExport
	extends SqlCommand
	implements RowActionMonitor, ProgressReporter
{
	public static final String VERB = "WBEXPORT";
	private DataExporter exporter;
	private boolean directExport = false;
	private boolean continueOnError = false;
	private String currentTable;
	private String defaultExtension;
	private boolean showProgress = true;
	private int progressInterval = 1;

	public static final String PARAM_CREATE_OUTPUTDIR = "createDir";
	public static final String PARAM_BLOB_TYPE = "blobType";
	public static final String PARAM_XML_VERSION = "xmlVersion";
	
	public WbExport()
	{
		cmdLine = new ArgumentParser();
		CommonArgs.addDelimiterParameter(cmdLine);
		CommonArgs.addEncodingParameter(cmdLine);
		CommonArgs.addProgressParameter(cmdLine);
		CommonArgs.addCommitParameter(cmdLine);
		CommonArgs.addVerboseXmlParameter(cmdLine);
		CommonArgs.addQuoteEscaping(cmdLine);
		CommonArgs.addSqlDateLiteralParameter(cmdLine);
		
		cmdLine.addArgument("type", StringUtil.stringToList("text,xml,sql,sqlinsert,sqlupdate,sqldeleteinsert,ods,xlsx,xls,html"));
		cmdLine.addArgument("file");
		cmdLine.addArgument("title");
		cmdLine.addArgument("table");
		cmdLine.addArgument("quotechar");
		cmdLine.addArgument("dateFormat");
		cmdLine.addArgument("timestampFormat");
		cmdLine.addArgument("decimal");
		cmdLine.addArgument("charFunc");
		cmdLine.addArgument("concat");
		cmdLine.addArgument("concatFunc");
		cmdLine.addArgument("header", ArgumentType.BoolArgument);
		cmdLine.addArgument("createTable", ArgumentType.BoolArgument);
		cmdLine.addArgument("keyColumns");
		cmdLine.addArgument("append", ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_XML_VERSION, StringUtil.stringToList("1.0", "1.1"));
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT);
		cmdLine.addArgument("escapeHTML", ArgumentType.BoolArgument);
		cmdLine.addArgument("createFullHTML", ArgumentType.BoolArgument);
		cmdLine.addArgument("sourceTable", ArgumentType.TableArgument);
		cmdLine.addArgument("outputDir");
		cmdLine.addArgument("useCDATA", ArgumentType.BoolArgument);
		cmdLine.addArgument("escapeText", StringUtil.stringToList("control,7bit,8bit,extended,none"));
		cmdLine.addArgument("quoteAlways", ArgumentType.BoolArgument);
		cmdLine.addArgument("lineEnding", StringUtil.stringToList("crlf,lf"));
		cmdLine.addArgument("showEncodings");
		cmdLine.addArgument("writeOracleLoader", ArgumentType.Deprecated);
		cmdLine.addArgument("formatFile", StringUtil.stringToList("oracle,sqlserver"));
		cmdLine.addArgument("compress", ArgumentType.BoolArgument);
		cmdLine.addArgument("blobIdCols", ArgumentType.Deprecated);
		cmdLine.addArgument("lobIdCols");
		cmdLine.addArgument("filenameColumn");
		cmdLine.addArgument(PARAM_BLOB_TYPE, BlobMode.getTypes());
		cmdLine.addArgument("clobAsFile", ArgumentType.BoolArgument);
		cmdLine.addArgument("continueOnError", ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_CREATE_OUTPUTDIR, ArgumentType.BoolArgument);
	}

	public String getVerb() { return VERB; }

	private void addWrongArgumentsMessage(StatementRunnerResult result)
	{
		if (WbManager.getInstance().isBatchMode()) return;
		String msg = getWrongArgumentsMessage();
		result.addMessageNewLine();
		result.addMessage(msg);
	}

	private String getWrongArgumentsMessage()
	{
		String msg = ResourceMgr.getString("ErrExportWrongParameters");
		String header = "text=" + Boolean.toString(getHeaderDefault("text"));
		header += ", ods="  + Boolean.toString(getHeaderDefault("ods"));
		header += ", xls="  + Boolean.toString(getHeaderDefault("xls"));
		header += ", xlsx="  + Boolean.toString(getHeaderDefault("xlsx"));
		
		msg = msg.replace("%header_flag_default%", header);
		msg = msg.replace("%verbose_default%", Boolean.toString(getVerboseXmlDefault()));
		msg = msg.replace("%date_literal_default%", Settings.getInstance().getDefaultExportDateLiteralType());
		msg = msg.replace("%default_encoding%", Settings.getInstance().getDefaultDataEncoding());
		msg = msg.replace("%xmlversion%", Settings.getInstance().getDefaultXmlVersion());
		return msg;
	}

	private boolean getVerboseXmlDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.export.xml.default.verbose", true);
	}

	private boolean getHeaderDefault(String type)
	{
		return Settings.getInstance().getBoolProperty("workbench.export." + type + ".default.header", false);
	}
	
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		cmdLine.parse(getCommandLine(sql));

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

		WbFile outputFile = evaluateFileArgument(cmdLine.getValue("file"));
		String type = cmdLine.getValue("type");
		
		if (type == null)
		{
			type = findTypeFromFilename(outputFile);
		}
		
		if (type == null)
		{
			result.addMessage(ResourceMgr.getString("ErrExportTypeRequired"));
			addWrongArgumentsMessage(result);
			result.setFailure();
			return result;
		}

		if (!isTypeValid(type))
		{
			result.addMessage(ResourceMgr.getString("ErrExportWrongType"));
			addWrongArgumentsMessage(result);
			result.setFailure();
			return result;
		}

		this.exporter = new DataExporter(this.currentConnection);

		String tables = cmdLine.getValue("sourcetable");
		String od = cmdLine.getValue("outputdir");
		WbFile outputdir = (od == null ? null : new WbFile(od));

		if (outputFile == null && outputdir == null)
		{
			result.addMessage(ResourceMgr.getString("ErrExportFileRequired"));
			addWrongArgumentsMessage(result);
			result.setFailure();
			return result;
		}

		if (outputFile == null && outputdir != null && tables == null)
		{
			result.addMessage(ResourceMgr.getString("ErrExportNoTablesDef"));
			result.setFailure();
			return result;
		}
		
		String updateTable = cmdLine.getValue("table");
		type = type.trim().toLowerCase();
		if ("txt".equals(type)) type = "text";

		String encoding = cmdLine.getValue("encoding");
		if (encoding != null)
		{
			exporter.setEncoding(encoding);
		}
		exporter.setAppendToFile(cmdLine.getBoolean("append"));
		exporter.setWriteClobAsFile(cmdLine.getBoolean("clobasfile", false));

		this.continueOnError = cmdLine.getBoolean("continueonerror", false);

		// Some properties used by more than one export type
		String format = cmdLine.getValue("dateformat");
		if (format != null) exporter.setDateFormat(format);

		format = cmdLine.getValue("timestampformat");
		if (format != null) exporter.setTimestampFormat(format);
		
		format = cmdLine.getValue("decimal");
		if (format != null) exporter.setDecimalSymbol(format);
		
		exporter.setPageTitle(cmdLine.getValue("title"));

		exporter.setExportHeaders(cmdLine.getBoolean("header", getHeaderDefault(type)));		
		
		if ("text".equals(type))
		{
			// Support old parameter Syntax
			if (cmdLine.getBoolean("writeoracleloader", false))
			{
				exporter.addControlFileFormat(ControlFileFormat.oracle);
			}
			
			exporter.addControlFileFormats(ControlFileFormat.parseCommandLine(cmdLine.getValue("formatfile")));

			String delimiter = cmdLine.getValue("delimiter");
			if (delimiter != null) exporter.setTextDelimiter(delimiter);

			String quote = cmdLine.getValue("quotechar");
			if (quote != null) exporter.setTextQuoteChar(quote);

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
					String msg = ResourceMgr.getString("ErrExportInvalidEscapeRangeIgnored").replace("%value%", escape);
					result.addMessage(msg);
				}
			}
			exporter.setQuoteAlways(cmdLine.getBoolean("quotealways"));
			exporter.setQuoteEscaping(CommonArgs.getQuoteEscaping(cmdLine));

			this.defaultExtension = ".txt";
		}
		else if (type.startsWith("sql"))
		{
			exporter.setIncludeCreateTable(cmdLine.getBoolean("createtable"));
			exporter.setChrFunction(cmdLine.getValue("charfunc"));
			exporter.setConcatFunction(cmdLine.getValue("concatfunc"));
			exporter.setConcatString(cmdLine.getValue("concat"));

			CommonArgs.setCommitEvery(exporter, cmdLine);

			exporter.setAppendToFile(cmdLine.getBoolean("append"));
			if (updateTable != null) exporter.setTableName(updateTable);
			String c = cmdLine.getValue("keycolumns");
			if (c != null)
			{
				List cols = StringUtil.stringToList(c, ",");
				exporter.setKeyColumnsToUse(cols);
			}
			String bmode = cmdLine.getValue(PARAM_BLOB_TYPE);
			exporter.setBlobMode(bmode);
			this.defaultExtension = ".sql";
			String literal = cmdLine.getValue(CommonArgs.ARG_DATE_LITERAL_TYPE);
			if (literal != null)
			{
				exporter.setDateLiteralType(literal);
      }
		}
		else if ("xml".equals(type))
		{
			String xsl = cmdLine.getValue(WbXslt.ARG_STYLESHEET);
			String output = cmdLine.getValue(WbXslt.ARG_OUTPUT);

			boolean verboseDefault = getVerboseXmlDefault();
			boolean verbose = cmdLine.getBoolean(CommonArgs.ARG_VERBOSE_XML, verboseDefault);
			exporter.setUseVerboseFormat(verbose);

			String version = cmdLine.getValue(PARAM_XML_VERSION);
			if (version != null)
			{
				exporter.setXMLVersion(version);
			}
			
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
					msg = msg.replace("%xslt%", f.getAbsolutePath());
					result.addMessage(msg);
				}
			}
			this.exporter.setUseCDATA(cmdLine.getBoolean("usecdata"));
			if (updateTable != null) exporter.setTableName(updateTable);
			this.defaultExtension = ".xml";
			if (encoding == null)
			{
				// Make sure to use UTF-8 as the default if no encoding is specified
				this.exporter.setEncoding("UTF-8");
			}
		}
		else if ("html".equals(type))
		{
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
		else if (type.equalsIgnoreCase("xls"))
		{
			if (!PoiHelper.isPoiAvailable())
			{
				result.setFailure();
				result.addMessage(ResourceMgr.getString("ErrExportNoPoi"));
				return result;
			}
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
				result.setWarning(true);
			}
		}

		List<String> columns = StringUtil.stringToList(cols, ",", true, true, false);
		this.exporter.setBlobIdColumns(columns);
		this.exporter.setCompressOutput(cmdLine.getBoolean("compress", false));

		this.exporter.setOutputFilename(outputFile != null ? outputFile.getFullPath() : null);

		// Setting the output type should be the last step in the configuration
		// of the exporter as this will trigger some initialization
		// that depends on the other properties
		setExportType(exporter, type);

		List<TableIdentifier> tablesToExport = null;
		try
		{
			SourceTableArgument argParser = new SourceTableArgument(tables, this.currentConnection);
			tablesToExport = argParser.getTables();
			if (tablesToExport.size() == 0 && argParser.wasWildCardArgument())
			{
				result.addMessage(ResourceMgr.getFormattedString("ErrExportNoTablesFound", tables));
				result.setFailure();
				return result;
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbExport.runTableExports()", "Could not retrieve table list", e);
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
			return result;
		}

		this.directExport = (tablesToExport.size() > 0);

		CommonArgs.setProgressInterval(this, cmdLine);
		this.showProgress = (this.progressInterval > 0);

		boolean create = cmdLine.getBoolean(PARAM_CREATE_OUTPUTDIR, false);

		if (create)
		{
			WbFile dir = null;
			if (outputFile != null)
			{
				dir = new WbFile(outputFile.getParentFile());
			}
			else if (outputdir != null)
			{
				dir = outputdir;
			}

			if (!dir.exists())
			{
				if (!dir.mkdirs())
				{
					result.addMessage(ResourceMgr.getFormattedString("ErrCreateDir", dir.getFullPath()));
					result.setFailure();
					return result;
				}
				else
				{
					result.addMessage(ResourceMgr.getFormattedString("MsgDirCreated", dir.getFullPath()));
				}
			}
		}
		
		if (outputdir != null && !outputdir.exists())
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrOutputDirNotFound", outputdir.getFullPath()));
			result.setFailure();
			return result;
		}

		if (outputFile != null)
		{
			// For a single table export the definition of a
			String extCol = cmdLine.getValue("filenameColumn");
			exporter.setFilenameColumn(extCol);

			// Check the outputfile right now, so the user does not have
			// to wait for a possible error message until the ResultSet
			// from the SELECT statement comes in...
			boolean canWrite = true;
			String msg = null;
			try
			{
				// File.canWrite() does not work reliably. It will report
				// an error if the file does not exist, but still could
				// be written.
				if (outputFile.exists())
				{
					msg = ResourceMgr.getString("ErrExportFileWrite");
					canWrite = outputFile.canWrite();
				}
				else
				{
					// try to create the file
					outputFile.tryCreate();
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
			this.exporter.setReportInterval(this.progressInterval);

			String msg = ResourceMgr.getString("MsgSpoolInit");
			msg = msg.replace("%type%", exporter.getTypeDisplay());
			String out = null;
			if (outputFile != null)
			{
				out = outputFile.getFullPath();
			}
			else if (outputdir != null)
			{
				out = outputdir.getFullPath();
			}
			msg = msg.replace("%file%", out);
			result.addMessage(msg);
			if (this.maxRows > 0)
			{
				msg = ResourceMgr.getString("MsgExportMaxRowsWarning").replace("%maxrows%", Integer.toString(maxRows));
				result.addMessage(msg);
			}
		}
		else
		{
			try
			{
				exporter.setRowMonitor(this);
				exporter.setReportInterval(this.progressInterval);
				exporter.setContinueOnError(this.continueOnError);
				runDirectExport(tablesToExport, result, outputdir);
				addMessages(result);
			}
			catch (Exception e)
			{
				LogMgr.logError("WbExport.execute()", "Error when running table export", e);
				addErrorInfo(result, sql, e);
				result.setFailure();
			}
		}
		return result;
	}

	boolean isTypeValid(String type)
	{
		if (type == null) return false;
		Collection<String> types = cmdLine.getAllowedValues("type");
		return types.contains(type);
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
		else if (type.equals("ods"))
		{
			exporter.setOutputTypeOds();
		}
		else if (type.equals("xls"))
		{
			exporter.setOutputTypeXls();
		}
		else if (type.equals("xlsx"))
		{
			exporter.setOutputTypeXlsXML();
		}
	}

	private void runDirectExport(List<TableIdentifier> tableList, StatementRunnerResult result, File outdir)
		throws SQLException
	{
		if (tableList.size() > 1)
		{
			exportTableList(tableList, result, outdir);
		}
		else
		{
			exportSingleTable(tableList.get(0), result, outdir);
		}
	}

	private void exportSingleTable(TableIdentifier table, StatementRunnerResult result, File outdir)
		throws SQLException
	{
		String outfile = this.exporter.getOutputFilename();

		if (StringUtil.isEmptyString(outfile))
		{
			outfile = StringUtil.makeFilename(table.getTableName());
			File f = new File(outdir, outfile + defaultExtension);
			outfile = f.getAbsolutePath();
		}
		exporter.addTableExportJob(outfile, table);
		exporter.runJobs();
		if (exporter.isSuccess())
		{
			long rows = exporter.getTotalRows();
			String msg = ResourceMgr.getString("MsgExportTableExported");
			msg = msg.replace("%file%", exporter.getFullOutputFilename());
			msg = msg.replace("%tablename%", table.getTableExpression());
			msg = msg.replace("%rows%", Long.toString(rows));
			result.addMessage(msg);
		}
		else
		{
			result.setFailure();
		}
	}

	private void exportTableList(List<TableIdentifier> tableList, StatementRunnerResult result, File outdir)
		throws SQLException
	{
		result.setSuccess();

		int tableCount = tableList.size();

		// when more than one table is selected or no outputfile is specified
		// then we require an output directory
		if (outdir == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrExportOutputDirRequired"));
			return;
		}

		if (outdir == null || !outdir.exists())
		{
			String msg = ResourceMgr.getFormattedString("ErrOutputDirNotFound", outdir.getAbsolutePath());
			result.addMessage(msg);
			result.setFailure();
			return;
		}

		if (!outdir.isDirectory())
		{
			String msg = ResourceMgr.getString("ErrExportOutputDirNotDir");
			msg = msg.replace("%dir%", outdir.getAbsolutePath());
			result.addMessage(msg);
			result.setFailure();
			return;
		}


		for (TableIdentifier tbl : tableList)
		{
			String fname = StringUtil.makeFilename(tbl.getTableExpression());
			File f = new File(outdir, fname + defaultExtension);
			try
			{
				exporter.addTableExportJob(f.getAbsolutePath(), tbl);
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

		exporter.runJobs();

		if (exporter.isSuccess())
		{
			tableCount = exporter.getNumberExportedTables();
			String msg = ResourceMgr.getString("MsgExportNumTables");
			msg = msg.replace("%numtables%", Integer.toString(tableCount));
			msg = msg.replace("%dir%", outdir.getAbsolutePath());
			result.addMessage(msg);
			result.setSuccess();
		}
		else
		{
			result.setFailure();
		}
	}

	private void addMessages(StatementRunnerResult result)
	{
		result.addMessage(this.exporter.getErrors());

		if (exporter.hasWarning())
		{
			result.addMessage(ResourceMgr.getString("TxtWarnings"));
			result.addMessage(exporter.getWarnings());
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
				String sql = aResult.getSourceCommand();
				this.exporter.setSql(sql);
				ResultSet toExport = aResult.getResultSets().get(0);
				// The exporter has already closed the resultSet that it exported
				// so we can remove it from the list of ResultSets in the StatementRunnerResult
				// object. Thus the later call to clearResultSets() will only free any not used
				// ResultSet
				aResult.getResultSets().remove(0);
				long rowCount = this.exporter.startExport(toExport);

				String msg = null;

				if (exporter.isSuccess())
				{
					msg = ResourceMgr.getString("MsgSpoolOk").replace("%rows%", Long.toString(rowCount));
					aResult.addMessage(""); // force new line in output
					aResult.addMessage(msg);
					msg = ResourceMgr.getString("MsgSpoolTarget") + " " + this.exporter.getFullOutputFilename();
					aResult.addMessage(msg);
				}
				addMessages(aResult);

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
			aResult.addMessage(ResourceMgr.getString("ErrExportExecute"));
			aResult.addMessage(ExceptionUtil.getAllExceptions(e));
			LogMgr.logError("WbExportCommand.consumeResult()", "Error spooling data", e);
		}
		finally
		{
			aResult.clearResultSets();
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

	public void setReportInterval(int interval)
	{
		this.progressInterval = interval;
	}

	public int getMonitorType() { return RowActionMonitor.MONITOR_PLAIN; }
	public void setMonitorType(int aType) {}
	public void saveCurrentType(String type) {}
	public void restoreType(String type) {}

	protected String findTypeFromFilename(WbFile f)
	{
		if (f == null) return null;
		String fname = f.getFullPath();
		if (fname == null) return null;
		if (fname.toLowerCase().endsWith(".txt")) return "text";
		if (fname.toLowerCase().endsWith(".xml")) return "xml";
		if (fname.toLowerCase().endsWith(".text")) return "text";
		if (fname.toLowerCase().endsWith(".csv")) return "text";
		if (fname.toLowerCase().endsWith(".htm")) return "html";
		if (fname.toLowerCase().endsWith(".html")) return "html";
		if (fname.toLowerCase().endsWith(".sql")) return "sqlinsert";
		if (fname.toLowerCase().endsWith(".xls")) return "xls";
		if (fname.toLowerCase().endsWith(".ods")) return "ods";
		return null;
	}

}
