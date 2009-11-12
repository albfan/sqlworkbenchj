/*
 * WbImport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.importer.ConstantColumnValues;
import workbench.db.importer.CycleErrorException;
import workbench.db.importer.DataImporter;
import workbench.db.importer.DeleteType;
import workbench.db.importer.ImportFileLister;
import workbench.db.importer.ParsingInterruptedException;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.TableStatements;
import workbench.db.importer.TextFileParser;
import workbench.db.importer.XmlDataFileParser;
import workbench.interfaces.ImportFileParser;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.ConverterException;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;
import workbench.util.WbFile;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbImport
	extends SqlCommand
{
	public static final String VERB = "WBIMPORT";

	public static final String ARG_TYPE = "type";
	public static final String ARG_FILE = "file";
	public static final String ARG_TARGETTABLE = "table";
	public static final String ARG_QUOTE = "quotechar";
	public static final String ARG_CONTAINSHEADER = "header";
	public static final String ARG_FILECOLUMNS = "fileColumns";
	public static final String ARG_MODE = "mode";
	public static final String ARG_KEYCOLUMNS = "keyColumns";
	public static final String ARG_EMPTY_STRING_IS_NULL = "emptyStringIsNull";
	public static final String ARG_DECODE = "decode";
	public static final String ARG_IMPORTCOLUMNS = "importColumns";
	public static final String ARG_COL_FILTER = "columnFilter";
	public static final String ARG_LINE_FILTER = "lineFilter";
	public static final String ARG_DIRECTORY = "sourceDir";
	public static final String ARG_TARGET_SCHEMA = "schema";
	public static final String ARG_USE_TRUNCATE = "useTruncate";
	public static final String ARG_TRIM_VALUES = "trimValues";
	public static final String ARG_FILE_EXT = "extension";
	public static final String ARG_UPDATE_WHERE = "updateWhere";
	public static final String ARG_CREATE_TABLE = "createTarget";
	public static final String ARG_BLOB_ISFILENAME = "blobIsFilename";
	public static final String ARG_CLOB_ISFILENAME = "clobIsFilename";
	public static final String ARG_MULTI_LINE = "multiLine";
	public static final String ARG_START_ROW = "startRow";
	public static final String ARG_END_ROW = "endRow";
	public static final String ARG_BADFILE = "badFile";
	public static final String ARG_CONSTANTS = "constantValues";
	public static final String ARG_COL_WIDTHS = "columnWidths";
	public static final String ARG_IGNORE_OWNER = "ignoreOwner";
	public static final String ARG_EXCLUDE_FILES = "excludeFiles";
	public static final String ARG_USE_SAVEPOINT = "useSavepoint";

	private DataImporter imp;

	public WbImport()
	{
		super();
		this.isUpdatingCommand = true;
		this.cmdLine = new ArgumentParser();
		CommonArgs.addDelimiterParameter(cmdLine);
		CommonArgs.addEncodingParameter(cmdLine);
		CommonArgs.addProgressParameter(cmdLine);
		CommonArgs.addCommitParameter(cmdLine);
		CommonArgs.addContinueParameter(cmdLine);
		CommonArgs.addCommitAndBatchParams(cmdLine);
		CommonArgs.addQuoteEscaping(cmdLine);
		CommonArgs.addConverterOptions(cmdLine, true);
		CommonArgs.addCheckDepsParameter(cmdLine);
		CommonArgs.addTableStatements(cmdLine);
		CommonArgs.addTransactionControL(cmdLine);
		CommonArgs.addImportModeParameter(cmdLine);

		cmdLine.addArgument(ARG_TYPE, StringUtil.stringToList("text,xml"));
		cmdLine.addArgument(ARG_UPDATE_WHERE);
		cmdLine.addArgument(ARG_FILE);
		cmdLine.addArgument(ARG_TARGETTABLE, ArgumentType.TableArgument);
		cmdLine.addArgument(ARG_QUOTE);
		cmdLine.addArgument(ARG_CONTAINSHEADER, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_FILECOLUMNS);
		cmdLine.addArgument(ARG_KEYCOLUMNS);
		cmdLine.addArgument(CommonArgs.ARG_DELETE_TARGET, ArgumentType.BoolArgument);
		cmdLine.addArgument(CommonArgs.ARG_TRUNCATE_TABLE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_EMPTY_STRING_IS_NULL, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_DECODE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_IMPORTCOLUMNS);
		cmdLine.addArgument(ARG_COL_FILTER);
		cmdLine.addArgument(ARG_LINE_FILTER);
		cmdLine.addArgument(ARG_DIRECTORY);
		cmdLine.addArgument(ARG_TARGET_SCHEMA);
		cmdLine.addArgument(ARG_USE_TRUNCATE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_TRIM_VALUES, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_FILE_EXT);
		cmdLine.addArgument(ARG_CREATE_TABLE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_BLOB_ISFILENAME, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_CLOB_ISFILENAME, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_MULTI_LINE, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_START_ROW, ArgumentType.IntegerArgument);
		cmdLine.addArgument(ARG_END_ROW, ArgumentType.IntegerArgument);
		cmdLine.addArgument(ARG_BADFILE);
		cmdLine.addArgument(ARG_CONSTANTS);
		cmdLine.addArgument(ARG_COL_WIDTHS);
		cmdLine.addArgument(ARG_EXCLUDE_FILES);
		cmdLine.addArgument(ARG_IGNORE_OWNER, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_USE_SAVEPOINT, ArgumentType.BoolArgument);
		ModifierArguments.addArguments(cmdLine);
	}

	public String getVerb() { return VERB; }

	private void addWrongParamsMessage(StatementRunnerResult result)
	{
		if (WbManager.getInstance().isBatchMode()) return;
		String msg = getWrongParamsMessage();
		result.addMessageNewLine();
		result.addMessage(msg);
		result.setFailure();
	}

	private String getWrongParamsMessage()
	{
		String result = ResourceMgr.getString("ErrImportWrongParameters");
		result = StringUtil.replace(result, "%continue_default%", Boolean.toString(getContinueDefault()));
		result = StringUtil.replace(result, "%multiline_default%", Boolean.toString(getMultiDefault()));
		result = StringUtil.replace(result, "%header_default%", Boolean.toString(getHeaderDefault()));
		result = StringUtil.replace(result, "%trim_default%", Boolean.toString(getTrimDefault()));
		result = StringUtil.replace(result, "%default_encoding%", Settings.getInstance().getDefaultDataEncoding());
		boolean useSP = (currentConnection == null ? false : currentConnection.getDbSettings().useSavepointForImport());
		result = result.replace("%savepoint_default%", Boolean.toString(useSP));
		return result;
	}

	private boolean getContinueDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.import.default.continue", false);
	}

	private boolean getHeaderDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.import.default.header", true);
	}

	private boolean getMultiDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.import.default.multilinerecord", false);
	}

	private boolean getTrimDefault()
	{
		return Settings.getInstance().getBoolProperty("workbench.import.default.trimvalues", false);
	}

	public StatementRunnerResult execute(final String sqlCommand)
		throws SQLException
	{
		imp = new DataImporter();
		this.imp.setConnection(currentConnection);

		StatementRunnerResult result = new StatementRunnerResult(sqlCommand);
		String options = getCommandLine(sqlCommand);

		cmdLine.parse(options);

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, getWrongParamsMessage());
			return result;
		}

		if (!cmdLine.hasArguments())
		{
			addWrongParamsMessage(result);
			return result;
		}

		WbFile inputFile = evaluateFileArgument(cmdLine.getValue(ARG_FILE));
		String type = cmdLine.getValue(ARG_TYPE);
		String dir = cmdLine.getValue(ARG_DIRECTORY);
		String defaultExtension = null;

		if (inputFile == null && dir == null)
		{
			result.addMessage(ResourceMgr.getString("ErrImportFileMissing"));
			addWrongParamsMessage(result);
			return result;
		}

		if (type == null && inputFile != null)
		{
			type = findTypeFromFilename(inputFile.getFullPath());
		}

		if (type == null)
		{
			result.addMessage(ResourceMgr.getString("ErrImportTypeMissing"));
			addWrongParamsMessage(result);
			return result;
		}

		if (!type.equalsIgnoreCase("text") && !type.equalsIgnoreCase("txt") && !type.equalsIgnoreCase("xml"))
		{
			result.addMessage(ResourceMgr.getString("ErrImportInvalidType"));
			result.setFailure();
			return result;
		}

		String badFile = cmdLine.getValue(ARG_BADFILE);
		if (badFile != null)
		{
			boolean multiFileImport = (dir != null && inputFile == null);
			File bf = new File(badFile);
			if (multiFileImport && !bf.isDirectory())
			{
				result.addMessage(ResourceMgr.getString("ErrImportBadFileNoDir"));
				result.setFailure();
				return result;
			}
		}
		CommonArgs.setCommitAndBatchParams(imp, cmdLine);

		boolean continueOnError = cmdLine.getBoolean(CommonArgs.ARG_CONTINUE, getContinueDefault());
		imp.setContinueOnError(continueOnError);

		imp.setUseSavepoint(cmdLine.getBoolean(ARG_USE_SAVEPOINT, currentConnection.getDbSettings().useSavepointForImport()));

		String table = cmdLine.getValue(ARG_TARGETTABLE);
		String schema = cmdLine.getValue(ARG_TARGET_SCHEMA);

		if (inputFile != null)
		{
			if (!inputFile.exists())
			{
				String msg = ResourceMgr.getString("ErrImportFileNotFound");
				msg = StringUtil.replace(msg, "%filename%", inputFile.getFullPath());
				LogMgr.logError("WbImport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
		}
		else
		{
			WbFile d = evaluateFileArgument(dir);
			if (!d.exists())
			{
				String msg = ResourceMgr.getString("ErrImportSourceDirNotFound");
				msg = StringUtil.replace(msg, "%dir%", dir);
				LogMgr.logError("WbImport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
			if (!d.isDirectory())
			{
				String msg = ResourceMgr.getString("ErrImportNoDir");
				msg = StringUtil.replace(msg, "%dir%", dir);
				LogMgr.logError("WbImport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
		}

		String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING);
		ImportFileParser parser = null;

		if ("text".equalsIgnoreCase(type) || "txt".equalsIgnoreCase(type))
		{
			if (table == null && dir == null)
			{
				String msg = ResourceMgr.getString("ErrTextImportRequiresTableName");
				LogMgr.logError("WbImport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
				return result;
			}

			defaultExtension = "txt";

			TextFileParser textParser = new TextFileParser();
			parser = textParser;

			textParser.setTableName(table);

			if (inputFile != null)
			{
				textParser.setInputFile(inputFile);
			}

			boolean multi = cmdLine.getBoolean(ARG_MULTI_LINE, getMultiDefault());
			textParser.setEnableMultilineRecords(multi);
			textParser.setTargetSchema(schema);
			textParser.setConnection(currentConnection);
			textParser.setAbortOnError(!continueOnError);
			textParser.setTreatClobAsFilenames(cmdLine.getBoolean(ARG_CLOB_ISFILENAME, false));

			String delimiter = StringUtil.trimQuotes(cmdLine.getValue(CommonArgs.ARG_DELIM));
			if (delimiter != null) textParser.setDelimiter(delimiter);

			String quote = cmdLine.getValue(ARG_QUOTE);
			if (quote != null) textParser.setQuoteChar(quote);

			textParser.setDecodeUnicode(cmdLine.getBoolean(ARG_DECODE, false));

			if (encoding != null) textParser.setEncoding(encoding);

			textParser.setEmptyStringIsNull(cmdLine.getBoolean(ARG_EMPTY_STRING_IS_NULL, true));

			boolean headerDefault = Settings.getInstance().getBoolProperty("workbench.import.default.header", true);
			boolean header = cmdLine.getBoolean(ARG_CONTAINSHEADER, headerDefault);

			String filecolumns = cmdLine.getValue(ARG_FILECOLUMNS);

			// The flag for a header lines must be specified before setting the columns
			textParser.setContainsHeader(header);

			if (StringUtil.isNonBlank(table))
			{
				try
				{
					textParser.checkTargetTable();
				}
				catch (Exception e)
				{
					result.addMessage(textParser.getMessages());
					result.setFailure();
					return result;
				}

				// read column definition from header line
				// if no header was specified, the text parser
				// will assume the columns in the text file
				// map to the column in the target table
				try
				{
					if (StringUtil.isBlank(filecolumns))
					{
						textParser.setupFileColumns();
					}
					else
					{
						List<ColumnIdentifier> fileCols = stringToCols(filecolumns);
						textParser.setColumns(fileCols);
					}
				}
				catch (Exception e)
				{
					result.setFailure();
					result.addMessage(textParser.getMessages());
					LogMgr.logError("WbImport.execute()", ExceptionUtil.getDisplay(e),null);
					return result;
				}

				String importcolumns = cmdLine.getValue(ARG_IMPORTCOLUMNS);
				if (StringUtil.isNonBlank(importcolumns))
				{
					List<ColumnIdentifier> toImport = stringToCols(importcolumns);
					textParser.retainColumns(toImport);
				}
			}

			// The column filter has to bee applied after the
			// columns are defined!
			String colFilter = cmdLine.getValue(ARG_COL_FILTER);
			if (colFilter != null)
			{
				addColumnFilter(colFilter, textParser);
			}

			textParser.setTreatBlobsAsFilenames(cmdLine.getBoolean(ARG_BLOB_ISFILENAME, true));

			String filter = cmdLine.getValue(ARG_LINE_FILTER);
			if (filter != null)
			{
				textParser.setLineFilter(StringUtil.trimQuotes(filter));
			}
			textParser.setQuoteEscaping(CommonArgs.getQuoteEscaping(cmdLine));

			// when all columns are defined we can check for a fixed-width import
			String width = cmdLine.getValue(ARG_COL_WIDTHS);
			if (!StringUtil.isEmptyString(width))
			{
				try
				{
					ColumnWidthDefinition def = new ColumnWidthDefinition(width);
					textParser.setColumnWidths(def.getColumnWidths());
				}
				catch (MissingWidthDefinition e)
				{
					result.addMessage(ResourceMgr.getFormattedString("ErrImpWrongWidth", e.getColumnName()));
					result.setFailure();
					return result;
				}
			}
			imp.setProducer(textParser);
		}
		else if ("xml".equalsIgnoreCase(type))
		{
			defaultExtension = "xml";

			XmlDataFileParser xmlParser = new XmlDataFileParser();
			xmlParser.setConnection(currentConnection);
			xmlParser.setAbortOnError(!continueOnError);
			parser = xmlParser;

			// The encoding must be set as early as possible
			// as the XmlDataFileParser might need it to read
			// the table structure!
			if (encoding != null) xmlParser.setEncoding(encoding);
			if (table != null) xmlParser.setTableName(table);

			if (dir == null)
			{
				xmlParser.setInputFile(inputFile);
				String cols = cmdLine.getValue(ARG_IMPORTCOLUMNS);
				if (cols != null)
				{
					try
					{
						xmlParser.setColumns(cols);
					}
					catch (Exception e)
					{
						result.setFailure();
						String col = xmlParser.getMissingColumn();
						String msg = ResourceMgr.getFormattedString("ErrImportColumnNotFound", col, table);
						result.addMessage(msg);
						LogMgr.logError("WbImport.execute()", msg, null);
						return result;
					}
				}
			}
			imp.setCreateTarget(cmdLine.getBoolean(ARG_CREATE_TABLE, false));
			imp.setProducer(xmlParser);
		}

		ImportFileLister sorter = getFileNameSorter(cmdLine, defaultExtension);
		if (sorter != null)
		{
			parser.setSourceFiles(sorter);
		}
		parser.setTrimValues(cmdLine.getBoolean(ARG_TRIM_VALUES, getTrimDefault()));

		ValueConverter converter = null;
		try
		{
			converter = CommonArgs.getConverter(cmdLine);
		}
		catch (Exception e)
		{
			LogMgr.logError("WbImport.execute()", "Error creating ValueConverter", e);
			result.setFailure();
			result.addMessage(e.getMessage());
			return result;
		}

		imp.setTransactionControl(cmdLine.getBoolean(CommonArgs.ARG_TRANS_CONTROL, true));

		RowDataProducer prod = imp.getProducer();
		if (prod != null)
		{
			prod.setValueConverter(converter);
		}

		try
		{
			// Column Modifiers will only be evaluated for
			// single file imports to avoid confusion of columns
			if (dir == null)
			{
				ModifierArguments args = new ModifierArguments(cmdLine);
				parser.setValueModifier(args.getModifier());
			}
		}
		catch (NumberFormatException e)
		{
			result.addMessage(ResourceMgr.getString("ErrImportWrongLimit"));
			result.setFailure();
			return result;
		}

		if (!StringUtil.isEmptyString(table) && !StringUtil.isEmptyString(dir))
		{
			parser.setMultiFileImport(true);
		}

		if (badFile != null) imp.setBadfileName(badFile);

		String value = cmdLine.getValue(CommonArgs.ARG_PROGRESS);
		if (value == null && inputFile != null)
		{
			int interval = DataImporter.estimateReportIntervalFromFileSize(inputFile);
			imp.setReportInterval(interval);
		}
		else if ("true".equalsIgnoreCase(value))
		{
			this.imp.setReportInterval(1);
		}
		else if ("false".equalsIgnoreCase(value))
		{
			this.imp.setReportInterval(0);
		}
		else if (value != null)
		{
			int interval = StringUtil.getIntValue(value, 0);
			this.imp.setReportInterval(interval);
		}
		else
		{
			this.imp.setReportInterval(10);
		}

		String constants = cmdLine.getValue(ARG_CONSTANTS);
		if (!StringUtil.isEmptyString(constants))
		{
			try
			{
				ConstantColumnValues values = new ConstantColumnValues(constants, this.currentConnection, table, converter);
 				imp.setConstantColumnValues(values);
			}
			catch (Exception e)
			{
				LogMgr.logError("WbImport.execute()", "Column constants could no be parsed", e);
				result.setFailure();
				result.addMessage(e.getMessage());
				return result;
			}
		}

		String mode = cmdLine.getValue(ARG_MODE);
		if (mode != null)
		{
			if (!imp.setMode(mode))
			{
				result.addMessage(ResourceMgr.getString("ErrInvalidModeIgnored").replace("%mode%", mode));
			}
		}

		String where = cmdLine.getValue(ARG_UPDATE_WHERE);
		imp.setWhereClauseForUpdate(where);

		if (schema != null)
		{
			imp.setTargetSchema(schema);
		}

		String keyColumns = cmdLine.getValue(ARG_KEYCOLUMNS);
		imp.setKeyColumns(keyColumns);

		if (!parser.isMultiFileImport())
		{
			DeleteType delete = CommonArgs.getDeleteType(cmdLine);
			imp.setDeleteTarget(delete);
		}

		int startRow = cmdLine.getIntValue(ARG_START_ROW, -1);
		if (startRow > 0) imp.setStartRow(startRow);

		int endRow = cmdLine.getIntValue(ARG_END_ROW, -1);
		if (endRow > 0) imp.setEndRow(endRow);

		imp.setRowActionMonitor(this.rowMonitor);
		imp.setPerTableStatements(new TableStatements(cmdLine));

		try
		{
			imp.startImport();
			if (imp.isSuccess())
			{
				result.setSuccess();
			}
			else
			{
				result.setFailure();
			}
			result.setWarning(imp.hasWarnings());
		}
		catch (CycleErrorException e)
		{
			// Logging already done.
			result.setFailure();
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbImport.execute()", "Error importing " + (inputFile == null ? dir : inputFile), e);
			result.setFailure();
		}
		catch (ConverterException e)
		{
			LogMgr.logError("WbImport.execute()", "Error importing " + (inputFile == null ? dir : inputFile), e);
			result.setFailure();
		}
		catch (ParsingInterruptedException e)
		{
			// Logging already done by DataImporter
			result.setFailure();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbImport.execute()", "Error importing '" + inputFile +"': " + e.getMessage(), e);
			result.setFailure();
			addErrorInfo(result, sqlCommand, e);
		}
		result.addMessage(imp.getMessages());

		return result;
	}

	private List<ColumnIdentifier> stringToCols(String columns)
	{
		List<String> names = StringUtil.stringToList(columns, ",", true, true);
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>(names.size());
		for (String name : names)
		{
			cols.add(new ColumnIdentifier(name));
		}
		return cols;
	}

	private ImportFileLister getFileNameSorter(ArgumentParser cmdLine, String defaultExt)
	{
		String dir = cmdLine.getValue(ARG_DIRECTORY);
		if (dir == null) return null;
		String ext = cmdLine.getValue(ARG_FILE_EXT);
		if (ext == null) ext = defaultExt;

		WbFile fdir = evaluateFileArgument(dir);
		ImportFileLister lister = new ImportFileLister(this.currentConnection, fdir, ext);
		lister.setIgnoreSchema(cmdLine.getBoolean(ARG_IGNORE_OWNER, false));
		lister.ignoreFiles(cmdLine.getListValue(ARG_EXCLUDE_FILES));
		lister.setCheckDependencies(cmdLine.getBoolean(CommonArgs.ARG_CHECK_FK_DEPS, false));
		return lister;
	}

	private void addColumnFilter(String filters, TextFileParser textParser)
	{
		List<String> filterList = StringUtil.stringToList(filters, ",", false);

		if (filterList.size() < 1) return;

		for (String filterDef : filterList)
		{
			List<String> l = StringUtil.stringToList(filterDef, "=", true);
			if (l.size() != 2) continue;

			String col = l.get(0);
			String regex = l.get(1);
			textParser.addColumnFilter(col, StringUtil.trimQuotes(regex));
		}
	}

	public void done()
	{
		super.done();
		this.imp = null;
	}

	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.imp != null)
		{
			this.imp.cancelExecution();
		}
	}

	protected String findTypeFromFilename(String fname)
	{
		if (fname == null) return null;
		if (fname.toLowerCase().endsWith(".txt")) return "text";
		if (fname.toLowerCase().endsWith(".xml")) return "xml";
		if (fname.toLowerCase().endsWith(".text")) return "text";
		if (fname.toLowerCase().endsWith(".csv")) return "text";
		return null;
	}
}
