/*
 * WbExport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import workbench.WbManager;
import workbench.interfaces.ProgressReporter;
import workbench.interfaces.ResultSetConsumer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.TableIdentifier;
import workbench.db.exporter.BlobMode;
import workbench.db.exporter.ControlFileFormat;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.ExportDataModifier;
import workbench.db.exporter.ExportType;
import workbench.db.exporter.InfinityLiterals;
import workbench.db.exporter.PoiHelper;
import workbench.db.exporter.WrongFormatFileException;

import workbench.storage.DataStore;
import workbench.storage.MergeGenerator;
import workbench.storage.RowActionMonitor;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CharacterEscapeType;
import workbench.util.CharacterRange;
import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.QuoteEscapeType;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.XsltTransformer;

/**
 * SQL Command for running an export.
 * @see workbench.db.exporter.DataExporter
 *
 * @author  Thomas Kellerer
 */
public class WbExport
	extends SqlCommand
	implements RowActionMonitor, ProgressReporter, ResultSetConsumer
{
	public static final String VERB = "WbExport";

	// <editor-fold defaultstate="collapsed" desc=" Arguments ">
	public static final String ARG_SOURCETABLE = "sourceTable";
	public static final String ARG_OUTPUTDIR = "outputDir";
	public static final String ARG_EXPORT_TYPE = "type";
	public static final String ARG_OUTPUT_FILENAME = "file";

	public static final String ARG_CREATE_OUTPUTDIR = "createDir";
	public static final String ARG_BLOB_TYPE = "blobType";
	public static final String ARG_XML_VERSION = "xmlVersion";
	public static final String ARG_ROWNUM = "rowNumberColumn";
	public static final String ARG_EMPTY_RESULTS = "writeEmptyResults";
	public static final String ARG_TABLE_PREFIX = "sourceTablePrefix";
	public static final String ARG_USE_CDATA = "useCDATA";
	public static final String ARG_USE_SCHEMA = "useSchema";
	public static final String ARG_FORMATFILE = "formatFile";
	public static final String ARG_COL_COMMENTS = "includeColumnComments";
	public static final String ARG_DISTRIBUTE_LOB_FILES = "lobsPerDirectory";
	public static final String ARG_QUOTE_ALWAYS = "quoteAlways";
	public static final String ARG_QUOTECHAR = "quotechar";
	public static final String ARG_APPEND = "append";
	public static final String ARG_CLOB_AS_FILE = "clobAsFile";
	public static final String ARG_CONTINUE_ON_ERROR = "continueOnError";
	public static final String ARG_HEADER = "header";
	public static final String ARG_TABLEWHERE = "tableWhere";

	// spreadsheet options
	public static final String ARG_ADD_INFOSHEET = "infoSheet";
	public static final String ARG_AUTOFILTER = "autoFilter";
	public static final String ARG_OPT_WIDTH = "autoColWidth";
	public static final String ARG_FIXED_HEADER = "fixedHeader";
	public static final String ARG_TARGET_SHEET_IDX = "targetSheet";
	public static final String ARG_TARGET_SHEET_NAME = "targetSheetName";

	public static final String ARG_PAGE_TITLE = "title";

	public static final String ARG_CHARFUNC = "charFunc";
	public static final String ARG_CONCAT_FUNCTION = "concatFunc";
	public static final String ARG_CONCAT_OPERATOR = "concat";
	public static final String ARG_CREATEFULL_HTML_PAGE = "createFullHTML";
	public static final String ARG_DATEFORMAT = "dateFormat";
	public static final String ARG_DECIMAL_SYMBOL = "decimal";
	public static final String ARG_DECIMAL_DIGITS = "decimalDigits";
	public static final String ARG_ESCAPETEXT = "escapeText";
	public static final String ARG_ESCAPE_HTML = "escapeHTML";
	public static final String ARG_INCLUDE_CREATETABLE = "createTable";
	public static final String ARG_KEY_COLUMNS = "keyColumns";
	public static final String ARG_LINEENDING = "lineEnding";
	public static final String ARG_POSTDATA_HTML = "postDataHtml";
	public static final String ARG_PREDATA_HTML = "preDataHtml";
	public static final String ARG_SHOW_ENCODINGS = "showEncodings";
	public static final String ARG_TABLE = "table";
	public static final String ARG_TIMEFORMAT = "timeFormat";
	public static final String ARG_TIMESTAMP_FORMAT = "timestampFormat";
	public static final String ARG_INFINITY_LITERALS = "infinityLiterals";
	public static final String ARG_MERGE_TYPE = "mergeType";
	public static final String ARG_NULL_STRING = "nullString";
	public static final String ARG_TRIM_CHARDATA = "trimCharData";

	public static final String ARG_INCLUDE_IDENTITY = "includeAutoIncColumns";
	public static final String ARG_INCLUDE_READONLY = "includeReadOnlyColumns";

	// </editor-fold>

	private DataExporter exporter;
	private WbFile pendingOutput;

	private boolean consumeQuery;
	private boolean continueOnError;
	private String currentTable;
	private String defaultExtension;
	private boolean showProgress = true;
	private int progressInterval = 1;

	private final String exportTypes = "text,xml,sql,sqlinsert,sqlupdate,sqldeleteinsert,sqlmerge,sqldelete,ods,xlsm,html,xlsx,xls,json";

	public WbExport()
	{
		super();
		cmdLine = new ArgumentParser();
		CommonArgs.addDelimiterParameter(cmdLine);
		CommonArgs.addEncodingParameter(cmdLine);
		CommonArgs.addProgressParameter(cmdLine);
		CommonArgs.addCommitParameter(cmdLine);
		CommonArgs.addVerboseXmlParameter(cmdLine);
		CommonArgs.addQuoteEscaping(cmdLine);
		CommonArgs.addSqlDateLiteralParameter(cmdLine);

		cmdLine.addArgument(ARG_EXPORT_TYPE, StringUtil.stringToList(exportTypes));
		cmdLine.addArgument(ARG_OUTPUT_FILENAME, ArgumentType.Filename);
		cmdLine.addArgument(ARG_TABLE_PREFIX);
		cmdLine.addArgument(ARG_PAGE_TITLE);
		cmdLine.addArgument(ARG_TABLE);
		cmdLine.addArgument(ARG_QUOTECHAR);
		cmdLine.addArgument(ARG_DATEFORMAT);
		cmdLine.addArgument(ARG_TIMESTAMP_FORMAT);
		cmdLine.addArgument(ARG_TIMEFORMAT);
		cmdLine.addArgument(ARG_DECIMAL_SYMBOL);
		cmdLine.addArgument(ARG_DECIMAL_DIGITS);
		cmdLine.addArgument(ARG_CHARFUNC);
		cmdLine.addArgument(ARG_CONCAT_OPERATOR);
		cmdLine.addArgument(ARG_CONCAT_FUNCTION);
		cmdLine.addArgument(ARG_HEADER, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_CREATETABLE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_KEY_COLUMNS);
		cmdLine.addArgument(ARG_APPEND, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_XML_VERSION, StringUtil.stringToList("1.0", "1.1"));
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET, ArgumentType.Filename);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT, ArgumentType.Filename);
		cmdLine.addArgument(WbImport.ARG_FILE_EXT);
		cmdLine.addArgument(ARG_ESCAPE_HTML, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_CREATEFULL_HTML_PAGE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_PREDATA_HTML);
		cmdLine.addArgument(ARG_POSTDATA_HTML);
		cmdLine.addArgument(ARG_SOURCETABLE, ArgumentType.TableArgument);
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(ARG_OUTPUTDIR, ArgumentType.DirName);
		cmdLine.addArgument(ARG_USE_CDATA, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_ESCAPETEXT, StringUtil.stringToList("control,7bit,8bit,extended,none,pgcopy"));
		cmdLine.addArgument(ARG_QUOTE_ALWAYS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_LINEENDING, StringUtil.stringToList("crlf,lf"));
		cmdLine.addArgument(ARG_SHOW_ENCODINGS);
		cmdLine.addArgument(ARG_FORMATFILE, StringUtil.stringToList("postgres,oracle,sqlserver,db2,mysql"));
		cmdLine.addArgument("compress", ArgumentType.BoolSwitch);
		cmdLine.addArgument(ARG_EMPTY_RESULTS, ArgumentType.BoolArgument);
		cmdLine.addArgument("blobIdCols", ArgumentType.Deprecated);
		cmdLine.addArgument("lobIdCols");
		cmdLine.addArgument("filenameColumn");
		cmdLine.addArgument(ARG_BLOB_TYPE, BlobMode.getTypes());
		cmdLine.addArgument(ARG_CLOB_AS_FILE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_CONTINUE_ON_ERROR, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_CREATE_OUTPUTDIR, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_ROWNUM);
		cmdLine.addArgument(ARG_TABLEWHERE);
		cmdLine.addArgument(ARG_ADD_INFOSHEET, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_OPT_WIDTH, ArgumentType.BoolSwitch);
		cmdLine.addArgument(ARG_AUTOFILTER, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_FIXED_HEADER, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_USE_SCHEMA, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_COL_COMMENTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbImport.ARG_IGNORE_OWNER, ArgumentType.BoolArgument);
		cmdLine.addArgument(CommonArgs.ARG_EXCLUDE_TABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(CommonArgs.ARG_TYPES, ArgumentType.ObjectTypeArgument);
		cmdLine.addArgument(ARG_DISTRIBUTE_LOB_FILES, ArgumentType.IntegerArgument);
		cmdLine.addArgument(ARG_INFINITY_LITERALS, ArgumentType.ListArgument);
		cmdLine.addArgument(ARG_MERGE_TYPE, MergeGenerator.Factory.getSupportedTypes());
		cmdLine.addArgument(ARG_NULL_STRING);
		cmdLine.addArgument(ARG_TRIM_CHARDATA, ArgumentType.BoolSwitch);
		cmdLine.addArgument(ARG_TARGET_SHEET_IDX);
		cmdLine.addArgument(ARG_TARGET_SHEET_NAME);
		cmdLine.addArgument(ARG_INCLUDE_IDENTITY, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_READONLY, ArgumentType.BoolArgument);
		RegexModifierParameter.addArguments(cmdLine);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

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
		header += ", xlsm="  + Boolean.toString(getHeaderDefault("xlsm"));

		msg = msg.replace("%header_flag_default%", header);
		msg = msg.replace("%verbose_default%", Boolean.toString(getVerboseXmlDefault()));
		msg = msg.replace("%date_literal_default%", Settings.getInstance().getDefaultExportDateLiteralType());
		msg = msg.replace("%default_encoding%", Settings.getInstance().getDefaultDataEncoding());
		msg = msg.replace("%xmlversion%", Settings.getInstance().getDefaultXmlVersion());
		msg = msg.replace("%empty_results_default%", Boolean.toString(Settings.getInstance().getDefaultWriteEmptyExports()));
		msg = msg.replace("%use_schema_default%", Boolean.toString(Settings.getInstance().getIncludeOwnerInSqlExport()));

		String info = "ods="  + Boolean.toString(getInfoSheetDefault("ods"));
		info += ", xls="  + Boolean.toString(getInfoSheetDefault("xls"));
		info += ", xlsx="  + Boolean.toString(getInfoSheetDefault("xlsx"));
		info += ", xlsm="  + Boolean.toString(getInfoSheetDefault("xlsm"));
		msg = msg.replace("%infosheet_defaults%", info);
		msg = msg.replace("%types%", exportTypes);
		msg = msg.replace("%default_ts_format%", getTSFormatDefault());
		msg = msg.replace("%default_dt_format%", getDTFormatDefault());
		return msg;
	}

	private String getTSFormatDefault()
	{
		return Settings.getInstance().getDefaultTimestampFormat();
	}

	private String getDTFormatDefault()
	{
		return Settings.getInstance().getDefaultDateFormat();
	}

	private boolean getInfoSheetDefault(String type)
	{
		return Settings.getInstance().getDefaultExportInfoSheet(type);
	}

	private boolean getVerboseXmlDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.export.xml.default.verbose", true);
	}

	private boolean getHeaderDefault(String type)
	{
		return Settings.getInstance().getBoolProperty("workbench.export." + type + ".default.header", false);
	}

	private boolean getIgnoreOwnerDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.export.default.ignoreowner", true);
	}

	@Override
	public boolean ignoreMaxRows()
	{
		return true;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		cmdLine.parse(getCommandLine(sql));

		if (cmdLine.isArgPresent(ARG_SHOW_ENCODINGS))
		{
			result.addMessage(ResourceMgr.getString("MsgAvailableEncodings"));
			result.addMessage("");
			String[] encodings = EncodingUtil.getEncodings();
			for (String encoding : encodings)
			{
				result.addMessage(encoding);
			}
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, getWrongArgumentsMessage());
			return result;
		}

		WbFile outputFile = evaluateFileArgument(cmdLine.getValue(ARG_OUTPUT_FILENAME));
		String type = cmdLine.getValue(ARG_EXPORT_TYPE);

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


		if (type.equals("sqlmerge"))
		{
			boolean mergeAvailable = MergeGenerator.Factory.createGenerator(currentConnection) != null;
			if (!mergeAvailable)
			{
				result.addMessage("MERGE not supported for: " + currentConnection.getMetadata().getProductName());
				result.setFailure();
				return result;
			}
		}

		if ((type.equals("xls") || type.equals("xlsx")) && !PoiHelper.isPoiAvailable())
		{
			result.addMessage(ResourceMgr.getString("ErrNoXLS"));
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrExportUseXLSM"));
			result.setFailure();
			return result;
		}

		if (type.equals("xlsx") && !PoiHelper.isXLSXAvailable())
		{
			result.addMessage(ResourceMgr.getString("ErrNoXLSX"));
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrExportUseXLSM"));
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

		type = type.trim().toLowerCase();
		if ("txt".equals(type)) type = "text";

		if ("text".equals(type) && !CommonArgs.checkQuoteEscapting(cmdLine))
		{
			String msg = ResourceMgr.getString("ErrQuoteAlwaysEscape");
			LogMgr.logError("WbExport.execute()", msg, null);
			result.addMessage(msg);
			result.setFailure();
			return result;
		}

		this.exporter = new DataExporter(this.currentConnection);
		if (cmdLine.isArgPresent(ARG_TRIM_CHARDATA))
		{
			this.exporter.setTrimCharData(cmdLine.getBoolean(ARG_TRIM_CHARDATA, false));
		}

		String tables = cmdLine.getValue(ARG_SOURCETABLE);
		WbFile outputdir = evaluateFileArgument(cmdLine.getValue(ARG_OUTPUTDIR));

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

		Set<String> noAppendTypes = CollectionUtil.caseInsensitiveSet("html","json","xml","xlsm", "ods");
		boolean appendToFile = cmdLine.getBoolean(ARG_APPEND, false);
		if (appendToFile && noAppendTypes.contains(type))
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getFormattedString("ErrNoAppend", type));
			return result;
		}

		if (cmdLine.isArgPresent(ARG_INFINITY_LITERALS))
		{
			List<String> literals = cmdLine.getListValue(ARG_INFINITY_LITERALS);
			if (literals != null && literals.size() == 2)
			{
				exporter.setInfinityLiterals(new InfinityLiterals(literals.get(0), literals.get(1)));
			}
			else
			{
				result.addMessage("Illegal infinity definition ignored");
				result.setWarning(true);
			}
		}

		String updateTable = cmdLine.getValue(ARG_TABLE);

		String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING);
		if (encoding != null)
		{
			exporter.setEncoding(encoding);
		}

		exporter.setWriteEmptyResults(cmdLine.getBoolean(ARG_EMPTY_RESULTS, true));
		exporter.setWriteClobAsFile(cmdLine.getBoolean(ARG_CLOB_AS_FILE, false));
		boolean includeIdentityDefault = !Settings.getInstance().getGenerateInsertIgnoreIdentity();
		exporter.setIncludeIdentityCols(cmdLine.getBoolean(ARG_INCLUDE_IDENTITY, includeIdentityDefault));
		exporter.setIncludeReadOnlyCols(cmdLine.getBoolean(ARG_INCLUDE_READONLY, true));

		this.continueOnError = cmdLine.getBoolean(ARG_CONTINUE_ON_ERROR, false);

		String format = cmdLine.getValue(ARG_DATEFORMAT);
		if (format != null) exporter.setDateFormat(format);

		format = cmdLine.getValue(ARG_TIMESTAMP_FORMAT);
		if (format != null) exporter.setTimestampFormat(format);

		format = cmdLine.getValue(ARG_TIMEFORMAT);
		if (format != null) exporter.setTimeFormat(format);

		String decimal = cmdLine.getValue(ARG_DECIMAL_SYMBOL);
		int digits = cmdLine.getIntValue(ARG_DECIMAL_DIGITS, -1);

		if (digits > 0)
		{
			exporter.setFixedDigits(digits, decimal == null ? "." : decimal);
		}
		else if (decimal != null)
		{
			exporter.setDecimalSymbol(format);
		}

		exporter.setAppendInfoSheet(cmdLine.getBoolean(ARG_ADD_INFOSHEET, Settings.getInstance().getDefaultExportInfoSheet(type)));

		exporter.setTargetSheetIndex(cmdLine.getIntValue(ARG_TARGET_SHEET_IDX, -1));
		exporter.setTargetSheetName(cmdLine.getValue(ARG_TARGET_SHEET_NAME));

		// the formatting options should only default to true if no target sheet was specified
		boolean doFormatting = cmdLine.getIntValue(ARG_TARGET_SHEET_IDX, -1) < 0 && cmdLine.getValue(ARG_TARGET_SHEET_NAME) == null;
		exporter.setEnableAutoFilter(cmdLine.getBoolean(ARG_AUTOFILTER, doFormatting));
		exporter.setEnableFixedHeader(cmdLine.getBoolean(ARG_FIXED_HEADER, doFormatting));
		exporter.setOptimizeSpreadsheetColumns(cmdLine.getBoolean(ARG_OPT_WIDTH, doFormatting));

		exporter.setExportHeaders(cmdLine.getBoolean(ARG_HEADER, getHeaderDefault(type)));
		exporter.setPageTitle(cmdLine.getValue(ARG_PAGE_TITLE));
		exporter.setIncludeColumnComments(cmdLine.getBoolean(ARG_COL_COMMENTS, false));

		ExportDataModifier modifier = RegexModifierParameter.buildFromCommandline(cmdLine);
		exporter.setDataModifier(modifier);

		String bmode = cmdLine.getValue(ARG_BLOB_TYPE);
		boolean containerFileSupported = DataExporter.isContainerType(type);
		BlobMode btype = BlobMode.getMode(bmode);

		if (bmode != null && btype == null)
		{
			String types = StringUtil.listToString(BlobMode.getTypes(), ',');
			String msg = ResourceMgr.getFormattedString("ErrExportInvalidBlobType", bmode, types);
			result.addMessage(msg);
			result.setFailure();
			return result;
		}

		if (btype == null)
		{
			btype = BlobMode.SaveToFile;
		}

		exporter.setBlobMode(btype);
		if (updateTable != null) exporter.setTableName(updateTable);

		if (cmdLine.isArgPresent(ARG_USE_SCHEMA))
		{
			exporter.setUseSchemaInSql(cmdLine.getBoolean(ARG_USE_SCHEMA));
		}

		if ("text".equals(type))
		{
			try
			{
				Set<ControlFileFormat> formats = ControlFileFormat.parseCommandLine(cmdLine.getValue(ARG_FORMATFILE));
				exporter.addControlFileFormats(formats);
			}
			catch (WrongFormatFileException wf)
			{
				result.addMessage(ResourceMgr.getFormattedString("ErrExpWrongCtl", wf.getFormat()));
				result.setFailure();
				return result;
			}

			exporter.setNullString(cmdLine.getValue(ARG_NULL_STRING, null));

			String delimiter = cmdLine.getValue(CommonArgs.ARG_DELIM);
			if (delimiter != null) exporter.setTextDelimiter(delimiter);

			String quote = cmdLine.getValue(ARG_QUOTECHAR);
			if (quote != null) exporter.setTextQuoteChar(quote);

			String escape = cmdLine.getValue(ARG_ESCAPETEXT);
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
				else if ("pgcopy".equalsIgnoreCase(escape))
				{
					exporter.setEscapeRange(CharacterRange.RANGE_CONTROL);
					exporter.setEscapeType(CharacterEscapeType.pgHex);
				}
				else
				{
					exporter.setEscapeRange(null);
					String msg = ResourceMgr.getString("ErrExportInvalidEscapeRangeIgnored").replace("%value%", escape);
					result.addMessage(msg);
				}
			}
			exporter.setQuoteAlways(cmdLine.getBoolean(ARG_QUOTE_ALWAYS));
			QuoteEscapeType quoteEscaping = CommonArgs.getQuoteEscaping(cmdLine);
			if (quoteEscaping != QuoteEscapeType.none && StringUtil.isBlank(quote))
			{
				result.addMessageByKey("ErrExpQuoteRequired");
				result.setFailure();
				return result;
			}
			exporter.setQuoteEscaping(quoteEscaping);
			exporter.setRowIndexColumnName(cmdLine.getValue(ARG_ROWNUM));
			this.defaultExtension = ".txt";
		}
		else if (type.startsWith("sql"))
		{
			exporter.setIncludeCreateTable(cmdLine.getBoolean(ARG_INCLUDE_CREATETABLE));
			exporter.setChrFunction(cmdLine.getValue(ARG_CHARFUNC));
			exporter.setConcatFunction(cmdLine.getValue(ARG_CONCAT_FUNCTION));
			exporter.setConcatString(cmdLine.getValue(ARG_CONCAT_OPERATOR));
			exporter.setMergeType(cmdLine.getValue(ARG_MERGE_TYPE, currentConnection.getDbId()));

			CommonArgs.setCommitEvery(exporter, cmdLine);

			String c = cmdLine.getValue(ARG_KEY_COLUMNS);
			if (c != null)
			{
				List cols = StringUtil.stringToList(c, ",");
				exporter.setKeyColumnsToUse(cols);
			}
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

			String version = cmdLine.getValue(ARG_XML_VERSION);
			if (version != null)
			{
				exporter.setXMLVersion(version);
			}

			if (xsl != null && output != null)
			{
				XsltTransformer transformer = new XsltTransformer();
				File f = transformer.findStylesheet(xsl);
				if (f.exists())
				{
					exporter.setXsltTransformation(xsl);
					exporter.setXsltTransformationOutput(output);
				}
				else
				{
					String msg = ResourceMgr.getFormattedString("ErrXsltNotFound", f.getAbsolutePath());
					result.addMessage(msg);
				}
			}
			this.exporter.setUseCDATA(cmdLine.getBoolean(ARG_USE_CDATA));
			this.defaultExtension = ".xml";
			if (encoding == null)
			{
				// Make sure to use UTF-8 as the default if no encoding is specified
				this.exporter.setEncoding("UTF-8");
			}
		}
		else if ("html".equals(type))
		{
			String value = cmdLine.getValue(ARG_ESCAPE_HTML);
			if (value != null)
			{
				exporter.setEscapeHtml("true".equalsIgnoreCase(value));
			}
			value = cmdLine.getValue(ARG_CREATEFULL_HTML_PAGE);
			if (value != null)
			{
				exporter.setCreateFullHtmlPage("true".equalsIgnoreCase(value));
			}

			exporter.setHtmlHeading(cmdLine.getValue(ARG_PREDATA_HTML));
			exporter.setHtmlTrailer(cmdLine.getValue(ARG_POSTDATA_HTML));

			this.defaultExtension = ".html";
		}
		else if ("json".equals(type))
		{
			exporter.setNullString(cmdLine.getValue(ARG_NULL_STRING, null));
		}

		String ext = cmdLine.getValue(WbImport.ARG_FILE_EXT, null);
		if (ext != null)
		{
			defaultExtension = ext;
		}

		exporter.setAppendToFile(appendToFile);

		String ending = cmdLine.getValue(ARG_LINEENDING);
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
		exporter.setBlobIdColumns(columns);
		if (cmdLine.isArgPresent(ARG_DISTRIBUTE_LOB_FILES))
		{
			exporter.setMaxLobFilesPerDirectory(cmdLine.getIntValue(ARG_DISTRIBUTE_LOB_FILES, -1));
		}
		exporter.setCompressOutput(cmdLine.getBoolean("compress", false));

		// Setting the output type should be the last step in the configuration
		// of the exporter as this will trigger some initialization
		// that depends on the other properties
		setExportType(exporter, type);

		List<TableIdentifier> tablesToExport = null;
		try
		{
			String excluded = cmdLine.getValue(CommonArgs.ARG_EXCLUDE_TABLES);
			String schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
			String[] types = SourceTableArgument.parseTypes(cmdLine.getValue(CommonArgs.ARG_TYPES), currentConnection);
			SourceTableArgument argParser = new SourceTableArgument(tables, excluded, schema, types, this.currentConnection);
			tablesToExport = argParser.getTables();
			if (tablesToExport.isEmpty() && cmdLine.isArgPresent(ARG_SOURCETABLE))
			{
				String key = "ErrTableNotFound";
				if (argParser.wasWildcardArgument())
				{
					key = "ErrExportNoTablesFound";
				}
				result.addMessage(ResourceMgr.getFormattedString(key, tables));
				result.setFailure();
				return result;
			}
			if (cmdLine.isArgPresent(ARG_SOURCETABLE))
			{
				LogMgr.logDebug("WbExport.execute()", "Exporting tables: " + StringUtil.listToString(tablesToExport, ','));
			}
			List<String> notFound = argParser.getMissingTables();
			if (CollectionUtil.isNonEmpty(notFound))
			{
				String tableList = StringUtil.listToString(notFound, ',');
				String msg = ResourceMgr.getString("MsgTablesNotFound") + " " + tableList;
				result.addMessage(msg);
				result.addMessageNewLine();
				result.setWarning(true);
				LogMgr.logWarning("WbExport.execute()", "The following tables were not found: " + tableList);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbExport.runTableExports()", "Could not retrieve table list", e);
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
			return result;
		}

		// if the user cancelled the statement while the tables were retrieved,
		// The exported isn't actually running until now, so the cancel request was essentially ignored
		if (this.isCancelled)
		{
			result.setWarning(true);
			result.addMessageByKey("MsgExportCancelled");
			return result;
		}

		this.consumeQuery = tablesToExport.isEmpty();

		CommonArgs.setProgressInterval(this, cmdLine);
		this.showProgress = (this.progressInterval > 0);

		boolean create = cmdLine.getBoolean(ARG_CREATE_OUTPUTDIR, false);

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
			// Define the column that contains the value for blob extensions
			// this is only valid for single table exports
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
				msg = ResourceMgr.getString("ErrFileCreate") + " " + e.getMessage();
			}

			if (!canWrite)
			{
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
		}

		if (consumeQuery)
		{
			// Waiting for the next SQL Statement...
			this.exporter.setRowMonitor(this.rowMonitor);
			this.exporter.setReportInterval(this.progressInterval);
			this.runner.setConsumer(this);
			if (outputFile != null)
			{
				this.pendingOutput = outputFile;
			}
			else if (outputdir != null)
			{
				this.pendingOutput = outputdir;
			}

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
		}
		else
		{
			boolean ignoreOwner = cmdLine.getBoolean(WbImport.ARG_IGNORE_OWNER, getIgnoreOwnerDefault());
			String where = cmdLine.getValue(ARG_TABLEWHERE);
			try
			{
				exporter.setRowMonitor(this);
				exporter.setReportInterval(this.progressInterval);
				exporter.setContinueOnError(this.continueOnError);
				if (tablesToExport.size() > 1 || outputdir != null)
				{
					exportTableList(tablesToExport, result, outputdir, outputFile, cmdLine.getValue(ARG_TABLE_PREFIX), where, ignoreOwner, containerFileSupported);
				}
				else
				{
					exportSingleTable(tablesToExport.get(0), result, outputFile, where);
				}
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
		return cmdLine.isAllowedValue(ARG_EXPORT_TYPE, type);
	}

	private void setExportType(DataExporter exporter, String code)
	{
		ExportType type = ExportType.getExportType(code);
		exporter.setOutputType(type);
	}

	private void exportSingleTable(TableIdentifier table, StatementRunnerResult result, File outfile, String where)
		throws SQLException
	{
		exporter.addTableExportJob(outfile, table, where);
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

	private void exportTableList(List<TableIdentifier> tableList, StatementRunnerResult result, File outdir, File outfile, String prefix, String where, boolean ignoreOwner, boolean exporterSupportsContainers)
		throws SQLException
	{
		result.setSuccess();

		int tableCount = tableList.size();

		if (exporterSupportsContainers)
		{
			exporter.setAppendToFile(true);
			exporter.setPageTitle(null);
			if (outfile != null && outfile.exists())
			{
				outfile.delete();
			}
		}
		else
		{
			// when more than one table is selected or no outputfile is specified then we require an output directory
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
				String msg = ResourceMgr.getFormattedString("ErrExportOutputDirNotDir", outdir.getAbsolutePath());
				result.addMessage(msg);
				result.setFailure();
				return;
			}
		}

		for (TableIdentifier tbl : tableList)
		{
			String fname = StringUtil.makeFilename(ignoreOwner ? tbl.getTableName() : tbl.getTableExpression());
			WbFile f = null;
			if (outdir == null)
			{
				f = new WbFile(outfile);
			}
			else
			{
				f = new WbFile(outdir, fname + defaultExtension);
			}
			try
			{
				if (StringUtil.isBlank(prefix))
				{
					exporter.addTableExportJob(f, tbl, where);
				}
				else
				{
					String sql = "SELECT * FROM " + prefix + tbl.getTableExpression(currentConnection);
					exporter.addQueryJob(sql, f, where);
				}
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
			if (outdir == null)
			{
				msg = msg.replace("%dir%", outfile.getAbsolutePath());
			}
			else
			{
				msg = msg.replace("%dir%", outdir.getAbsolutePath());
			}
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

	@Override
	public void consumeResult(StatementRunnerResult toConsume)
	{
		if (!toConsume.isSuccess()) return;

		// Run an export that is defined by a SQL Statement
		// i.e. no sourcetable given in the initial wbexport command
		try
		{
			long rowCount = 0;
			if (toConsume.hasResultSets())
			{
				String sql = toConsume.getSourceCommand();

				ResultSet toExport = toConsume.getResultSets().get(0);
				// The exporter will close the resultSet that it exported
				// so we can remove it from the list of ResultSets in the StatementRunnerResult
				// object.
				// Thus the later call to clearResultSets() will only free any not used ResultSet
				toConsume.getResultSets().remove(0);
				rowCount = this.exporter.exportResultSet(pendingOutput, toExport, sql);
			}
			else if (toConsume.hasDataStores() && pendingOutput != null)
			{
				rowCount = exportDataStores(toConsume.getDataStores());
			}

			if (exporter.isSuccess())
			{
				toConsume.addMessage(""); // force new line in output
				toConsume.addMessage(ResourceMgr.getFormattedString("MsgSpoolOk", Long.toString(rowCount)));
				toConsume.addMessage(ResourceMgr.getString("MsgSpoolTarget") + " " + this.exporter.getFullOutputFilename());
			}
			addMessages(toConsume);

			if (exporter.isSuccess())
			{
				toConsume.setSuccess();
			}
			else
			{
				toConsume.setFailure();
			}
		}
		catch (Exception e)
		{
			toConsume.setFailure();
			toConsume.addMessage(ResourceMgr.getString("ErrExportExecute"));
			toConsume.addMessage(ExceptionUtil.getAllExceptions(e));
			LogMgr.logError("WbExportCommand.consumeResult()", "Error spooling data", e);
		}
		finally
		{
			toConsume.clearResultData();
			// Tell the statement runner we're done
			runner.setConsumer(null);
		}
	}

	private long exportDataStores(List<DataStore> data)
		throws Exception
	{
		if (this.pendingOutput == null) return 0;
		if (data == null || data.isEmpty()) return 0;
		long rowCount = 0;

		boolean isContainer = this.exporter.isContainerExport();
		boolean isFile = !pendingOutput.isDirectory(); // I cannot use isFile() as that returns false if the file does not exist

		this.exporter.setAppendToFile(isContainer);
		File baseDir;
		if (pendingOutput.isDirectory())
		{
			baseDir = pendingOutput;
		}
		else
		{
			baseDir = pendingOutput.getParentFile();
		}

		for (int resultNr=0; resultNr < data.size(); resultNr ++)
		{
			DataStore ds = data.get(resultNr);
			exporter.setPageTitle(ds.getResultName());
			WbFile output = null;
			if (isFile || isContainer)
			{
				output = pendingOutput;
			}
			else
			{
				// export to multiple files, so create the filename based on the result's name
				String name = ds.getResultName();
				if (name == null) name = "export_result_" + Integer.toString(resultNr + 1);
				output = new WbFile(baseDir, StringUtil.makeFilename(name) + defaultExtension);
			}
			rowCount += exporter.startExport(output, ds, null);
		}
		return rowCount;
	}

	@Override
	public void done()
	{
		super.done();
		exporter = null;
		maxRows = 0;
		consumeQuery = false;
		currentTable = null;
		defaultExtension = null;
	}

	@Override
	public void cancel()
		throws SQLException
	{
		if (this.exporter != null)
		{
			this.exporter.cancelExecution();
		}
		super.cancel();
	}

	@Override
	public void jobFinished()
	{
	}

	@Override
	public void setCurrentObject(String object, long number, long total)
	{
		this.currentTable = object;
		if (!this.showProgress && this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentObject(this.currentTable, -1, -1);
		}
	}

	@Override
	public void setCurrentRow(long currentRow, long totalRows)
	{
		if (this.showProgress && this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentObject(this.currentTable, currentRow, -1);
		}
	}

	@Override
	public void setReportInterval(int interval)
	{
		this.progressInterval = interval;
	}

	@Override
	public int getMonitorType()
	{
		return RowActionMonitor.MONITOR_PLAIN;
	}

	@Override
	public void setMonitorType(int aType)
	{
	}

	@Override
	public void saveCurrentType(String type)
	{
	}

	@Override
	public void restoreType(String type)
	{
	}

	protected String findTypeFromFilename(WbFile f)
	{
		if (f == null) return null;
		String fname = f.getFullPath().toLowerCase();
		if (fname.endsWith(".txt")) return "text";
		if (fname.endsWith(".xml")) return "xml";
		if (fname.endsWith(".text")) return "text";
		if (fname.endsWith(".csv")) return "text";
		if (fname.endsWith(".htm")) return "html";
		if (fname.endsWith(".html")) return "html";
		if (fname.endsWith(".sql")) return "sqlinsert";
		if (fname.endsWith(".xls")) return "xls";
		if (fname.endsWith(".xlsm")) return "xlsm";
		if (fname.endsWith(".xlsx")) return "xlsx";
		if (fname.endsWith(".ods")) return "ods";
		if (fname.endsWith(".json")) return "json";
		return null;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
