/*
 * CommonArgs.java
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

import java.util.List;

import workbench.interfaces.BatchCommitter;
import workbench.interfaces.Committer;
import workbench.interfaces.ProgressReporter;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.DropType;
import workbench.db.importer.DeleteType;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.QuoteEscapeType;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;

/**
 * A class to manage common parameters for various WbCommands.
 *
 * When adding the parameters to the ArgumentParser, the necessary
 * values for the code completion are also supplied.
 * For parameters with no fixed set of values a sample list
 * with popular (or so I think) values is added, e.g. the -encoding
 * parameter.
 *
 * @author Thomas Kellerer
 */
public class CommonArgs
{
	public static final String ARG_PROGRESS = "showProgress";
	public static final String ARG_ENCODING = "encoding";
	public static final String ARG_COMMIT = "commitEvery";
	public static final String ARG_DELIM = "delimiter";
	public static final String ARG_VERBOSE_XML = "verboseXML";
	public static final String ARG_IMPORT_MODE = "mode";
	public static final String ARG_CONTINUE = "continueOnError";
	public static final String ARG_BATCHSIZE = "batchSize";
	public static final String ARG_COMMIT_BATCH = "commitBatch";
	public static final String ARG_QUOTE_ESCAPE = "quoteCharEscaping";
	public static final String ARG_AUTO_BOOLEAN = "booleanToNumber";
	public static final String ARG_DATE_FORMAT = "dateFormat";
	public static final String ARG_TIMESTAMP_FORMAT = "timestampFormat";
	public static final String ARG_DECIMAL_CHAR = "decimal";
	public static final String ARG_NUMERIC_TRUE = "numericTrue";
	public static final String ARG_NUMERIC_FALSE = "numericFalse";
	public static final String ARG_FALSE_LITERALS = "literalsFalse";
	public static final String ARG_TRUE_LITERALS = "literalsTrue";
	public static final String ARG_CHECK_FK_DEPS = "checkDependencies";
	public static final String ARG_PRE_TABLE_STMT = "preTableStatement";
	public static final String ARG_POST_TABLE_STMT = "postTableStatement";
	public static final String ARG_IGNORE_TABLE_STMT_ERRORS = "ignorePrePostErrors";
	public static final String ARG_RUN_POST_STMT_ON_ERROR = "runTableStatementOnError";
	public static final String ARG_TRANS_CONTROL = "transactionControl";
	public static final String ARG_DATE_LITERAL_TYPE = "sqlDateLiterals";
	public static final String ARG_DELETE_TARGET = "deleteTarget";
	public static final String ARG_TRUNCATE_TABLE = "truncateTable";
	public static final String ARG_SCHEMA = "schema";
	public static final String ARG_CATALOG = "catalog";
	public static final String ARG_EXCLUDE_TABLES = "excludeTables";
	public static final String ARG_SCHEMAS = "schemas";
	public static final String ARG_TYPES = "types";
	public static final String ARG_OBJECTS = "objects";
	public static final String ARG_IGNORE_IDENTITY = "ignoreIdentityColumns";
	public static final String ARG_HELP = "help";
	public static final String ARG_VERBOSE = "verbose";
	public static final String ARG_FILE = "file";
	public static final String ARG_OUTPUT_FILE = "outputFile";


	private static List<String> getDelimiterArguments()
	{
		return StringUtil.stringToList("'\\t',';',\"','\",'|',<char>");
	}

