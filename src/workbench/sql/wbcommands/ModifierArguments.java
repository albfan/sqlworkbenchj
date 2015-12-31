/*
 * ModifierArguments.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import workbench.db.ColumnIdentifier;
import workbench.db.importer.modifier.ImportValueModifier;
import workbench.db.importer.modifier.RegexModifier;
import workbench.db.importer.modifier.SubstringModifier;
import workbench.db.importer.modifier.ValueFilter;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;

/**
 * A class to evaluate arguments for import-modifiers.
 *
 * @author Thomas Kellerer
 * @see workbench.interfaces.ImportFileParser#setValueModifier(workbench.db.importer.modifier.ImportValueModifier)
 * @see workbench.db.importer.TextFileParser#setValueModifier(workbench.db.importer.modifier.ImportValueModifier)
 */
public class ModifierArguments
{
	public static final String ARG_SUBSTRING = "colSubstring";
	public static final String ARG_REGEX = "colReplacement";
	public static final String ARG_MAXLENGTH = "maxLength";

	private SubstringModifier substring = new SubstringModifier();
	private RegexModifier regex = new RegexModifier();

	public static void addArguments(ArgumentParser cmdLine)
	{
		cmdLine.addArgument(ARG_REGEX);
		cmdLine.addArgument(ARG_SUBSTRING, ArgumentType.Repeatable);
		cmdLine.addArgument(ARG_MAXLENGTH);
	}

	public ModifierArguments(ArgumentParser cmdLine)
		throws NumberFormatException
	{
		String value = cmdLine.getValue(ARG_MAXLENGTH);
		parseSubstring(value);
		value = cmdLine.getValue(ARG_SUBSTRING);
		parseSubstring(value);
		value = cmdLine.getValue(ARG_REGEX);
		parseRegex(value);
	}

	private void parseRegex(String parameterValue)
	{
		if (parameterValue == null) return;

		List<String> entries = StringUtil.stringToList(parameterValue, ",", true, true, false);
		if (entries.isEmpty()) return;

		for (String entry : entries)
		{
			String[] parts = entry.split("=");
			if (parts.length == 2 && parts[0] != null && parts[1] != null)
			{
				ColumnIdentifier col = new ColumnIdentifier(parts[0]);
				String[] limits = parts[1].split(":");
				String expression = null;
				String replacement = "";

				if (limits.length == 1)
				{
					expression = limits[0].trim();
				}
				else if (limits.length == 2)
				{
					expression = limits[0].trim();
					replacement = limits[1].trim();
				}
				regex.addDefinition(col, expression, replacement);
			}
		}
	}

	/**
	 * Parses a parameter value for column substring definitions.
	 * e.g. description=0:5,firstname=5:10
	 * the two values define the parameters for String.substring(int, int)
	 *
	 * If only one value is supplied, it is assumed to be a maxlength,
	 * so description=5 is equivalent to description=0:5
	 */
	private void parseSubstring(String parameterValue)
		throws NumberFormatException
	{
		if (parameterValue == null) return;

		List<String> entries = StringUtil.stringToList(parameterValue, ",", true, true, false);
		if (entries.isEmpty()) return;

		for (String entry : entries)
		{
			String[] parts = entry.split("=");
			if (parts.length == 2 && parts[0] != null && parts[1] != null)
			{
				ColumnIdentifier col = new ColumnIdentifier(parts[0]);
				String[] limits = parts[1].split(":");
				int start = 0;
				int end = 0;

				if (limits.length == 1)
				{
					end = Integer.parseInt(limits[0].trim());
				}
				else if (limits.length == 2)
				{
					start = Integer.parseInt(limits[0].trim());
					end = Integer.parseInt(limits[1].trim());
				}
				substring.addDefinition(col, start, end);
			}
		}
	}

	/**
	 * For testing purposes to access the initialized modifier
	 */
	SubstringModifier getSubstringModifier()
	{
		return substring;
	}

	/**
	 * For testing purposes to access the initialized modifier
	 */
	RegexModifier getRegexModifier()
	{
		return regex;
	}

	/**
	 * Returns a combined modifier with substring and regex modifications.
	 * the substring modifier will be applied before the regex modifier
	 * during import.
	 *
	 * @return an ImportValueModifier that applies substring and regex modifications.
	 */
	public ImportValueModifier getModifier()
	{
		ValueFilter filter = new ValueFilter();
		if (substring.getSize() > 0) filter.addColumnModifier(substring);
		if (regex.getSize() > 0) filter.addColumnModifier(regex);
		if (filter.getSize() > 0)
		{
			return filter;
		}
		return null;
	}

}
