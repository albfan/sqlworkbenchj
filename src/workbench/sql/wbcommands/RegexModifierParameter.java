/*
 *  RegexModifierParameter.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.util.regex.PatternSyntaxException;
import workbench.db.exporter.RegexReplacingModifier;
import workbench.log.LogMgr;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexModifierParameter
{

	public static final String ARG_REPLACE_REGEX = "replaceExpression";
	public static final String ARG_REPLACE_WITH = "replaceWith";

	public static void addArguments(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_REPLACE_WITH);
		cmdLine.addArgument(ARG_REPLACE_REGEX);
	}

	public static RegexReplacingModifier buildFromCommandline(ArgumentParser cmdLine)
	{
		String regex = cmdLine.getValue(ARG_REPLACE_REGEX);
		String replacement = cmdLine.getValue(ARG_REPLACE_WITH);

		if (StringUtil.isNonBlank(regex) && replacement != null)
		{
			try
			{
				RegexReplacingModifier modifier = new RegexReplacingModifier(regex, replacement);
				return modifier;
			}
			catch (PatternSyntaxException ex)
			{
				LogMgr.logError("RegexModifierParameter.parseParameterValue()", "Could not create modifier", ex);
			}
		}
		return null;
	}

}
