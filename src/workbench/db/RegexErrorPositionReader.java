/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
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

package workbench.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexErrorPositionReader
	implements ErrorPositionReader
{
	private final Pattern lineInfoPattern;
	private final Pattern columnInfoPattern;
	private final Pattern noNumbers = Pattern.compile("[^0-9]");

	public RegexErrorPositionReader(String lineRegex, String columnRegex)
		throws PatternSyntaxException
	{
		lineInfoPattern = Pattern.compile(lineRegex);
		columnInfoPattern = Pattern.compile(columnRegex);
	}

	@Override
	public int getErrorPosition(WbConnection con, String sql, Exception ex)
	{
		if (ex == null) return -1;
		String msg = ex.getMessage();

		int line = -1;
		int column = -1;
		Matcher lm = lineInfoPattern.matcher(msg);
		if (lm.find())
		{
			String lineInfo = noNumbers.matcher(msg.substring(lm.start(), lm.end())).replaceAll("");
			line = StringUtil.getIntValue(lineInfo, -1) - 1;
		}

		if (line == -1)
		{
			return -1;
		}

		Matcher cm = columnInfoPattern.matcher(msg);
		if (cm.find())
		{
			String colInfo = noNumbers.matcher(msg.substring(cm.start(), cm.end())).replaceAll("");
			column = StringUtil.getIntValue(colInfo, -1) - 1;
		}

		if (column == -1)
		{
			return -1;
		}

		int offset = StringUtil.getLineStartOffset(sql, line);
		if (offset < 0)
		{
			return -1;
		}
		return offset + column;
	}

	@Override
	public String enhanceErrorMessage(String sql, String originalMessage, int errorPosition)
	{
		String indicator = SqlUtil.getErrorIndicator(sql, errorPosition);
		if (indicator != null)
		{
			originalMessage += "\n\n" + indicator;
		}
		return originalMessage;
	}

}
