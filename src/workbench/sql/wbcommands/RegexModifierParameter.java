/*
 * RegexModifierParameter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
