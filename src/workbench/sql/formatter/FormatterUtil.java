/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.sql.formatter;

import workbench.resource.GeneratedIdentifierCase;
import workbench.resource.Settings;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FormatterUtil
{

	public static String getIdentifier(String input)
	{
		if (SqlUtil.isQuotedIdentifier(input)) return input;
		return adjustCase(input, Settings.getInstance().getFormatterIdentifierCase());
	}

	public static String getFunction(String input)
	{
		return adjustCase(input, Settings.getInstance().getFormatterIdentifierCase());
	}

	public static String getKeyword(String input)
	{
		return adjustCase(input, Settings.getInstance().getFormatterKeywordsCase());
	}

	private static String adjustCase(String input, GeneratedIdentifierCase keywordCase)
	{
		if (input == null) return null;
		switch (keywordCase)
		{
			case asIs:
				return input;
			case lower:
				return input.toLowerCase();
			case upper:
				return input.toUpperCase();
		}
		return input;
	}

}