	public static void addTransactionControL(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_TRANS_CONTROL, ArgumentType.BoolArgument);
	}

	public static List<String> getListArgument(ArgumentParser cmdLine, String arg)
	{
		String value = cmdLine.getValue(arg);
		if (StringUtil.isEmptyString(value)) return null;
		List<String> items = StringUtil.stringToList(value, ",");
		return items;
	}

	public static DeleteType getDeleteType(ArgumentParser cmdLine)
	{
		if (cmdLine.getBoolean(ARG_TRUNCATE_TABLE, false)) return DeleteType.truncate;
		if (cmdLine.getBoolean(ARG_DELETE_TARGET, false)) return DeleteType.delete;
		return DeleteType.none;
	}

	public static void addTableStatements(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_PRE_TABLE_STMT);
		cmdLine.addArgument(ARG_POST_TABLE_STMT);
		cmdLine.addArgument(ARG_IGNORE_TABLE_STMT_ERRORS, ArgumentType.BoolSwitch);
		cmdLine.addArgument(ARG_RUN_POST_STMT_ON_ERROR, ArgumentType.BoolSwitch);
	}

	public static void addCheckDepsParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_CHECK_FK_DEPS, ArgumentType.BoolArgument);
	}

	public static void addContinueParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_CONTINUE, ArgumentType.BoolArgument);
	}

	public static void addImportModeParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_IMPORT_MODE, StringUtil.stringToList("insert,update,\"update,insert\",\"insert,update\""));
	}

	public static void addSqlDateLiteralParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_DATE_LITERAL_TYPE, Settings.getInstance().getLiteralTypeList());
	}

	public static void addVerboseXmlParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_VERBOSE_XML, ArgumentType.BoolArgument);
	}

	public static void addCommitParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_COMMIT, StringUtil.stringToList("none,atEnd,<number>"));
	}

	public static void addDelimiterParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_DELIM, getDelimiterArguments());
	}

	public static boolean checkQuoteEscapting(ArgumentParser cmdLine)
	{
		boolean quoteAlways = cmdLine.getBoolean(WbExport.ARG_QUOTE_ALWAYS, false);
		QuoteEscapeType escape = getQuoteEscaping(cmdLine);
		if (quoteAlways && escape == QuoteEscapeType.duplicate) return false;
		return true;
	}

	/**
	 * Adds the quoteCharEscaping argument. Valid values
	 * are none, duplicate, escape.
	 * @see workbench.util.QuoteEscapeType
	 */
	public static void addQuoteEscaping(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_QUOTE_ESCAPE, StringUtil.stringToList("none,duplicate,escape"));
	}

	/**
	 * Adds the -encoding parameter to the ArgumentParser.
	 * The encodings that are added to the code completion list
	 * are retrieved from the Settings class.
	 * @param cmdLine the ArgumentParser to which the parameter should be added
	 * @see workbench.resource.Settings#getPopularEncodings()
	 */
	public static void addEncodingParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_ENCODING, StringUtil.stringToList(Settings.getInstance().getPopularEncodings()));
	}

	public static void addProgressParameter(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_PROGRESS, StringUtil.stringToList("true,false,<number>"));
	}

	public static void setProgressInterval(ProgressReporter reporter, ArgumentParser cmdLine)
	{
		String value = cmdLine.getValue(ARG_PROGRESS);

		if ("true".equalsIgnoreCase(value))
		{
			reporter.setReportInterval(1);
		}
		else if ("false".equalsIgnoreCase(value))
		{
			reporter.setReportInterval(0);
		}
		else if (value != null)
		{
			int interval = StringUtil.getIntValue(value, 0);
			reporter.setReportInterval(interval);
		}
		else
		{
			reporter.setReportInterval(ProgressReporter.DEFAULT_PROGRESS_INTERVAL);
		}
	}

	public static void setCommitEvery(Committer committer, ArgumentParser cmdLine)
	{
		String commitParam = cmdLine.getValue("commitevery");
		if (commitParam == null) return;

		if ("none".equalsIgnoreCase(commitParam) || "false".equalsIgnoreCase(commitParam))
		{
			committer.commitNothing();
		}
		else if ("atEnd".equalsIgnoreCase(commitParam))
		{
			committer.setCommitEvery(0);
		}
		else
		{
			committer.setCommitEvery(StringUtil.getIntValue(commitParam,0));
		}

	}

	public static void addCommitAndBatchParams(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_BATCHSIZE);
		cmdLine.addArgument(ARG_COMMIT_BATCH, ArgumentType.BoolArgument);
	}

	public static void setCommitAndBatchParams(BatchCommitter committer, ArgumentParser cmdLine)
	{
		int batchSize = cmdLine.getIntValue(ARG_BATCHSIZE,-1);
		String commitParam = cmdLine.getValue("commitevery");

		if (batchSize > 0)
		{
			committer.setUseBatch(true);
			committer.setBatchSize(batchSize);

			if (cmdLine.isArgPresent(ARG_COMMIT_BATCH))
			{
				committer.setCommitBatch(cmdLine.getBoolean(ARG_COMMIT_BATCH, false));
			}
			else if ("none".equalsIgnoreCase(commitParam) || "false".equalsIgnoreCase(commitParam))
			{
				committer.commitNothing();
			}
		}
		else
		{
			setCommitEvery(committer, cmdLine);
		}
	}

	public static QuoteEscapeType getQuoteEscaping(ArgumentParser cmdLine)
	{
		String esc = cmdLine.getValue(ARG_QUOTE_ESCAPE);
		if (esc != null)
		{
			try
			{
				QuoteEscapeType escapeType = QuoteEscapeType.valueOf(esc.trim().toLowerCase());
				return escapeType;
			}
			catch (Exception e)
			{
				// ignore --> return none
			}
		}
		return QuoteEscapeType.none;
	}

	public static void addConverterOptions(ArgumentParser cmdLine, boolean includeDateFormats)
	{
		cmdLine.addArgument(ARG_AUTO_BOOLEAN, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_DECIMAL_CHAR);
		if (includeDateFormats)
		{
			cmdLine.addArgument(ARG_DATE_FORMAT);
			cmdLine.addArgument(ARG_TIMESTAMP_FORMAT);
		}
		cmdLine.addArgument(ARG_FALSE_LITERALS);
		cmdLine.addArgument(ARG_TRUE_LITERALS);
		cmdLine.addArgument(ARG_NUMERIC_FALSE);
		cmdLine.addArgument(ARG_NUMERIC_TRUE);
	}

	public static ValueConverter getConverter(ArgumentParser cmdLine)
		throws IllegalArgumentException
	{
		ValueConverter converter = new ValueConverter();
		converter.setAutoConvertBooleanNumbers(cmdLine.getBoolean(ARG_AUTO_BOOLEAN, true));

		String format = null;
		try
		{
			format = cmdLine.getValue(ARG_DATE_FORMAT);
			if (format != null) converter.setDefaultDateFormat(format);

			format = cmdLine.getValue(ARG_TIMESTAMP_FORMAT);
			if (format != null) converter.setDefaultTimestampFormat(format);
		}
		catch (Exception e)
		{
			String msg = ResourceMgr.getFormattedString("ErrIllegalDateTimeFormat", format);
			throw new IllegalArgumentException(msg, e);
		}

		String decimal = cmdLine.getValue(ARG_DECIMAL_CHAR);
		if (decimal != null) converter.setDecimalCharacter(decimal.charAt(0));

		List<String> falseValues = cmdLine.getListValue(ARG_FALSE_LITERALS);
		List<String> trueValues = cmdLine.getListValue(ARG_TRUE_LITERALS);
		if (falseValues.size() > 0 && trueValues.size() > 0)
		{
			converter.setBooleanLiterals(trueValues, falseValues);
		}

		if (cmdLine.isArgPresent(ARG_NUMERIC_FALSE) && cmdLine.isArgPresent(ARG_NUMERIC_TRUE))
		{
			int trueValue = cmdLine.getIntValue(ARG_NUMERIC_TRUE, 1);
			int falseValue = cmdLine.getIntValue(ARG_NUMERIC_FALSE, 0);
			converter.setNumericBooleanValues(falseValue, trueValue);
			converter.setAutoConvertBooleanNumbers(true);
		}
		return converter;
	}

	public static DropType getDropType(ArgumentParser cmdLine)
	{
		String drop = cmdLine.getValue(WbCopy.PARAM_DROPTARGET, null);
		DropType dropType = DropType.none;
		if (drop != null)
		{
			if (drop.startsWith("cascade"))
			{
				dropType = DropType.cascaded;
			}
			else if (StringUtil.stringToBool(drop))
			{
				dropType = DropType.regular;
			}
		}
		return dropType;
	}

	public static void appendArgument(StringBuilder result, String arg, String value, CharSequence indent)
	{
		if (StringUtil.isNonBlank(value))
		{
			if (indent.charAt(0) != '\n')
			{
				result.append('\n');
			}
			result.append(indent);
			result.append('-');
			result.append(arg);
			result.append('=');

			if (value.indexOf('-') > -1 || value.indexOf(';') > -1)
			{
				result.append('"');
			}
			else if ("\"".equals(value))
			{
				result.append('\'');
			}
			else if ("\'".equals(value))
			{
				result.append('\"');
			}

			result.append(value);

			if (value.indexOf('-') > -1 || value.indexOf(';') > -1)
			{
				result.append('"');
			}
			else if ("\"".equals(value))
			{
				result.append('\'');
			}
			else if ("\'".equals(value))
			{
				result.append('\"');
			}
		}
	}

}
