/*
 * CommonArguments.java
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
import workbench.resource.Settings;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;

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
	
	private static List<String> getDelimiterArguments()
	{
		return StringUtil.stringToList("'\\t',';',\"','\",'|',<char>");
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
}
