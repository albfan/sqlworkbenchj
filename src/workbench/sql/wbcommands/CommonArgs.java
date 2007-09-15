/*
 * CommonArgs.java
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

import java.util.List;
import workbench.interfaces.BatchCommitter;
import workbench.interfaces.Committer;
import workbench.interfaces.ProgressReporter;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
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
 * @author support@sql-workbench.net
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
	public static final String ARG_DECCHAR = "decimal";
	public static final String ARG_FALSE_LITERALS = "literalsFalse";
	public static final String ARG_TRUE_LITERALS = "literalsTrue";
	public static final String ARG_CHECK_FK_DEPS = "checkDependencies";
	
	private static List<String> getDelimiterArguments()
	{
		return StringUtil.stringToList("'\\t',';',\"','\",'|',<char>");
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

	/**
	 * Adds the quoteCharEscaping argument. Valid values 
	 * are none, duplicate, escape.
	 * @see workbench.util.QuoteEscapeType
	 */
	public static void addQuoteEscapting(ArgumentParser cmdLine)
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
		
		int queueSize = cmdLine.getIntValue(ARG_BATCHSIZE,-1);
		String commitParam = cmdLine.getValue("commitevery");
		
		if (queueSize > 0)
		{
			committer.setUseBatch(true);
			committer.setBatchSize(queueSize);
			
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
		cmdLine.addArgument(ARG_DECCHAR);
		if (includeDateFormats)
		{
			cmdLine.addArgument(ARG_DATE_FORMAT);
			cmdLine.addArgument(ARG_TIMESTAMP_FORMAT);
		}
		cmdLine.addArgument(ARG_FALSE_LITERALS);
		cmdLine.addArgument(ARG_TRUE_LITERALS);
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
			throw new IllegalArgumentException(msg);
		}
		
		String decimal = cmdLine.getValue(ARG_DECCHAR);
		if (decimal != null) converter.setDecimalCharacter(decimal.charAt(0));

		List<String> falseValues = StringUtil.stringToList(cmdLine.getValue(ARG_FALSE_LITERALS), ",", true, true, false);
		List<String> trueValues = StringUtil.stringToList(cmdLine.getValue(ARG_TRUE_LITERALS), ",", true, true, false);
		if (falseValues.size() > 0 && trueValues.size() > 0)
		{
			converter.setBooleanLiterals(trueValues, falseValues);
		}
		return converter;
	}
	
}
